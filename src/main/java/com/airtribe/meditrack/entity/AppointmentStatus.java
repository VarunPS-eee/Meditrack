package com.airtribe.meditrack.entity;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

public enum AppointmentStatus {

    /** Created but not yet confirmed by the clinic. */
    PENDING("Pending", false),

    /** Confirmed and scheduled. */
    CONFIRMED("Confirmed", false),

    /** Consultation finished; billing may proceed. */
    COMPLETED("Completed", true),

    /** Cancelled by patient or clinic; terminal. */
    CANCELLED("Cancelled", true),

    /** Patient did not attend; terminal. */
    NO_SHOW("No Show", true);

    private final String displayName;

    /** A terminal status admits no further transitions. */
    private final boolean terminal;

    AppointmentStatus(String displayName, boolean terminal) {
        this.displayName = displayName;
        this.terminal = terminal;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isTerminal() {
        return terminal;
    }

    public boolean isBillable() {
        return this == COMPLETED;
    }

    public boolean canTransitionTo(AppointmentStatus target) {
        if (target == null || this == target || this.terminal) {
            return false;
        }
        return switch (this) {
            case PENDING -> Set.of(CONFIRMED, CANCELLED).contains(target);
            case CONFIRMED -> Set.of(COMPLETED, CANCELLED, NO_SHOW).contains(target);
            default -> false;
        };
    }

    public static Optional<AppointmentStatus> fromString(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalised = value.trim().replace(' ', '_').toUpperCase();
        return Arrays.stream(values())
                .filter(s -> s.name().equals(normalised) || s.displayName.equalsIgnoreCase(value.trim()))
                .findFirst();
    }

    @Override
    public String toString() {
        return displayName;
    }
}
