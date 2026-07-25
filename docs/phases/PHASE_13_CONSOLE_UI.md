# Phase 13 — Console UI

| | |
|---|---|
| **Owner** | All three (joint — `Main.java` is the one unavoidable shared file) |
| **Depends on** | Phases 9, 10, 11, 12 |
| **Blocks** | Phase 14 |
| **Rubric** | Application Logic — menu-driven UI |
| **Status** | ✅ Complete |

---

## Objective

Wire everything into a menu-driven console application, with argument parsing and graceful error
handling.

---

## Why this phase was scheduled last

`Main.java` is the only file all three members need to touch. Every other package has a single
owner. Scheduling it after the parallel tracks converged meant it was written once, jointly,
rather than becoming a merge battleground.

---

## Tasks

### Startup
- [x] ASCII banner with version and runtime info
- [x] Command-line parsing: `--loadData`, `--seedDemo`, `--runTests`, `--help`
- [x] `--help` prints usage and exits
- [x] `--runTests` delegates to `TestRunner` and exits
- [x] Service wiring
- [x] Observer registration
- [x] Reminder scheduler started when enabled
- [x] Clean shutdown — scheduler stopped, scanner closed

### Menu structure
- [x] Main menu with nine sections
- [x] **1. Patients** — add, list, update, delete, history, allergy
- [x] **2. Doctors** — add, list, update, delete, rate, by speciality
- [x] **3. Appointments** — book, list, confirm, cancel, complete, reschedule, upcoming, free slots
- [x] **4. Billing** — generate, pay, print, list, unpaid, compare strategies
- [x] **5. Search** — six search modes exercising the overloads
- [x] **6. AI Triage** — recommend, suggest slots, screening prompts
- [x] **7. Reports** — eight analytics views
- [x] **8. Data & Settings** — save, load, seed, config, reminders, id counters
- [x] **9. OOP Demonstrations** — dispatch, copying, immutability, singletons

### Input handling
- [x] `readLine` helper with prompt
- [x] `NumberFormatException` caught at every numeric input
- [x] Checked exceptions caught and printed as friendly messages
- [x] EOF handled — returns `"0"` so piped input terminates cleanly
- [x] Unrecognised menu options re-prompt rather than crash
- [x] Blank input means "skip this field" on update screens

### Output
- [x] `printf` formatting for aligned tables
- [x] `StringBuilder` for multi-part output
- [x] Text blocks for menu literals
- [x] Contact numbers masked in all output
- [x] Empty-state messages ("No patients registered yet.")

---

## The demonstrations menu

Option **9** exists specifically to make abstract OOP concepts observable during a walkthrough:

| Option | Demonstrates |
|---|---|
| 9 → 1 | **Dynamic dispatch** — one `List<MedicalEntity>`, one loop, three different outputs |
| 9 → 2 | **Deep vs shallow copy** — mutate the original, watch the copies diverge |
| 9 → 3 | **Immutability** — attempt to mutate `BillSummary`, catch the exception |
| 9 → 4 | **Singleton identity** — eager vs lazy, with identity hashes |

Sample from 9 → 2:

```
  Original history : [diabetes]
  After mutating the ORIGINAL:
    original : [diabetes, MUTATION-AFTER-COPY]
    deep copy: [diabetes]                        <- unaffected
    shallow  : [diabetes, MUTATION-AFTER-COPY]   <- changed too

  The shallow copy shares the original's list; the deep copy owns its own.
```

---

## Error handling philosophy

The UI catches; the domain throws. Every service call is wrapped:

```java
try {
    Patient patient = patientService.addPatient(name, age, contact, bloodGroup, insured);
    System.out.println("  Registered " + patient.getId());
} catch (NumberFormatException e) {
    System.out.println("  Age must be a whole number.");
} catch (InvalidDataException e) {
    System.out.println("  " + e.getMessage());
}
```

Because the domain exceptions are **checked**, the compiler will not let a handler be forgotten.
The app cannot be crashed by bad input — which is the point of choosing checked exceptions in
Phase 5.

---

## Keeping `Main` thin

`Main.java` reads input, calls a service, prints the result. It contains no business rules:

- No validation — that is `Validator`
- No booking rules — that is `AppointmentService`
- No formatting of bills — that is `Bill.getFormattedBill()`
- No pricing decisions — that is the Factory and Strategy

Without this discipline, console applications reliably become 2,000-line god objects.

---

## Command-line interface

```bash
java -cp out com.airtribe.meditrack.Main --seedDemo    # demo clinic
java -cp out com.airtribe.meditrack.Main --loadData    # restore from data/
java -cp out com.airtribe.meditrack.Main --runTests    # test suite, then exit
java -cp out com.airtribe.meditrack.Main --help        # usage
java -cp out com.airtribe.meditrack.Main               # empty clinic
```

Flags compose: `--loadData --seedDemo` loads then seeds.

---

## Deliverables

| File | Lines |
|---|---|
| `Main.java` | 878 |

---

## Exit criteria

- [x] Every service operation reachable through a menu
- [x] All four CLI arguments work
- [x] No input can crash the application
- [x] Piped input terminates cleanly (used by the verification scripts)
- [x] OOP demonstrations produce observable output
- [x] Scheduler stops on exit — no hanging JVM

---

**Previous:** [Phase 12](./PHASE_12_STREAMS_CONCURRENCY.md) ·
**Next:** [Phase 14 — Testing](./PHASE_14_TESTING.md) ·
[Phase index](./README.md)
