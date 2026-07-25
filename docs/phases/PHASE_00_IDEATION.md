# Phase 0 — Ideation & Requirements

| | |
|---|---|
| **Owner** | All three members |
| **Depends on** | Nothing — this is the start |
| **Blocks** | Phases 1 and 2 |
| **Status** | ✅ Complete |

---

## Objective

Agree what MediTrack *is* before writing a line of code: the problem, the users, the scope
boundaries, and how the team will work together.

---

## The problem

A small clinic runs on paper registers and a whiteboard. That produces four recurring failures:

1. **Double-booked doctors** — two receptionists write in the same slot.
2. **Lost patient history** — a paper file that is not in the room might as well not exist.
3. **Billing errors** — tax and concessions computed by hand, differently each time.
4. **No visibility** — nobody can answer "which doctor is busiest?" without counting by hand.

MediTrack addresses all four in a single-user console application.

---

## Users

| Actor | Needs |
|---|---|
| **Receptionist** | Register patients, book/cancel appointments, take payments |
| **Doctor** | See their schedule, review patient history, mark consultations complete |
| **Clinic manager** | Revenue, utilisation and cancellation reporting |

---

## Scope

### In scope
- [x] Patient CRUD with medical history and allergies
- [x] Doctor roster with specialities, fees and ratings
- [x] Appointment booking, confirming, cancelling, rescheduling, completing
- [x] Billing with tax, multiple bill types and pricing policies
- [x] Search across patients, doctors and appointments
- [x] Rule-based triage recommending a doctor from symptoms
- [x] File persistence (CSV + serialization)
- [x] Analytics and reporting
- [x] Menu-driven console UI

### Out of scope — decided deliberately
- [x] Authentication and user roles — no marks attached, significant work
- [x] Networked or multi-user access — assignment is Core Java
- [x] A real database — file persistence is the stated requirement
- [x] Prescription and pharmacy management — scope creep
- [x] A GUI — console is specified

---

## Requirements

### Functional

| ID | Requirement | Phase |
|---|---|---|
| FR-01 | Register, view, update and delete patients | 8 |
| FR-02 | Maintain a doctor roster with specialities and fees | 8 |
| FR-03 | Book an appointment for a patient with a doctor at a slot | 8 |
| FR-04 | Prevent double-booking a doctor | 8 |
| FR-05 | Cancel, reschedule and complete appointments | 8 |
| FR-06 | Generate a bill for a completed appointment | 9 |
| FR-07 | Apply tax and pricing policies (insurance, senior concession) | 9 |
| FR-08 | Accept full and partial payments | 9 |
| FR-09 | Search patients by id, name, age and age range | 8 |
| FR-10 | Search doctors by speciality, fee and experience | 8 |
| FR-11 | Recommend a doctor from described symptoms | 11 |
| FR-12 | Suggest free appointment slots | 11 |
| FR-13 | Persist and restore all data | 10 |
| FR-14 | Report on utilisation, revenue and demographics | 12 |
| FR-15 | Notify on appointment events | 9 |

### Non-functional

| ID | Requirement | How it was met |
|---|---|---|
| NFR-01 | No third-party dependencies | Core Java only; no Maven/Gradle |
| NFR-02 | Runs on Windows, macOS and Linux | Verified: Windows host + Alpine container |
| NFR-03 | Thread-safe id generation | `AtomicInteger`; 1,000-id concurrent test |
| NFR-04 | Invalid data never reaches the domain model | Centralised `Validator` |
| NFR-05 | Bad input must not crash the app | Checked exceptions caught by the UI |
| NFR-06 | Deterministic output across runs | `LinkedHashMap`, `TreeMap`, stable comparators |
| NFR-07 | Every public type documented | JavaDoc throughout |

---

## Team agreements

- [x] Repository created — `VarunPS-eee/Meditrack`
- [x] All members have access
- [x] `main` is protected; no direct pushes
- [x] Branch naming: `feature/*`, `docs/*`, `fix/*`
- [x] Every change lands via PR with at least one approval
- [x] Work split by **package**, not by file, to minimise conflicts
- [x] Task ownership agreed (see below)

### Ownership

| Member | Area |
|---|---|
| **Varun P S** | Core entities, OOP, cloning, Factory pattern |
| **Zubair** | Services, exceptions, Observer, AI feature |
| **Sunil Kumar B A** | Utils, storage, Singletons, testing, documentation, Docker |

---

## Risks identified

| Risk | Mitigation | Outcome |
|---|---|---|
| Merge conflicts on shared files | Split by package; schedule shared files late | Only `Main.java` needed joint work |
| One member blocked waiting on another | Foundation phase (5) first, then parallel tracks | Zubair briefly blocked on 6+7, as planned |
| Scope creep from four bonus features | Rubric requires two; treat the rest as stretch | All four delivered |
| Package naming mistakes | Review early | ⚠️ **Did occur** — see below |

### The one risk that materialised

The initial skeleton used `package main.java.com.airtribe.meditrack.*`, treating the directory
path as part of the package name, and a package literally named `interface` — a reserved word.
Both were caught and corrected (Phase 5). Recorded in
[Design_Decisions.md §4](../Design_Decisions.md#4-deviations-from-the-assignment-skeleton).

**Lesson:** validate the package structure against the rubric *before* filling it with classes.
The fix touched every file in the repository.

---

## Exit criteria

- [x] Problem statement agreed
- [x] Scope boundaries written down, including exclusions
- [x] Functional and non-functional requirements listed
- [x] Ownership assigned
- [x] Git workflow agreed
- [x] Risks identified with mitigations

---

**Next:** [Phase 1 — Environment Setup & JVM](./PHASE_01_SETUP_JVM.md) ·
[Phase index](./README.md)
