# MediTrack — Clinic & Appointment Management System

> A clinic and appointment management system built in **Core Java 21** — no frameworks,
> no third-party dependencies, no build tool. Written to be *read*, not just run.

[![Java](https://img.shields.io/badge/Java-21%20LTS-orange)](https://adoptium.net/temurin/releases/?version=21)
[![Tests](https://img.shields.io/badge/tests-325%2F325%20passing-brightgreen)](#testing)
[![Bugs](https://img.shields.io/badge/SonarQube%20bugs-0-brightgreen)](docs/CODE_QUALITY.md)
[![Docker](https://img.shields.io/badge/docker-286MB-blue)](#run-with-docker)
[![Dependencies](https://img.shields.io/badge/dependencies-zero-lightgrey)](#)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen)](CONTRIBUTING.md)

MediTrack runs a small clinic from a single console menu: patient records,
doctor rosters, appointment booking with real scheduling rules, itemised billing
with GST, and rule-based symptom triage. It persists to plain CSV, ships in a
286 MB container, and has no dependencies to install.

`--seedDemo` starts you in a clinic mid-operation — **20 doctors across 14
specialities, 72 patients each with a real medical history, 72 appointments
spread across every lifecycle state, and 33 bills in every payment state.** The
dataset is deterministic, and patients are routed to a doctor who actually
treats their condition, so the reports and the triage engine have sensible
material to work with. Ask for any patient id and you get their whole case sheet:
numbered history, allergies, every appointment, every bill.

It exists for two audiences. **Use it** as a working clinic manager, or **read
it** as a worked example of object-oriented design in plain Java — the Factory,
Strategy, Observer, Singleton and Template Method patterns all doing real work
rather than sitting in a tutorial. Menu 9 exists purely to make those concepts
observable while the program runs.

**Open source under [MIT](LICENSE) · [Contributions welcome](CONTRIBUTING.md)**

**Airtribe Java Track** · Team: [Sunil Kumar B A](docs/team/SUNIL.md) ·
[Varun P S](docs/team/VARUN.md) · [Zubair](docs/team/ZUBAIR.md) ·
[Anushtha Sharma](docs/team/ANUSHTHA.md)

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
| **Patients** | Register, update, delete, medical history, allergies, blood group, insurance — plus a **full case sheet** per patient id |
| **Doctors** | Roster across **14 specialities** with fees, experience, ratings, availability — plus a **practice sheet** per doctor id |
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

    [NOTIFY] BOOKED     APT-0001 — Dr. Sunil Kumar B A with B A Sunil Kumar, 26 Jul 2026, 09:00 am (in 9 hours)
    [SMS -> ******0001] MediTrack: your appointment APT-0001 is booked for 26 Jul 2026, 09:00 am
    [NOTIFY] BOOKED     APT-0003 — Dr. Farah Khan with Ahmed Zubair, 28 Jul 2026, 09:00 am (in 2 days)
    ...
  Demo clinic ready — 20 doctors across 14 specialities, 72 patients, 72 appointments, 33 bills.
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

**45 Java files · 9,742 lines · zero dependencies**

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

## Code quality

Measured, not asserted — SonarQube 26.7 Community and JaCoCo 0.8.12:

| Bugs | Vulnerabilities | Duplication | Maintainability | Tests |
|:---:|:---:|:---:|:---:|:---:|
| **0** 🟢 A | **0** 🟢 A | **0.0%** | 🟢 A | **325/325** |

Thirteen bugs were found and fixed in the last pass, taking reliability from **E
to A** — including a leaked `ExecutorService`, uptime measured off a wall clock
that could run backwards, and DST-unsafe duration maths that would have put
reminders an hour out twice a year.

Coverage is **50.6%** instruction overall — **62.7%** excluding the interactive `Main` loop — and the gaps are documented
rather than hidden. **[CODE_QUALITY.md](docs/CODE_QUALITY.md)** has the full
breakdown, every bug explained, and the reproduction commands.

---

## Team

Four roles, each owning a vertical slice of the SDLC. Ownership is by **package,
not by file** — the choice that kept merge conflicts near zero.

| Member | Role | Owns | Sheet |
|---|---|---|---|
| **Sunil Kumar B A** | System Design & Data Architecture | HLD/LLD, DB design, `util/`, persistence, testing, Docker, docs | [SUNIL.md](docs/team/SUNIL.md) |
| **Varun P S** | Core Domain Engineering | `entity/`, `factory/`, `strategy/`, `interfaces/` | [VARUN.md](docs/team/VARUN.md) |
| **Zubair** | Services & Integration Engineering | `service/`, `observer/`, `exception/`, analytics | [ZUBAIR.md](docs/team/ZUBAIR.md) |
| **Anushtha Sharma** | Experience & Interface Design | Menu flow, prompt wording, user manual, demo walkthrough | [ANUSHTHA.md](docs/team/ANUSHTHA.md) |

**[TEAM_AND_WORKFLOW.md](docs/TEAM_AND_WORKFLOW.md)** has the dependency graph,
the topological build order, where the parallel tracks ran, and a worked example
tracing one bill through all four people's code.

---

## Contributing

**MediTrack is open source under the [MIT License](LICENSE), and contributions
are welcome** — from a typo fix to a new billing strategy.

New to open source? Issues tagged
[`good first issue`](../../issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22)
are scoped to be finishable in an evening.

| | |
|---|---|
| 🛠️ **[Contributing guide](CONTRIBUTING.md)** | Setup, conventions, PR process |
| 🤝 **[Code of conduct](CODE_OF_CONDUCT.md)** | How we work together |
| 🔒 **[Security policy](SECURITY.md)** | Private disclosure — never file security issues publicly |
| 🐛 **[Report a bug](../../issues/new?template=bug_report.yml)** | Include the exact menu path |
| ✨ **[Request a feature](../../issues/new?template=feature_request.yml)** | |

Three ground rules define what this project is:

1. **Zero third-party runtime dependencies.** JDK standard library only
2. **The test suite stays green.** It gates the Docker build
3. **No real patient data. Ever.** Not in code, fixtures, issues or screenshots

---

## License

[MIT](LICENSE) © 2026 Sunil Kumar B A, Varun P S, Zubair, Anushtha Sharma

---

*Built for the Airtribe Java Track — and released as open source for anyone
learning Core Java, OOP and design patterns.*
