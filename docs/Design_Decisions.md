# Design Decisions

> **Owner:** Sunil Kumar B A (compiled from all three members' work) · **Phases:** 2–3

Each entry records a decision, the alternatives rejected, and *why*. Where we deviated from
the assignment skeleton, that is called out explicitly.

---

## Contents

1. [Architecture](#1-architecture)
2. [SOLID principles](#2-solid-principles-applied)
3. [Design patterns](#3-design-patterns)
4. [Deviations from the assignment skeleton](#4-deviations-from-the-assignment-skeleton)
5. [Data type and API choices](#5-data-type-and-api-choices)
6. [Copy semantics](#6-copy-semantics-deep-vs-shallow)
7. [Immutability](#7-immutability)
8. [Exception strategy](#8-exception-strategy)
9. [Concurrency](#9-concurrency)
10. [Persistence](#10-persistence)
11. [Testing](#11-testing)
12. [Known limitations](#12-known-limitations)

---

## 1. Architecture

### 1.1 Layered, one direction only

```
   Main (console UI)
        │ depends on
        ▼
   service/          DoctorService · PatientService · AppointmentService
        │            BillingService · NotificationService
        ▼
   entity/ ◄──────── factory/ · strategy/ · observer/
        │
        ▼
   util/             DataStore<T> · Validator · DateUtil · CSVUtil · IdGenerator
        │
        ▼
   constants/ · interfaces/ · exception/
```

**Decision:** dependencies point downward only. `util` never imports `service`; `entity` never
imports `service`.

**Why:** it makes the system testable in isolation. `TestRunner` constructs a `DataStore<Patient>`
without booting the UI, and a `Validator` test needs nothing else at all. A cycle anywhere in
this graph would force whole-application setup for every test.

### 1.2 Services own use-cases, entities own themselves

**Decision:** entities carry behaviour that is *about themselves* (`isSeniorCitizen()`,
`transitionTo()`, `displayDetails()`). Services carry behaviour that *coordinates* entities
(booking, billing, searching).

**Why:** the alternative — anaemic entities plus fat services — pushes rules like "can this
appointment be cancelled?" into whichever service happens to need it, and they drift. Putting
the state machine in `AppointmentStatus` means there is exactly one answer.

---

## 2. SOLID principles applied

### S — Single Responsibility

| Class | Its one job | What it deliberately does *not* do |
|---|---|---|
| `Validator` | Decide if input is valid | Store, format, or construct entities |
| `DataStore<T>` | Store and retrieve | Validate or apply business rules |
| `CSVUtil` | Translate entities ↔ CSV rows | Decide *when* to persist |
| `BillFactory` | Choose which `Bill` subclass | Calculate amounts |
| `NotificationService` | Fan events out | Know what a channel does with them |
| `AppointmentService` | Orchestrate booking use-cases | Validate fields, format output |

The clearest test: **the reason to change**. `Validator` changes when a business rule changes.
`CSVUtil` changes when the file format changes. These are different reasons, so they are
different classes.

### O — Open/Closed

**Adding a pricing policy** requires writing one class implementing `BillingStrategy`. No
existing file is edited:

```java
public class CorporateBillingStrategy implements BillingStrategy {
    public double calculate(double base) { return base * 0.85; }
}
```

**Adding a notification channel** requires one class implementing `AppointmentObserver` and one
`register()` call. `AppointmentService` is untouched.

Where we are *not* open/closed, by choice: adding a `BillType` requires editing `BillFactory`.
That is deliberate — the `switch` is exhaustive over the enum, so a new constant becomes a
**compile error** rather than a silently-unhandled case. We chose compile-time safety over
extensibility here, because a mis-priced bill is worse than an extra edit.

### L — Liskov Substitution

Every `Bill` subclass is usable wherever a `Bill` is expected. `Main.printBill()` calls
`bill.getFormattedBill()` with no idea which subclass it holds.

The rule we enforced to keep this true: **no subclass strengthens a precondition or throws
where the parent does not**. `EmergencyBill` adds a surcharge by overriding a hook that already
exists and already returns a number — it does not, for example, refuse to bill senior citizens.

`Patient` and `Doctor` are both substitutable for `Person`, which is why
`List<MedicalEntity>` iteration in the demo menu works at all.

### I — Interface Segregation

Four small interfaces rather than one large one:

| Interface | Methods | Implemented by |
|---|---|---|
| `Searchable` | 1 abstract + 3 default | Every `MedicalEntity` |
| `Payable` | 3 abstract + 3 default + 1 static | `Bill` only |
| `BillingStrategy` | 1 abstract | Pricing policies |
| `AppointmentObserver` | 1 abstract + 2 default | Notification channels |

**Why not one `Manageable` interface?** `Patient` is searchable but not payable. A combined
interface would force `Patient` to implement `processPayment()` and throw
`UnsupportedOperationException` — the exact smell ISP exists to prevent.

### D — Dependency Inversion

`AppointmentService` depends on `NotificationService` through its **constructor**, and on
`AppointmentObserver` — an abstraction — for the channels:

```java
public AppointmentService(DataStore<Appointment> store, NotificationService notifications)
```

**Why constructor injection over singletons:** `TestRunner` hands in a fresh `DataStore` and a
fresh `NotificationService` for every test. Had the service reached for a singleton internally,
tests would share state and their order would matter.

The two singletons we *do* have (`AppConfig`, `IdGenerator`) are read-mostly infrastructure with
no business behaviour, and `IdGenerator` exposes `resetAll()` precisely so tests can isolate.

---

## 3. Design patterns

| Pattern | Where | Problem it solves |
|---|---|---|
| **Singleton (eager)** | `AppConfig` | One config, thread-safe with no locking |
| **Singleton (lazy)** | `IdGenerator` | Deferred construction, still thread-safe |
| **Factory** | `BillFactory` | Callers pick a bill *type*, not a class |
| **Strategy** | `BillingStrategy` + 3 impls | Swap pricing without touching billing code |
| **Template Method** | `Bill.generateBill()` | Fix the algorithm's order, vary its steps |
| **Observer** | `NotificationService` | Decouple booking from notification |
| **Null Object** | `StandardBillingStrategy` | Remove null checks from the calculation path |
| **Iterator** | `DataStore<T>` | For-each support without exposing the map |

### 3.1 Eager vs lazy singleton — why both

We implemented both deliberately, to contrast them.

```java
// AppConfig — EAGER. Built during class initialisation.
private static final AppConfig INSTANCE = new AppConfig();
```
Thread-safe because the JVM guarantees class initialisation runs once, under its own lock. Cost:
built even if unused. Fine for config, which is cheap and always needed.

```java
// IdGenerator — LAZY, via initialisation-on-demand holder.
private static final class Holder {
    private static final IdGenerator INSTANCE = new IdGenerator();
}
public static IdGenerator getInstance() { return Holder.INSTANCE; }
```
`Holder` is not initialised until first referenced, so construction is deferred to the first
`getInstance()` call — and the same class-init lock still guarantees exactly once.

**Rejected alternatives:**

| Approach | Why rejected |
|---|---|
| `if (instance == null) instance = new ...` | Broken: two threads can both see `null` |
| `synchronized getInstance()` | Correct but locks on *every* call, forever, to guard a one-time race |
| Double-checked locking | Works only with `volatile`; subtle and easy to get wrong |
| Enum singleton | Genuinely the most robust, but obscures the lazy-loading mechanism we wanted to demonstrate |

You can see the difference at runtime: `[AppConfig]` prints during startup, `[IdGenerator]`
prints only when the first id is requested.

### 3.2 Template Method vs "override `generateBill()`"

**Decision:** `Bill.generateBill()` is `final`. Subclasses override *steps*
(`calculateBaseAmount()`, `applySurcharge()`, `calculateTax()`), not the method itself.

**Why:** the assignment asks for polymorphic `generateBill()` behaviour, and this delivers
exactly that — three `Bill` references, one call, three different totals — while making it
*impossible* for a subclass to forget the tax step or apply the discount after tax. The billing
sequence is a business invariant; the amounts are not.

```java
public final BillSummary generateBill() {
    baseAmount          = calculateBaseAmount();   // abstract — subclass must supply
    afterStrategyAmount = applyStrategy(base);     // Strategy decides
    surchargeAmount     = applySurcharge(...);     // hook — default 0.0
    taxAmount           = calculateTax(taxable);   // hook — default GST
    totalAmount         = taxable + taxAmount;     // invariant
}
```

Verified in `TestRunner`: `emergency.getTotalAmount() > consultation.getTotalAmount()` for the
identical base fee, because `EmergencyBill` overrides `applySurcharge()`.

### 3.3 Factory + Strategy composition

`BillFactory.chooseStrategy(patient)` derives the pricing policy from the patient's own
attributes, so the two patterns cooperate rather than each re-deciding:

```
insured?          -> InsuranceBillingStrategy      (contractual, checked first)
senior citizen?   -> SeniorCitizenBillingStrategy  (self-payers only)
otherwise         -> StandardBillingStrategy
```

Precedence is deliberate and documented: insurance is a contract, the senior concession is a
courtesy, and stacking both would double-discount.

---

## 4. Deviations from the assignment skeleton

Four, each with a reason.

### 4.1 `interface/` → `interfaces/`

**`interface` is a reserved Java keyword and cannot be a package name.** `javac` rejects it
outright. *(Caught and fixed by Varun in commit `71d61f0`, before this phase.)*

### 4.2 Package renamed from `main.java.com.airtribe.meditrack` to `com.airtribe.meditrack`

The original code declared `package main.java.com.airtribe.meditrack.entity`, treating the
directory path as part of the package name. It compiled — IntelliJ had `src` marked as the
source root, so path and package agreed — but:

- the rubric specifies base package **`com.airtribe.meditrack`**;
- every new service and util would have imported `main.java.com.airtribe...`, permanently
  baking the mistake into the codebase;
- `Main.java` had **no** package declaration at all, sitting in the default package while
  physically located under `.../meditrack/`.

**Fix:** package declarations corrected across all files, and `MediTrack.iml` re-pointed its
source root from `src` to `src/main/java`.

> **If IntelliJ shows package errors after pulling this change**, it has cached the old source
> root. **File → Reload All from Disk**.

### 4.3 Added `factory/`, `strategy/` and `observer/` packages

The skeleton lists neither. We could have put `BillFactory` in `util/` and the strategies in
`service/`, but pattern implementations are neither utilities nor services — grouping them by
the pattern they implement makes the architecture self-documenting. `interfaces/` still holds
the contracts; these packages hold the implementations.

### 4.4 Added entity types beyond the skeleton

`ConsultationBill`, `ProcedureBill`, `EmergencyBill`, `BillType`, `AppointmentStatus`,
`MedicalEntity`. The first three are required for the Factory to have anything to choose
*between* — a factory returning one type is not a factory.

---

## 5. Data type and API choices

### 5.1 `java.time.LocalDateTime` instead of `java.util.Date`

**Decision:** replaced `Date` throughout.

**Why:** `Date` is **mutable**. Every getter returning one needs a defensive copy, and missing a
single one is a silent aliasing bug:

```java
// The Date version — one forgotten copy and callers can mutate our state
public Date getBillingDate() { return new Date(billingDate.getTime()); }

// LocalDateTime — immutable, so the whole defect class disappears
public LocalDateTime getBillingDate() { return billingDate; }
```

`Date` is also badly designed (months are 0-indexed, years offset from 1900) and its formatter,
`SimpleDateFormat`, is **not thread-safe** — a real problem given our reminder daemon.
`DateTimeFormatter` is immutable and shareable, so `DateUtil` holds them as `static final`.

**Trade-off:** this removed `Date.clone()` as a deep-copy demonstration. We replaced it with a
better one — mutable *collections* in `Patient`, which is the case that actually bites in
practice.

### 5.2 `Optional<T>` for lookups that may miss

`DataStore.findById()` returns `Optional<T>`; `getById()` throws. Two methods because there are
genuinely two cases: "check if this exists" and "this must exist". Returning `null` would
conflate them and invite a `NullPointerException` at the call site.

### 5.3 `LinkedHashMap` for storage

Insertion-ordered iteration means console output and CSV exports are **stable across runs**,
which makes diffing and screenshotting sane. `HashMap` would reorder unpredictably.

### 5.4 `StringBuilder` over `+` in loops

Used in `Bill.getFormattedBill()`, `displayDetails()`, `AIHelper.buildTriageReport()` and the
test summary.

**Why it matters:** `String` is immutable, so `s += x` inside a loop allocates a *new* `String`
every iteration — O(n²) copying. `getFormattedBill()` appends ~20 fragments plus one per line
item. Outside a loop, plain `+` is fine and clearer (javac compiles it to `StringBuilder`
anyway), so we did not apply this dogmatically.

Buffers are pre-sized (`new StringBuilder(512)`) to avoid intermediate array growth.

---

## 6. Copy semantics: deep vs shallow

**Decision:** `Patient` provides **both** `clone()` (deep) and `shallowCopy()` (shallow).

**Why keep a deliberately-wrong method:** it makes the difference *demonstrable* rather than
merely asserted. `TestRunner` mutates the original and observes both copies:

```java
original.addMedicalHistoryEntry("NEW ENTRY");
// original : 2 entries
// deep copy: 1 entry   <- unaffected
// shallow  : 2 entries <- shares the list, changed too
```

### 6.1 Appointment: selective deep copy

`Appointment.clone()` deep-copies its `Patient` but **shares** its `Doctor` by reference.

**Why not deep-copy everything:** a doctor is one clinic-wide entity. Cloning them per
appointment would mean 40 `Doctor` objects for 40 appointments, and updating a fee would leave
39 stale copies. Deep copy is a judgement about **ownership**, not a blanket rule: an
appointment owns its snapshot of the patient's context; it does not own the doctor.

---

## 7. Immutability

`BillSummary` applies all five rules:

1. `final` class — no subclass can add mutable state
2. All fields `private final`
3. No setters, no mutating methods
4. Defensive copy **in** — the incoming list is copied
5. Unmodifiable view **out** — `getLineItems()` throws on mutation

**Why it is thread-safe with no synchronisation:** the fields are `final` and set before the
constructor returns. The Java Memory Model guarantees that any thread seeing a reference to the
object also sees fully-initialised fields. There is no state to race on — which is why the
reminder daemon can read summaries while the main thread bills, with zero coordination.

`Bill.LineItem` is itself immutable, so copying the list is genuinely sufficient. Had elements
been mutable, the list copy alone would have been false comfort.

**"Mutation" returns a new object:** `withPayment(500)` follows the `String.trim()` /
`LocalDate.plusDays()` convention.

---

## 8. Exception strategy

### 8.1 All custom exceptions are checked

| Exception | Thrown when |
|---|---|
| `InvalidDataException` | Input fails validation |
| `AppointmentNotFoundException` | Appointment id unknown |
| `EntityNotFoundException` | Generic store lookup misses |
| `SlotUnavailableException` | Doctor cannot take the slot |
| `DataPersistenceException` | File I/O or serialization failure |

**Why checked, not unchecked:** every one of these is an **expected, recoverable** condition in
a console app. A mistyped id is routine. Checked exceptions force the UI to handle it and
re-prompt rather than crash. We reserve unchecked exceptions for programmer errors —
`AssertionError` in utility-class constructors, `IllegalArgumentException` for a null
appointment passed to the factory.

### 8.2 Chaining preserves the root cause

```java
try {
    return Integer.parseInt(trimmed);
} catch (NumberFormatException e) {
    throw new InvalidDataException("age", ageText, "must be a whole number", e);
}
```

The user sees a domain message; `getCause()` still holds the original. Asserted in
`TestRunner`: *"chained cause is NumberFormatException"*.

### 8.3 try-with-resources everywhere

Every file operation in `CSVUtil` and `DataStore` opens its stream in a try-with-resources
header. Resources close automatically, in reverse order, on both the success and exception
paths — replacing the `finally { if (r != null) r.close(); }` dance that was routinely written
incorrectly.

### 8.4 Observers fail in isolation

`NotificationService` wraps each observer call in its own try/catch. A broken SMS channel must
not prevent the audit log from recording, and must certainly not fail the booking that
triggered it.

---

## 9. Concurrency

Three mechanisms, each for a specific reason.

| Mechanism | Where | Why that one |
|---|---|---|
| `AtomicInteger` | `IdGenerator`, `MedicalEntity` counter | `count++` is a read-modify-write that loses updates under contention |
| `ConcurrentHashMap` | `IdGenerator.counters` | Safe concurrent insertion of new prefixes |
| `CopyOnWriteArrayList` | `NotificationService.observers` | Rare writes, frequent iteration; lock-free reads, no `ConcurrentModificationException` |
| `synchronized` block | Reminder sweep | Prevents an in-flight sweep from overlapping the next tick |
| `Timer` + `TimerTask` | Reminder scheduler | Scheduled background work |

**The daemon thread decision:** `new Timer("meditrack-reminder", true)`. The `true` marks it
daemon, so the JVM exits when the user quits the menu. A non-daemon timer thread would keep the
process alive indefinitely after "Goodbye" — a genuinely confusing bug.

**Proof the atomics work:** `TestRunner` spawns 10 threads generating 100 ids each and asserts
1,000 **unique** ids. With a plain `int++` this test fails intermittently.

---

## 10. Persistence

### 10.1 Both CSV and Java serialization

| | CSV | Serialization |
|---|---|---|
| Human-readable | Yes | No |
| Survives class changes | Yes | No — `serialVersionUID` mismatch |
| Handles object graphs | No — needs flattening | Yes, automatically |
| Used for | `--loadData`, inspection | `DataStore.serializeTo()` |

CSV is primary because a reviewer can open `data/patients.csv` and read it.

### 10.2 Appointments store *ids*, not nested objects

```csv
id,patientId,doctorId,slot,status,symptoms,notes
APT-0001,PAT-0001,DOC-0001,2026-07-26 10:00,PENDING,chest pain;breathless,
```

The object graph is flattened to foreign keys and re-linked on load. Rows referencing unknown
ids are **skipped** rather than loaded with nulls — a referential-integrity check the file
format cannot enforce itself.

### 10.3 Escaping

`String.split(",")` is used as the assignment requires, so commas inside values must not reach
the file. Values are escaped on write (`,` → `||`) and unescaped on read. Round-tripping
`"Migraine, recurring"` is asserted in `TestRunner`.

### 10.4 `transient` on `BillingStrategy`

A strategy is **behaviour, not data**, and is often a lambda — which is not `Serializable`.
Marking it `transient` prevents `NotSerializableException`; the bill keeps its already-computed
totals, and `applyStrategy()` falls back to identity so a reloaded bill never silently reprices
itself.

---

## 11. Testing

**Decision:** a hand-written `TestRunner` (no JUnit, as required) with 325 assertions.

It implements the minimum a framework actually needs: named assertions, pass/fail tally,
exception capture so one failure does not abort the run, suite grouping, and **a non-zero exit
code on failure** — which is what lets the Dockerfile use it as a build gate.

```dockerfile
RUN javac -d out @sources.txt && \
    java -cp out com.airtribe.meditrack.test.TestRunner   # fails the build if red
```

**One deliberate design note:** `assertNoThrow` treats a `null` return as failure, which made
`validateEmail(null)` — correctly returning `null` for an optional field — look like a bug. We
fixed the *test*, not the code, and left a comment saying so. Tests are wrong at least as often
as code.

---

## 12. Known limitations

Honest scope boundaries, all deliberate:

| Limitation | Why | What production would need |
|---|---|---|
| In-memory storage | Assignment is Core Java only | A real database |
| No authentication | Out of scope | Login, roles, audit identity |
| Single-user console | Assignment scope | Web or desktop UI |
| Rule-based "AI" | No ML libraries permitted | Trained triage model — though rule-based is *more* auditable for clinical use |
| CSV concurrency | Two processes writing `data/` would corrupt it | File locking or a database |
| Ratings are a running average of two | Simplification | Store individual ratings |
| No pagination | Datasets are small | Paged queries |

---

*Part of the [MediTrack documentation set](./). See also
[JVM_Report.md](./JVM_Report.md), [DB_Design.md](./DB_Design.md) and
[Setup_Instructions.md](./Setup_Instructions.md).*
