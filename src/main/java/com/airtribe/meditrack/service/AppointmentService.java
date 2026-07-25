// Contributed by Zubair: Core validation exception framework for inputs
package com.airtribe.meditrack.service;

import com.airtribe.meditrack.constants.Constants;
import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.entity.AppointmentStatus;
import com.airtribe.meditrack.entity.Doctor;
import com.airtribe.meditrack.entity.Patient;
import com.airtribe.meditrack.exception.AppointmentNotFoundException;
import com.airtribe.meditrack.exception.DataPersistenceException;
import com.airtribe.meditrack.exception.InvalidDataException;
import com.airtribe.meditrack.exception.SlotUnavailableException;
import com.airtribe.meditrack.util.CSVUtil;
import com.airtribe.meditrack.util.DataStore;
import com.airtribe.meditrack.util.DateUtil;
import com.airtribe.meditrack.util.IdGenerator;
import com.airtribe.meditrack.util.Validator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Booking, cancelling, rescheduling and reporting on appointments.
 *
 * <p>This is the busiest service, and the one that ties the others together: it validates
 * through {@link Validator}, enforces clinic rules itself, stores through
 * {@link DataStore} and announces every change through {@link NotificationService}.</p>
 *
 * <p><b>Why the notification service is injected.</b> The booking logic does not know or
 * care that SMS and audit observers exist. Were it to call them directly, adding a channel
 * would mean editing this class — the exact coupling the Observer pattern removes.</p>
 *
 * @author Zubair (Services, Logic, Observer and AI)
 */
public class AppointmentService {

    private final DataStore<Appointment> store;
    private final IdGenerator idGenerator;
    private final NotificationService notificationService;

    public AppointmentService() {
        this(new DataStore<>("Appointment"), new NotificationService());
    }

    /**
     * @param store               the backing store
     * @param notificationService the event dispatcher
     */
    public AppointmentService(DataStore<Appointment> store, NotificationService notificationService) {
        this.store = store;
        this.notificationService = notificationService;
        this.idGenerator = IdGenerator.getInstance();
    }

    // --------------------------------------------------------------------- book
    /**
     * Books an appointment after checking every clinic rule.
     *
     * @param patient  the patient attending
     * @param doctor   the doctor consulting
     * @param slot     the requested date and time
     * @param symptoms reported symptoms, may be empty
     * @return the booked appointment
     * @throws InvalidDataException     if the patient, doctor or slot is invalid
     * @throws SlotUnavailableException if the doctor cannot take the slot
     */
    public Appointment bookAppointment(Patient patient, Doctor doctor, LocalDateTime slot,
                                       List<String> symptoms)
            throws InvalidDataException, SlotUnavailableException {

        Validator.requireNonNull(patient, "patient");
        Validator.requireNonNull(doctor, "doctor");
        Validator.validateAppointmentSlot(slot);

        if (!doctor.isAvailable()) {
            throw new SlotUnavailableException(doctor.getId(), slot, "doctor is not accepting appointments");
        }
        if (isDoctorBooked(doctor.getId(), slot)) {
            throw new SlotUnavailableException(doctor.getId(), slot, "doctor already has an appointment then");
        }
        if (countForDoctorOnDate(doctor.getId(), slot.toLocalDate())
                >= Constants.MAX_APPOINTMENTS_PER_DOCTOR_PER_DAY) {
            throw new SlotUnavailableException(doctor.getId(), slot,
                    "doctor has reached the daily limit of "
                            + Constants.MAX_APPOINTMENTS_PER_DOCTOR_PER_DAY);
        }

        Appointment appointment = new Appointment(
                idGenerator.nextAppointmentId(), patient, doctor,
                DateUtil.alignToSlot(slot), AppointmentStatus.PENDING,
                symptoms == null ? List.of() : symptoms, null);

        store.save(appointment);
        notificationService.notifyObservers(appointment, NotificationService.EVENT_BOOKED);
        return appointment;
    }

    /** Convenience overload with no symptoms recorded. */
    public Appointment bookAppointment(Patient patient, Doctor doctor, LocalDateTime slot)
            throws InvalidDataException, SlotUnavailableException {
        return bookAppointment(patient, doctor, slot, List.of());
    }

    /** Registers a pre-built appointment, syncing the id counter — used when importing. */
    public Appointment register(Appointment appointment) {
        if (appointment != null) {
            idGenerator.syncFromId(appointment.getId());
        }
        return store.save(appointment);
    }

    // ------------------------------------------------------------------ lifecycle
    /**
     * Confirms a pending appointment.
     *
     * @param appointmentId the appointment to confirm
     * @return the confirmed appointment
     * @throws AppointmentNotFoundException if the id is unknown
     * @throws InvalidDataException         if the current status forbids confirming
     */
    public Appointment confirmAppointment(String appointmentId)
            throws AppointmentNotFoundException, InvalidDataException {
        Appointment appointment = requireAppointment(appointmentId);
        if (!appointment.confirm()) {
            throw new InvalidDataException("status", appointment.getStatus(),
                    "cannot confirm an appointment that is " + appointment.getStatus());
        }
        return appointment;
    }

    /**
     * Cancels an appointment and notifies every channel.
     *
     * @param appointmentId the appointment to cancel
     * @return the cancelled appointment
     * @throws AppointmentNotFoundException if the id is unknown
     * @throws InvalidDataException         if the appointment is already in a terminal state
     */
    public Appointment cancelAppointment(String appointmentId)
            throws AppointmentNotFoundException, InvalidDataException {
        Appointment appointment = requireAppointment(appointmentId);
        if (!appointment.cancel()) {
            throw new InvalidDataException("status", appointment.getStatus(),
                    "cannot cancel an appointment that is already " + appointment.getStatus());
        }
        notificationService.notifyObservers(appointment, NotificationService.EVENT_CANCELLED);
        return appointment;
    }

    /**
     * Marks an appointment complete, making it billable.
     *
     * @param appointmentId the appointment to complete
     * @return the completed appointment
     * @throws AppointmentNotFoundException if the id is unknown
     * @throws InvalidDataException         if the current status forbids completing
     */
    public Appointment completeAppointment(String appointmentId)
            throws AppointmentNotFoundException, InvalidDataException {
        Appointment appointment = requireAppointment(appointmentId);
        if (!appointment.complete()) {
            throw new InvalidDataException("status", appointment.getStatus(),
                    "only a confirmed appointment can be completed (this one is "
                            + appointment.getStatus() + ")");
        }
        notificationService.notifyObservers(appointment, NotificationService.EVENT_COMPLETED);
        return appointment;
    }

    /**
     * Moves an appointment to a new slot, re-running availability checks.
     *
     * @param appointmentId the appointment to move
     * @param newSlot       the new date and time
     * @return the rescheduled appointment
     * @throws AppointmentNotFoundException if the id is unknown
     * @throws InvalidDataException         if the new slot is invalid
     * @throws SlotUnavailableException     if the doctor cannot take the new slot
     */
    public Appointment rescheduleAppointment(String appointmentId, LocalDateTime newSlot)
            throws AppointmentNotFoundException, InvalidDataException, SlotUnavailableException {

        Appointment appointment = requireAppointment(appointmentId);
        if (appointment.getStatus().isTerminal()) {
            throw new InvalidDataException("status", appointment.getStatus(),
                    "cannot reschedule an appointment that is " + appointment.getStatus());
        }
        Validator.validateAppointmentSlot(newSlot);

        String doctorId = appointment.getDoctor().getId();
        if (isDoctorBookedExcluding(doctorId, newSlot, appointmentId)) {
            throw new SlotUnavailableException(doctorId, newSlot, "doctor already has an appointment then");
        }

        appointment.setSlot(DateUtil.alignToSlot(newSlot));
        notificationService.notifyObservers(appointment, NotificationService.EVENT_RESCHEDULED);
        return appointment;
    }

    // ---------------------------------------------------------------------- read
    /**
     * @param appointmentId the id to look up
     * @return the appointment
     * @throws AppointmentNotFoundException if the id is unknown
     */
    public Appointment requireAppointment(String appointmentId) throws AppointmentNotFoundException {
        return store.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));
    }

    public Optional<Appointment> findById(String appointmentId) {
        return store.findById(appointmentId);
    }

    public List<Appointment> getAllAppointments() {
        return store.findAllSorted(Appointment.BY_SLOT);
    }

    public int count() {
        return store.count();
    }

    /**
     * @param patientId the patient
     * @return that patient's appointments, soonest first
     */
    public List<Appointment> getAppointmentsForPatient(String patientId) {
        return store.stream()
                .filter(a -> a.getPatient() != null && a.getPatient().getId().equals(patientId))
                .sorted(Appointment.BY_SLOT)
                .collect(Collectors.toList());
    }

    /**
     * @param doctorId the doctor
     * @return that doctor's appointments, soonest first
     */
    public List<Appointment> getAppointmentsForDoctor(String doctorId) {
        return store.stream()
                .filter(a -> a.getDoctor() != null && a.getDoctor().getId().equals(doctorId))
                .sorted(Appointment.BY_SLOT)
                .collect(Collectors.toList());
    }

    /**
     * @param status the status to filter on
     * @return matching appointments
     */
    public List<Appointment> getByStatus(AppointmentStatus status) {
        return store.findBy(a -> a.getStatus() == status);
    }

    /** @return every non-terminal appointment still in the future */
    public List<Appointment> getUpcomingAppointments() {
        return store.stream()
                .filter(Appointment::isUpcoming)
                .sorted(Appointment.BY_SLOT)
                .collect(Collectors.toList());
    }

    /**
     * @param keyword the search term
     * @return appointments matching on patient, doctor, status, symptoms or notes
     */
    public List<Appointment> searchAppointments(String keyword) {
        return store.search(keyword);
    }

    // ------------------------------------------------------------ availability
    /**
     * @param doctorId the doctor
     * @param slot     the slot to test
     * @return {@code true} if the doctor already has a live appointment at that time
     */
    public boolean isDoctorBooked(String doctorId, LocalDateTime slot) {
        return isDoctorBookedExcluding(doctorId, slot, null);
    }

    private boolean isDoctorBookedExcluding(String doctorId, LocalDateTime slot, String excludeId) {
        LocalDateTime aligned = DateUtil.alignToSlot(slot);
        return store.stream()
                .filter(a -> !a.getId().equals(excludeId))
                .filter(a -> !a.getStatus().isTerminal())
                .filter(a -> a.getDoctor() != null && a.getDoctor().getId().equals(doctorId))
                .anyMatch(a -> aligned.equals(DateUtil.alignToSlot(a.getSlot())));
    }

    /**
     * @param doctorId the doctor
     * @param date     the day
     * @return how many live appointments that doctor has on that day
     */
    public long countForDoctorOnDate(String doctorId, LocalDate date) {
        return store.stream()
                .filter(a -> !a.getStatus().isTerminal())
                .filter(a -> a.getDoctor() != null && a.getDoctor().getId().equals(doctorId))
                .filter(a -> a.getSlot() != null && a.getSlot().toLocalDate().equals(date))
                .count();
    }

    /**
     * Every slot a doctor still has open on a given day.
     *
     * @param doctorId the doctor
     * @param date     the day
     * @return the free slots, in order
     */
    public List<LocalDateTime> getAvailableSlots(String doctorId, LocalDate date) {
        List<LocalDateTime> booked = store.stream()
                .filter(a -> !a.getStatus().isTerminal())
                .filter(a -> a.getDoctor() != null && a.getDoctor().getId().equals(doctorId))
                .map(a -> DateUtil.alignToSlot(a.getSlot()))
                .toList();

        return DateUtil.generateSlotsForDay(date).stream()
                .filter(slot -> !booked.contains(slot))
                .filter(DateUtil::isFuture)
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------- streams & analytics
    /**
     * @return doctor name to appointment count, busiest reporting first
     */
    public Map<String, Long> getAppointmentsPerDoctor() {
        return store.stream()
                .filter(a -> a.getDoctor() != null)
                .collect(Collectors.groupingBy(
                        a -> a.getDoctor().getName(),
                        java.util.TreeMap::new,
                        Collectors.counting()));
    }

    /**
     * @return how many appointments sit in each status
     */
    public Map<AppointmentStatus, Long> getStatusBreakdown() {
        return store.stream()
                .collect(Collectors.groupingBy(
                        Appointment::getStatus,
                        () -> new java.util.EnumMap<>(AppointmentStatus.class),
                        Collectors.counting()));
    }

    /**
     * @return the busiest doctor by appointment count, if any
     */
    public Optional<Map.Entry<String, Long>> getBusiestDoctor() {
        return getAppointmentsPerDoctor().entrySet().stream()
                .max(Map.Entry.comparingByValue());
    }

    /**
     * @return the fraction of non-pending appointments that were cancelled, 0.0–1.0
     */
    public double getCancellationRate() {
        long total = store.count();
        if (total == 0) {
            return 0.0;
        }
        long cancelled = store.countBy(a -> a.getStatus() == AppointmentStatus.CANCELLED);
        return (double) cancelled / total;
    }

    /**
     * @return total revenue booked across completed appointments
     */
    public double getTotalConsultationRevenue() {
        return store.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .mapToDouble(Appointment::getConsultationFee)
                .sum();
    }

    // ------------------------------------------------------------- persistence
    public void saveToFile() throws DataPersistenceException {
        CSVUtil.writeAppointments(Constants.APPOINTMENTS_FILE, store.findAll());
    }

    /**
     * Loads appointments and re-links them to the supplied patients and doctors.
     *
     * @param patients patients by id
     * @param doctors  doctors by id
     * @return how many appointments were loaded
     * @throws DataPersistenceException if the CSV cannot be read
     */
    public int loadFromFile(Map<String, Patient> patients, Map<String, Doctor> doctors)
            throws DataPersistenceException {
        List<Appointment> loaded =
                CSVUtil.readAppointments(Constants.APPOINTMENTS_FILE, patients, doctors);
        loaded.forEach(this::register);
        return loaded.size();
    }

    public DataStore<Appointment> getStore() {
        return store;
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }

    /** Prints every appointment, soonest first. */
    public void displayAll() {
        if (store.isEmpty()) {
            System.out.println("  No appointments booked yet.");
            return;
        }
        System.out.println("\n  Appointments (" + store.count() + ")");
        System.out.println("  " + "-".repeat(100));
        store.findAllSorted(Appointment.BY_SLOT).forEach(Appointment::displayDetails);
    }
}
