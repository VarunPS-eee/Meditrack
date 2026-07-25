# Phase 6 — Domain Model

| | |
|---|---|
| **Owner** | **Varun P S** |
| **Depends on** | Phase 5 |
| **Runs parallel with** | Phase 7 (Sunil) |
| **Blocks** | Phases 8, 9 |
| **Rubric** | Core OOP — **35 pts** (encapsulation 8, inheritance 10, polymorphism 7, abstraction 10) |
| **Status** | ✅ Complete |

---

## Objective

Build the entity layer: the inheritance hierarchy, the bill hierarchy, cloning semantics,
immutability and the enums. This is the largest single rubric block in the project.

---

## Tasks

### Hierarchy
- [x] `MedicalEntity` abstract root implementing `Searchable`, `Serializable`, `Comparable`
- [x] `Person` abstract, extending `MedicalEntity`
- [x] `Patient` extending `Person`, implementing `Cloneable`
- [x] `Doctor` extending `Person`
- [x] `Appointment` extending `MedicalEntity`, implementing `Cloneable`
- [x] `Bill` abstract, extending `MedicalEntity`, implementing `Payable`

### Encapsulation (8 pts)
- [x] Every field `private`
- [x] Getters and setters throughout
- [x] Setters call `touch()` so audit timestamps stay honest
- [x] Collections returned as **unmodifiable views**
- [x] Defensive copies on collection inputs
- [x] `getMaskedContactNumber()` so screenshots do not leak numbers

### Inheritance (10 pts)
- [x] Three-level hierarchy: `MedicalEntity` → `Person` → `Patient`/`Doctor`
- [x] `super(...)` used in every subclass constructor
- [x] **Constructor chaining** with `this(...)` in `Person`, `Doctor`, `Patient`, `Appointment`
- [x] `protected` constructors on abstract classes
- [x] Convenience constructors delegating to canonical ones

### Polymorphism (7 pts)
- [x] **Overriding** — `displayDetails()` in `Patient`, `Doctor`, `Appointment`, `Bill`
- [x] **Overriding** — `getSearchableText()` calling `super.getSearchableText()`
- [x] **Overriding** — `getBillDescription()` per bill type
- [x] **Overriding** — `applySurcharge()` in `EmergencyBill`
- [x] **Overloading** — `Bill.addLineItem()` ×3
- [x] **Overloading** — constructors across every entity
- [x] **Dynamic dispatch demo** — `List<MedicalEntity>` iterated in the app's menu 9→1

### Abstraction (10 pts)
- [x] `MedicalEntity` abstract with abstract `displayDetails()`, `getEntityType()`
- [x] `Person` abstract — a bare "person" is not actionable
- [x] `Bill` abstract with abstract `calculateBaseAmount()`
- [x] `Searchable` and `Payable` implemented by the right types only

### Advanced OOP
- [x] **Deep copy** — `Patient.clone()` copies both mutable lists
- [x] **Shallow copy** — `Patient.shallowCopy()` kept deliberately, to contrast
- [x] **Selective deep copy** — `Appointment.clone()` deep-copies patient, shares doctor
- [x] **Immutable class** — `BillSummary`, all five rules applied
- [x] **Enums with behaviour** — `Specialization`, `AppointmentStatus`, `BillType`
- [x] **State machine in an enum** — `AppointmentStatus.canTransitionTo()`
- [x] **Static block** — `MedicalEntity` records class-load time
- [x] **Static counter** — `AtomicInteger` shared across all instances
- [x] **`equals`/`hashCode`** — on the business key, `final` to keep the hierarchy consistent
- [x] **`Comparable`** — natural order by id
- [x] **`Comparator` constants** — `Doctor.BY_FEE`, `BY_NAME`, `BY_EXPERIENCE`, `BY_RATING`

### Bill hierarchy (Template Method)
- [x] `Bill.generateBill()` `final` — the algorithm's order is invariant
- [x] `calculateBaseAmount()` abstract
- [x] `applySurcharge()` and `calculateTax()` as overridable hooks
- [x] `ConsultationBill` — fee + registration on first visit
- [x] `ProcedureBill` — base from line items
- [x] `EmergencyBill` — overrides the surcharge hook
- [x] `Bill.LineItem` nested immutable value type
- [x] `getFormattedBill()` using a single pre-sized `StringBuilder`

---

## Design highlights

### Deep vs shallow, made observable

The project keeps a deliberately-wrong `shallowCopy()` so the difference can be *seen*:

```java
original.addMedicalHistoryEntry("NEW ENTRY");
// original : 2 entries
// deep copy: 1 entry    <- unaffected
// shallow  : 2 entries  <- shares the list, changed too
```

Run it yourself: menu **9 → 2**.

### Appointment shares its doctor on purpose

`Appointment.clone()` deep-copies the patient but **shares** the doctor reference. A doctor is
one clinic-wide entity; cloning them per appointment would produce 40 copies for 40 appointments
and leave 39 stale when a fee changed.

Deep copy is a judgement about **ownership**, not a blanket rule.

### `equals`/`hashCode` are `final`

```java
@Override
public final boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;   // not instanceof
    ...
}
```

`getClass()` rather than `instanceof` keeps the relation symmetric — a `Doctor` and a `Patient`
sharing an id are still not equal. `final` prevents a subclass from breaking the contract.

### The enum owns its state machine

```java
public boolean canTransitionTo(AppointmentStatus target) {
    if (target == null || this == target || this.terminal) return false;
    return switch (this) {
        case PENDING   -> Set.of(CONFIRMED, CANCELLED).contains(target);
        case CONFIRMED -> Set.of(COMPLETED, CANCELLED, NO_SHOW).contains(target);
        default        -> false;
    };
}
```

Putting the rule here means there is exactly one answer to "can this be cancelled?", rather than
one per service that happens to ask.

---

## Deliverables

| File | Purpose |
|---|---|
| `MedicalEntity.java` | Abstract root, static block, identity |
| `Person.java` | Abstract, constructor chaining |
| `Patient.java` | Deep + shallow copy |
| `Doctor.java` | Comparators, seniority bands |
| `Appointment.java` | Selective deep copy, state transitions |
| `Bill.java` | Template Method, `LineItem`, formatted output |
| `ConsultationBill` / `ProcedureBill` / `EmergencyBill` | Concrete bill types |
| `BillSummary.java` | Immutable snapshot |
| `Specialization` / `AppointmentStatus` / `BillType` | Enums with behaviour |

---

## Verification

Covered by these `TestRunner` suites:
- *Entities — inheritance, encapsulation, identity*
- *Cloning — deep vs shallow copy*
- *BillSummary — immutability* (including reflection checks that the class is `final`, all
  fields are `final`, and no setters exist)
- *Enums — state machine and lookups*
- *Billing — Factory and Template Method*

---

## Exit criteria

- [x] Full hierarchy compiles and behaves
- [x] Deep vs shallow copy demonstrably different
- [x] `BillSummary` immutability verified by reflection
- [x] Enum state machine rejects illegal transitions
- [x] Dynamic dispatch demonstrated in a live menu option
- [x] **Zubair unblocked for Phase 8**

---

**Previous:** [Phase 5](./PHASE_05_FOUNDATION.md) ·
**Next:** [Phase 7 — Utils & Storage](./PHASE_07_UTILS_STORAGE.md) ·
[Phase index](./README.md) · [Varun's tasks](../team/VARUN.md)
