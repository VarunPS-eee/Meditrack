package com.airtribe.meditrack.entity;

import java.util.Arrays;
import java.util.Optional;

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
