package com.airtribe.meditrack.observer;

import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.interfaces.AppointmentObserver;
import com.airtribe.meditrack.util.DateUtil;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AuditLogObserver implements AppointmentObserver {

    /** One immutable line of the audit trail. */
    public record AuditEntry(LocalDateTime at, String appointmentId, String eventType, String detail) {

        @Override
        public String toString() {
            return String.format("%s  %-12s %-10s %s",
                    DateUtil.format(at), appointmentId, eventType, detail);
        }
    }

    private final List<AuditEntry> entries = new ArrayList<>();

    /** Silent by default — the trail is inspected on demand, not streamed to the console. */
    private final boolean echoToConsole;

    public AuditLogObserver() {
        this(false);
    }

    public AuditLogObserver(boolean echoToConsole) {
        this.echoToConsole = echoToConsole;
    }

    @Override
    public void update(Appointment appointment, String eventType) {
        if (appointment == null) {
            return;
        }
        String detail = String.format("patient=%s doctor=%s slot=%s status=%s",
                appointment.getPatient() == null ? "?" : appointment.getPatient().getId(),
                appointment.getDoctor() == null ? "?" : appointment.getDoctor().getId(),
                DateUtil.format(appointment.getSlot()),
                appointment.getStatus().name());

        AuditEntry entry = new AuditEntry(LocalDateTime.now(), appointment.getId(), eventType, detail);
        entries.add(entry);

        if (echoToConsole) {
            System.out.println("    [AUDIT] " + entry);
        }
    }

    /** @return an unmodifiable view of the trail */
    public List<AuditEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public int size() {
        return entries.size();
    }

    /** Prints the whole audit trail. */
    public void printTrail() {
        if (entries.isEmpty()) {
            System.out.println("  No audit entries recorded yet.");
            return;
        }
        StringBuilder sb = new StringBuilder(entries.size() * 90);
        sb.append("\n  Audit trail (").append(entries.size()).append(" entries)\n")
                .append("  ").append("-".repeat(76)).append('\n');
        for (AuditEntry entry : entries) {
            sb.append("  ").append(entry).append('\n');
        }
        System.out.print(sb);
    }

    @Override
    public String getObserverName() {
        return "Audit";
    }
}
