# MediTrack — Clinic & Appointment Management System

> A modular, object-oriented clinic management system built in **Core Java 21** — no frameworks,
> no third-party dependencies, no build tool.

[![Java](https://img.shields.io/badge/Java-21%20LTS-orange)](https://adoptium.net/temurin/releases/?version=21)
[![Tests](https://img.shields.io/badge/tests-325%2F325%20passing-brightgreen)](#testing)
[![Docker](https://img.shields.io/badge/docker-286MB-blue)](#run-with-docker)
[![Dependencies](https://img.shields.io/badge/dependencies-zero-lightgrey)](#)

**Airtribe Java Track** · Team: [Varun P S](docs/team/VARUN.md) ·
[Zubair](docs/team/ZUBAIR.md) · [Sunil Kumar B A](docs/team/SUNIL.md)

---

## Contents

- [Quick start](#quick-start)
- [What it does](#what-it-does)
- [Sample run](#sample-run)
- [Architecture](#architecture)
- [What this project demonstrates](#what-this-project-demonstrates)
- [Testing](#testing)
- [Documentation](#documentation)
- [Project structure](#project-structure)

---

## Quick start

### Run with Docker

No Java installation required. The image compiles the source **and runs the test suite** during
build, so a successful build is a green test run.

```bash
docker build -t meditrack:1.0.0 .
docker run -it --rm meditrack:1.0.0 --seedDemo
```

> **`-it` is required** — MediTrack is an interactive console menu. Without a TTY it reads EOF
> and exits at the first prompt.

```bash
docker run --rm meditrack:1.0.0 --runTests                    # tests only
docker run -it --rm -v "$(pwd)/data:/data" meditrack:1.0.0 --loadData   # persist to host
docker compose run --rm meditrack                             # or via compose
```

### Run with a JDK

Requires **JDK 21+**.

```bash
git clone https://github.com/VarunPS-eee/Meditrack.git
cd Meditrack

# Linux / macOS / Git Bash
javac -encoding UTF-8 -d out $(find src/main/java -name "*.java")

# Windows PowerShell
# $s = Get-ChildItem src\main\java -Filter *.java -Recurse | % { $_.FullName }
# javac -encoding UTF-8 -d out $s

java -cp out com.airtribe.meditrack.Main --seedDemo
```

### Command-line options

| Flag | Effect |
|---|---|
| `--seedDemo` | Start with a populated demo clinic — **recommended first run** |
| `--loadData` | Restore patients, doctors and appointments from `data/` |
| `--runTests` | Run the 325-assertion suite and exit |
| `--help` | Print usage |
| *(none)* | Start with an empty clinic |

Full setup guide including IntelliJ: **[docs/Setup_Instructions.md](docs/Setup_Instructions.md)**

---

## What it does

| Area | Capabilities |
|---|---|
| **Patients** | Register, update, delete, medical history, allergies, blood group, insurance |
| **Doctors** | Roster with specialities, fees, experience, ratings, availability |
| **Appointments** | Book, confirm, cancel, reschedule, complete — with double-booking prevention |
| **Billing** | Three bill types, GST, insurance and senior-citizen policies, partial payments |
| **Search** | Overloaded search by id, name, age, age range, speciality, fee |
| **AI Triage** | Rule-based doctor recommendation from symptoms, with urgency detection |
| **Reports** | Utilisation, revenue, demographics, cancellation rates — via Java Streams |
| **Persistence** | CSV and Java serialization, restored with `--loadData` |
| **Notifications** | Console, SMS and audit-log observers with a background reminder scheduler |

---

## Sample run

### Startup

```
[Constants] Static block executed — data directory resolved to: data
[AppConfig] Eager singleton constructed at 2026-07-25 14:16
[IdGenerator] Lazy singleton constructed on first use.

============================================================
   __  __          _ _ _____              _
  |  \/  | ___  __| (_)_   _| __ __ _  ___| | __
  | |\/| |/ _ \/ _` | | | || '__/ _` |/ __| |/ /
  | |  | |  __/ (_| | | | || | | (_| | (__|   <
  |_|  |_|\___|\__,_|_| |_||_|  \__,_|\___|_|\_\
============================================================
    MediTrack v1.0.0 — Airtribe Java Track
    Java 21.0.11 on Windows 11
  ============================================================
[MedicalEntity] Class loaded and initialised at 2026-07-25T14:16:26

    [NOTIFY] BOOKED     APT-0001 — Dr. Anita Rao with Ravi Kumar, 26 Jul 2026, 10:00 am (in 19 hours)
    [SMS -> ******5670] MediTrack: your appointment APT-0001 is booked for 26 Jul 2026, 10:00 am
  Demo data seeded: 6 doctors, 5 patients, 4 appointments.
  [Reminders] Background scheduler started (every 300s, daemon thread).
```

> The four static-block messages appear in **class-initialisation order**, and `[MedicalEntity]`
> comes *after* the banner — proving the JVM loaded it lazily, at first use. See
> [JVM_Report §2](docs/JVM_Report.md#2-class-loader-subsystem).

### Main menu

```
============================================================
  MAIN MENU
============================================================
  1. Patients          5. Search
  2. Doctors           6. AI Triage
  3. Appointments      7. Reports & Analytics
  4. Billing           8. Data & Settings
                       9. OOP Demonstrations
  0. Exit
============================================================
```

### AI triage — menu 6 → 1

```
  ========================================================================
   AI TRIAGE — rule-based recommendation
  ========================================================================
   Symptoms: severe chest pain and breathless

   ** URGENT ** These symptoms suggest emergency care. Escalate before booking a routine slot.

   Speciality match
   --------------------------------------------------------------------
   Cardiology           2 pts (100% confidence)  matched: chest pain, breathless

   Recommended doctors
   --------------------------------------------------------------------
   1. Dr. Anita Rao            score 23.6
      specialises in Cardiology; rating 4.8, 16y experience, 1 booked
   2. Dr. Rahul Mehta          score 14.4
      adjacent speciality (Orthopedics); rating 4.2, 22y experience, 0 booked

   Earliest slot: Dr. Anita Rao at 25 Jul 2026, 02:30 pm
  ========================================================================
```

Note that Rahul Mehta has *more* experience and *no* bookings, yet Anita Rao still wins — the
speciality match is weighted to dominate, by design.

### Billing — menu 4 → 1

```
==============================================================
              MediTrack — CONSULTATION BILL
==============================================================
  Bill ID    : BIL-0001
  Patient    : Ravi Kumar (PAT-0001)
  Appointment: APT-0001
  Date       : 25 Jul 2026, 02:16 pm
  Policy     : Senior Citizen
--------------------------------------------------------------
  ITEMS
    Doctor consultation                x1        ₹1,400.00
--------------------------------------------------------------
  Base amount                                       ₹1,400.00
  Policy adjustment (Senior Citizen)                 ₹-140.00
  GST @ 18%                                           ₹226.80
--------------------------------------------------------------
  TOTAL                                             ₹1,486.80
  Paid                                                  ₹0.00
  Balance due                                       ₹1,486.80
  Status                                               UNPAID
==============================================================
```

The Factory selected `SeniorCitizenBillingStrategy` automatically — Ravi is 67 and uninsured.

### Strategy comparison — menu 4 → 6

```
  Same ₹1,000.00 consultation for Ravi Kumar, priced three ways:

    Standard                     total    ₹1,180.00   — Standard rate — no adjustment applied.
    Insurance (Default Insurer)  total      ₹354.00   — Default Insurer covers 70%; patient pays the 30% co-pay.
    Senior Citizen               total    ₹1,062.00   — Senior citizen concession of 10% applied.
```

### Deep vs shallow copy — menu 9 → 2

```
  Original history : [Hypertension diagnosed 2021]
  After mutating the ORIGINAL:
    original : [Hypertension diagnosed 2021, MUTATION-AFTER-COPY]
    deep copy: [Hypertension diagnosed 2021]                        <- unaffected
    shallow  : [Hypertension diagnosed 2021, MUTATION-AFTER-COPY]   <- changed too

  The shallow copy shares the original's list; the deep copy owns its own.
```

### Analytics — menu 7 → 2

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

---

## Architecture

```
   Main.java                      Console UI — thin: read, delegate, print
        │
        ▼
   service/                       Use-case orchestration
   PatientService · DoctorService · AppointmentService
   BillingService · NotificationService
        │
        ▼
   entity/  ◄──── factory/ · strategy/ · observer/    Domain model + patterns
   MedicalEntity → Person → Patient/Doctor
   Appointment · Bill hierarchy · BillSummary
        │
        ▼
   util/                          Infrastructure
   DataStore<T> · Validator · DateUtil · CSVUtil
   IdGenerator · AppConfig · AIHelper
        │
        ▼
   constants/ · interfaces/ · exception/
```

**Dependencies point downward only.** `util` never imports `service`; `entity` never imports
`service`. That is what lets the test suite construct any layer in isolation.

---

## What this project demonstrates

### Core OOP

| Concept | Where to look |
|---|---|
| **Encapsulation** | Private fields, validated setters, unmodifiable collection views |
| **Inheritance** | `MedicalEntity` → `Person` → `Patient`/`Doctor`, constructor chaining |
| **Polymorphism (override)** | `displayDetails()`, `getBillDescription()`, billing hooks |
| **Polymorphism (overload)** | `searchPatient()` ×4, `searchDoctor()` ×4 |
| **Abstraction** | `MedicalEntity`, `Person`, `Bill` all abstract |
| **Dynamic dispatch** | Menu 9 → 1 — one loop over `List<MedicalEntity>` |

### Advanced OOP

| Concept | Where to look |
|---|---|
| **Deep vs shallow copy** | `Patient.clone()` vs `Patient.shallowCopy()` — menu 9 → 2 |
| **Selective deep copy** | `Appointment.clone()` — deep-copies patient, *shares* doctor |
| **Immutability** | `BillSummary` — verified by reflection in the test suite |
| **Enums with behaviour** | `AppointmentStatus` owns its own state machine |
| **Static blocks** | `Constants`, `MedicalEntity` — visible on startup |
| **equals/hashCode** | `final`, on the business key, using `getClass()` not `instanceof` |

### Design patterns

| Pattern | Implementation |
|---|---|
| **Singleton — eager** | `AppConfig` |
| **Singleton — lazy** | `IdGenerator`, via the initialisation-on-demand holder idiom |
| **Factory** | `BillFactory` |
| **Strategy** | `BillingStrategy` + three policies |
| **Template Method** | `Bill.generateBill()` — `final`; subclasses override *steps* |
| **Observer** | `NotificationService` + three channels |
| **Null Object** | `StandardBillingStrategy` |
| **Iterator** | `DataStore<T>` implements `Iterable` |

### Java language features

- **Generics** — `DataStore<T extends MedicalEntity>` with a bounded type parameter
- **Collections** — `LinkedHashMap`, `EnumMap`, `TreeMap`, `ConcurrentHashMap`, `CopyOnWriteArrayList`
- **Comparators** — `Doctor.BY_RATING` chaining `comparing().thenComparing().reversed()`
- **Streams & lambdas** — `groupingBy`, `flatMap`, `summaryStatistics`, method references
- **Records** — `AuditEntry`, `SpecialityMatch`, `DoctorRecommendation`
- **Text blocks**, **switch expressions**, **pattern matching for `instanceof`**
- **Exceptions** — five checked types with chaining; try-with-resources throughout
- **Concurrency** — `AtomicInteger`, `synchronized`, `Timer`/`TimerTask` on a daemon thread
- **File I/O** — CSV with escaping, plus Java serialization
- **`StringBuilder`** — pre-sized, used where `+` in a loop would be O(n²)

---

## Testing

```bash
java -cp out com.airtribe.meditrack.test.TestRunner
```

```
  ============================================================
    TEST SUMMARY
  ============================================================
    Total  : 325
    Passed : 325
    Failed : 0
    Rate   : 100.0%
  ============================================================
    ALL TESTS PASSED
  ============================================================
```

A hand-written runner — **no JUnit**, as the assignment requires — with 20 suites. It exits
non-zero on failure, which is what lets the Dockerfile use it as a **build gate**: the image
cannot be built from failing code.

Notable coverage:
- Immutability verified by **reflection** (class is `final`, all fields `final`, no setters)
- Concurrency: 10 threads × 100 ids, asserting **1,000 unique ids**
- Exception chaining: the original `NumberFormatException` survives as `getCause()`
- CSV round-trip against the **real filesystem**, including comma-containing fields

Verified identically on Windows 11 and Alpine Linux — same bytecode, both green. That is the
[Write Once, Run Anywhere](docs/JVM_Report.md#5-write-once-run-anywhere) proof.

---

## Documentation

| Document | Contents |
|---|---|
| **[Setup_Instructions.md](docs/Setup_Instructions.md)** | Three install routes, troubleshooting, guided tour |
| **[JVM_Report.md](docs/JVM_Report.md)** | Class loader, memory areas, JIT, GC, WORA — tied to this codebase |
| **[Design_Decisions.md](docs/Design_Decisions.md)** | Architecture, SOLID, patterns, deviations, limitations |
| **[DB_Design.md](docs/DB_Design.md)** | ER model, CSV schemas, integrity rules, SQL migration path |
| **[Phase plan](docs/phases/README.md)** | Phases 0–16 with tasks, owners and dependency graph |
| **[Team task sheets](docs/team/)** | Per-member checklists and decisions |
| **[Pull request log](docs/pull-requests/)** | Every PR, with rationale and review notes |

---

## Project structure

```
Meditrack/
├── src/main/java/com/airtribe/meditrack/
│   ├── Main.java                     Console UI (878 lines)
│   ├── constants/     (1 file)       Constants with a static block
│   ├── entity/       (13 files)      Domain model + Bill hierarchy
│   ├── exception/     (5 files)      Checked exceptions with chaining
│   ├── factory/       (1 file)       BillFactory
│   ├── interfaces/    (4 files)      Searchable, Payable, BillingStrategy, AppointmentObserver
│   ├── observer/      (3 files)      Console, SMS, Audit channels
│   ├── service/       (5 files)      Business logic
│   ├── strategy/      (3 files)      Pricing policies
│   ├── test/          (1 file)       TestRunner — 325 assertions
│   └── util/          (8 files)      DataStore<T>, Validator, DateUtil, CSVUtil, singletons
├── docs/             (23 files)      Full documentation set
├── Dockerfile                        Multi-stage: JDK build → JRE runtime
├── docker-compose.yml
└── README.md
```

**45 Java files · 8,930 lines · zero dependencies**

---

## Known limitations

Deliberate scope boundaries, documented honestly in
[Design_Decisions §12](docs/Design_Decisions.md#12-known-limitations):

- In-memory storage with file persistence — no database
- No authentication or user roles
- Single-user console application
- `double` is used for money; production billing would need `BigDecimal`
- The AI is rule-based keyword matching — it misses paraphrases like *"my chest hurts"*

---

## Team

| Member | Role | Task sheet |
|---|---|---|
| **Varun P S** | Core entities, OOP, Factory & Strategy | [VARUN.md](docs/team/VARUN.md) |
| **Zubair** | Services, exceptions, Observer, AI | [ZUBAIR.md](docs/team/ZUBAIR.md) |
| **Sunil Kumar B A** | Utils, storage, Singletons, testing, docs, Docker | [SUNIL.md](docs/team/SUNIL.md) |

---

*Built for the Airtribe Java Track.*
 
