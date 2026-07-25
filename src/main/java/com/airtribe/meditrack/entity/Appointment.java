package com.airtribe.meditrack.entity;

import com.airtribe.meditrack.util.DateUtil;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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

    public Appointment(String appointmentId, Patient patient, Doctor doctor, LocalDateTime slot) {
        this(appointmentId, patient, doctor, slot, AppointmentStatus.PENDING, new ArrayList<>(), null);
    }

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
