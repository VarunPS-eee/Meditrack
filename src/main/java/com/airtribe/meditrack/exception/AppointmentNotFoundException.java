package com.airtribe.meditrack.exception;

public class AppointmentNotFoundException extends Exception {

    private static final long serialVersionUID = 1L;

    /** The appointment id that could not be resolved. */
    private final String appointmentId;

    public AppointmentNotFoundException(String appointmentId) {
        super("No appointment found with id: " + appointmentId);
        this.appointmentId = appointmentId;
    }

    public AppointmentNotFoundException(String appointmentId, String message) {
        super(message);
        this.appointmentId = appointmentId;
    }

    public AppointmentNotFoundException(String appointmentId, String message, Throwable cause) {
        super(message, cause);
        this.appointmentId = appointmentId;
    }

    public String getAppointmentId() {
        return appointmentId;
    }
}
