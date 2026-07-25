package com.airtribe.meditrack.util;

import com.airtribe.meditrack.constants.Constants;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class IdGenerator {

    private static final class Holder {
        private static final IdGenerator INSTANCE = new IdGenerator();

        private Holder() {
        }
    }

    /** One atomic counter per id prefix; the map itself is concurrent for safe insertion. */
    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    private IdGenerator() {
        counters.put(Constants.PATIENT_ID_PREFIX, new AtomicInteger(0));
        counters.put(Constants.DOCTOR_ID_PREFIX, new AtomicInteger(0));
        counters.put(Constants.APPOINTMENT_ID_PREFIX, new AtomicInteger(0));
        counters.put(Constants.BILL_ID_PREFIX, new AtomicInteger(0));
        System.out.println("[IdGenerator] Lazy singleton constructed on first use.");
    }

    /**
     * @return the one and only generator, constructed on first call
     */
    public static IdGenerator getInstance() {
        return Holder.INSTANCE;
    }

    public String nextId(String prefix) {
        AtomicInteger counter = counters.computeIfAbsent(prefix, k -> new AtomicInteger(0));
        int value = counter.incrementAndGet();
        return prefix + Constants.ID_SEPARATOR
                + String.format("%0" + Constants.ID_PADDING_WIDTH + "d", value);
    }

    public String nextPatientId() {
        return nextId(Constants.PATIENT_ID_PREFIX);
    }

    public String nextDoctorId() {
        return nextId(Constants.DOCTOR_ID_PREFIX);
    }

    public String nextAppointmentId() {
        return nextId(Constants.APPOINTMENT_ID_PREFIX);
    }

    public String nextBillId() {
        return nextId(Constants.BILL_ID_PREFIX);
    }

    public void syncCounter(String prefix, int value) {
        counters.computeIfAbsent(prefix, k -> new AtomicInteger(0))
                .accumulateAndGet(value, Math::max);
    }

    public void syncFromId(String id) {
        if (id == null || !id.contains(Constants.ID_SEPARATOR)) {
            return;
        }
        String[] parts = id.split(Constants.ID_SEPARATOR, 2);
        try {
            syncCounter(parts[0], Integer.parseInt(parts[1]));
        } catch (NumberFormatException e) {
            // A malformed id in a data file must not stop the import; skip it.
        }
    }

    public int getCurrentValue(String prefix) {
        AtomicInteger counter = counters.get(prefix);
        return counter == null ? 0 : counter.get();
    }

    /** Resets every counter — used by {@code TestRunner} to isolate test runs. */
    public void resetAll() {
        counters.values().forEach(c -> c.set(0));
    }

    /** Prints the current value of every counter. */
    public void printCounters() {
        StringBuilder sb = new StringBuilder(160);
        sb.append("  Id counters: ");
        counters.forEach((prefix, counter) ->
                sb.append(prefix).append('=').append(counter.get()).append("  "));
        System.out.println(sb);
    }
}
