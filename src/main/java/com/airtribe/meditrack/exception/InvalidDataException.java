package com.airtribe.meditrack.exception;

/**
 * Thrown when input fails validation before it can reach the domain model.
 *
 * <p>A <b>checked</b> exception: bad user input is an expected, recoverable
 * condition that every caller should be forced to handle — the console UI catches
 * it, prints the message and re-prompts, rather than crashing.</p>
 *
 * <p>Carries an optional {@code fieldName} so the UI can point at exactly which
 * input to correct, and supports <b>exception chaining</b> via the {@code cause}
 * constructors so a low-level {@link NumberFormatException} is not lost.</p>
 *
 * @author Zubair (Services, Logic, Observer and AI)
 */
public class InvalidDataException extends Exception {

    private static final long serialVersionUID = 1L;

    /** The field that failed validation, or {@code null} if not field-specific. */
    private final String fieldName;

    /** The value that was rejected, retained for diagnostics. */
    private final Object rejectedValue;

    public InvalidDataException(String message) {
        super(message);
        this.fieldName = null;
        this.rejectedValue = null;
    }

    public InvalidDataException(String message, Throwable cause) {
        super(message, cause);
        this.fieldName = null;
        this.rejectedValue = null;
    }

    public InvalidDataException(String fieldName, Object rejectedValue, String message) {
        super(String.format("Invalid value for '%s' [%s] — %s", fieldName, rejectedValue, message));
        this.fieldName = fieldName;
        this.rejectedValue = rejectedValue;
    }

    /**
     * Full form with chaining — preserves the original failure as the cause.
     *
     * @param fieldName     the offending field
     * @param rejectedValue the value that was rejected
     * @param message       what was wrong with it
     * @param cause         the underlying exception, if any
     */
    public InvalidDataException(String fieldName, Object rejectedValue, String message, Throwable cause) {
        super(String.format("Invalid value for '%s' [%s] — %s", fieldName, rejectedValue, message), cause);
        this.fieldName = fieldName;
        this.rejectedValue = rejectedValue;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getRejectedValue() {
        return rejectedValue;
    }
}
