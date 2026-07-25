package com.airtribe.meditrack.observer;

import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.interfaces.AppointmentObserver;
import com.airtribe.meditrack.util.DateUtil;

import java.util.Set;

public class SmsReminderObserver implements AppointmentObserver {

    /** SMS is reserved for events the patient must act on. */
    private static final Set<String> SUBSCRIBED_EVENTS =
            Set.of("BOOKED", "CANCELLED", "RESCHEDULED", "REMINDER");

    private int messagesSent;

    @Override
    public void update(Appointment appointment, String eventType) {
        if (appointment == null || appointment.getPatient() == null) {
            return;
        }
        messagesSent++;
        System.out.printf("    [SMS -> %s] %s%n",
                appointment.getPatient().getMaskedContactNumber(),
                composeMessage(appointment, eventType));
    }

    private String composeMessage(Appointment appointment, String eventType) {
        StringBuilder sb = new StringBuilder(140);
        sb.append("MediTrack: your appointment ").append(appointment.getId());
        switch (eventType) {
            case "BOOKED" -> sb.append(" is booked for ")
                    .append(DateUtil.formatForDisplay(appointment.getSlot()));
            case "CANCELLED" -> sb.append(" has been cancelled.");
            case "RESCHEDULED" -> sb.append(" moved to ")
                    .append(DateUtil.formatForDisplay(appointment.getSlot()));
            case "REMINDER" -> sb.append(" is ")
                    .append(DateUtil.describeTimeUntil(appointment.getSlot())).append('.');
            default -> sb.append(" was updated: ").append(eventType);
        }
        return sb.toString();
    }

    @Override
    public boolean isInterestedIn(String eventType) {
        return SUBSCRIBED_EVENTS.contains(eventType);
    }

    @Override
    public String getObserverName() {
        return "SMS";
    }

    public int getMessagesSent() {
        return messagesSent;
    }
}
