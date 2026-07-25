package com.airtribe.meditrack.interfaces;

import com.airtribe.meditrack.entity.Appointment;

/**
 * Observer Pattern — receives notifications about appointment lifecycle events.
 *
 * <p>Implementations are registered with the notification service and are called
 * whenever an appointment is booked, cancelled, rescheduled or completed. The
 * service never knows what the observers actually do, which is what lets us add
 * SMS, email or audit-log channels without touching appointment logic.</p>
 *
 * @author Zubair (Services, Logic, Observer and AI)
 */
public interface AppointmentObserver {

    /**
     * Called when an appointment event occurs.
     *
     * @param appointment the appointment the event concerns
     * @param eventType   what happened, e.g. {@code BOOKED}, {@code CANCELLED}
     */
    void update(Appointment appointment, String eventType);

    /**
     * @return the channel name used in console output, e.g. {@code SMS}
     */
    default String getObserverName() {
        return getClass().getSimpleName();
    }

    /**
     * Lets an observer opt out of specific event types.
     * Defaults to receiving everything.
     *
     * @param eventType the event about to be dispatched
     * @return {@code true} if this observer wants the event
     */
    default boolean isInterestedIn(String eventType) {
        return true;
    }
}
