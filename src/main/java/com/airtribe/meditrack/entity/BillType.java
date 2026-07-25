package com.airtribe.meditrack.entity;

import java.util.Arrays;
import java.util.Optional;

/**
 * The kinds of bill {@code BillFactory} knows how to build.
 *
 * <p>This enum is the factory's <em>discriminator</em>: callers name a
 * {@code BillType} and the factory returns the matching {@link Bill} subclass,
 * so no caller ever writes {@code new EmergencyBill(...)} directly.</p>
 *
 * @author Varun (Core Entities, OOP and Factory)
 */
public enum BillType {

    /** Routine outpatient consultation. */
    CONSULTATION("Consultation"),

    /** Consultation plus a clinical procedure. */
    PROCEDURE("Procedure"),

    /** Out-of-hours or emergency attendance; attracts a surcharge. */
    EMERGENCY("Emergency");

    private final String displayName;

    BillType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Optional<BillType> fromString(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalised = value.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(t -> t.name().equals(normalised) || t.displayName.equalsIgnoreCase(value.trim()))
                .findFirst();
    }

    @Override
    public String toString() {
        return displayName;
    }
}
