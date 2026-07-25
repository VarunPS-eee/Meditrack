# Phase 7 — Utilities, Storage & Singletons

| | |
|---|---|
| **Owner** | **Sunil Kumar B A** |
| **Depends on** | Phase 5 |
| **Runs parallel with** | Phase 6 (Varun) |
| **Blocks** | Phases 8, 10 |
| **Rubric** | Encapsulation (part of 8 pts) · **Bonus B** — Singleton |
| **Status** | ✅ Complete |

---

## Objective

Build the infrastructure layer: generic storage, centralised validation, date handling, and both
singleton flavours.

---

## Tasks

### `DataStore<T extends MedicalEntity>` — generic storage
- [x] Generic class with a **bounded type parameter**
- [x] `LinkedHashMap` backing for deterministic iteration
- [x] CRUD — `save`, `findById`, `findAll`, `deleteById`, `exists`, `count`
- [x] `findById` returns `Optional<T>`; `getById` throws
- [x] `saveAll` for bulk import
- [x] `findBy(Predicate<T>)` for arbitrary filtering
- [x] `search(String)` leaning on the `Searchable` bound
- [x] `findAllSorted(Comparator<T>)`
- [x] `stream()`, `findFirst`, `countBy`
- [x] Implements `Iterable<T>` with a **hand-written iterator**
- [x] Iterator `remove()` throws — deletions must go through `deleteById`
- [x] Serialization with try-with-resources

### `Validator` — centralised validation
- [x] `validateName` — length + character rules
- [x] `validateAge` — range, plus a `String` overload that chains `NumberFormatException`
- [x] `validateContactNumber` — normalises `+91`, spaces, hyphens
- [x] `validateEmail` — optional, but must be well-formed if supplied
- [x] `validateBloodGroup`
- [x] `validateFee` — non-negative, bounded
- [x] `validateId` — `PREFIX-####` format
- [x] `validateAppointmentSlot` — future + within clinic hours
- [x] `requireNonNull`, `requireNonBlank` generics
- [x] `isValidX` boolean variants for stream filters

### `DateUtil` — `java.time` helpers
- [x] Thread-safe `static final DateTimeFormatter` constants
- [x] `format`, `formatForDisplay`, `formatDate` — null-safe
- [x] `parseDateTime`, `parseDate` — chain `DateTimeParseException`
- [x] `parseDateTimeOrNull` — lenient, for imports
- [x] `isWithinClinicHours`, `isWeekend`, `isFuture`
- [x] `alignToSlot` — rounds down to a slot boundary
- [x] `generateSlotsForDay`
- [x] `hoursBetween`, `daysBetween`, `describeTimeUntil`

### `IdGenerator` — **lazy** singleton
- [x] Initialisation-on-demand **holder idiom**
- [x] Private constructor
- [x] `AtomicInteger` per prefix
- [x] `ConcurrentHashMap` for the counter map
- [x] `nextPatientId`, `nextDoctorId`, `nextAppointmentId`, `nextBillId`
- [x] `syncCounter` / `syncFromId` using `accumulateAndGet(value, Math::max)`
- [x] `resetAll()` so tests can isolate

### `AppConfig` — **eager** singleton
- [x] `private static final INSTANCE = new AppConfig()`
- [x] Private constructor
- [x] Settings map with defaults
- [x] Feature flags (persistence, reminders, verbose)
- [x] Unmodifiable settings view
- [x] `printConfiguration()`

### `DemoDataSeeder`
- [x] 6 doctors across all specialities
- [x] 5 patients with varied ages, insurance and histories
- [x] 4 appointments with symptoms
- [x] Failures during seeding never abort startup

---

## The two singletons, contrasted

This is the phase's centrepiece — both flavours, implemented deliberately.

### Eager — `AppConfig`

```java
private static final AppConfig INSTANCE = new AppConfig();
public static AppConfig getInstance() { return INSTANCE; }
```

Thread-safe **for free**: the JVM guarantees class initialisation runs exactly once, under a lock
it holds internally. No `synchronized` anywhere. Cost: built even if never used — fine for
config.

### Lazy — `IdGenerator`

```java
private static final class Holder {
    private static final IdGenerator INSTANCE = new IdGenerator();
}
public static IdGenerator getInstance() { return Holder.INSTANCE; }
```

A nested class is not initialised until first referenced, so construction is deferred to the
first `getInstance()` call — and the same class-init lock still guarantees exactly once. **Lazy
and thread-safe, with no synchronisation in our code.**

### Why not the obvious alternatives

| Approach | Problem |
|---|---|
| `if (instance == null) instance = new ...` | Two threads can both see `null` and both construct |
| `synchronized getInstance()` | Locks on *every* call forever, to guard a one-time race |
| Double-checked locking | Requires `volatile`; subtle and easy to get wrong |
| Enum singleton | Most robust, but hides the lazy-loading mechanism we wanted to show |

**Observable at runtime:** `[AppConfig]` prints during startup; `[IdGenerator]` prints only when
the first id is requested.

---

## Why generics, concretely

Without `DataStore<T>` we would have written `PatientStore`, `DoctorStore`, `AppointmentStore`
and `BillStore` — four copies of identical CRUD that drift apart. The alternative,
`Map<String, Object>`, pushes a cast onto every caller and turns a typo into a runtime
`ClassCastException`.

The **bound** is what makes it work:

```java
public class DataStore<T extends MedicalEntity> implements Iterable<T>
```

An unbounded `<T>` would leave the compiler unable to prove `getId()` and `matches()` exist.

---

## `AtomicInteger`, and why it is not optional

```java
int value = counter.incrementAndGet();   // one atomic operation
```

`count++` is a *read-modify-write* triple. Two threads can read the same value, both increment,
and both write — producing one id twice.

**Proven:** `TestRunner` spawns 10 threads generating 100 ids each and asserts **1,000 unique
ids**. With a plain `int` this test fails intermittently.

---

## Deliverables

| File | Lines |
|---|---|
| `DataStore.java` | 322 |
| `Validator.java` | 271 |
| `DateUtil.java` | 258 |
| `IdGenerator.java` | 163 |
| `AppConfig.java` | 156 |
| `DemoDataSeeder.java` | 116 |

---

## Verification

`TestRunner` suites: *Validator*, *DateUtil*, *Singletons*, *DataStore<T>*,
*Generics and Iterator contract*, *Concurrency*.

---

## Exit criteria

- [x] `DataStore<T>` handles every entity type with no casts at call sites
- [x] Both singletons proven to return one instance
- [x] 1,000-id concurrency test passes with zero collisions
- [x] Validation rejects every invalid input case tested
- [x] Iterator contract honoured, including `remove()` refusal
- [x] **Zubair unblocked for Phase 8**

---

**Previous:** [Phase 6](./PHASE_06_DOMAIN_MODEL.md) ·
**Next:** [Phase 8 — Services](./PHASE_08_SERVICES.md) ·
[Phase index](./README.md) · [Sunil's tasks](../team/SUNIL.md)
