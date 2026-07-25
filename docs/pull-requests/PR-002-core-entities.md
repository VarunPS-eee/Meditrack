# PR-002 — Core entities and Specialization enum

| | |
|---|---|
| **Commit** | `3ec86d0` |
| **Author** | Varun P S |
| **Status** | ✅ Merged |
| **Date** | 2026-07-24 |

---

## What

First real domain code: the `Person` → `Doctor`/`Patient` hierarchy, `Appointment`,
`MedicalEntity`, and the `Specialization` and `AppointmentStatus` enums. `Main.java` was
relocated into the package directory.

## Why

Unblock the service layer — nothing else could be built against a domain that did not exist.

## Contents

- [x] `MedicalEntity` — abstract root with an id and abstract `displayDetails()`
- [x] `Person` — abstract, extends `MedicalEntity`, constructor chaining via `super`
- [x] `Doctor` — speciality and consultation fee, overrides `displayDetails()`
- [x] `Patient` — medical history, implements `Cloneable`
- [x] `Appointment` — implements `Cloneable` with a **deep copy** of patient and date
- [x] `Specialization` enum — five constants
- [x] `AppointmentStatus` enum — four constants
- [x] `Main.java` moved under `com/airtribe/meditrack/`

## Rubric impact

| Item | Status after this PR |
|---|---|
| Inheritance (10 pts) | ✅ Three-level hierarchy with `super` and chaining |
| Abstraction (part of 10) | ✅ `MedicalEntity`, `Person` abstract |
| Advanced OOP — cloning | ✅ Deep copy in `Appointment`, shallow in `Patient` |
| Advanced OOP — enums | ✅ Two enums replacing string constants |

## Review notes

Good early work — the deep-copy implementation in `Appointment` correctly cloned both the nested
`Patient` and the mutable `Date`, with comments explaining why.

## Issues later addressed

- Package declared as `main.java.com.airtribe.meditrack.entity` — fixed in
  [PR-005](./PR-005-complete-meditrack.md)
- `Main.java` moved into the directory but given **no package declaration** — fixed in PR-005
- `java.util.Date` later migrated to `LocalDateTime` (mutable type, non-thread-safe formatter)

---

*[PR index](./README.md)*
