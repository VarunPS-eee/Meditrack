# MediTrack — Demonstration & Output

> Every block on this page is **real captured output** from
> `java -cp out com.airtribe.meditrack.Main --seedDemo`, taken on Java 21.0.11 /
> Windows 11. Nothing here is illustrative or hand-written.

New to the app? Read the **[User Manual](./USER_MANUAL.md)** first — it explains
every menu option. This page is the guided tour.

---

## Contents

- [1. Startup](#1-startup)
- [2. The main menu](#2-the-main-menu)
- [3. Patient records](#3-patient-records)
- [4. The appointment lifecycle](#4-the-appointment-lifecycle)
- [5. Billing — Factory and Strategy in action](#5-billing--factory-and-strategy-in-action)
- [6. AI triage](#6-ai-triage)
- [7. Reports and analytics](#7-reports-and-analytics)
- [8. OOP demonstrations](#8-oop-demonstrations)
- [9. The test suite](#9-the-test-suite)

---

## 1. Startup

```
javac -encoding UTF-8 -d out $(find src/main/java -name "*.java")
java -cp out com.airtribe.meditrack.Main --seedDemo
```

```
[Constants] Static block executed — data directory resolved to: data
[AppConfig] Eager singleton constructed at 2026-07-25 22:13
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
[MedicalEntity] Class loaded and initialised at 2026-07-25T22:13:45.951989500
```

**Read those four lines in order — they are the JVM class loader, observable.**

| Line | What it proves |
|---|---|
| `[Constants] Static block executed` | Static initialiser runs at class initialisation, before any instance exists |
| `[AppConfig] Eager singleton constructed` | Eager holder — built during class init, thread-safe for free |
| `[IdGenerator] Lazy singleton constructed **on first use**` | Initialisation-on-demand holder — deferred until first `getInstance()` |
| `[MedicalEntity] Class loaded` **last** | **Lazy loading.** `MedicalEntity` is the hierarchy root, yet initialises last, because the JVM loads a class on first *active use*, not at startup |

That ordering is not cosmetic. It is the evidence behind
[JVM_Report.md](./JVM_Report.md) §2.

Seeding then fires the observers:

```
    [NOTIFY] BOOKED     APT-0001 — Dr. Anita Rao with Ravi Kumar, 26 Jul 2026, 10:00 am (in 11 hours)
    [SMS -> ******5670] MediTrack: your appointment APT-0001 is booked for 26 Jul 2026, 10:00 am
    [NOTIFY] BOOKED     APT-0002 — Dr. Vikram Nair with Meera Joshi, 26 Jul 2026, 11:00 am (in 12 hours)
    [SMS -> ******5671] MediTrack: your appointment APT-0002 is booked for 26 Jul 2026, 11:00 am
    [NOTIFY] BOOKED     APT-0003 — Dr. Sneha Iyer with Aditya Verma, 27 Jul 2026, 11:00 am (in 1 day)
    [SMS -> ******5672] MediTrack: your appointment APT-0003 is booked for 27 Jul 2026, 11:00 am
    [NOTIFY] BOOKED     APT-0004 — Dr. Priya Sharma with Fatima Sheikh, 27 Jul 2026, 02:00 pm (in 1 day)
    [SMS -> ******5673] MediTrack: your appointment APT-0004 is booked for 27 Jul 2026, 02:00 pm
  Demo data seeded: 6 doctors, 5 patients, 4 appointments.
  [Reminders] Background scheduler started (every 300s, daemon thread).
```

**One booking, two reactions.** `AppointmentService` fired a single event and
knows nothing about SMS or console banners — `NotificationService` fanned it out
to every registered observer. Adding a third channel means writing a class, not
editing the service. Note the contact number is **masked** (`******5670`) at the
point of display.

---

## 2. The main menu

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
  Choose an option:
```

Nine sections. Every one returns here after a single action — there is no nested
loop to get lost in.

---

## 3. Patient records

**Menu 1 → 2 (List patients)**

```
  Patients (5)
  ----------------------------------------------------------------------------
  [PAT-0001] Ravi Kumar  |  Age: 67 (SENIOR)  |  Blood: O+  |  Contact: ******5670  |  Insured: NO
        History  : Hypertension diagnosed 2021
        Allergies: Penicillin
  [PAT-0002] Meera Joshi  |  Age: 34 (ADULT)  |  Blood: A+  |  Contact: ******5671  |  Insured: YES
        History  : Migraine, recurring
  [PAT-0003] Aditya Verma  |  Age: 8 (CHILD)  |  Blood: B+  |  Contact: ******5672  |  Insured: YES
        Allergies: Peanuts
  [PAT-0004] Fatima Sheikh  |  Age: 45 (ADULT)  |  Blood: AB+  |  Contact: ******5673  |  Insured: NO
  [PAT-0005] Karan Singh  |  Age: 72 (SENIOR)  |  Blood: O-  |  Contact: ******5674  |  Insured: NO
```

Two details worth noticing:

- **`SENIOR` / `ADULT` / `CHILD` is derived, never stored.** `getAgeGroup()`
  computes it from age, so it cannot drift out of sync with the record.
- **`Migraine, recurring` contains a comma** and still round-trips through CSV
  intact. `CSVUtil` escapes on write and reverses on read — without that, every
  subsequent column would shift by one.

---

## 4. The appointment lifecycle

An appointment moves `PENDING → CONFIRMED → COMPLETED`, and the enum itself owns
that state machine, so "can this be cancelled?" has exactly one answer.

**Menu 3 → 3 (Confirm)**

```
  Appointment id: APT-0001
  Appointment is now Confirmed.
```

**Menu 3 → 5 (Complete)**

```
  Appointment id: APT-0001
    [NOTIFY] COMPLETED  APT-0001 — Dr. Anita Rao with Ravi Kumar, 26 Jul 2026, 10:00 am
  Appointment is now Completed.
```

The observers fire again — completion is an event like any other.

### Invalid transitions are refused, not crashed

Try to bill an appointment that is still pending:

```
  Appointment id: APT-0001
  Bill types: 1) Consultation  2) Procedure  3) Emergency
  Type: 1
  Invalid value for 'status' [Pending] — only a COMPLETED appointment can be billed
```

A clear domain message, and the app returns to the menu. Because the domain
exceptions are **checked**, the compiler will not let a handler be forgotten —
which is why no input can crash this application.

---

## 5. Billing — Factory and Strategy in action

**Menu 4 → 1 (Generate bill for appointment)** on the now-completed `APT-0001`:

```
==============================================================
              MediTrack — CONSULTATION BILL
  Bill ID    : BIL-0001
  Patient    : Ravi Kumar (PAT-0001)
  Appointment: APT-0001
  Date       : 25 Jul 2026, 10:15 pm
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
```

**This single bill exercises three patterns at once.**

| Line | Pattern | What happened |
|---|---|---|
| `CONSULTATION BILL` | **Factory** | `BillFactory` chose `ConsultationBill` from `BillType`. Its switch is exhaustive, so adding a type without handling it is a *compile* error, not a silently unpriced bill |
| `Policy : Senior Citizen` | **Strategy** | Nobody chose this. Ravi Kumar is 67, so `chooseStrategy()` derived `SeniorCitizenBillingStrategy` from the patient's own attributes |
| `Base → adjustment → GST → TOTAL` | **Template Method** | `generateBill()` is `final`. The *sequence* is a business invariant, so no subclass can discount after tax or skip the tax step |

Check the arithmetic: ₹1,400 base, −₹140 (10% senior concession), = ₹1,260, and
18% GST on ₹1,260 is ₹226.80 → **₹1,486.80**. The discount is applied *before*
tax, which is the correct order and the one the template enforces.

> Had Ravi been insured, `InsuranceBillingStrategy` would have won instead —
> insurance is checked **before** the senior concession because it is
> contractual, and stacking both would double-discount.

---

## 6. AI triage

**Menu 6 → 1**, symptoms: `chest pain and breathlessness`

```
========================================================================
 AI TRIAGE — rule-based recommendation
========================================================================
 Symptoms: chest pain and breathlessness

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
 3. Dr. Vikram Nair          score 11.5
    adjacent speciality (Neurology); rating 4.5, 11y experience, 1 booked
 ========================================================================

 Earliest slot: Dr. Anita Rao at 26 Jul 2026, 09:00 am
```

No machine learning and no network call — **keyword rules declared on the
`Specialization` enum**, plus weighted scoring. It is deterministic and fully
explainable, and the output shows its work: which keywords matched, the
confidence, and why each doctor scored what it did.

Two deliberate scoring rules are visible above:

- **Experience is capped at 20 years.** Dr. Rahul Mehta has 22 years but ranks
  second, because a long-serving generalist must not outrank a well-matched
  specialist on longevity alone.
- **Current bookings are penalised** (`1 booked` counts against), so patients
  spread across the roster instead of piling onto one doctor.

> ⚠️ This is a coursework demonstrator, **not a medical device**. It must never
> be used for real clinical decisions.

---

## 7. Reports and analytics

**Menu 7 → 6 (Revenue summary)** — on a fresh seed, before any billing:

```
    Total billed      : ₹0.00
    Total collected   : ₹0.00
    Outstanding       : ₹0.00
    GST collected     : ₹0.00
    Average bill value: ₹0.00
```

Zero because the demo seed books appointments but raises no bills. The
interesting part is that **it does not divide by zero** — `getAverageBillValue()`
returns `0.0` on an empty store rather than `NaN`, and there is an explicit
assertion for exactly that (`empty store averages to zero without throwing`).

Every report in this section is a **Java Streams pipeline** — `groupingBy`,
`averagingDouble`, `summaryStatistics`, `flatMap` + `distinct`. See
[PHASE_12](./phases/PHASE_12_STREAMS_CONCURRENCY.md).

---

## 8. OOP demonstrations

Menu 9 exists to make abstract concepts *observable* during a walkthrough.

### 9 → 2 — Deep vs shallow copy

```
  Original history : [Hypertension diagnosed 2021]
  After mutating the ORIGINAL:
    original : [Hypertension diagnosed 2021, MUTATION-AFTER-COPY]
    deep copy: [Hypertension diagnosed 2021]   <- unaffected
    shallow  : [Hypertension diagnosed 2021, MUTATION-AFTER-COPY]   <- changed too

  The shallow copy shares the original's list; the deep copy owns its own.
```

The "wrong" copy is kept **on purpose**. Asserting that deep copy works proves
little; watching the shallow copy change alongside the original is what makes
the difference land. This is heap aliasing, visible.

### 9 → 4 — Singleton identity, eager vs lazy

```
  AppConfig (eager) — same instance twice? true
  IdGenerator (lazy) — same instance twice? true
  AppConfig identity hash : 1343441044
  IdGenerator identity hash: 693632176
  Id counters: PAT=5  BIL=0  APT=4  DOC=6
```

Both are singletons; both got there differently. `AppConfig` is **eager** —
thread-safe for free, because the JVM runs class initialisation exactly once
under its own lock. `IdGenerator` is **lazy** via the
initialisation-on-demand-holder idiom — deferred to first use, and *still*
exactly-once, with no `synchronized` anywhere in our code.

The counters confirm the seed: 5 patients, 4 appointments, 6 doctors, 0 bills.

---

## 9. The test suite

```bash
java -cp out com.airtribe.meditrack.Main --runTests
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

325 assertions across 20 suites, in a hand-written runner with no JUnit. The
runner exits non-zero on failure, which is what lets the Docker build gate on it
— **an image cannot be produced from failing code**.

For what this suite does and does not reach, and the current SonarQube standing,
see **[CODE_QUALITY.md](./CODE_QUALITY.md)**.

---

## Reproducing this page

```bash
javac -encoding UTF-8 -d out $(find src/main/java -name "*.java")

# every capture above, in order
printf '1\n2\n0\n0\n'                     | java -cp out com.airtribe.meditrack.Main --seedDemo
printf '3\n3\nAPT-0001\n3\n5\nAPT-0001\nnotes\n4\n1\nAPT-0001\n1\n0\n' \
                                          | java -cp out com.airtribe.meditrack.Main --seedDemo
printf '6\n1\nchest pain and breathlessness\n0\n' \
                                          | java -cp out com.airtribe.meditrack.Main --seedDemo
printf '9\n2\n0\n'                        | java -cp out com.airtribe.meditrack.Main --seedDemo
printf '9\n4\n0\n'                        | java -cp out com.airtribe.meditrack.Main --seedDemo
java -cp out com.airtribe.meditrack.Main --runTests
```

On Windows, add `-Dfile.encoding=UTF-8` so the `₹` symbol renders. In Docker the
`-it` flag is **required** — without a TTY the app reads EOF and exits at the
first prompt.
