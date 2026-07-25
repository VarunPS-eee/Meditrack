package com.airtribe.meditrack.entity;

import com.airtribe.meditrack.interfaces.Searchable;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Root of every persistable thing in MediTrack.
 *
 * <p><b>Abstraction.</b> This class fixes what all entities share — an immutable id,
 * a creation timestamp, identity semantics — and declares what it cannot know:
 * {@link #displayDetails()} and {@link #getEntityType()} are abstract, forcing each
 * subclass to describe itself. Because {@code MedicalEntity} implements
 * {@link Searchable}, every entity is searchable by construction.</p>
 *
 * <p><b>Static state.</b> {@link #totalEntitiesCreated} is a class-level counter shared
 * across all instances, contrasted with the instance fields below it. It uses
 * {@link AtomicInteger} so the count stays correct when the reminder thread and the
 * main thread create entities concurrently.</p>
 *
 * @author Varun (Core Entities, OOP and Factory)
 */
public abstract class MedicalEntity implements Searchable, Serializable, Comparable<MedicalEntity> {

    private static final long serialVersionUID = 1L;

    // ------------------------------------------------------------------ static scope
    /** Shared across every instance — one counter for the whole JVM. */
    private static final AtomicInteger totalEntitiesCreated = new AtomicInteger(0);

    /** Stamped once, when the class is initialised. */
    private static final LocalDateTime CLASS_LOADED_AT;

    /*
     * Static initialisation block — runs once, at class-initialisation time, before
     * the first instance is ever constructed. Contrast with the instance initialiser
     * effect of the constructor below, which runs once per object.
     */
    static {
        CLASS_LOADED_AT = LocalDateTime.now();
        System.out.println("[MedicalEntity] Class loaded and initialised at " + CLASS_LOADED_AT);
    }

    // ---------------------------------------------------------------- instance scope
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

    // ------------------------------------------------------------------- accessors
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

    // ------------------------------------------------------------------- abstract
    /**
     * Prints a human-readable summary of this entity to the console.
     * The canonical <b>dynamic dispatch</b> demonstration: iterate a
     * {@code List<MedicalEntity>} and each element prints itself its own way.
     */
    public abstract void displayDetails();

    /**
     * @return the entity's type label, e.g. {@code "Patient"} — used by the generic
     *         {@code DataStore<T>} for error messages and CSV headers
     */
    public abstract String getEntityType();

    // ------------------------------------------------------------------- identity
    /**
     * Identity is the business key alone. Two records describing the same patient
     * are the same patient regardless of whether a phone number was edited.
     *
     * <p>Note {@code getClass() != o.getClass()} rather than {@code instanceof}: a
     * {@code Doctor} and a {@code Patient} that somehow shared an id are still not
     * equal, which keeps the relation symmetric across the hierarchy.</p>
     */
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

    /** Consistent with {@link #equals(Object)} — same field, so equal objects hash alike. */
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
