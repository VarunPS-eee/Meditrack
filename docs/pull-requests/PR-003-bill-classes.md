# PR-003 — Bill and immutable BillSummary

| | |
|---|---|
| **Commit** | `159e5bb` |
| **Author** | Varun P S |
| **Status** | ✅ Merged |
| **Date** | 2026-07-24 |

---

## What

Added the `Bill` entity and `BillSummary` as an immutable value type.

## Why

Billing is a graded feature, and `BillSummary` covers the rubric's immutable-class requirement.

## Contents

- [x] `Bill` — id, patient, total amount, date, with getters and setters
- [x] `BillSummary` — **immutable**
  - [x] `final` class
  - [x] All fields `private final`
  - [x] No setters
  - [x] **Defensive copy** of the mutable `Date` in the constructor
  - [x] **Defensive copy** on the date getter

## Rubric impact

| Item | Status after this PR |
|---|---|
| Advanced OOP — immutable class | ✅ All rules applied, with numbered comments |

## Review notes

`BillSummary` was textbook — the defensive copying on both the constructor and the getter was
correct and clearly commented, which is exactly the part people usually miss.

## Later extended in PR-005

`Bill` became an **abstract Template Method** base with three concrete subclasses
(`ConsultationBill`, `ProcedureBill`, `EmergencyBill`), so the Factory pattern had types to
choose between, and `Payable` had an implementer.

`BillSummary` gained line items, payment tracking, and a `withPayment()` method that returns a
new instance rather than mutating — extending the immutability rather than compromising it. Its
`Date` field became `LocalDateTime`, which is immutable and therefore needs no defensive copy at
all.

---

*[PR index](./README.md)*
