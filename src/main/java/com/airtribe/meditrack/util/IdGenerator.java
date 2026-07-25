package com.airtribe.meditrack.util;

import com.airtribe.meditrack.constants.Constants;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sequential id generator — the <b>lazy</b> Singleton, via the initialisation-on-demand
 * holder idiom.
 *
 * <h2>Why the holder idiom rather than {@code synchronized}</h2>
 *
 * <p>The naive lazy singleton is broken under concurrency:</p>
 * <pre>{@code
 * if (instance == null) {          // thread A and B can both see null here
 *     instance = new IdGenerator(); // ...and both construct one
 * }
 * }</pre>
 *
 * <p>Marking {@code getInstance()} {@code synchronized} fixes correctness but pays for a
 * lock on <em>every</em> call, forever, to guard a race that can only happen once.
 * Double-checked locking works only with a {@code volatile} field and is easy to get
 * subtly wrong.</p>
 *
 * <p>The {@link Holder} class below sidesteps all of it. A nested class is not initialised
 * until it is first referenced, so {@code Holder.INSTANCE} is created on the first call to
 * {@link #getInstance()} and not before. The JVM's class-initialisation lock guarantees
 * that happens exactly once. Lazy <em>and</em> thread-safe, with no synchronisation in
 * our code.</p>
 *
 * <h2>Counters</h2>
 * <p>Each entity prefix gets its own {@link AtomicInteger}. {@code incrementAndGet()} is a
 * single atomic CPU instruction — unlike {@code count++}, which is a read-modify-write
 * triple that can interleave and hand two entities the same id.</p>
 *
 * @author Sunil (Utils, Storage, Singleton, Docs and Testing)
 */
public final class IdGenerator {

    /**
     * Initialisation-on-demand holder. Loaded by the ClassLoader only when
     * {@link #getInstance()} first touches it.
     */
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

    // ------------------------------------------------------------------ generate
    /**
     * Produces the next id for a prefix, e.g. {@code PAT-0001}.
     *
     * @param prefix the entity prefix
     * @return a unique, zero-padded id
     */
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

    // -------------------------------------------------------------------- state
    /**
     * Fast-forwards a counter so freshly generated ids do not collide with ids loaded
     * from disk. Called after a {@code --loadData} import.
     *
     * <p>Uses {@code accumulateAndGet} so a concurrent {@code nextId()} cannot lose the
     * update — a plain {@code if (current < value) set(value)} could.</p>
     *
     * @param prefix the counter to advance
     * @param value  the highest id number already in use
     */
    public void syncCounter(String prefix, int value) {
        counters.computeIfAbsent(prefix, k -> new AtomicInteger(0))
                .accumulateAndGet(value, Math::max);
    }

    /**
     * Extracts the numeric part of an id and syncs the matching counter.
     *
     * @param id an existing id such as {@code PAT-0007}
     */
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

    /**
     * @param prefix the counter to read
     * @return how many ids have been issued for that prefix
     */
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
