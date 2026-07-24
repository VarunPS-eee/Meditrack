package main.java.com.airtribe.meditrack.entity;

import java.util.Date;

public class Appointment implements Cloneable {
    private String appointmentId;
    private Patient patient;
    private Doctor doctor;
    private Date appointmentDate;
    private AppointmentStatus status;

    public Appointment(String appointmentId, Patient patient, Doctor doctor, Date appointmentDate, AppointmentStatus status) {
        this.appointmentId = appointmentId;
        this.patient = patient;
        this.doctor = doctor;
        this.appointmentDate = appointmentDate;
        this.status = status;
    }

    // Getters and Setters
    public String getAppointmentId() { return appointmentId; }
    public Patient getPatient() { return patient; }
    public Doctor getDoctor() { return doctor; }

    public Date getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(Date appointmentDate) { this.appointmentDate = appointmentDate; }

    public AppointmentStatus getStatus() { return status; }
    public void setStatus(AppointmentStatus status) { this.status = status; }

    // Demonstrating Deep Copy
    @Override
    public Object clone() throws CloneNotSupportedException {
        // 1. Perform the shallow copy first
        Appointment clonedAppointment = (Appointment) super.clone();

        // 2. Perform deep copies of the mutable reference types
        // Patient has its own clone() method we built earlier
        if (this.patient != null) {
            clonedAppointment.patient = (Patient) this.patient.clone();
        }

        // Date is a mutable object in Java, so we clone it to prevent external modification
        if (this.appointmentDate != null) {
            clonedAppointment.appointmentDate = (Date) this.appointmentDate.clone();
        }

        return clonedAppointment;
    }
}
