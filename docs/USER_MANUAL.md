# MediTrack — User Manual

A complete guide to running and using MediTrack, written for someone who has
never seen it before. Every menu option is documented.

Want to see it working first? The **[Demonstration & Output](./DEMO_OUTPUT.md)**
page is a guided tour with real captured output.

---

## Contents

- [Getting started](#getting-started)
- [Command-line flags](#command-line-flags)
- [How the interface works](#how-the-interface-works)
- [Menu 1 — Patients](#menu-1--patients)
- [Menu 2 — Doctors](#menu-2--doctors)
- [Menu 3 — Appointments](#menu-3--appointments)
- [Menu 4 — Billing](#menu-4--billing)
- [Menu 5 — Search](#menu-5--search)
- [Menu 6 — AI Triage](#menu-6--ai-triage)
- [Menu 7 — Reports & Analytics](#menu-7--reports--analytics)
- [Menu 8 — Data & Settings](#menu-8--data--settings)
- [Menu 9 — OOP Demonstrations](#menu-9--oop-demonstrations)
- [Input rules](#input-rules)
- [Troubleshooting](#troubleshooting)

---

## Getting started

You need **JDK 21 or newer**. There is no build tool, no `npm install`, nothing
to download.

```bash
git clone https://github.com/VarunPS-eee/Meditrack.git
cd Meditrack

# Linux / macOS / Git Bash
javac -encoding UTF-8 -d out $(find src/main/java -name "*.java")

# Windows PowerShell
$s = Get-ChildItem src\main\java -Filter *.java -Recurse | % { $_.FullName }
javac -encoding UTF-8 -d out $s

java -cp out com.airtribe.meditrack.Main --seedDemo
```

**Start with `--seedDemo`.** It gives you a clinic mid-operation rather than a
set of empty menus:

| | |
|---|---|
| **20 doctors** | across all 14 specialities, with ratings and fees |
| **72 patients** | **every one** carrying a real medical history and allergy record |
| **72 appointments** | spread across Pending, Confirmed, Completed and Cancelled |
| **33 bills** | in every payment state — paid, part-paid and outstanding |

The dataset is **deterministic** — the same run produces the same records every
time, so anything you see here you can reproduce. (Timestamps show the real
clock, and emergency bills carry a doubled surcharge outside clinic hours, so
those two figures will differ from the transcripts if you run at another hour.)
Patients are routed to a doctor who actually treats their condition, so the AI
triage and the reports have sensible material to work with.

### With Docker

```bash
docker build -t meditrack:1.0.0 .
docker run -it --rm meditrack:1.0.0 --seedDemo
```

> **The `-it` flag is not optional.** MediTrack is an interactive menu. Without a
> TTY it reads end-of-file and exits at the very first prompt, which looks like a
> crash but is not.

To keep your data between runs, mount a volume:

```bash
docker run -it --rm -v "$(pwd)/data:/data" meditrack:1.0.0 --loadData
```

---

## Command-line flags

| Flag | Effect |
|---|---|
| `--seedDemo` | Start with a populated demo clinic — **recommended first run** |
| `--loadData` | Restore patients, doctors and appointments from `data/` |
| `--runTests` | Run the 325-assertion suite and exit (exit code 1 on failure) |
| `--help` | Print usage and exit |
| *(none)* | Start with an empty clinic |

---

## How the interface works

Pick a number, press Enter. That is the whole interaction model.

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

Three things to know:

1. **`0` always means "back" or "exit."** It never deletes anything.
2. **You return to the main menu after each action.** Sub-menus do not loop, so
   you cannot get lost several levels deep.
3. **Nothing is saved automatically.** Use **Menu 8 → 1** before you quit, or
   your session is lost. This is deliberate — an accidental keystroke should not
   overwrite the clinic's records.

IDs follow a fixed shape: `PAT-0001`, `DOC-0001`, `APT-0001`, `BIL-0001`. Type
them exactly as shown, including the leading zeros.

### If you type the wrong thing

MediTrack tells you what happened rather than silently returning:

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

Typing the *name* of a section works as a hint — the menu will point you at the
right number.

---

## Menu 1 — Patients

```
--- PATIENTS ---
  1. Add patient          5. Add medical history
  2. List patients        6. Add allergy
  3. Update patient       7. View full profile  <-- case sheet
  4. Delete patient
  0. Back
```

| Option | What it does | Notes |
|---|---|---|
| **1. Add patient** | Creates a record and assigns the next `PAT-` id | Name, age, contact, blood group and insurance flag are prompted in turn |
| **2. List patients** | All patients with history and allergies | Age group (`CHILD`/`ADULT`/`SENIOR`) is **derived**, never typed |
| **3. Update patient** | Edit an existing record | Asks for the id first |
| **4. Delete patient** | Removes a patient | Fails cleanly if the id does not exist |
| **5. Add medical history** | Appends a history entry | Commas are safe — they survive the CSV round trip |
| **6. Add allergy** | Appends an allergy | Duplicates are ignored |
| **7. View full profile** | **The complete case sheet for one patient** | Enter a patient id — see below |

### Option 7 — the case sheet

This is the screen to use when you have a patient id and want *everything* about
them. The list view has to fit a patient on two lines, so it joins their medical
history with commas — which stops being readable at three entries. The case sheet
gives you, for one `PAT-` id:

- Demographics, blood group, masked contact, insurance status
- **Which billing policy their attributes will select**, and why
- **Medical history, numbered in order** — each entry a separate clinical event
- Allergies, called out separately
- **Every appointment** with date, doctor, status and recorded symptoms
- **Every bill** with total and balance, plus lifetime billed and outstanding

```
   PATIENT CASE SHEET — Ravi Kumar (PAT-0005)
  ========================================================================
   Age            : 67 (SENIOR)
   Blood group    : O+
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
```

**Contact numbers are masked on display** (`******5670`). The full number is held
in the record; only the presentation is redacted.

**Insurance matters later.** A patient marked insured is billed under
`InsuranceBillingStrategy` automatically — you never pick a pricing policy by
hand.

---

## Menu 2 — Doctors

```
--- DOCTORS ---
  1. Add doctor           5. Rate doctor
  2. List doctors         6. List by speciality
  3. Update doctor        7. View full profile  <-- practice sheet
  4. Delete doctor
  0. Back
```

| Option | What it does | Notes |
|---|---|---|
| **1. Add doctor** | Creates a record, assigns the next `DOC-` id | Speciality is chosen from the `Specialization` enum |
| **2. List doctors** | All doctors with fee, rating and experience | Sorted by rating, best first |
| **3. Update doctor** | Edit fee, rating or experience | |
| **4. Delete doctor** | Removes a doctor | |
| **5. Rate doctor** | Records a rating from 1.0 to 5.0 | Feeds the AI triage ranking |
| **6. List by speciality** | Filter to one speciality | |
| **7. View full profile** | **The complete practice sheet for one doctor** | Enter a doctor id |

### Option 7 — the practice sheet

The mirror of the patient case sheet. For one `DOC-` id it shows the speciality,
fee, experience band and rating; the **symptom keywords AI triage scores them
against**; their **entire caseload** broken down by status and listed in date
order; and the **consultation revenue** earned from completed visits.

"How busy is this doctor and what have they earned" is otherwise only answerable
by reading two separate reports and doing the arithmetic yourself.

### Specialities

MediTrack ships with **14 specialities**: Cardiology, Dermatology, Neurology,
Orthopedics, Pediatrics, Gynecology, ENT, Ophthalmology, Psychiatry, Dentistry,
Pulmonology, Gastroenterology, Endocrinology and General Practice.

Each carries its own **consultation fee** and the **symptom keywords** that AI
triage scores against — so adding a speciality automatically teaches the triage
helper about it, with no lookup table to keep in sync.

---

## Menu 3 — Appointments

```
--- APPOINTMENTS ---
  1. Book appointment       5. Complete appointment
  2. List appointments      6. Reschedule appointment
  3. Confirm appointment    7. Upcoming appointments
  4. Cancel appointment     8. Doctor's free slots
  0. Back
```

### The lifecycle

```
PENDING ──confirm──> CONFIRMED ──complete──> COMPLETED
   │                     │
   └──────cancel─────────┴──> CANCELLED
```

A **COMPLETED** appointment cannot be cancelled, and only a **COMPLETED**
appointment can be billed. The `AppointmentStatus` enum owns these rules, so
every part of the app gets the same answer.

| Option | What it does | Notes |
|---|---|---|
| **1. Book appointment** | Creates an appointment | Needs patient id, doctor id and a slot |
| **2. List appointments** | Everything, with status | |
| **3. Confirm** | `PENDING` → `CONFIRMED` | |
| **4. Cancel** | → `CANCELLED`, releasing the slot | Refused once completed |
| **5. Complete** | → `COMPLETED`, unlocking billing | Prompts for notes |
| **6. Reschedule** | Moves to a new slot | Re-checks every booking rule |
| **7. Upcoming** | Future appointments only | |
| **8. Doctor's free slots** | Available slots for a doctor on a date | Use this before booking |

### Booking rules, checked cheapest-first

A booking is rejected if it is in the **past**, outside **clinic hours**, on a
slot the doctor **already has**, or beyond the doctor's **daily cap**. The checks
run in that order deliberately — an obviously invalid request fails before
anything scans the appointment list.

Each rejection is a clear message, never a stack trace:

```
  Invalid value for 'status' [Pending] — only a COMPLETED appointment can be billed
```

---

## Menu 4 — Billing

```
--- BILLING ---
  1. Generate bill for appointment    4. List bills
  2. Record payment                   5. Unpaid bills
  3. Print bill                       6. Compare billing strategies
  0. Back
```

| Option | What it does | Notes |
|---|---|---|
| **1. Generate bill** | Raises a bill for a **completed** appointment | Asks the bill type: Consultation, Procedure or Emergency |
| **2. Record payment** | Applies a payment | Overpayment, zero and negative amounts are refused |
| **3. Print bill** | Full itemised bill by id | |
| **4. List bills** | Every bill with status | |
| **5. Unpaid bills** | Outstanding balances only | |
| **6. Compare strategies** | Same bill priced three ways, side by side | The clearest way to see Strategy at work |

### You never choose a pricing policy

The policy is **derived from the patient**:

| Patient | Policy applied |
|---|---|
| Insured | Insurance |
| Not insured, 60+ | Senior Citizen |
| Everyone else | Standard |

Insurance is checked **before** the senior concession, because it is contractual
— stacking both would double-discount.

The billing order is fixed and cannot be varied by any bill type:

```
base amount → policy adjustment → surcharge → GST @ 18% → TOTAL
```

Discount before tax, always.

---

## Menu 5 — Search

```
--- SEARCH ---
  1. Patients by keyword     4. Doctors by speciality
  2. Patients by age         5. Doctors under a fee
  3. Patients by age range   6. Appointments by keyword
  0. Back
```

Keyword search is **case-insensitive** and matches across name, id and other
searchable fields. An empty keyword matches everything.

Options 1–3 are the same method name (`searchPatient`) with different parameter
types — **method overloading**, resolved at compile time.

---

## Menu 6 — AI Triage

```
--- AI TRIAGE ---
  1. Recommend a doctor from symptoms
  2. Suggest appointment slots
  3. Screening prompts for a patient
  0. Back
```

Type symptoms in plain English — `chest pain and breathlessness` — and you get a
speciality match, a ranked doctor list with the reasoning shown, and the earliest
free slot.

**How it ranks.** Specialities declare symptom keywords; matches score points.
Doctors are then weighted by speciality fit, rating and experience — with
experience **capped at 20 years**, so a long-serving generalist cannot outrank a
well-matched specialist on longevity alone. Current bookings count *against* a
doctor, spreading patients across the roster.

Severe symptom combinations raise an `** URGENT **` banner.

> ⚠️ **Not a medical device.** This is keyword matching, entirely deterministic,
> with no machine learning and no network call. It must never inform a real
> clinical decision.

---

## Menu 7 — Reports & Analytics

```
--- REPORTS & ANALYTICS (Java Streams) ---
  1. Appointments per doctor      5. Patient demographics
  2. Average fee by speciality    6. Revenue summary
  3. Appointment status breakdown 7. Audit trail
  4. Top rated doctors            8. Notification channels
  0. Back
```

| Option | Shows |
|---|---|
| **1** | Booking load per doctor |
| **2** | Mean consultation fee per speciality |
| **3** | How many appointments in each status |
| **4** | Highest-rated doctors first |
| **5** | Age-band and insurance breakdown |
| **6** | Billed, collected, outstanding and GST totals |
| **7** | Every mutating operation, recorded by the audit observer |
| **8** | Registered notification channels and events dispatched |

Empty data gives you `0.00`, never `NaN` and never a crash.

**Option 7 (Audit trail)** is worth a look — nobody wrote an audit call into the
services. `AuditLogObserver` subscribes to the same events the SMS channel does.

---

## Menu 8 — Data & Settings

```
--- DATA & SETTINGS ---
  1. Save all to CSV        4. Show configuration
  2. Load all from CSV      5. Toggle reminders
  3. Seed demo data         6. Show id counters
  0. Back
```

| Option | What it does |
|---|---|
| **1. Save all to CSV** | Writes everything to `data/`. **Do this before quitting** |
| **2. Load all from CSV** | Restores from `data/`, replacing what is in memory |
| **3. Seed demo data** | Loads the demo clinic |
| **4. Show configuration** | Active settings and JVM uptime |
| **5. Toggle reminders** | Starts/stops the background reminder thread |
| **6. Show id counters** | Current value of each id sequence |

Files land in `data/`, or wherever `MEDITRACK_DATA_DIR` points. They are plain
CSV — open them in any spreadsheet.

**Loading raises the id counters** to match what was imported, so a restored
session never reissues an id that already exists.

---

## Menu 9 — OOP Demonstrations

```
--- OOP DEMONSTRATIONS ---
  1. Dynamic dispatch (polymorphism)
  2. Deep vs shallow copy
  3. Immutability of BillSummary
  4. Singleton identity (eager vs lazy)
  0. Back
```

These change no data — they exist to make language behaviour observable during a
walkthrough. See [DEMO_OUTPUT.md §8](./DEMO_OUTPUT.md#8-oop-demonstrations) for
the actual output and what each one proves.

---

## Input rules

| Input | Rule |
|---|---|
| **Name** | Letters, spaces, apostrophes and hyphens. Not blank |
| **Age** | 0–120, whole number |
| **Contact** | 10 digits starting 6–9. `+91 98765-43210` is normalised automatically |
| **Email** | Optional. Must look like an email if given |
| **Blood group** | One of `A+ A- B+ B- AB+ AB- O+ O-`. Case-insensitive |
| **Date / time** | `yyyy-MM-dd HH:mm`, e.g. `2026-08-01 10:00`. Snapped to the nearest slot |
| **Fee / amount** | Positive number |
| **Rating** | 1.0 to 5.0 |
| **IDs** | Exact, with leading zeros: `PAT-0001` |

Invalid input is **rejected with an explanation and re-prompted**. It never
crashes the application — every domain failure is a checked exception, so the
compiler will not let a handler be forgotten.

---

## Troubleshooting

**The app exits immediately in Docker.**
You omitted `-it`. Without a TTY the app reads EOF at the first prompt. Use
`docker run -it --rm meditrack:1.0.0 --seedDemo`.

**`₹` prints as `?`.**
Your console is not UTF-8. Run with `-Dfile.encoding=UTF-8`. On Windows, also
try `chcp 65001`. The Docker image sets `LANG`, `LC_ALL` and
`JAVA_TOOL_OPTIONS` already — Alpine defaults to the POSIX locale, which is why
they are set explicitly.

**"Only a COMPLETED appointment can be billed."**
Working as designed. Confirm it (3 → 3), then complete it (3 → 5), then bill it.

**My data vanished between runs.**
Nothing saves automatically. Use **Menu 8 → 1** before quitting and start the
next session with `--loadData`.

**`javac` says "file not found" on Windows.**
The `$(find ...)` form is Git Bash syntax. Use the PowerShell variant in
[Getting started](#getting-started).

**The app prints reminder messages while I am typing.**
That is the background reminder thread. It is a **daemon** thread, so it never
prevents the JVM exiting. Turn it off with **Menu 8 → 5**.

---

## Where to go next

| Document | For |
|---|---|
| **[DEMO_OUTPUT.md](./DEMO_OUTPUT.md)** | A guided tour with real output |
| **[Setup_Instructions.md](./Setup_Instructions.md)** | Detailed setup, including IntelliJ |
| **[Design_Decisions.md](./Design_Decisions.md)** | Why the architecture is the way it is |
| **[CODE_QUALITY.md](./CODE_QUALITY.md)** | SonarQube standing and test coverage |
| **[../CONTRIBUTING.md](../CONTRIBUTING.md)** | How to contribute |
