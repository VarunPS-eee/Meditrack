# Phase 12 — Streams, Analytics & Concurrency

| | |
|---|---|
| **Owner** | All three (Varun: entity streams · Zubair: service analytics · Sunil: concurrency) |
| **Depends on** | Phase 8 |
| **Rubric** | **Bonus D — Streams & Lambdas (10 pts)** · Intro to Concurrency |
| **Status** | ✅ Complete |

---

## Objective

Add the analytics layer using streams and lambdas, and the concurrency primitives the rubric
asks for — threads, synchronisation, `AtomicInteger` and `TimerTask`.

---

## Tasks

### Streams — doctors
- [x] `getAverageFeeBySpecialization` — `groupingBy` + `averagingDouble`
- [x] `countBySpecialization` — `groupingBy` + `counting`
- [x] `getFeeStatistics` — `summaryStatistics()` in one pass
- [x] `getTopRatedDoctors` — `sorted` + `limit`
- [x] `getMostExpensiveDoctor` — `max` with a comparator
- [x] `getUnstaffedSpecializations` — set difference via streams
- [x] `searchDoctor(double)` — `filter` + `sorted`

### Streams — patients
- [x] `countByAgeGroup` — `groupingBy`
- [x] `getAverageAge` — `mapToInt` + `average`
- [x] `getSeniorCitizens` / `getInsuredPatients` — method-reference predicates
- [x] `getAllKnownAllergies` — **`flatMap`** + `distinct` + `sorted`
- [x] `asMap` — `toMap`

### Streams — appointments
- [x] `getAppointmentsPerDoctor` — `groupingBy` into a `TreeMap`
- [x] `getStatusBreakdown` — `groupingBy` into an **`EnumMap`**
- [x] `getBusiestDoctor` — `max(Map.Entry.comparingByValue())`
- [x] `getCancellationRate`, `getTotalConsultationRevenue`
- [x] `getAvailableSlots` — filter against booked slots

### Streams — billing
- [x] `getTotalBilled` / `Collected` / `Outstanding` / `TaxCollected` — `mapToDouble` + `sum`
- [x] `getRevenueByBillType` — `groupingBy` + `summingDouble`
- [x] `getAllSummaries` — `map` + `sorted`

### Lambdas & functional interfaces
- [x] `Predicate<T>` in `DataStore.findBy`
- [x] `Supplier<List<Appointment>>` for the reminder scheduler
- [x] `Function<E, String>` in `CSVUtil` helpers
- [x] `BillingStrategy` as a `@FunctionalInterface` — lambdas are valid policies
- [x] Method references throughout (`Patient::isSeniorCitizen`, `Bill::getTotalAmount`)
- [x] `Comparator` chaining — `comparing().thenComparing().reversed()`

### Concurrency
- [x] **`AtomicInteger`** — `IdGenerator` counters
- [x] **`AtomicInteger`** — `MedicalEntity` instance counter
- [x] **`ConcurrentHashMap`** — `IdGenerator` counter map
- [x] **`CopyOnWriteArrayList`** — observer list
- [x] **`synchronized`** block — guards the reminder sweep
- [x] **`Timer` + `TimerTask`** — periodic reminder scheduler
- [x] **Daemon thread** — so the JVM can exit
- [x] `accumulateAndGet(value, Math::max)` for atomic monotonic counter sync
- [x] Load test: 10 threads × 100 ids, asserting 1,000 unique

---

## Streams that earn their place

```java
public Map<Specialization, Double> getAverageFeeBySpecialization() {
    return store.stream()
            .filter(d -> d.getSpecialization() != null)
            .collect(Collectors.groupingBy(
                    Doctor::getSpecialization,
                    TreeMap::new,                                    // deterministic order
                    Collectors.averagingDouble(Doctor::getConsultationFee)));
}
```

Imperatively this is a map of running sums and counts, a loop, and a second pass to divide.
Declaratively it is one expression that says what it computes.

The `TreeMap::new` is deliberate — `groupingBy`'s default `HashMap` would reorder output between
runs, making reports and screenshots inconsistent.

`flatMap` over nested collections:

```java
return store.stream()
        .flatMap(p -> p.getAllergies().stream())   // List<List<String>> -> Stream<String>
        .map(String::trim)
        .filter(a -> !a.isEmpty())
        .distinct()
        .sorted()
        .collect(Collectors.toList());
```

---

## Concurrency — three mechanisms, three reasons

| Mechanism | Where | Why that one |
|---|---|---|
| `AtomicInteger` | Id counters | `count++` is read-modify-write; two threads lose an update |
| `ConcurrentHashMap` | Counter map | Safe concurrent insertion of new prefixes |
| `CopyOnWriteArrayList` | Observers | Rare writes, frequent iteration → lock-free reads |
| `synchronized` | Reminder sweep | Stops a long sweep overlapping the next tick |
| `Timer` (daemon) | Scheduler | Background work that must not block JVM exit |

### The daemon flag

```java
reminderTimer = new Timer("meditrack-reminder", true);   // <- daemon
```

Without `true`, the timer thread keeps the JVM alive indefinitely after the user chooses Exit.
The app would print "Goodbye" and then hang — a genuinely confusing bug.

### Why `CopyOnWriteArrayList` and not `synchronized`

Observers are registered once at startup and iterated on every appointment event, from both the
main thread and the timer thread. Copy-on-write makes reads lock-free and removes any chance of
`ConcurrentModificationException` if an observer registers another mid-dispatch.

### The atomicity proof

```java
// 10 threads × 100 ids each, into a synchronized Set
assertEquals("AtomicInteger issued 1000 unique ids with no collisions",
             1000, ids.size());
```

With a plain `int++` this test fails intermittently — which is exactly what makes it a good test.

---

## Reports in the app

Menu **7** exposes the analytics:

```
  Average consultation fee by speciality
    Cardiology            ₹1,400.00
    Dermatology             ₹850.00
    Neurology             ₹1,600.00
    Orthopedics           ₹1,100.00
    Pediatrics              ₹750.00
    General Practice        ₹500.00
    OVERALL               ₹1,033.33  (min ₹500.00, max ₹1,600.00 across 6 doctors)
```

Also available: appointments per doctor, status breakdown, patient demographics, revenue
summary, audit trail.

---

## Verification

`TestRunner` suites *Streams and lambdas — analytics* and *Concurrency — AtomicInteger and
thread safety*.

Sample assertions:
- *"groupingBy + averagingDouble"*
- *"flatMap + distinct de-duplicated"*
- *"summaryStatistics in one pass"*
- *"empty store averages to zero without throwing"*
- *"AtomicInteger issued 1000 unique ids with no collisions"*
- *"scheduler starts and stops cleanly"*

---

## Exit criteria

- [x] Analytics across all four services
- [x] `groupingBy`, `flatMap`, `summaryStatistics`, `partitioning`-style filters all used
- [x] Deterministic output via `TreeMap` / `EnumMap`
- [x] Empty-collection edge cases return sensible defaults rather than throwing
- [x] 1,000-id concurrency test passes
- [x] Scheduler starts and stops cleanly on a daemon thread

---

**Previous:** [Phase 11](./PHASE_11_AI_FEATURE.md) ·
**Next:** [Phase 13 — Console UI](./PHASE_13_CONSOLE_UI.md) ·
[Phase index](./README.md)
