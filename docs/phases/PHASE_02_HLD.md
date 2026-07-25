# Phase 2 — High-Level Design

| | |
|---|---|
| **Owner** | All three members (joint design session) |
| **Depends on** | Phase 0 |
| **Blocks** | Phase 3 |
| **Status** | ✅ Complete |

---

## Objective

Decide the system's shape: what the layers are, which direction dependencies flow, and which
package each member owns. Getting this right is what allows three people to work in parallel
without treading on each other.

---

## Layered architecture

```
┌─────────────────────────────────────────────────────────────┐
│  PRESENTATION            Main.java                          │
│  Menu loop, input parsing, output formatting                │
└──────────────────────────┬──────────────────────────────────┘
                           │ calls
┌──────────────────────────▼──────────────────────────────────┐
│  SERVICE                 service/                           │
│  PatientService · DoctorService · AppointmentService        │
│  BillingService · NotificationService                       │
│  Use-case orchestration, business rules                     │
└──────────────────────────┬──────────────────────────────────┘
                           │ operates on
┌──────────────────────────▼──────────────────────────────────┐
│  DOMAIN                  entity/                            │
│  MedicalEntity → Person → Patient/Doctor                    │
│  Appointment · Bill hierarchy · BillSummary · enums         │
│                                                             │
│  PATTERNS      factory/ · strategy/ · observer/             │
└──────────────────────────┬──────────────────────────────────┘
                           │ uses
┌──────────────────────────▼──────────────────────────────────┐
│  INFRASTRUCTURE          util/                              │
│  DataStore<T> · Validator · DateUtil · CSVUtil              │
│  IdGenerator · AppConfig · AIHelper                         │
└──────────────────────────┬──────────────────────────────────┘
                           │ built on
┌──────────────────────────▼──────────────────────────────────┐
│  FOUNDATION   constants/ · interfaces/ · exception/         │
└─────────────────────────────────────────────────────────────┘
```

### The one architectural rule

**Dependencies point downward only.** `util` never imports `service`. `entity` never imports
`service`.

This is what makes the system testable in isolation — `TestRunner` builds a `DataStore<Patient>`
without booting the UI, and a `Validator` test needs nothing else at all. A single upward
dependency would force whole-application setup for every test.

---

## Tasks

### Architecture
- [x] Choose a layered architecture and justify it
- [x] Fix the dependency direction as a rule
- [x] Assign responsibilities per layer
- [x] Verify no cycles exist between packages

### Package structure
- [x] Base package confirmed: **`com.airtribe.meditrack`**
- [x] Sub-packages agreed against the rubric
- [x] Deviations identified and justified (`interfaces/`, `factory/`, `strategy/`, `observer/`)

### Component design
- [x] Identify the core domain entities
- [x] Decide the inheritance hierarchy root (`MedicalEntity`)
- [x] Decide which behaviours belong on entities vs services
- [x] Choose the storage abstraction — one generic `DataStore<T>`

### SOLID
- [x] Map each principle to a concrete decision
- [x] Identify where we deliberately *break* a principle and why

### Patterns
- [x] Select patterns and the problem each solves
- [x] Reject patterns that would be decoration rather than solution

### Ownership
- [x] Assign packages to members so tracks stay disjoint
- [x] Identify shared files as a conflict risk

---

## Package ownership

| Package | Owner | Rationale |
|---|---|---|
| `entity/` | Varun | Core OOP is his assigned area |
| `factory/`, `strategy/` | Varun | Bill creation is entity-adjacent |
| `service/` | Zubair | Business logic is his assigned area |
| `observer/`, `exception/` | Zubair | Notification and error handling |
| `util/` | Sunil | Utilities, storage, singletons |
| `test/`, `docs/`, Docker | Sunil | Testing and documentation |
| `interfaces/`, `constants/` | Joint (Phase 5) | Everyone depends on these — built first |
| `Main.java` | Joint (Phase 13) | Unavoidable shared file — scheduled late |

Owning whole **packages** rather than files within a package is the specific choice that kept
merge conflicts near zero.

---

## Pattern selection

| Pattern | Problem it solves | Verdict |
|---|---|---|
| Singleton | One config, one id sequence | ✅ Both eager and lazy, to contrast |
| Factory | Callers should not know `Bill` subclasses | ✅ |
| Strategy | Pricing policies must be swappable | ✅ |
| Template Method | Billing *order* is invariant, amounts are not | ✅ |
| Observer | Booking must not know about SMS | ✅ |
| Null Object | Remove null checks from the pricing path | ✅ `StandardBillingStrategy` |
| Iterator | For-each over a store without exposing the map | ✅ |
| Builder | — | ❌ Rejected: constructors are adequate here |
| Decorator | — | ❌ Rejected: no genuine layering need |
| Visitor | — | ❌ Rejected: would add indirection for no benefit |

The rejections matter as much as the selections. Adding Builder and Visitor would have
demonstrated pattern knowledge while making the code worse.

---

## Key decisions

| Decision | Alternative rejected | Why |
|---|---|---|
| `MedicalEntity` as hierarchy root | `Person` as root | Appointments and bills need ids and searchability too, and are not people |
| One generic `DataStore<T>` | `PatientStore`, `DoctorStore`, … | Three copies of identical CRUD that drift apart |
| `LocalDateTime` over `Date` | Keep `Date` | `Date` is mutable — every getter needs a defensive copy |
| Services hold stores by constructor injection | Services reach for singletons | Tests can hand in clean state per test |
| Checked exceptions for domain errors | Unchecked | Bad input is expected and recoverable; force callers to handle it |
| Validation centralised in `Validator` | Validation inside setters | One owner per rule, or rules drift |

---

## Deliverables

- [x] Layer diagram and dependency rule
- [x] Package ownership matrix
- [x] Pattern selection with rejections recorded
- [x] SOLID mapping → [Design_Decisions.md §2](../Design_Decisions.md#2-solid-principles-applied)
- [x] Architecture write-up → [Design_Decisions.md §1](../Design_Decisions.md#1-architecture)

---

## Exit criteria

- [x] Every member can name which package they own
- [x] Dependency direction agreed and documented
- [x] Patterns chosen with a stated problem each
- [x] No cyclic dependencies in the planned structure
- [x] Conflict-prone shared files identified and scheduled late

---

**Previous:** [Phase 1](./PHASE_01_SETUP_JVM.md) ·
**Next:** [Phase 3 — LLD](./PHASE_03_LLD.md) ·
[Phase index](./README.md)
