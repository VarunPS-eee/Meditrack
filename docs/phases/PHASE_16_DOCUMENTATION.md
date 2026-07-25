# Phase 16 — Documentation & Submission

| | |
|---|---|
| **Owner** | All three (Sunil leads) |
| **Depends on** | Phase 15 |
| **Status** | 🟡 In progress — **the only phase with outstanding work** |

---

## Objective

Complete the documentation set and submit. Everything remaining requires a human — screenshots,
a video, PR approvals, and an email.

---

## Tasks

### Core documents
- [x] `README.md` — setup, usage, sample output, architecture
- [x] `docs/JVM_Report.md` — all five required topics
- [x] `docs/Setup_Instructions.md` — three installation routes
- [x] `docs/Design_Decisions.md` — architecture, SOLID, patterns, deviations
- [x] `docs/DB_Design.md` — ER model, schemas, SQL migration path

### Phase documentation
- [x] `docs/phases/README.md` — index, dependency graph, parallelisation
- [x] Phase 0 — Ideation & Requirements
- [x] Phase 1 — Environment Setup & JVM
- [x] Phase 2 — High-Level Design
- [x] Phase 3 — Low-Level Design
- [x] Phase 4 — Data & Persistence Design
- [x] Phase 5 — Foundation Layer
- [x] Phase 6 — Domain Model
- [x] Phase 7 — Utilities, Storage & Singletons
- [x] Phase 8 — Service Layer
- [x] Phase 9 — Design Patterns
- [x] Phase 10 — Persistence & File I/O
- [x] Phase 11 — AI Feature
- [x] Phase 12 — Streams, Analytics & Concurrency
- [x] Phase 13 — Console UI
- [x] Phase 14 — Testing & Quality
- [x] Phase 15 — Dockerization
- [x] Phase 16 — Documentation & Submission (this file)

### Per-member documentation
- [x] `docs/team/VARUN.md` — tasks, status, PR record
- [x] `docs/team/ZUBAIR.md` — tasks, status, PR record
- [x] `docs/team/SUNIL.md` — tasks, status, PR record
- [x] `docs/pull-requests/README.md` — PR index and template

### Code documentation
- [x] JavaDoc on every public class
- [x] JavaDoc on every public method
- [x] `@param` / `@return` / `@throws` throughout
- [x] Design rationale in class-level docs where a choice is non-obvious
- [ ] **Generate JavaDoc HTML** — command documented, output not committed

### Outstanding — needs a human
- [ ] **Capture screenshots** — placeholders marked in `Setup_Instructions.md`
  - [ ] `java -version` output
  - [ ] Project structure in the IDE
  - [ ] Docker build succeeding
  - [ ] App running with the main menu
  - [ ] IntelliJ run configuration
- [ ] **Record the 2–3 minute walkthrough video** (optional but recommended)
- [ ] **UML class diagram export** (optional — Mermaid source already in `DB_Design.md`)
- [ ] **Open the PR** for `feature/complete-meditrack`
- [ ] **Get review approval** from at least one teammate
- [ ] **Merge to `main`**
- [ ] **Send the submission email** — subject `Java Assignment - [Your Name]`, with the repo and
      PR links

---

## Suggested video script (2–3 minutes)

| Time | Content |
|---|---|
| 0:00–0:20 | Repo tour — package structure, `docs/` |
| 0:20–0:40 | `docker run -it --rm meditrack:1.0.0 --seedDemo` — no Java needed |
| 0:40–1:00 | Menu 9 → 1: dynamic dispatch across `List<MedicalEntity>` |
| 1:00–1:20 | Menu 9 → 2: deep vs shallow copy diverging |
| 1:20–1:40 | Menu 6 → 1: AI triage with urgency flag and explanations |
| 1:40–2:00 | Menu 4 → 6: Strategy — one charge, three totals |
| 2:00–2:20 | Menu 8 → 1, exit, `--loadData` — persistence round-trip |
| 2:20–2:40 | `--runTests` — 325/325 |
| 2:40–3:00 | `docs/phases/` — the phase plan |

---

## Documentation inventory

| Document | Lines | Status |
|---|---|---|
| `README.md` | ~430 | ✅ |
| `docs/JVM_Report.md` | ~330 | ✅ |
| `docs/Setup_Instructions.md` | ~370 | ✅ |
| `docs/Design_Decisions.md` | ~450 | ✅ |
| `docs/DB_Design.md` | ~460 | ✅ |
| `docs/phases/` × 18 | ~2,100 | ✅ |
| `docs/team/` × 3 | ~600 | ✅ |
| `docs/pull-requests/` | ~200 | ✅ |

---

## Deliverables checklist (against the brief)

| Required deliverable | Status |
|---|---|
| Complete Java source in GitHub with readable commits & branches | ✅ |
| `README.md` with setup, usage, demo instructions, sample output | ✅ |
| `docs/JVM_Report.md` | ✅ |
| `docs/Setup_Instructions.md` | ✅ (screenshots pending) |
| `docs/Design_Decisions.md` | ✅ |
| `TestRunner.java` for manual tests | ✅ 325 assertions |
| UML class diagram *(optional)* | ✅ Mermaid in `DB_Design.md` |
| Walkthrough video *(optional)* | ⬜ Script provided |
| JavaDoc HTML *(bonus)* | ⬜ Command documented |
| **GitHub link** | ✅ |
| **PR link** | ⬜ Needs the PR opened |

---

## Exit criteria

- [x] Every required document written
- [x] Every phase documented with tasks and status
- [x] Per-member task and PR records complete
- [x] Rubric coverage mapped and verified
- [ ] Screenshots captured
- [ ] PR opened, approved and merged
- [ ] Submission email sent

---

**Previous:** [Phase 15](./PHASE_15_DOCKER.md) · [Phase index](./README.md)
