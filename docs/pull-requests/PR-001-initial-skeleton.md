# PR-001 — Initial project skeleton

| | |
|---|---|
| **Branch** | `setup/initial-skeleton` → `main` |
| **Author** | Varun P S |
| **Status** | ✅ Merged (`edce362`, `aa198c7`) |
| **Date** | 2026-07-21 → 2026-07-23 |

---

## What

Created the repository, the package directory structure under
`src/main/java/com/airtribe/meditrack/`, the `docs/` folder with placeholder files, `.gitignore`,
and IntelliJ project configuration.

## Why

Give all three members a common structure to branch from, so parallel work would merge cleanly.

## Contents

- [x] `src/main/java/com/airtribe/meditrack/` with sub-package directories
- [x] `docs/` with `JVM_Report.md`, `Setup_Instructions.md`, `Design_Decisions.md` (empty stubs)
- [x] `README.md` (stub)
- [x] `.gitignore` for IntelliJ, Eclipse, NetBeans, VS Code, macOS
- [x] `MediTrack.iml`, `.idea/` configuration
- [x] `main` branch protection enabled

## Notes

A merge conflict arose between the initial commit and the skeleton branch, resolved in `aa198c7`
by keeping the skeleton files.

## Issues later found in this skeleton

Two problems originated here and were fixed in later PRs — both worth recording:

1. **Package `interface/`** — a reserved Java keyword; `javac` rejects it outright.
   Fixed in [PR-004](./PR-004-interfaces.md).
2. **Source root set to `src`** rather than `src/main/java`, which led to package declarations of
   the form `main.java.com.airtribe.meditrack.*`. Fixed in
   [PR-005](./PR-005-complete-meditrack.md).

**Lesson:** validate the package structure against the rubric *before* filling it with classes.
The eventual fix touched every file in the repository.

---

*[PR index](./README.md)*
