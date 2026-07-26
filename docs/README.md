# MediTrack Documentation

Everything written about this project, indexed.

---

## Start here

| Document | Read this if you want to… |
|---|---|
| **[USER_MANUAL.md](./USER_MANUAL.md)** | **Use the app** — every menu option explained |
| **[DEMO_OUTPUT.md](./DEMO_OUTPUT.md)** | **See it working** — guided tour with real captured output |
| **[🎬 ../video/](../video/)** | **Watch it** — the 32-minute film: architecture, the JVM at work, and every menu |
| **[Setup_Instructions.md](./Setup_Instructions.md)** | Get it running — Docker, CLI or IntelliJ |
| **[../README.md](../README.md)** | See what it does, with sample output |

---

## Required deliverables

| Document | Covers |
|---|---|
| **[JVM_Report.md](./JVM_Report.md)** | Class loader, runtime data areas, execution engine, JIT vs interpreter, WORA |
| **[Setup_Instructions.md](./Setup_Instructions.md)** | JDK install, three run routes, troubleshooting |
| **[Design_Decisions.md](./Design_Decisions.md)** | Architecture, SOLID, patterns, deviations, known limitations |

---

## Design

| Document | Covers |
|---|---|
| **[DB_Design.md](./DB_Design.md)** | ER model, class diagram, key strategy, CSV schemas, referential integrity, SQL migration |

---

## Project management

| Document | Covers |
|---|---|
| **[TEAM_AND_WORKFLOW.md](./TEAM_AND_WORKFLOW.md)** | Roles, SDLC cycle, module + work dependency graphs, parallel tracks |
| **[CODE_QUALITY.md](./CODE_QUALITY.md)** | SonarQube results, every bug explained, coverage and its gaps |
| **[phases/README.md](./phases/README.md)** | Phase index, dependency graph, parallelisation, rubric coverage |
| **[team/](./team/)** | Per-member task sheets with checkboxes and decisions |
| **[pull-requests/](./pull-requests/)** | Every PR with rationale and review notes |

---

## Open source

| Document | Covers |
|---|---|
| **[../CONTRIBUTING.md](../CONTRIBUTING.md)** | Setup, conventions, PR process, ground rules |
| **[../CODE_OF_CONDUCT.md](../CODE_OF_CONDUCT.md)** | Contributor Covenant 2.1 |
| **[../SECURITY.md](../SECURITY.md)** | Private disclosure, scope, security posture |
| **[../LICENSE](../LICENSE)** | MIT |

---

## Phase plan (0 → 16)

| # | Phase | Owner | Status |
|---|---|---|---|
| 0 | [Ideation & Requirements](./phases/PHASE_00_IDEATION.md) | All | ✅ |
| 1 | [Environment Setup & JVM](./phases/PHASE_01_SETUP_JVM.md) | Sunil | ✅ |
| 2 | [High-Level Design](./phases/PHASE_02_HLD.md) | All | ✅ |
| 3 | [Low-Level Design](./phases/PHASE_03_LLD.md) | All | ✅ |
| 4 | [Data & Persistence Design](./phases/PHASE_04_DATA_DESIGN.md) | Sunil | ✅ |
| 5 | [Foundation Layer](./phases/PHASE_05_FOUNDATION.md) | Varun + Zubair | ✅ |
| 6 | [Domain Model](./phases/PHASE_06_DOMAIN_MODEL.md) | Varun | ✅ |
| 7 | [Utilities, Storage & Singletons](./phases/PHASE_07_UTILS_STORAGE.md) | Sunil | ✅ |
| 8 | [Service Layer](./phases/PHASE_08_SERVICES.md) | Zubair | ✅ |
| 9 | [Design Patterns](./phases/PHASE_09_PATTERNS.md) | Varun + Zubair | ✅ |
| 10 | [Persistence & File I/O](./phases/PHASE_10_PERSISTENCE.md) | Sunil | ✅ |
| 11 | [AI Feature](./phases/PHASE_11_AI_FEATURE.md) | Zubair | ✅ |
| 12 | [Streams, Analytics & Concurrency](./phases/PHASE_12_STREAMS_CONCURRENCY.md) | All | ✅ |
| 13 | [Console UI](./phases/PHASE_13_CONSOLE_UI.md) | Anushtha | ✅ |
| 14 | [Testing & Quality](./phases/PHASE_14_TESTING.md) | Sunil | ✅ |
| 15 | [Dockerization](./phases/PHASE_15_DOCKER.md) | Sunil | ✅ |
| 16 | [Documentation & Submission](./phases/PHASE_16_DOCUMENTATION.md) | Anushtha + Sunil | 🟡 |

---

## Team

Four roles, each owning a vertical slice of the SDLC — see
**[TEAM_AND_WORKFLOW.md](./TEAM_AND_WORKFLOW.md)** for the dependency graphs and
how the work interlocks.

| Member | Role | Sheet |
|---|---|---|
| Sunil Kumar B A | System Design & Data Architecture | [SUNIL.md](./team/SUNIL.md) |
| Varun P S | Core Domain Engineering | [VARUN.md](./team/VARUN.md) |
| Zubair | Services & Integration Engineering | [ZUBAIR.md](./team/ZUBAIR.md) |
| Anushtha Sharma | Experience & Interface Design | [ANUSHTHA.md](./team/ANUSHTHA.md) |

---

## Generating JavaDoc

```bash
javadoc -d docs/javadoc -encoding UTF-8 -charset UTF-8 \
        -windowtitle "MediTrack API" \
        -sourcepath src/main/java -subpackages com.airtribe.meditrack
```
