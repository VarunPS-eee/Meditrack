# Zubair — Task Sheet

> **Role:** Services, Logic, Observer & AI
> **Phases owned:** 5 (exceptions), 8 (services), 11 (AI), 12 (service analytics)
> **Packages owned:** `service/`, `observer/`, `exception/`, `util/AIHelper.java`
> **Status:** ✅ All assigned work complete

---

## Original assignment (from the team brief)

> Build DoctorService, PatientService, and AppointmentService.
> Create custom exceptions (AppointmentNotFoundException, InvalidDataException).
> Bonus 1 (Observer): Implement console reminders for appointments.
> Bonus 2 (AI Feature): Build AIHelper.java for rule-based doctor recommendations and slot
> suggestions.

---

## Task checklist

### Phase 5 — Exceptions
- [x] `InvalidDataException` — field name, rejected value, **chaining**
- [x] `AppointmentNotFoundException` — captures the id
- [x] `EntityNotFoundException` — for the generic store
- [x] `SlotUnavailableException` — doctor + slot + reason
- [x] `DataPersistenceException` — wraps `IOException`
- [x] All **checked**, all with `serialVersionUID`
- [x] Chaining constructors on every type

### Phase 8 — Services
- [x] `PatientService`
  - [x] Full CRUD with validation
  - [x] **`searchPatient(String)`** — keyword
  - [x] **`searchPatient(int)`** — exact age
  - [x] **`searchPatient(int, int)`** — age range
  - [x] **`searchPatient(String, boolean)`** — name, exact or partial
  - [x] `addMedicalHistory`, `addAllergy`
  - [x] Analytics: age groups, average age, allergies via `flatMap`
  - [x] CSV save/load, `asMap()` for re-linking
- [x] `DoctorService`
  - [x] Full CRUD, `rateDoctor`
  - [x] Four `searchDoctor` overloads
  - [x] `getAvailableBySpecialization`
  - [x] Analytics: average fee by speciality, fee statistics, top-rated, staffing gaps
- [x] `AppointmentService`
  - [x] `bookAppointment` with the full rule chain
  - [x] Reject past slots, out-of-hours, unavailable doctors
  - [x] **Reject double-booking**
  - [x] Enforce the daily cap
  - [x] Confirm / cancel / complete / reschedule via the state machine
  - [x] `getAvailableSlots`, `isDoctorBooked`, `countForDoctorOnDate`
  - [x] Analytics: per doctor, status breakdown, busiest, cancellation rate
  - [x] Fires observer events on every mutation
- [x] `BillingService`
  - [x] Bill generation — only `COMPLETED` appointments
  - [x] Explicit-strategy override
  - [x] Payment recording with balance validation
  - [x] Revenue analytics

### Phase 9 (shared) — Observer
- [x] `NotificationService` — the Subject
  - [x] `CopyOnWriteArrayList` for observers
  - [x] `AtomicInteger` event counter
  - [x] Per-observer try/catch — one failure cannot break the rest
  - [x] `synchronized` reminder sweep
  - [x] `Timer` + `TimerTask` on a **daemon** thread
- [x] `ConsoleReminderObserver`
- [x] `SmsReminderObserver` — selective subscription, masked numbers
- [x] `AuditLogObserver` — `record`-based entries, unmodifiable trail

### Phase 11 — AI feature
- [x] `recommendSpecialities` — every speciality scored
- [x] `recommendSpecialization` — best match, with General Practice fallback
- [x] `isUrgent` — urgency keyword detection
- [x] `recommendDoctors` — weighted scoring
- [x] Experience **capped at 20 years** so seniority cannot dominate
- [x] Load penalty spreads patients across the roster
- [x] `suggestSlots`, `suggestEarliestAppointment`
- [x] `buildTriageReport` — explainable output with matched keywords
- [x] `suggestScreeningPrompts`
- [x] `SpecialityMatch` / `DoctorRecommendation` records

---

## Notable decisions

### `searchPatientById` is *not* a fifth overload

Two methods differing only in *intent*, not signature, cannot be overloaded — Java resolves by
type, not by what you meant. Given `searchPatient(String)` already exists for keyword search, the
id lookup needed a distinct name. Documented in the class JavaDoc so it does not read as an
oversight.

### Booking checks are ordered cheapest-first

```
null checks -> future? -> clinic hours? -> doctor available? -> slot free? -> under cap?
```

An obviously invalid request fails before anything scans the appointment list.

### The service never calls observers directly

`AppointmentService` fires an event and knows nothing about SMS, audit or console output. Were it
to call channels directly, adding one would mean editing this class — the exact coupling Observer
removes.

### Rule-based AI, chosen not merely accepted

No ML libraries were permitted, but rule-based is also the *right* answer for clinical triage:
every recommendation is explainable ("matched: chest pain, breathless") and auditable. A black
box recommending a cardiologist with no stated reason is not usable in a medical setting.

The honest limitation — keyword matching misses paraphrases like *"my chest hurts"* — is recorded
in [Design_Decisions §12](../Design_Decisions.md#12-known-limitations).

### Observers fail in isolation

```java
try { observer.update(appointment, eventType); }
catch (RuntimeException e) { /* warn, continue to the next channel */ }
```

A broken SMS gateway must not prevent the audit log from recording, and must certainly not fail
the booking that triggered it.

---

## Pull requests

| PR | Branch | Scope | Status |
|---|---|---|---|
| — | `feature/services-and-ai` | *Originally assigned branch — never created* | ⬜ |
| #5 | `feature/complete-meditrack` | Services, exceptions, observers, AI — see [PR-005](../pull-requests/PR-005-complete-meditrack.md) | 🟡 Open |

> **Note:** Zubair's assigned branch was never pushed. This work was completed as part of the
> consolidated build-out. Review from Zubair is the important next step — see
> [PR-005](../pull-requests/PR-005-complete-meditrack.md).

---

## Files owned

```
src/main/java/com/airtribe/meditrack/
├── service/          (5 files)  ← primary ownership
├── observer/         (3 files)
├── exception/        (5 files)
└── util/AIHelper.java (1 file)
```

---

## Verification

`TestRunner` suites covering this work:
- *PatientService — CRUD and overloaded search* (15 assertions)
- *DoctorService — roster and analytics* (15)
- *AppointmentService — booking rules and lifecycle* (18)
- *Observer — notification fan-out* (15)
- *AIHelper — rule-based triage* (22)
- *Exceptions — custom types and chaining* (14)

**Total: 99 assertions against Zubair's code. All passing.**

---

*[Phase index](../phases/README.md) · [Varun](./VARUN.md) · [Sunil](./SUNIL.md)*
