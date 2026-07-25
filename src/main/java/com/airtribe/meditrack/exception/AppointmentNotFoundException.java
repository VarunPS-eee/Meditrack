package com.airtribe.meditrack.exception;

/**
 * Thrown when an appointment lookup by id yields nothing.
 *
 * <p>Checked, for the same reason as {@link InvalidDataException}: a mistyped
 * appointment id is routine, and the caller should be made to decide what to do
 * about it.</p>
 *
 * @author Zubair (Services, Logic, Observer and AI)
 */
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

    /**
     * Chaining form — wraps a lower-level failure (e.g. a parse error while
     * reloading appointments from CSV) without discarding it.
     *
     * @param appointmentId the id being looked up
     * @param message       context for the failure
     * @param cause         the underlying exception
     */
    public AppointmentNotFoundException(String appointmentId, String message, Throwable cause) {
        super(message, cause);
        this.appointmentId = appointmentId;
    }

    public String getAppointmentId() {
        return appointmentId;
    }
}
