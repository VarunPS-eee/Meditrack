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

Seeding then fires the observers — 110 events in all:

```
    [NOTIFY] BOOKED     APT-0001 — Dr. Sunil Kumar B A with B A Sunil Kumar, 26 Jul 2026, 09:00 am (in 9 hours)
    [SMS -> ******0001] MediTrack: your appointment APT-0001 is booked for 26 Jul 2026, 09:00 am
    [NOTIFY] BOOKED     APT-0002 — Dr. Rahul Mehta with P S Varun, 27 Jul 2026, 09:00 am (in 1 day)
    [SMS -> ******0002] MediTrack: your appointment APT-0002 is booked for 27 Jul 2026, 09:00 am
    [NOTIFY] BOOKED     APT-0003 — Dr. Farah Khan with Ahmed Zubair, 28 Jul 2026, 09:00 am (in 2 days)
    [SMS -> ******0003] MediTrack: your appointment APT-0003 is booked for 28 Jul 2026, 09:00 am
    ...
  Demo clinic ready — 20 doctors across 14 specialities, 72 patients, 72 appointments, 33 bills.
  Every patient carries a medical history. Try: Menu 1 -> 2, then Menu 5 -> 1 to search.
  [Reminders] Background scheduler started (every 300s, daemon thread).
```

**One booking, two reactions.** `AppointmentService` fired a single event and
knows nothing about SMS or console banners — `NotificationService` fanned it out
to every registered observer. Adding a third channel means writing a class, not
editing the service. Note the contact number is **masked** (`******0001`) at the
point of display.

Three details in those three lines are worth pausing on:

- **The project team appears twice, in two formats.** `Dr. Sunil Kumar B A` is
  the doctor; `B A Sunil Kumar` is the patient. Same person, deliberately
  distinct renderings, so the two records are never mistaken for each other.
- **Patients are routed to a doctor who treats their condition.** Zubair's record
  carries "mild asthma diagnosed 2019", and he is booked with Dr. Farah Khan —
  *Pulmonology*. Nobody hard-coded that pairing; the seeder matches each
  patient's condition against the speciality roster.
- **The dataset is deterministic.** The seeder's `Random` takes a fixed seed, so
  this output is byte-identical on every run. A demo that reshuffles cannot be
  documented, screenshotted or asserted against.

### What the seed contains

| | |
|---|---|
| **20 doctors** | all 14 specialities covered, with ratings and fees |
| **72 patients** | **every one** with a medical history and an allergy record |
| **72 appointments** | Pending 13 · Confirmed 21 · Completed 33 · Cancelled 5 |
| **33 bills** | across all three bill types, in every payment state |

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

### Wrong input is explained, not swallowed

```
  "patients" is not a main menu option.
  Did you mean 1. Patients? Enter 1.

  "99" is not a main menu option.
  The main menu has options 0 to 9.
  Not sure where to start? 1 lists patients, 7 shows reports.

  "77" is not an option on the Patients menu.
  This menu goes up to 6. Enter 0 to go back.

  "quit" is not a main menu option.
  To leave MediTrack, enter 0.
  Unsaved changes are lost on exit — save first with 8 -> 1.
```

Sub-menus used to drop silently back to the caller on unrecognised input, which
is **indistinguishable from a successful "back"** — the user cannot tell whether
the keystroke was rejected or obeyed. Naming what was typed, and what the valid
range is, turns a dead end into a hint.

People type the *name* of what they want more often than a wrong number, so the
main menu matches the input against its section names first and answers the
question actually being asked.

---

## 3. Patient records

**Menu 1 → 2 (List patients)** lists all 72, two lines each:

```
  [PAT-0005] Ravi Kumar  |  Age: 67 (SENIOR)  |  Blood: O+  |  Contact: ******0005  |  Insured: NO
        History  : Hypertension diagnosed 2021, on amlodipine, Coronary angiogram 2023 — mild stenosis, medically managed, Cholesterol elevated, statin started 2024
        Allergies: Penicillin
```

Two details worth noticing:

- **`SENIOR` / `ADULT` / `CHILD` is derived, never stored.** `getAgeGroup()`
  computes it from age, so it cannot drift out of sync with the record.
- **`Coronary angiogram 2023 — mild stenosis, medically managed` contains
  commas** and still round-trips through CSV intact. `CSVUtil` escapes on write
  and reverses on read — without that, every subsequent column would shift.

But look at that history line. Three separate clinical events, comma-joined into
one unreadable run because the list view has two lines per patient to work with.
That is what **Menu 1 → 7** exists to solve.

### Menu 1 → 7 — the patient case sheet

```
   PATIENT CASE SHEET — Ravi Kumar (PAT-0005)
  ========================================================================
   Age            : 67 (SENIOR)
   Blood group    : O+
   Contact        : ******0005
   Insurance      : Self-paying
   Billing policy : Senior Citizen concession

   MEDICAL HISTORY
   ----------------------------------------------------------------------
   1. Hypertension diagnosed 2021, on amlodipine
   2. Coronary angiogram 2023 — mild stenosis, medically managed
   3. Cholesterol elevated, statin started 2024

   ALLERGIES
   ----------------------------------------------------------------------
   Penicillin

   APPOINTMENTS (1)
   ----------------------------------------------------------------------
   APT-0005  30 Jul 2026, 09:00 am   Dr. Varun P S           Confirmed
       symptoms: chest pain, heart

   BILLS (0)
   ----------------------------------------------------------------------
   (none raised)
  ========================================================================
```

Same three history entries, now **numbered and separate** — they are distinct
clinical events and the screen says so. The sheet also answers the questions the
list cannot: which billing policy this patient's own attributes will select and
why, every appointment with its symptoms, every bill with the running balance.

Note the routing: Ravi's history is cardiac, and he is booked with **Dr. Varun P S
— Cardiology**.

### Menu 2 → 7 — the doctor practice sheet

The mirror image, for a `DOC-` id:

```
   PRACTICE SHEET — Dr. Varun P S (DOC-0002)
  ========================================================================
   Speciality     : Cardiology
   Consultation   : ₹1,500.00
   Experience     : 13 years (SENIOR)
   Rating         : 4.8 / 5.0

   TREATS (symptom keywords used by AI triage)
   ----------------------------------------------------------------------
   chest pain, heart, palpitation, breathless, bp, blood pressure, cholesterol

   CASELOAD (5 appointments)
   ----------------------------------------------------------------------
   Pending      2
   Confirmed    3

   APT-0051  26 Jul 2026, 09:00 am   Kabir Verma         Confirmed
   APT-0022  27 Jul 2026, 09:00 am   Leela Menon         Confirmed
   APT-0053  28 Jul 2026, 09:00 am   Saanvi Kaur         Pending
   APT-0005  30 Jul 2026, 09:00 am   Ravi Kumar          Confirmed
   APT-0070  30 Jul 2026, 10:00 am   Rekha Sharma        Pending
```

The **TREATS** line is not a hard-coded string — it is the speciality's own
symptom keywords, the same list AI triage scores against. Add a keyword to the
enum and it appears here automatically.

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

**Menu 7 → 3 (Appointment status breakdown)**

```
  Appointment status breakdown
    Pending      13
    Confirmed    21
    Completed    33
    Cancelled    5
    Cancellation rate: 6.9%
```

**Menu 7 → 6 (Revenue summary)**

```
  Revenue summary
    Total billed      : ₹34,735.66
    Total collected   : ₹22,924.75
    Outstanding       : ₹11,810.92
    GST collected     : ₹5,298.66
    Average bill value: ₹1,052.60
    By bill type:
      Consultation Bill    ₹26,029.62
      Emergency Bill       ₹4,545.36
      Procedure Bill       ₹4,160.68
```

The seed settles bills **unevenly on purpose** — roughly half in full, a third
partially, the rest untouched. A dataset where every bill is paid makes both the
"unpaid bills" screen and the outstanding-revenue figure useless, and those are
exactly the screens worth seeing populated.

It also **does not divide by zero** on an empty clinic — `getAverageBillValue()`
returns `0.0` rather than `NaN`, and there is an explicit assertion for exactly
that (`empty store averages to zero without throwing`).

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
