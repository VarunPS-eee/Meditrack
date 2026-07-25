package com.airtribe.meditrack.exception;

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
