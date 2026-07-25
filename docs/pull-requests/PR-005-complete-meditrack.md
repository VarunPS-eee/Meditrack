# PR-005 — Complete MediTrack build-out

| | |
|---|---|
| **Branch** | `feature/complete-meditrack` → `main` |
| **Status** | 🟡 Open — awaiting review |
| **Scope** | All three members' areas |
| **Files** | 45 Java files (8,930 lines) + 23 documentation files + Docker |

---

## What

Takes the project from the domain-model skeleton to a submission-ready application: service
layer, utilities, design patterns, persistence, AI triage, analytics, console UI, a 325-assertion
test suite, Docker packaging, and the full documentation set.

## Why

At the previous commit (`71d61f0`) the project was at roughly **20% of the rubric** — entities
existed, but there were no services, no utils, no exceptions, no UI, no tests, and all four
documentation files were empty (0 bytes).

---

## ⚠️ Breaking change — package rename

**Every Java file changed its package declaration.**

```diff
- package main.java.com.airtribe.meditrack.entity;
+ package com.airtribe.meditrack.entity;
```

`Main.java` also gained a package declaration; it previously had none, sitting in the default
package while physically located under `.../meditrack/`.

`MediTrack.iml` re-points the source root from `src` to `src/main/java`.

### Why this was necessary

1. The rubric specifies base package **`com.airtribe.meditrack`** — marks were at stake.
2. Every new service and util would otherwise have imported `main.java.com.airtribe...`,
   baking the mistake in permanently.
3. The old form compiled only because IntelliJ had `src` marked as the source root, so path and
   package agreed. That is exactly what made it easy to miss.

### 🔴 Action required for reviewers

If IntelliJ reports package errors after checking out this branch, it has cached the old source
root:

**File → Reload All from Disk**

Then confirm `src/main/java` is marked as **Sources** (blue folder icon) under
*File → Project Structure → Modules*.

---

## How — what was added

### Varun's areas — `entity/`, `factory/`, `strategy/`
- `MedicalEntity` abstract root with static block and `AtomicInteger` counter
- `Person` / `Patient` / `Doctor` / `Appointment` completed with encapsulation and comparators
- **Deep vs shallow copy** — `Patient.clone()` and `Patient.shallowCopy()` side by side
- `Bill` as a **Template Method** with `final generateBill()` and three concrete subclasses
- `BillSummary` rebuilt as a fully immutable type
- Three enums carrying behaviour, including a state machine on `AppointmentStatus`
- `BillFactory` and three `BillingStrategy` implementations

### Zubair's areas — `service/`, `observer/`, `exception/`, `AIHelper`
- Five services with full CRUD, booking rules and analytics
- **Four `searchPatient` overloads**, four `searchDoctor` overloads
- Five checked exceptions with chaining
- Three observers plus `NotificationService` with a `TimerTask` daemon scheduler
- `AIHelper` — rule-based triage with explainable scoring

### Sunil's areas — `util/`, `test/`, `docs/`, Docker
- `DataStore<T extends MedicalEntity>` — generic, `Iterable`, custom iterator
- `Validator`, `DateUtil`, `CSVUtil`
- **Both singleton flavours** — eager `AppConfig`, lazy `IdGenerator`
- `TestRunner` with 325 assertions
- Multi-stage Dockerfile where tests gate the build
- All documentation

### Joint
- `Main.java` — nine-section menu UI with CLI argument handling

### Also changed
- **Migrated off `java.util.Date`** to `java.time.LocalDateTime` throughout. `Date` is mutable
  (every getter needs a defensive copy) and `SimpleDateFormat` is not thread-safe, which matters
  given the reminder daemon.

---

## Rubric impact

| Item | Points | Before | After |
|---|---|---|---|
| Environment Setup & JVM | 10 | ❌ empty files | ✅ |
| Package Structure & Basics | 10 | ⚠️ wrong base package | ✅ |
| Encapsulation | 8 | ⚠️ no validation | ✅ |
| Inheritance | 10 | ✅ | ✅ |
| Polymorphism | 7 | ⚠️ no overloading | ✅ |
| Abstraction & Interfaces | 10 | ⚠️ unimplemented | ✅ |
| Advanced OOP | — | ⚠️ partial | ✅ |
| Application Logic | 15 | ❌ | ✅ |
| Bonus A — File I/O | 10 | ❌ | ✅ |
| Bonus B — Design Patterns | 10 | ❌ | ✅ |
| Bonus C — AI Feature | 10 | ❌ | ✅ |
| Bonus D — Streams | 10 | ❌ | ✅ |

The brief asks for **any two** bonuses. All four are implemented.

---

## Testing

```
Total  : 325
Passed : 325
Failed : 0
Rate   : 100.0%
```

20 suites. Verified on **Windows 11 (host)** and **Alpine Linux (container)** — same bytecode,
both green.

### Manual verification
- Full menu walkthrough, every option
- Seed → book → confirm → complete → bill → pay
- CSV save → exit → `--loadData` → state restored
- Comma-containing field round-trip verified in file and on screen
- Docker: build, test, interactive run
- UTF-8 (`₹`) on both platforms

---

## Review notes

Please look closely at:

1. **The package rename** — it touches every file. Confirm your IDE picks up the new source root.
2. **`Bill.generateBill()` is `final`** — subclasses override *steps*, not the method. This is a
   deliberate reading of "polymorphic `generateBill()`"; rationale in
   [Design_Decisions §3.2](../Design_Decisions.md#32-template-method-vs-override-generatebill).
3. **`Appointment.clone()` shares its doctor by reference** — deliberate; rationale in the class
   JavaDoc.
4. **`Patient.shallowCopy()` is intentionally "wrong"** — kept so the deep/shallow difference is
   demonstrable.
5. **`Date` → `LocalDateTime` migration** — changes signatures on Varun's original entities.
6. **Two test fixes** — the first run was 321/323 and both failures were *test* bugs, not product
   bugs. Reasoning in [Phase 14](../phases/PHASE_14_TESTING.md#two-bugs-the-suite-caught--in-the-tests).

---

## Reviewers

| Reviewer | Focus | Status |
|---|---|---|
| **Varun** | Package rename, entity changes, `Date` migration, Factory/Strategy | ⬜ Pending |
| **Zubair** | Services, exceptions, observers, AI scoring model | ⬜ Pending |

---

## How to verify locally

```bash
git checkout feature/complete-meditrack

# Tests — expect 325/325
javac -encoding UTF-8 -d out $(find src/main/java -name '*.java')
java -cp out com.airtribe.meditrack.test.TestRunner

# Or with Docker — no JDK needed
docker build -t meditrack:1.0.0 .
docker run -it --rm meditrack:1.0.0 --seedDemo
```

---

*[PR index](./README.md) · [Phase plan](../phases/README.md)*
