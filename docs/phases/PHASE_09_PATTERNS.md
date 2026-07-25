# Phase 9 — Design Patterns

| | |
|---|---|
| **Owner** | Varun (Factory, Strategy, Template Method) + Zubair (Observer) |
| **Depends on** | Phases 6, 8 |
| **Rubric** | **Bonus B — Design Patterns (10 pts)** |
| **Status** | ✅ Complete |

---

## Objective

Implement the patterns chosen in Phase 2, each solving a real problem in this codebase rather
than being added for display.

---

## Patterns delivered

| Pattern | Implementation | Problem solved |
|---|---|---|
| **Singleton (eager)** | `AppConfig` | One config, thread-safe with no locking |
| **Singleton (lazy)** | `IdGenerator` | Deferred construction, still exactly-once |
| **Factory** | `BillFactory` | Callers name a *type*, not a class |
| **Strategy** | `BillingStrategy` + 3 | Swap pricing without touching billing |
| **Template Method** | `Bill.generateBill()` | Fix the algorithm order, vary the steps |
| **Observer** | `NotificationService` | Decouple booking from notification |
| **Null Object** | `StandardBillingStrategy` | Remove null checks from the pricing path |
| **Iterator** | `DataStore<T>` | For-each without exposing the map |

---

## Tasks

### Factory — `factory/BillFactory.java`
- [x] `createBill(BillType, ...)` returning the right subclass
- [x] Exhaustive `switch` over `BillType` — a new constant becomes a compile error
- [x] Overload taking an explicit strategy
- [x] `createBillForAppointment` deriving what it can from an appointment
- [x] `chooseStrategy(Patient)` — policy from patient attributes
- [x] Id assignment delegated to `IdGenerator`
- [x] Null `BillType` defaults to `CONSULTATION`
- [x] Private constructor — static factory, never instantiated

### Strategy — `strategy/`
- [x] `StandardBillingStrategy` — Null Object; no adjustment
- [x] `InsuranceBillingStrategy` — configurable coverage, clamped to 0–1
- [x] `SeniorCitizenBillingStrategy` — configurable discount
- [x] `BillingStrategy` marked `@FunctionalInterface` so lambdas work
- [x] `describe()` for the comparison menu
- [x] Runtime swapping via `Bill.setBillingStrategy()`

### Template Method — `entity/Bill.java`
- [x] `generateBill()` **`final`**
- [x] Step 1 `calculateBaseAmount()` — abstract
- [x] Step 2 `applyStrategy()` — delegates to the Strategy
- [x] Step 3 `applySurcharge()` — hook, default `0.0`
- [x] Step 4 `calculateTax()` — hook, default GST
- [x] Step 5 total — invariant
- [x] `EmergencyBill` overrides step 3 only

### Observer — `observer/` + `NotificationService`
- [x] `AppointmentObserver` interface with an `isInterestedIn` opt-out
- [x] `ConsoleReminderObserver`
- [x] `SmsReminderObserver` — selective subscription, masked numbers
- [x] `AuditLogObserver` — records everything, `record`-based entries
- [x] `register` / `unregister`, duplicate and null guards
- [x] `CopyOnWriteArrayList` for lock-free iteration
- [x] Per-observer try/catch — one failure cannot break the rest
- [x] `AtomicInteger` event counter
- [x] `TimerTask` reminder sweep on a **daemon** thread

---

## Why each pattern earns its place

### Factory — and where we deliberately break Open/Closed

Adding a `BillType` **does** require editing `BillFactory`. That is intentional: the `switch` is
exhaustive over the enum, so a new constant becomes a **compile error** rather than a silently
unhandled case.

We chose compile-time safety over extensibility here, because a mis-priced bill is worse than an
extra edit. Strategy, by contrast, *is* fully open/closed — new policies need no existing file
changed.

### Factory + Strategy composition

`chooseStrategy(Patient)` derives the policy from the patient, so the two patterns cooperate
rather than each re-deciding:

```
insured?        -> InsuranceBillingStrategy      (contractual — checked first)
senior citizen? -> SeniorCitizenBillingStrategy  (self-payers only)
otherwise       -> StandardBillingStrategy
```

Precedence is deliberate: stacking both would double-discount.

### Template Method vs plainly overriding `generateBill()`

The rubric asks for polymorphic `generateBill()` behaviour. Template Method delivers exactly
that — three `Bill` references, one call, three different totals — while making it *impossible*
for a subclass to forget the tax step or apply the discount after tax.

The billing **sequence** is a business invariant; the **amounts** are not. `final` on the method
encodes that distinction.

Verified: `emergency.getTotalAmount() > consultation.getTotalAmount()` for an identical base fee.

### Observer — auditing is the clearest case

Without it, `AppointmentService` would have to call an audit method at the end of every mutating
operation — forever, and correctly, at every new call site. With it, the service fires one event
and the audit trail takes care of itself.

`SmsReminderObserver` shows the other half: it opts out of `COMPLETED` events via
`isInterestedIn()`, so the dispatcher never needs to know which channel cares about what.

---

## Live demonstration

Menu **4 → 6** prices one charge three ways:

```
  Same ₹1,000.00 consultation for Ravi Kumar, priced three ways:

    Standard                     total    ₹1,180.00   — Standard rate — no adjustment applied.
    Insurance (Default Insurer)  total      ₹354.00   — Default Insurer covers 70%; patient pays the 30% co-pay.
    Senior Citizen               total    ₹1,062.00   — Senior citizen concession of 10% applied.
```

Same bill type, same base fee, three strategies, three totals. Menu **9 → 4** proves singleton
identity.

---

## Deliverables

| File |
|---|
| `factory/BillFactory.java` |
| `strategy/StandardBillingStrategy.java` |
| `strategy/InsuranceBillingStrategy.java` |
| `strategy/SeniorCitizenBillingStrategy.java` |
| `observer/ConsoleReminderObserver.java` |
| `observer/SmsReminderObserver.java` |
| `observer/AuditLogObserver.java` |
| `service/NotificationService.java` |

---

## Verification

`TestRunner` suites: *Billing — Factory and Template Method*, *Strategy*, *Observer*,
*Singletons*.

Sample assertions:
- *"factory built a ConsultationBill"* / *"ProcedureBill"* / *"EmergencyBill"*
- *"emergency total exceeds consultation total"*
- *"insured patient gets the insurance strategy"*
- *"lambda strategy applies"*
- *"SMS ignores COMPLETED"*
- *"duplicate registration ignored"*

---

## Exit criteria

- [x] Eight patterns implemented, each solving a stated problem
- [x] Both singleton flavours, contrasted in code and docs
- [x] Factory returns all three bill types correctly
- [x] Strategies produce measurably different totals
- [x] Template Method order is unchangeable by subclasses
- [x] Observers fire on every lifecycle event and fail in isolation

---

**Previous:** [Phase 8](./PHASE_08_SERVICES.md) ·
**Next:** [Phase 10 — Persistence](./PHASE_10_PERSISTENCE.md) ·
[Phase index](./README.md)
