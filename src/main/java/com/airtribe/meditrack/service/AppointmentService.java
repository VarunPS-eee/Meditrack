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

public class AppointmentService {

    private final DataStore<Appointment> store;
    private final IdGenerator idGenerator;
    private final NotificationService notificationService;

    public AppointmentService() {
        this(new DataStore<>("Appointment"), new NotificationService());
    }

    public AppointmentService(DataStore<Appointment> store, NotificationService notificationService) {
        this.store = store;
        this.notificationService = notificationService;
        this.idGenerator = IdGenerator.getInstance();
    }

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

    public Appointment confirmAppointment(String appointmentId)
            throws AppointmentNotFoundException, InvalidDataException {
        Appointment appointment = requireAppointment(appointmentId);
        if (!appointment.confirm()) {
            throw new InvalidDataException("status", appointment.getStatus(),
                    "cannot confirm an appointment that is " + appointment.getStatus());
        }
        return appointment;
    }

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

    public List<Appointment> getAppointmentsForPatient(String patientId) {
        return store.stream()
                .filter(a -> a.getPatient() != null && a.getPatient().getId().equals(patientId))
                .sorted(Appointment.BY_SLOT)
                .collect(Collectors.toList());
    }

    public List<Appointment> getAppointmentsForDoctor(String doctorId) {
        return store.stream()
                .filter(a -> a.getDoctor() != null && a.getDoctor().getId().equals(doctorId))
                .sorted(Appointment.BY_SLOT)
                .collect(Collectors.toList());
    }

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

    public List<Appointment> searchAppointments(String keyword) {
        return store.search(keyword);
    }

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

    public long countForDoctorOnDate(String doctorId, LocalDate date) {
        return store.stream()
                .filter(a -> !a.getStatus().isTerminal())
                .filter(a -> a.getDoctor() != null && a.getDoctor().getId().equals(doctorId))
                .filter(a -> a.getSlot() != null && a.getSlot().toLocalDate().equals(date))
                .count();
    }

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

    public void saveToFile() throws DataPersistenceException {
        CSVUtil.writeAppointments(Constants.APPOINTMENTS_FILE, store.findAll());
    }

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
