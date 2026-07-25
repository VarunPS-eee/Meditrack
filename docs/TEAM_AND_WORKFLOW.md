# Team, Roles & Workflow

How four people built MediTrack without stepping on each other — the roles, the
dependency graph that ordered the work, and where the parallel tracks ran.

---

## Contents

- [The four roles](#the-four-roles)
- [Why ownership is by package, not by file](#why-ownership-is-by-package-not-by-file)
- [The SDLC cycle](#the-sdlc-cycle)
- [Module dependency graph](#module-dependency-graph)
- [Work dependency graph — topological order](#work-dependency-graph--topological-order)
- [Where the parallel tracks ran](#where-the-parallel-tracks-ran)
- [How the roles interlock](#how-the-roles-interlock)
- [Integration points](#integration-points)

---

## The four roles

Each role owns a **vertical slice of the SDLC**, not a pile of files. The names
describe the discipline, because that is what actually distinguishes them.

| Member | Role | Owns | Primary artefacts |
|---|---|---|---|
| **Sunil Kumar B A** | **System Design & Data Architecture** | The shape of the system and everything that persists | HLD, LLD, DB design, `util/`, `constants/`, CSV persistence, singletons, `test/`, Docker, the documentation set |
| **Varun P S** | **Core Domain Engineering** | The object model and the patterns expressed through it | `entity/`, `factory/`, `strategy/`, `interfaces/`, the OOP hierarchy |
| **Zubair** | **Services & Integration Engineering** | Use-case orchestration and the seams between components | `service/`, `observer/`, `exception/`, stream analytics |
| **Anushtha Sharma** | **Experience & Interface Design** | Everything a user actually touches | `Main` menu flow, prompt and error wording, the user manual, the demonstration walkthrough |

Read that table as four **disciplines**, not four sizes. A system designer and an
interface designer produce different artefacts, judged by different standards —
comparing their line counts would tell you nothing useful about either.

---

## Why ownership is by package, not by file

This is the single choice that kept merge conflicts near zero.

Two people editing different methods in the same file still conflict. Two people
editing different *packages* essentially never do. So each member owns whole
directories, and the interfaces between them were agreed **before** anyone
started filling those directories in.

The interfaces were designed first precisely so they could be handed over as
contracts. `BillingStrategy` existed as a signature before a single strategy was
written, which is what let the domain and service tracks proceed in parallel.

---

## The SDLC cycle

```mermaid
flowchart LR
    R["<b>1 · Requirements</b><br/>Phase 0<br/><i>All four</i>"]
    D["<b>2 · Design</b><br/>Phases 2-4<br/><i>Sunil leads</i>"]
    I["<b>3 · Implementation</b><br/>Phases 5-11<br/><i>Varun · Zubair · Sunil</i>"]
    N["<b>4 · Integration</b><br/>Phases 12-13<br/><i>Zubair · Anushtha</i>"]
    T["<b>5 · Verification</b><br/>Phase 14<br/><i>Sunil</i>"]
    P["<b>6 · Deployment</b><br/>Phase 15<br/><i>Sunil</i>"]
    M["<b>7 · Documentation</b><br/>Phase 16<br/><i>Anushtha · Sunil</i>"]

    R --> D --> I --> N --> T --> P --> M
    T -. "defects reopen<br/>implementation" .-> I
    M -. "gaps reopen<br/>design" .-> D

    style R fill:#e8f0fe,stroke:#4285f4,color:#000
    style D fill:#e6f4ea,stroke:#34a853,color:#000
    style I fill:#fef7e0,stroke:#fbbc04,color:#000
    style N fill:#fce8e6,stroke:#ea4335,color:#000
    style T fill:#f3e8fd,stroke:#a142f4,color:#000
    style P fill:#e4f7fb,stroke:#12b5cb,color:#000
    style M fill:#f1f3f4,stroke:#5f6368,color:#000
```

The two dotted edges are the ones that actually fired. Verification sent work
back to implementation twice during Phase 14, and writing the documentation
exposed a design gap that sent Phase 4 back for revision.

### Phase ownership

| Phase | SDLC stage | Lead | Supporting |
|---|---|---|---|
| 0 · Ideation & requirements | Requirements | All four | — |
| 1 · Environment & JVM study | Design | Sunil | — |
| 2 · High-level design | Design | Sunil | Varun |
| 3 · Low-level design | Design | Sunil | Varun, Zubair |
| 4 · Data & persistence design | Design | Sunil | — |
| 5 · Foundation layer | Implementation | Varun | Zubair, Sunil |
| 6 · Domain model | Implementation | Varun | — |
| 7 · Utilities, storage, singletons | Implementation | Sunil | — |
| 8 · Service layer | Implementation | Zubair | Sunil |
| 9 · Design patterns | Implementation | Varun | Zubair |
| 10 · Persistence & file I/O | Implementation | Sunil | — |
| 11 · AI triage feature | Implementation | Zubair | Sunil |
| 12 · Streams & concurrency | Integration | Zubair | Varun, Sunil |
| 13 · Console UI | Integration | Anushtha | Zubair |
| 14 · Testing & quality | Verification | Sunil | All |
| 15 · Dockerisation | Deployment | Sunil | — |
| 16 · Documentation | Documentation | Anushtha | Sunil |

---

## Module dependency graph

Which package may import which. Arrows point **from** dependent **to**
dependency, and the graph is acyclic by design.

```mermaid
flowchart TD
    subgraph UI["Experience — Anushtha"]
        MAIN["Main<br/><i>menu, prompts, errors</i>"]
    end

    subgraph SVC["Services — Zubair"]
        SERVICE["service/"]
        OBSERVER["observer/"]
    end

    subgraph DOM["Domain — Varun"]
        ENTITY["entity/"]
        FACTORY["factory/"]
        STRATEGY["strategy/"]
    end

    subgraph SYS["System & Data — Sunil"]
        UTIL["util/"]
        CONST["constants/"]
    end

    subgraph CONTRACT["Shared contracts — designed first"]
        IFACE["interfaces/"]
        EXC["exception/"]
    end

    MAIN --> SERVICE
    MAIN --> ENTITY
    MAIN --> UTIL

    SERVICE --> ENTITY
    SERVICE --> FACTORY
    SERVICE --> UTIL
    SERVICE --> OBSERVER
    SERVICE --> EXC

    OBSERVER --> IFACE
    OBSERVER --> ENTITY

    FACTORY --> ENTITY
    FACTORY --> STRATEGY
    STRATEGY --> IFACE
    STRATEGY --> ENTITY

    ENTITY --> IFACE
    ENTITY --> CONST
    ENTITY --> EXC

    UTIL --> CONST
    UTIL --> EXC
    UTIL --> ENTITY

    style MAIN fill:#fce8e6,stroke:#ea4335,color:#000
    style SERVICE fill:#fef7e0,stroke:#fbbc04,color:#000
    style OBSERVER fill:#fef7e0,stroke:#fbbc04,color:#000
    style ENTITY fill:#e6f4ea,stroke:#34a853,color:#000
    style FACTORY fill:#e6f4ea,stroke:#34a853,color:#000
    style STRATEGY fill:#e6f4ea,stroke:#34a853,color:#000
    style UTIL fill:#e8f0fe,stroke:#4285f4,color:#000
    style CONST fill:#e8f0fe,stroke:#4285f4,color:#000
    style IFACE fill:#f1f3f4,stroke:#5f6368,color:#000
    style EXC fill:#f1f3f4,stroke:#5f6368,color:#000
```

Three rules this graph encodes:

1. **`Main` never reaches past the service layer for business logic.** It reads
   input, calls a service, prints the result. Without that discipline console
   apps reliably become god objects.
2. **`entity/` does not depend on `service/`.** The domain has no idea it is
   being orchestrated, which is why entities are unit-testable in isolation.
3. **`interfaces/` and `exception/` sit at the bottom and depend on nothing.**
   That is what let them be handed over as contracts before implementation
   started.

---

## Work dependency graph — topological order

The build order the team actually followed. Anything on the same rank could run
concurrently.

```mermaid
flowchart TD
    P0["Phase 0<br/>Requirements<br/><b>All four</b>"]
    P2["Phases 2-3<br/>HLD + LLD<br/><b>Sunil</b>"]
    P4["Phase 4<br/>Data design<br/><b>Sunil</b>"]
    P5["Phase 5<br/>Foundation:<br/>interfaces + exceptions<br/><b>Varun + Zubair</b>"]

    P6["Phase 6<br/>Domain model<br/><b>Varun</b>"]
    P7["Phase 7<br/>Utils + storage<br/><b>Sunil</b>"]

    P8["Phase 8<br/>Services<br/><b>Zubair</b>"]
    P9["Phase 9<br/>Patterns<br/><b>Varun</b>"]
    P10["Phase 10<br/>Persistence<br/><b>Sunil</b>"]
    P11["Phase 11<br/>AI triage<br/><b>Zubair</b>"]

    P12["Phase 12<br/>Streams + concurrency<br/><b>All</b>"]
    P13["Phase 13<br/>Console UI<br/><b>Anushtha</b>"]
    P14["Phase 14<br/>Testing<br/><b>Sunil</b>"]
    P15["Phase 15<br/>Docker<br/><b>Sunil</b>"]
    P16["Phase 16<br/>Documentation<br/><b>Anushtha + Sunil</b>"]

    P0 --> P2 --> P4 --> P5
    P5 --> P6
    P5 --> P7
    P6 --> P8
    P7 --> P8
    P6 --> P9
    P7 --> P10
    P8 --> P11
    P8 --> P12
    P9 --> P12
    P10 --> P12
    P11 --> P13
    P12 --> P13
    P13 --> P14
    P14 --> P15
    P15 --> P16

    style P0 fill:#e8f0fe,stroke:#4285f4,color:#000
    style P2 fill:#e8f0fe,stroke:#4285f4,color:#000
    style P4 fill:#e8f0fe,stroke:#4285f4,color:#000
    style P7 fill:#e8f0fe,stroke:#4285f4,color:#000
    style P10 fill:#e8f0fe,stroke:#4285f4,color:#000
    style P14 fill:#e8f0fe,stroke:#4285f4,color:#000
    style P15 fill:#e8f0fe,stroke:#4285f4,color:#000
    style P6 fill:#e6f4ea,stroke:#34a853,color:#000
    style P9 fill:#e6f4ea,stroke:#34a853,color:#000
    style P5 fill:#f1f3f4,stroke:#5f6368,color:#000
    style P8 fill:#fef7e0,stroke:#fbbc04,color:#000
    style P11 fill:#fef7e0,stroke:#fbbc04,color:#000
    style P12 fill:#fef7e0,stroke:#fbbc04,color:#000
    style P13 fill:#fce8e6,stroke:#ea4335,color:#000
    style P16 fill:#fce8e6,stroke:#ea4335,color:#000
```

**Phase 5 is the critical join.** Both implementation tracks are blocked on it
and neither can start early, which is exactly why the foundation layer was kept
deliberately small — four interfaces and five exceptions, nothing more. The
sooner it lands, the sooner two people work at once.

---

## Where the parallel tracks ran

| Window | Track A | Track B | Why they do not collide |
|---|---|---|---|
| Phases 6 ‖ 7 | **Varun** — domain model | **Sunil** — utils & storage | `DataStore<T extends MedicalEntity>` needs only the *bound*, which Phase 5 fixed. Different packages entirely |
| Phases 9 ‖ 10 | **Varun** — Factory & Strategy | **Sunil** — CSV persistence | Patterns touch `factory/`+`strategy/`; persistence touches `util/`. No shared file |
| Phases 11 ‖ 12 | **Zubair** — AI triage | **All** — streams & analytics | Triage reads the domain; analytics reads the stores. Both read-only against each other |
| Phase 13 ‖ 14 | **Anushtha** — console UI | **Sunil** — test suite | UI calls services; tests call services. Neither modifies the service layer |

Roughly **60% of implementation time had two or more people working
concurrently** — a direct consequence of designing the contracts up front rather
than discovering them mid-build.

---

## How the roles interlock

Each role's output is the next role's input. The chain is easiest to see by
following one feature all the way through.

### Worked example — a bill gets raised

```mermaid
sequenceDiagram
    participant A as Main<br/>(Anushtha)
    participant Z as BillingService<br/>(Zubair)
    participant V as BillFactory<br/>(Varun)
    participant S as IdGenerator / DataStore<br/>(Sunil)
    participant O as Observers<br/>(Zubair)

    A->>Z: generateBill(APT-0001, CONSULTATION)
    Note over A: reads input, validates nothing,<br/>prints whatever comes back
    Z->>Z: check appointment is COMPLETED
    Z->>V: create(BillType, patient, amount)
    V->>V: chooseStrategy(patient) → SeniorCitizen
    Note over V: policy derived from the patient,<br/>never passed in
    V->>S: nextBillId() → "BIL-0001"
    S-->>V: atomic, collision-free
    V-->>Z: ConsultationBill
    Z->>S: store.save(bill)
    Z->>O: fire(BILL_RAISED)
    O-->>O: SMS + audit, independently
    Z-->>A: Bill
    A->>A: print itemised bill
```

Four people's work, one operation, and **no layer reaches past its neighbour**.

### The handover contracts

| From | To | The contract |
|---|---|---|
| Sunil → everyone | Design | HLD/LLD fixed the package boundaries and the layering rule before code existed |
| Varun → Zubair | `entity/` + `factory/` | Services orchestrate entities; they never reimplement domain behaviour |
| Sunil → Varun & Zubair | `util/` | `DataStore<T>`, `Validator`, `IdGenerator` — one owner per rule, so nothing drifts |
| Varun → Zubair | `interfaces/` | `BillingStrategy` and `AppointmentObserver` existed as signatures first |
| Zubair → Anushtha | `service/` | Services return values and throw checked exceptions; the UI decides how to say it |
| Anushtha → Sunil | UI flow | Every menu path is a test path — the UI walkthrough defined what Phase 14 had to cover |
| Sunil → all | `test/` + Docker | The suite gates the image, so nobody's regression ships |

### Why this ordering, and not another

**Design before contracts, contracts before implementation.** The two structural
bugs the project actually hit — a package name treating the source path as part
of the package, and a reserved keyword in a package name — both originated in the
initial skeleton, *before* the design phase was taken seriously. Both were caught
in Phase 5 and both required touching every file written up to that point.

That is the lesson recorded in
[Design_Decisions.md](./Design_Decisions.md): validate the package structure
against the specification **before** filling it with classes. It is also why
Phase 5 is a hard join in the graph above rather than something the two
implementation tracks could each have done for themselves.

---

## Integration points

The places where two members' work meets, and how each was verified.

| Seam | Between | Verified by |
|---|---|---|
| `DataStore<T extends MedicalEntity>` | Sunil ↔ Varun | Generic bound tests; store round-trips every entity type |
| `BillingStrategy` | Varun ↔ Zubair | Same bill priced three ways; insurance must beat senior |
| `AppointmentObserver` | Zubair ↔ Varun | Observer fan-out with a deliberately failing channel |
| `Validator` | Sunil ↔ all | Every rule tested at its boundary, both sides |
| Service → UI | Zubair ↔ Anushtha | Every menu path exercised; no input crashes the app |
| CSV round trip | Sunil ↔ Varun | Comma-containing field survives write→read |
| Test suite → Docker | Sunil ↔ deployment | Build fails if any assertion fails |

---

*Phase detail: [phases/README.md](./phases/README.md) · Per-member sheets:
[team/](./team/) · PR history: [pull-requests/](./pull-requests/)*
