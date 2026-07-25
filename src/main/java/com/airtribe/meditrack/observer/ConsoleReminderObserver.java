package com.airtribe.meditrack.observer;

import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.interfaces.AppointmentObserver;
import com.airtribe.meditrack.util.DateUtil;

public class ConsoleReminderObserver implements AppointmentObserver {

    @Override
    public void update(Appointment appointment, String eventType) {
        if (appointment == null) {
            return;
        }
        System.out.printf("    [NOTIFY] %-10s %s — Dr. %s with %s, %s (%s)%n",
                eventType,
                appointment.getId(),
                appointment.getDoctor() == null ? "?" : appointment.getDoctor().getName(),
                appointment.getPatient() == null ? "?" : appointment.getPatient().getName(),
                DateUtil.formatForDisplay(appointment.getSlot()),
                DateUtil.describeTimeUntil(appointment.getSlot()));
    }

    @Override
    public String getObserverName() {
        return "Console";
    }
}
