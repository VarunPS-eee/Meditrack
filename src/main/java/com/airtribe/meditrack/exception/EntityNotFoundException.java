package com.airtribe.meditrack.exception;

/**
 * Thrown when a generic {@code DataStore<T>} lookup finds no entity for an id.
 *
 * <p>Used by the storage layer, which does not know whether it is holding patients,
 * doctors or bills — so it reports the entity type as a string rather than throwing
 * a type-specific exception.</p>
 *
 * @author Sunil (Utils, Storage, Singleton, Docs and Testing)
 */
public class EntityNotFoundException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String entityType;
    private final String entityId;

    public EntityNotFoundException(String entityType, String entityId) {
        super(String.format("%s not found with id: %s", entityType, entityId));
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public EntityNotFoundException(String entityType, String entityId, Throwable cause) {
        super(String.format("%s not found with id: %s", entityType, entityId), cause);
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }
}
