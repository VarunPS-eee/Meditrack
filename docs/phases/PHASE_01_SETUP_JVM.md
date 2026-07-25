# Phase 1 — Environment Setup & JVM Foundations

| | |
|---|---|
| **Owner** | **Sunil Kumar B A** |
| **Depends on** | Phase 0 |
| **Runs parallel with** | Phase 2 (HLD) |
| **Rubric** | Environment Setup & JVM Understanding — **10 pts** |
| **Status** | ✅ Complete (screenshots pending) |

---

## Objective

Get every member on an identical, working Java 21 toolchain, and document the JVM well enough
that the team can reason about class loading, memory and performance rather than guessing.

---

## Tasks

### Toolchain
- [x] Choose the JDK distribution — **Eclipse Temurin 21 LTS**
- [x] Install JDK 21 on each member's machine
- [x] Verify `java -version` **and** `javac -version` both respond
- [x] Confirm `JAVA_HOME` is set and on `PATH`
- [x] Agree the language level: **Java 21**
- [x] Confirm no build tool is needed (Core Java, zero dependencies)

### IDE
- [x] IntelliJ IDEA project opens cleanly
- [x] SDK set to JDK 21, language level 21
- [x] `src/main/java` marked as the source root
- [x] Run configurations documented (app + tests)
- [x] UTF-8 console encoding configured

### `docs/Setup_Instructions.md`
- [x] Prerequisites table
- [x] Per-OS JDK installation (Windows / macOS / Linux)
- [x] Verification commands with expected output
- [x] **Route A** — Docker (no JDK needed)
- [x] **Route B** — command line `javac`/`java`
- [x] **Route C** — IntelliJ IDEA
- [x] Jar packaging instructions
- [x] Two-minute guided tour of the app
- [x] Troubleshooting table
- [x] JavaDoc generation
- [x] Environment variables documented
- [ ] **Screenshots captured** — placeholders marked in the doc *(needs a human at a screen)*

### `docs/JVM_Report.md`
- [x] JDK vs JRE vs JVM, tied to the Docker stages
- [x] **Class Loader** — delegation hierarchy, Loading → Linking → Initialisation
- [x] Lazy loading proven with real startup output
- [x] **Runtime Data Areas** — Heap, Stack, Method Area, PC Register, Native Stack
- [x] Heap vs Stack explained via the shallow-copy bug
- [x] **Execution Engine** — interpreter, JIT, GC
- [x] **JIT vs Interpreter** comparison table
- [x] Tiered compilation (C1/C2) explained
- [x] **Write Once, Run Anywhere** — proven Windows → Alpine, same bytecode
- [x] Where WORA leaks (encoding, paths) and how we handled it
- [x] End-to-end startup trace
- [x] Self-verification commands (`-verbose:class`, `-XX:+PrintCompilation`, `javap`)

---

## Deliverables

| File | Status |
|---|---|
| [docs/Setup_Instructions.md](../Setup_Instructions.md) | ✅ |
| [docs/JVM_Report.md](../JVM_Report.md) | ✅ |
| `docs/images/*.png` | ⬜ Placeholders marked |

---

## Evidence

The JVM report is not theoretical — it cites observable behaviour from this codebase.

**Class initialisation order, captured from a real run:**
```
[Constants] Static block executed — data directory resolved to: data
[AppConfig] Eager singleton constructed at 2026-07-25 14:16
[IdGenerator] Lazy singleton constructed on first use.
[MedicalEntity] Class loaded and initialised at 2026-07-25T14:16:26
```

`MedicalEntity` appears *last* — after the banner — proving the JVM loaded it at first use
rather than at startup.

**WORA, verified rather than asserted:**

| Environment | Java | OS | Tests |
|---|---|---|---|
| Host | Temurin 21.0.11 | Windows 11 / x86-64 | 325/325 |
| Container | Temurin 21.0.11 | Alpine Linux | 325/325 |

Same `.class` files, no recompilation.

---

## Exit criteria

- [x] Every member can compile and run the project
- [x] `docs/Setup_Instructions.md` complete enough for a stranger to follow
- [x] `docs/JVM_Report.md` covers all five required topics
- [x] JVM claims backed by observable output from this project
- [ ] Screenshots embedded *(the only outstanding item)*

---

**Previous:** [Phase 0](./PHASE_00_IDEATION.md) ·
**Next:** [Phase 2 — HLD](./PHASE_02_HLD.md) ·
[Phase index](./README.md)
