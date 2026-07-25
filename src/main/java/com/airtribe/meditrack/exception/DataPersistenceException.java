package com.airtribe.meditrack.exception;

/**
 * Wraps any failure in the file-I/O / serialization layer.
 *
 * <p>This is the clearest <b>exception chaining</b> example in the project: the util
 * layer catches a low-level {@link java.io.IOException} or
 * {@link ClassNotFoundException} and rethrows it wrapped in a domain-meaningful
 * exception. Callers get "could not save patients" while
 * {@link Throwable#getCause()} still holds "disk full" for debugging.</p>
 *
 * @author Sunil (Utils, Storage, Singleton, Docs and Testing)
 */
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
