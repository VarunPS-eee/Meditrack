# Varun P S — Task Sheet

> **Role:** Core Entities, OOP & Factory
> **Phases owned:** 5 (interfaces), 6 (domain model), 9 (Factory/Strategy/Template Method), 12 (entity streams)
> **Packages owned:** `entity/`, `factory/`, `strategy/`, `interfaces/`
> **Status:** ✅ All assigned work complete

---

## Original assignment (from the team brief)

> Build the core domain models (Person, Doctor, Patient, Appointment, Bill, BillSummary).
> Handle inheritance, abstraction, and the Cloneable interface for deep/shallow copies.
> Implement the Factory Pattern for generating bills.

---

## Task checklist

### Phase 5 — Interfaces
- [x] `Searchable` — 1 abstract + 3 default methods
- [x] `Payable` — 3 abstract + 3 default + 1 static method
- [x] `BillingStrategy` — `@FunctionalInterface`
- [x] `AppointmentObserver` — contract for Zubair's observers
- [x] Fix `interface/` → `interfaces/` (reserved keyword) *(done in commit `71d61f0`)*

### Phase 6 — Domain model
- [x] `MedicalEntity` abstract root
  - [x] Implements `Searchable`, `Serializable`, `Comparable`
  - [x] Immutable `id`, `createdAt`; mutable `updatedAt` via `touch()`
  - [x] **Static block** recording class-load time
  - [x] **Static `AtomicInteger`** instance counter
  - [x] `final equals`/`hashCode` on the business key
- [x] `Person` abstract
  - [x] Constructor chaining `this(...)` → `super(...)`
  - [x] `isSeniorCitizen()`, `getAgeGroup()`, `getMaskedContactNumber()`
- [x] `Patient`
  - [x] `List<String> medicalHistory`, `List<String> allergies`
  - [x] **Deep** `clone()` — copies both lists
  - [x] **Shallow** `shallowCopy()` — kept deliberately, to contrast
  - [x] Unmodifiable views out, defensive copies in
- [x] `Doctor`
  - [x] Static `Comparator` constants: `BY_FEE`, `BY_NAME`, `BY_EXPERIENCE`, `BY_RATING`
  - [x] `getSeniorityBand()`
  - [x] Overrides `getSearchableText()` calling `super`
- [x] `Appointment`
  - [x] **Selective deep copy** — deep-copies patient, shares doctor
  - [x] State transitions via the enum state machine
- [x] `Bill` abstract — **Template Method**
  - [x] `final generateBill()`
  - [x] Abstract `calculateBaseAmount()`
  - [x] Hooks `applySurcharge()`, `calculateTax()`
  - [x] Nested immutable `LineItem`
  - [x] `getFormattedBill()` with a pre-sized `StringBuilder`
- [x] `ConsultationBill`, `ProcedureBill`, `EmergencyBill`
- [x] `BillSummary` — **immutable**, all five rules
- [x] Enums: `Specialization`, `AppointmentStatus`, `BillType`
  - [x] `AppointmentStatus` owns its state machine
  - [x] `Specialization` carries fee + AI symptom keywords

### Phase 9 — Patterns
- [x] `BillFactory` with exhaustive `switch`
- [x] `chooseStrategy(Patient)` — policy from patient attributes
- [x] `StandardBillingStrategy` (Null Object)
- [x] `InsuranceBillingStrategy` — configurable, clamped coverage
- [x] `SeniorCitizenBillingStrategy`

### Phase 12 — Streams in entities
- [x] `Bill.sumLineItems()` via `mapToDouble`
- [x] `Specialization.fromString` / `scoreAgainst` via streams

---

## Notable decisions

### Kept a deliberately-wrong `shallowCopy()`

It would have been tidier to delete it. Keeping it makes the deep/shallow difference
**observable** rather than asserted — mutate the original and watch the two copies diverge, live
in menu 9 → 2 and in the test suite.

### `Appointment` shares its doctor by reference

Deep-copying everything would produce 40 `Doctor` objects for 40 appointments, and a fee update
would leave 39 stale. Deep copy is a judgement about **ownership**: an appointment owns its
patient snapshot; it does not own the doctor.

### `generateBill()` is `final`

The rubric asks for polymorphic `generateBill()`. Template Method delivers that — three
references, one call, three totals — while making it *impossible* for a subclass to forget the
tax step. The billing **sequence** is a business invariant; the **amounts** are not.

### `getClass()` rather than `instanceof` in `equals`

Keeps the relation symmetric across the hierarchy: a `Doctor` and a `Patient` sharing an id are
still not equal. `final` on `equals`/`hashCode` stops a subclass breaking the contract.

---

## Pull requests

| PR | Branch | Scope | Status |
|---|---|---|---|
| #1 | `setup/initial-skeleton` | Project skeleton, package structure, doc stubs | ✅ Merged |
| #2 | *(direct to main)* | Core entities, `Specialization`, relocate `Main.java` | ✅ Merged (`3ec86d0`) |
| #3 | *(direct to main)* | `Bill` and immutable `BillSummary` | ✅ Merged (`159e5bb`) |
| #4 | *(direct to main)* | `Searchable`, `Payable`; fix reserved keyword package | ✅ Merged (`71d61f0`) |
| #5 | `feature/complete-meditrack` | Full build-out — see [PR-005](../pull-requests/PR-005-complete-meditrack.md) | 🟡 Open |

Detailed PR records: [docs/pull-requests/](../pull-requests/)

---

## Files owned

```
src/main/java/com/airtribe/meditrack/
├── entity/          (13 files)  ← primary ownership
├── factory/          (1 file)
├── strategy/         (3 files)
└── interfaces/       (4 files)  ← shared contract, authored here
```

---

## Verification

`TestRunner` suites covering this work:
- *Entities — inheritance, encapsulation, identity* (24 assertions)
- *Cloning — deep vs shallow copy* (14)
- *BillSummary — immutability* (11, including reflection checks)
- *Enums — state machine and lookups* (19)
- *Billing — Factory and Template Method* (27)
- *Strategy* (12)

**Total: 107 assertions against Varun's code. All passing.**

---

*[Phase index](../phases/README.md) · [Zubair](./ZUBAIR.md) · [Sunil](./SUNIL.md)*
