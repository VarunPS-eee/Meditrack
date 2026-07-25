package com.airtribe.meditrack.exception;

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
