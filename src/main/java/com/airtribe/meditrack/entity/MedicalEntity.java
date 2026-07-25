package com.airtribe.meditrack.entity;

import com.airtribe.meditrack.interfaces.Searchable;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class MedicalEntity implements Searchable, Serializable, Comparable<MedicalEntity> {

    private static final long serialVersionUID = 1L;

    /** Shared across every instance — one counter for the whole JVM. */
    private static final AtomicInteger totalEntitiesCreated = new AtomicInteger(0);

    /** Stamped once, when the class is initialised. */
    private static final LocalDateTime CLASS_LOADED_AT;

    static {
        CLASS_LOADED_AT = LocalDateTime.now();
        System.out.println("[MedicalEntity] Class loaded and initialised at " + CLASS_LOADED_AT);
    }

    /** Immutable business key; assigned at construction and never reassigned. */
    private final String id;

    /** When this record entered the system. {@code LocalDateTime} is immutable, so no defensive copy is needed. */
    private final LocalDateTime createdAt;

    /** Last mutation timestamp; the one piece of audit state that does change. */
    private LocalDateTime updatedAt;

    /**
     * @param id the immutable business key, e.g. {@code PAT-0001}
     */
    protected MedicalEntity(String id) {
        this.id = id;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        totalEntitiesCreated.incrementAndGet();
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /** Subclasses call this from their setters so audit state stays honest. */
    protected void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    /** @return how many entities have been created since the JVM started */
    public static int getTotalEntitiesCreated() {
        return totalEntitiesCreated.get();
    }

    /** @return the moment this class was initialised by the ClassLoader */
    public static LocalDateTime getClassLoadedAt() {
        return CLASS_LOADED_AT;
    }

    public abstract void displayDetails();

    public abstract String getEntityType();

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MedicalEntity other = (MedicalEntity) o;
        return Objects.equals(id, other.id);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(id);
    }

    /** Natural ordering is by id, which makes {@code TreeMap}/{@code sort} deterministic. */
    @Override
    public int compareTo(MedicalEntity other) {
        return String.CASE_INSENSITIVE_ORDER.compare(this.id, other.id);
    }

    @Override
    public String toString() {
        return getEntityType() + "{id='" + id + "'}";
    }
}
