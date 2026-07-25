# Phase 3 — Low-Level Design

| | |
|---|---|
| **Owner** | All three members |
| **Depends on** | Phase 2 |
| **Blocks** | Phases 4 and 5 |
| **Status** | ✅ Complete |

---

## Objective

Turn the layer diagram into signatures: exact classes, fields, methods and contracts, so three
people can implement in parallel and have the pieces fit on first merge.

---

## Class inventory

### `entity/` — Varun

| Class | Kind | Key design point |
|---|---|---|
| `MedicalEntity` | abstract | Root; `final equals`/`hashCode` on id; static counter + static block |
| `Person` | abstract | Constructor chaining via `this(...)` → `super(...)` |
| `Patient` | concrete | `Cloneable`; **both** deep `clone()` and `shallowCopy()` |
| `Doctor` | concrete | Static `Comparator` constants |
| `Appointment` | concrete | `Cloneable`; selective deep copy; state machine |
| `Bill` | abstract | **Template Method** — `final generateBill()` |
| `ConsultationBill` | concrete | Overrides `calculateBaseAmount()` |
| `ProcedureBill` | concrete | Base amount from line items |
| `EmergencyBill` | concrete | Overrides the `applySurcharge()` hook |
| `BillSummary` | **immutable** | `final` class, `final` fields, defensive copies |
| `Specialization` | enum | Carries fee + symptom keywords |
| `AppointmentStatus` | enum | Owns its own state machine |
| `BillType` | enum | Factory discriminator |

### `util/` — Sunil

| Class | Kind | Key design point |
|---|---|---|
| `DataStore<T extends MedicalEntity>` | generic | `Iterable`; bounded type parameter |
| `Validator` | static utility | Every rule, one place |
| `DateUtil` | static utility | Thread-safe `DateTimeFormatter` constants |
| `CSVUtil` | static utility | try-with-resources; escaping |
| `IdGenerator` | **lazy singleton** | Holder idiom + `AtomicInteger` |
| `AppConfig` | **eager singleton** | `static final` instance |
| `AIHelper` | static utility | Rule-based scoring |

### `service/` — Zubair

| Class | Key design point |
|---|---|
| `PatientService` | **Four `searchPatient` overloads** |
| `DoctorService` | Stream analytics |
| `AppointmentService` | Booking rules; fires observer events |
| `BillingService` | Strategy context; calls the factory |
| `NotificationService` | Observer subject + `TimerTask` scheduler |

---

## Interface contracts

Fixed in this phase so implementers could code against them before they existed.

```java
public interface Searchable {
    String getSearchableText();                        // abstract
    default boolean matches(String keyword);           // default
    default boolean matchesAll(String... keywords);
    default boolean matchesAny(String... keywords);
}

public interface Payable {
    double getAmountDue();
    boolean processPayment(double amount);
    double getAmountPaid();
    default boolean isFullyPaid();
    default String getPaymentStatus();
    static String formatCurrency(double amount);       // static
}

@FunctionalInterface
public interface BillingStrategy {
    double calculate(double baseAmount);
    default String getStrategyName();
}

public interface AppointmentObserver {
    void update(Appointment appointment, String eventType);
    default boolean isInterestedIn(String eventType);
}
```

`Payable` deliberately carries all three method kinds — abstract, `default` and `static` — as
the reference example of a modern Java interface.

---

## Tasks

### Class design
- [x] Every class, its kind, and its responsibility listed
- [x] Field lists with access modifiers
- [x] Method signatures fixed before implementation
- [x] Abstract vs concrete decided per class
- [x] `final` applied where subclassing would break an invariant

### Contracts
- [x] Four interfaces designed with default/static methods
- [x] Exception hierarchy designed (5 checked types)
- [x] Generic bounds decided (`T extends MedicalEntity`)

### OOP requirements
- [x] **Encapsulation** — private fields, validated setters, unmodifiable collection views
- [x] **Inheritance** — 3-level hierarchy with constructor chaining
- [x] **Polymorphism (overriding)** — `displayDetails()`, `getBillDescription()`, billing hooks
- [x] **Polymorphism (overloading)** — `searchPatient` ×4, `searchDoctor` ×4, `addLineItem` ×3
- [x] **Abstraction** — `MedicalEntity`, `Person`, `Bill` all abstract
- [x] **Cloning** — deep and shallow, side by side
- [x] **Immutability** — `BillSummary`, all five rules
- [x] **Enums** — three, each carrying behaviour
- [x] **Static blocks** — `Constants`, `MedicalEntity`
- [x] **equals/hashCode** — on the business key, `final` to keep the hierarchy consistent
- [x] **Comparators** — `Doctor.BY_FEE`, `BY_RATING`, `Appointment.BY_SLOT`

---

## Algorithm designs

### Template Method — `Bill.generateBill()`

```
1. baseAmount    = calculateBaseAmount()      [abstract — subclass supplies]
2. afterStrategy = applyStrategy(base)        [Strategy decides]
3. surcharge     = applySurcharge(after)      [hook — default 0.0]
4. tax           = calculateTax(taxable)      [hook — default GST]
5. total         = taxable + tax              [invariant]
```

`final` on the method: subclasses vary the steps, never the order.

### Booking validation order

```
1. patient and doctor non-null
2. slot is in the future
3. slot is within clinic hours
4. doctor is accepting appointments
5. doctor is free at that slot
6. doctor is under the daily cap
```

Cheapest checks first, so an obviously invalid request fails without scanning appointments.

### AI doctor scoring

```
score = 10.0 × specialityMatch
      +  2.0 × rating
      +  0.3 × min(yearsExperience, 20)     <- capped, so seniority cannot dominate
      -  0.8 × currentBookings              <- spreads load across the roster
```

---

## Deliverables

- [x] Full class inventory with responsibilities
- [x] Interface contracts fixed
- [x] Method signatures agreed
- [x] Algorithms specified
- [x] Class diagram → [DB_Design.md §2](../DB_Design.md#2-class-hierarchy-the-schema-in-java-terms)

---

## Exit criteria

- [x] Every class has a stated single responsibility
- [x] Signatures fixed so parallel work compiles on merge
- [x] Every rubric OOP requirement mapped to a specific class
- [x] Algorithms specified before implementation

---

**Previous:** [Phase 2](./PHASE_02_HLD.md) ·
**Next:** [Phase 4 — Data Design](./PHASE_04_DATA_DESIGN.md) ·
[Phase index](./README.md)
