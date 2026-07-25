# Sunil Kumar B A — Task Sheet

> **Role:** Utils, Storage, Singleton, Docs & Testing
> **Phases owned:** 1 (setup/JVM), 4 (data design), 7 (utils), 10 (persistence), 14 (testing), 15 (Docker)
> **Packages owned:** `util/`, `test/`, `constants/`, `docs/`, Docker
> **Status:** ✅ All assigned work complete (screenshots outstanding)

---

## Original assignment (from the team brief)

> Build the generic DataStore<T>, Validator, and DateUtil.
> Bonus (Singleton): Implement eager & lazy initialization for app config and IdGenerator.
> Write the manual tests in TestRunner.java.
> Research and write the documentation: JVM_Report.md, Setup_Instructions.md,
> Design_Decisions.md.

---

## Task checklist

### Phase 1 — Setup & JVM
- [x] `docs/Setup_Instructions.md` — three routes (Docker, CLI, IntelliJ)
- [x] Per-OS JDK installation, verification commands, troubleshooting
- [x] `docs/JVM_Report.md` — Class Loader, Runtime Data Areas, Execution Engine, JIT vs
      Interpreter, WORA
- [x] JVM claims tied to observable output from this project
- [ ] **Screenshots** — placeholders marked, capture pending

### Phase 4 — Data design
- [x] `docs/DB_Design.md` — ER model, class diagram, key strategy
- [x] CSV schemas with types and nullability
- [x] Referential integrity rules and load order
- [x] Full SQL DDL migration path

### Phase 7 — Utils & storage
- [x] `Constants` with a **static initialisation block** resolving `MEDITRACK_DATA_DIR`
- [x] `DataStore<T extends MedicalEntity>`
  - [x] Generic with a **bounded type parameter**
  - [x] `LinkedHashMap` for deterministic iteration
  - [x] Full CRUD; `Optional` returns; `getById` throws
  - [x] `findBy(Predicate)`, `search`, `findAllSorted`, `stream`
  - [x] Implements `Iterable<T>` with a hand-written iterator
  - [x] Iterator `remove()` throws — deletions go through `deleteById`
  - [x] Serialization with try-with-resources
- [x] `Validator` — every rule, one place; chaining on parse failures
- [x] `DateUtil` — thread-safe formatter constants, slot alignment, clinic hours
- [x] **`IdGenerator`** — lazy singleton via the **holder idiom**
  - [x] `AtomicInteger` per prefix
  - [x] `accumulateAndGet(value, Math::max)` for atomic monotonic sync
  - [x] `resetAll()` for test isolation
- [x] **`AppConfig`** — eager singleton
- [x] `DemoDataSeeder`

### Phase 10 — Persistence
- [x] `CSVUtil` with `String.split(",")` as required
- [x] Escaping: `,` → `||`, newlines flattened, `;` for lists
- [x] **try-with-resources** on every stream
- [x] Malformed rows skipped, not fatal
- [x] `--loadData` CLI integration
- [x] Foreign key re-linking with orphan rejection
- [x] UTF-8 explicitly

### Phase 14 — Testing
- [x] `TestRunner` — assertion vocabulary, suites, tally, failure list
- [x] `assertThrows` / `assertNoThrow` with functional interfaces
- [x] **`System.exit(1)` on failure** so it gates the Docker build
- [x] 20 suites, **325 assertions**
- [x] Reflection-based immutability verification
- [x] 10-thread concurrency load test
- [x] Real filesystem CSV round-trip

### Phase 15 — Docker
- [x] Multi-stage Dockerfile (JDK build → JRE runtime)
- [x] Test suite gates the image build
- [x] Non-root user
- [x] UTF-8 environment
- [x] Volume for `/data`
- [x] `.dockerignore`
- [x] `docker-compose.yml` with app and test services
- [x] Verified: builds, tests pass in container, UTF-8 renders, 286 MB

### Phase 16 — Documentation
- [x] `README.md`
- [x] `docs/Design_Decisions.md`
- [x] 18 phase documents
- [x] Three per-member task sheets
- [x] PR tracking docs

---

## Notable decisions

### Both singleton flavours, deliberately

`AppConfig` is eager, `IdGenerator` is lazy — implemented side by side so the difference is
demonstrable rather than described.

The lazy one uses the **initialisation-on-demand holder idiom** rather than the alternatives:

| Approach | Problem |
|---|---|
| `if (instance == null)` | Two threads can both see `null` |
| `synchronized getInstance()` | Locks every call forever, to guard a one-time race |
| Double-checked locking | Needs `volatile`; subtle and easy to get wrong |
| Enum singleton | Most robust, but hides the lazy-loading mechanism we wanted to show |

Observable at runtime: `[AppConfig]` prints during startup, `[IdGenerator]` only on first use.

### `AtomicInteger` is not decoration

`count++` is a read-modify-write triple. `TestRunner` spawns 10 threads generating 100 ids each
and asserts **1,000 unique ids** — a test that fails intermittently with a plain `int`.

### Migrated the project off `java.util.Date`

`Date` is mutable, so every getter handing one out needs a defensive copy and missing one is a
silent aliasing bug. `SimpleDateFormat` is also not thread-safe, which matters given the reminder
daemon. Swapped for `LocalDateTime` + `DateTimeFormatter` throughout.

Trade-off: this removed `Date.clone()` as a deep-copy demo. Replaced with mutable *collections*
in `Patient` — the case that actually bites in practice.

### Fixed the package structure

The codebase declared `package main.java.com.airtribe.meditrack.*`. It compiled, because
IntelliJ had `src` as the source root — which is what made it easy to miss. But the rubric
specifies `com.airtribe.meditrack`, and every new file would have inherited the mistake.
Corrected across all files; source root re-pointed to `src/main/java`.

### Two test bugs, fixed in the tests

The first run was 321/323. Both failures were assertion errors, not product bugs — see
[Phase 14](../phases/PHASE_14_TESTING.md#two-bugs-the-suite-caught--in-the-tests). Both times the
instinct was to "fix" the product, and both times that would have introduced a real defect.

---

## Pull requests

| PR | Branch | Scope | Status |
|---|---|---|---|
| — | `feature/utils-and-docs` | *Originally assigned branch — never created* | ⬜ |
| #5 | `feature/complete-meditrack` | Utils, storage, singletons, tests, docs, Docker — see [PR-005](../pull-requests/PR-005-complete-meditrack.md) | 🟡 Open |

---

## Files owned

```
src/main/java/com/airtribe/meditrack/
├── util/            (7 files)   ← primary ownership
├── constants/       (1 file)
└── test/            (1 file)

docs/                (23 files)
Dockerfile, docker-compose.yml, .dockerignore
```

---

## Verification

`TestRunner` suites covering this work:
- *Validator — centralised validation* (22 assertions)
- *DateUtil — java.time helpers* (14)
- *Singletons — eager and lazy* (16)
- *DataStore<T> — generic CRUD* (21)
- *Generics and Iterator contract* (12)
- *CSV — round-trip and try-with-resources* (20)
- *Concurrency — AtomicInteger and thread safety* (10)
- *Streams and lambdas — analytics* (14)

**Total: 129 assertions against Sunil's code. All passing.**

---

## Outstanding

- [ ] Capture the screenshots marked in `Setup_Instructions.md`
- [ ] Generate and optionally commit JavaDoc HTML
- [ ] Record the walkthrough video

---

*[Phase index](../phases/README.md) · [Varun](./VARUN.md) · [Zubair](./ZUBAIR.md)*
