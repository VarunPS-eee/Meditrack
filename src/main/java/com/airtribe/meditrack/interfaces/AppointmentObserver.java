package com.airtribe.meditrack.interfaces;

import com.airtribe.meditrack.entity.Appointment;

public interface AppointmentObserver {

    void update(Appointment appointment, String eventType);

    /**
     * @return the channel name used in console output, e.g. {@code SMS}
     */
    default String getObserverName() {
        return getClass().getSimpleName();
    }

    default boolean isInterestedIn(String eventType) {
        return true;
    }
}
