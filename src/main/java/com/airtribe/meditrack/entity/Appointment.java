package com.airtribe.meditrack.entity;

import com.airtribe.meditrack.util.DateUtil;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A booked consultation slot linking one {@link Patient} to one {@link Doctor}.
 *
 * <p>Also {@link Cloneable}, and a more interesting deep-copy case than {@link Patient}:
 * it holds references to <em>other entities</em>. The clone deep-copies the patient
 * (whose history is per-appointment context) but deliberately <b>shares</b> the doctor
 * reference — a doctor is a single clinic-wide entity, and duplicating them on every
 * appointment copy would be wrong, not merely wasteful. Deep copy is a judgement call
 * about ownership, not a blanket rule.</p>
 *
 * @author Varun (Core Entities, OOP and Factory)
 */
public class Appointment extends MedicalEntity implements Cloneable {

    private static final long serialVersionUID = 1L;

    /** Soonest appointment first. */
    public static final Comparator<Appointment> BY_SLOT =
            Comparator.comparing(Appointment::getSlot);

    /** Group by status, then by slot within each status. */
    public static final Comparator<Appointment> BY_STATUS_THEN_SLOT =
            Comparator.comparing((Appointment a) -> a.getStatus().name()).thenComparing(BY_SLOT);

    private Patient patient;
    private Doctor doctor;
    private LocalDateTime slot;
    private AppointmentStatus status;
    private List<String> symptoms;
    private String notes;

    /**
     * @param appointmentId business key
     * @param patient       the patient attending
     * @param doctor        the doctor consulting
     * @param slot          the scheduled date and time
     */
    public Appointment(String appointmentId, Patient patient, Doctor doctor, LocalDateTime slot) {
        this(appointmentId, patient, doctor, slot, AppointmentStatus.PENDING, new ArrayList<>(), null);
    }

    /**
     * Canonical constructor.
     *
     * @param appointmentId business key
     * @param patient       the patient attending
     * @param doctor        the doctor consulting
     * @param slot          the scheduled date and time
     * @param status        initial status
     * @param symptoms      reported symptoms
     * @param notes         free-text clinical notes
     */
    public Appointment(String appointmentId, Patient patient, Doctor doctor, LocalDateTime slot,
                       AppointmentStatus status, List<String> symptoms, String notes) {
        super(appointmentId);
        this.patient = patient;
        this.doctor = doctor;
        this.slot = slot;
        this.status = status == null ? AppointmentStatus.PENDING : status;
        this.symptoms = symptoms == null ? new ArrayList<>() : new ArrayList<>(symptoms);
        this.notes = notes;
    }

    // --------------------------------------------------------------- accessors
    /** Alias for {@link #getId()}, kept because callers read better with the domain name. */
    public String getAppointmentId() {
        return getId();
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
        touch();
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
        touch();
    }

    public LocalDateTime getSlot() {
        return slot;
    }

    public void setSlot(LocalDateTime slot) {
        this.slot = slot;
        touch();
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public List<String> getSymptoms() {
        return Collections.unmodifiableList(symptoms);
    }

    public void setSymptoms(List<String> symptoms) {
        this.symptoms = symptoms == null ? new ArrayList<>() : new ArrayList<>(symptoms);
        touch();
    }

    public void addSymptom(String symptom) {
        if (symptom != null && !symptom.isBlank()) {
            this.symptoms.add(symptom.trim());
            touch();
        }
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
        touch();
    }

    // ------------------------------------------------------------ state machine
    /**
     * Moves this appointment to a new status, but only if the enum's state machine
     * permits it. Invalid moves are rejected rather than silently applied.
     *
     * @param target the desired status
     * @return {@code true} if the transition happened
     */
    public boolean transitionTo(AppointmentStatus target) {
        if (!status.canTransitionTo(target)) {
            return false;
        }
        this.status = target;
        touch();
        return true;
    }

    public boolean confirm() {
        return transitionTo(AppointmentStatus.CONFIRMED);
    }

    public boolean cancel() {
        return transitionTo(AppointmentStatus.CANCELLED);
    }

    public boolean complete() {
        return transitionTo(AppointmentStatus.COMPLETED);
    }

    public boolean markNoShow() {
        return transitionTo(AppointmentStatus.NO_SHOW);
    }

    /** @return {@code true} if this appointment's slot is in the future */
    public boolean isUpcoming() {
        return slot != null && slot.isAfter(LocalDateTime.now()) && !status.isTerminal();
    }

    /** @return the base charge this appointment will bill at */
    public double getConsultationFee() {
        return doctor == null ? 0.0 : doctor.getConsultationFee();
    }

    // ------------------------------------------------------------ copy semantics
    /**
     * <b>Deep copy, selectively applied.</b>
     *
     * <ul>
     *   <li>{@code patient} — deep copied; each appointment owns its snapshot.</li>
     *   <li>{@code doctor} — <em>shared by reference</em>; the clinic has one of them.</li>
     *   <li>{@code symptoms} — deep copied; a mutable list we own.</li>
     *   <li>{@code slot}, {@code status}, {@code notes} — immutable, so sharing is safe.</li>
     * </ul>
     *
     * @return a copy safe to mutate without disturbing this appointment
     * @throws CloneNotSupportedException never, in practice
     */
    @Override
    public Appointment clone() throws CloneNotSupportedException {
        Appointment copy = (Appointment) super.clone();
        if (this.patient != null) {
            copy.patient = this.patient.clone();
        }
        copy.symptoms = new ArrayList<>(this.symptoms);
        // doctor intentionally shared — see the class note above.
        return copy;
    }

    // --------------------------------------------------------------- behaviour
    @Override
    public String getEntityType() {
        return "Appointment";
    }

    @Override
    public String getSearchableText() {
        return String.join(" ",
                getId(),
                patient == null ? "" : patient.getName(),
                doctor == null ? "" : doctor.getName(),
                status.name(),
                String.join(" ", symptoms),
                notes == null ? "" : notes);
    }

    @Override
    public void displayDetails() {
        StringBuilder sb = new StringBuilder(200);
        sb.append("  [").append(getId()).append("] ")
                .append(DateUtil.formatForDisplay(slot))
                .append("  |  Patient: ").append(patient == null ? "?" : patient.getName())
                .append("  |  Doctor: Dr. ").append(doctor == null ? "?" : doctor.getName())
                .append("  |  Status: ").append(status.getDisplayName());
        if (!symptoms.isEmpty()) {
            sb.append("\n        Symptoms: ").append(String.join(", ", symptoms));
        }
        if (notes != null && !notes.isBlank()) {
            sb.append("\n        Notes   : ").append(notes);
        }
        System.out.println(sb);
    }

    @Override
    public String toString() {
        return "Appointment{id='" + getId() + "', slot=" + slot + ", status=" + status + '}';
    }
}
