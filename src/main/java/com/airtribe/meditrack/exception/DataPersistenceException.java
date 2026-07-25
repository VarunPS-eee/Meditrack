package com.airtribe.meditrack.exception;

public class DataPersistenceException extends Exception {

    private static final long serialVersionUID = 1L;

    /** The file involved in the failed operation, if known. */
    private final String filePath;

    public DataPersistenceException(String message) {
        super(message);
        this.filePath = null;
    }

    public DataPersistenceException(String message, Throwable cause) {
        super(message, cause);
        this.filePath = null;
    }

    public DataPersistenceException(String filePath, String message, Throwable cause) {
        super(String.format("%s (file: %s)", message, filePath), cause);
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }
}
