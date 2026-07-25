# Phase 8 — Service Layer

| | |
|---|---|
| **Owner** | **Zubair** |
| **Depends on** | Phases 6 **and** 7 (both must land first) |
| **Blocks** | Phases 9, 11, 12, 13 |
| **Rubric** | Application Logic — **15 pts** · Polymorphism (overloading) |
| **Status** | ✅ Complete |

---

## Objective

Build the business logic: CRUD orchestration, booking rules, billing coordination, and the
overloaded search methods the rubric asks for.

---

## Tasks

### `PatientService`
- [x] `addPatient` with full validation, plus a convenience overload
- [x] `register` for imports (syncs the id counter)
- [x] `findById` / `getById`
- [x] `updatePatient` — null arguments mean "leave unchanged"
- [x] `deletePatient`
- [x] `addMedicalHistory`, `addAllergy`
- [x] **`searchPatient(String)`** — keyword
- [x] **`searchPatient(int)`** — exact age
- [x] **`searchPatient(int, int)`** — age range
- [x] **`searchPatient(String, boolean)`** — name, exact or partial
- [x] `getSeniorCitizens`, `getInsuredPatients`
- [x] `countByAgeGroup`, `getAverageAge`, `getAllKnownAllergies`
- [x] CSV save/load
- [x] `asMap()` for re-linking appointments

### `DoctorService`
- [x] `addDoctor` with validation + convenience overload
- [x] `updateDoctor`, `deleteDoctor`, `rateDoctor`
- [x] **`searchDoctor(String)`** — keyword
- [x] **`searchDoctor(Specialization)`** — speciality
- [x] **`searchDoctor(double)`** — fee ceiling
- [x] **`searchDoctor(Specialization, int)`** — speciality + minimum experience
- [x] `getAvailableDoctors`, `getAvailableBySpecialization`
- [x] `getAverageFeeBySpecialization`, `countBySpecialization`
- [x] `getFeeStatistics`, `getTopRatedDoctors`, `getMostExpensiveDoctor`
- [x] `getUnstaffedSpecializations` — staffing gap report
- [x] CSV save/load

### `AppointmentService`
- [x] `bookAppointment` with the full rule chain
- [x] Reject past slots
- [x] Reject slots outside clinic hours
- [x] Reject unavailable doctors
- [x] **Reject double-booking**
- [x] Enforce the daily cap per doctor
- [x] `confirmAppointment`, `cancelAppointment`, `completeAppointment`
- [x] `rescheduleAppointment` — re-runs availability checks
- [x] All transitions routed through the enum state machine
- [x] `getAppointmentsForPatient` / `ForDoctor` / `getByStatus` / `getUpcomingAppointments`
- [x] `isDoctorBooked`, `countForDoctorOnDate`, `getAvailableSlots`
- [x] `getAppointmentsPerDoctor`, `getStatusBreakdown`, `getBusiestDoctor`
- [x] `getCancellationRate`, `getTotalConsultationRevenue`
- [x] Fires observer events on book / cancel / complete / reschedule
- [x] CSV save/load with re-linking

### `BillingService`
- [x] `generateBillForAppointment` — only `COMPLETED` appointments
- [x] `generateBill` — standalone
- [x] `generateBillWithStrategy` — explicit policy override
- [x] `recordPayment`, `settleInFull`
- [x] `getBillsForPatient`, `getUnpaidBills`, `searchBills`
- [x] `getTotalBilled` / `Collected` / `Outstanding` / `TaxCollected`
- [x] `getRevenueByBillType`, `getCountByPaymentStatus`
- [x] `getHighestBill`, `getAverageBillValue`

---

## Overloading — the rubric requirement

`searchPatient` is overloaded four ways. All four are resolved at **compile time** from the
argument types — that is the distinction from overriding, where the JVM picks at run time.

```java
service.searchPatient("Ravi");           // -> searchPatient(String)
service.searchPatient(34);               // -> searchPatient(int)
service.searchPatient(30, 70);           // -> searchPatient(int, int)
service.searchPatient("Meera", true);    // -> searchPatient(String, boolean)
```

### A deliberate non-overload

`searchPatientById(String)` has a distinct name rather than being a fifth overload. Two methods
differing only in *intent*, not signature, cannot be overloaded — Java chooses by type, not by
what you meant. Documented in the class JavaDoc so it does not look like an oversight.

---

## Booking rule chain

Ordered cheapest-check-first, so an obviously invalid request fails without scanning appointments:

```
1. patient and doctor non-null            -> InvalidDataException
2. slot in the future                     -> InvalidDataException
3. slot within clinic hours               -> InvalidDataException
4. doctor is accepting appointments       -> SlotUnavailableException
5. doctor free at that slot               -> SlotUnavailableException
6. doctor under the daily cap             -> SlotUnavailableException
```

Each is a separate `TestRunner` assertion.

---

## Dependency injection, and why

```java
public AppointmentService(DataStore<Appointment> store, NotificationService notifications)
```

Collaborators arrive through the constructor rather than being fetched from singletons inside
methods. That is what lets `TestRunner` hand in a **fresh store and fresh notification service
per test** — had the service reached for a global, tests would share state and their order would
start to matter.

The no-arg constructor supplies sensible defaults for production use.

---

## Why the service does not call observers directly

`AppointmentService` fires events into `NotificationService` and knows nothing about SMS, audit
logs or console banners:

```java
notificationService.notifyObservers(appointment, NotificationService.EVENT_BOOKED);
```

Were it to call channels directly, adding one would mean editing this class — the exact coupling
Observer exists to remove.

---

## Deliverables

| File | Lines |
|---|---|
| `PatientService.java` | 316 |
| `DoctorService.java` | 302 |
| `AppointmentService.java` | 381 |
| `BillingService.java` | 244 |
| `NotificationService.java` | 258 |

---

## Verification

`TestRunner` suites: *PatientService*, *DoctorService*, *AppointmentService*, *Billing*,
*Observer*.

Sample assertions:
- *"double-booking the same slot is refused"*
- *"cannot complete a PENDING appointment"*
- *"cannot cancel a COMPLETED appointment"*
- *"searchPatient(int,int) — age range"*
- *"a live booking removes its slot from availability"*

---

## Exit criteria

- [x] Full CRUD for patients and doctors
- [x] Booking enforces every clinic rule
- [x] Lifecycle transitions respect the state machine
- [x] Four search overloads per service, each type-resolved
- [x] Events fired for every mutating operation
- [x] **Phases 9, 11, 12, 13 unblocked**

---

**Previous:** [Phase 7](./PHASE_07_UTILS_STORAGE.md) ·
**Next:** [Phase 9 — Design Patterns](./PHASE_09_PATTERNS.md) ·
[Phase index](./README.md) · [Zubair's tasks](../team/ZUBAIR.md)
