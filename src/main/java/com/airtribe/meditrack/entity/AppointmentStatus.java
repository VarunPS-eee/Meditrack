package com.airtribe.meditrack.entity;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

/**
 * Lifecycle states of an appointment.
 *
 * <p>Beyond replacing magic strings, this enum owns its own <b>state machine</b>:
 * {@link #canTransitionTo(AppointmentStatus)} encodes which moves are legal, so the
 * service layer cannot accidentally revive a cancelled appointment. Keeping the rule
 * inside the enum is the Single Responsibility Principle applied to a value type.</p>
 *
 * @author Varun (Core Entities, OOP and Factory)
 */
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

    /**
     * Whether a bill may be raised while in this state.
     *
     * @return {@code true} only for {@link #COMPLETED}
     */
    public boolean isBillable() {
        return this == COMPLETED;
    }

    /**
     * Legal-move check for the appointment state machine.
     *
     * <pre>
     *   PENDING ──▶ CONFIRMED ──▶ COMPLETED
     *      │             │
     *      └──▶ CANCELLED ◀┘
     *                    └──▶ NO_SHOW
     * </pre>
     *
     * @param target the status being moved to
     * @return {@code true} if the transition is allowed
     */
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

    /**
     * Case-insensitive lookup that also accepts the display name.
     *
     * @param value user input such as {@code "confirmed"} or {@code "No Show"}
     * @return the matching constant, if any
     */
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
