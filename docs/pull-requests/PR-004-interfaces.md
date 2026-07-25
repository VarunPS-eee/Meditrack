# PR-004 — Searchable and Payable interfaces + reserved keyword fix

| | |
|---|---|
| **Commit** | `71d61f0` |
| **Author** | Varun P S |
| **Status** | ✅ Merged |
| **Date** | 2026-07-24 |

---

## What

Added the `Searchable` and `Payable` interfaces, and renamed the package `interface` →
`interfaces`.

## Why

The assignment skeleton specifies a package named `interface`. **`interface` is a reserved Java
keyword and cannot be used as a package name** — `javac` rejects it outright.

Good catch. This would have blocked every subsequent commit.

## Contents

- [x] `interfaces/Searchable.java` — `matches(String)`
- [x] `interfaces/Payable.java` — `getAmountDue()`, `processPayment(double)`
- [x] Package renamed `interface` → `interfaces`

## Rubric impact

| Item | Status after this PR |
|---|---|
| Abstraction & Interfaces (10 pts) | ⚠️ Interfaces declared, but no class implemented them yet |

## Review notes

The reserved-keyword fix is the important part of this PR. Renaming to `interfaces` is the
conventional resolution and is documented in
[Design_Decisions §4.1](../Design_Decisions.md#41-interface--interfaces).

## Later extended in PR-005

Both interfaces gained **default methods**, and `Payable` gained a **static** method — so between
them they demonstrate all three method kinds a modern Java interface can carry:

```java
double getAmountDue();                               // abstract
default boolean isFullyPaid() { ... }                // default
static String formatCurrency(double amount) { ... }  // static
```

`Searchable` was also restructured so implementers supply only `getSearchableText()` and inherit
the matching logic — keeping the rule in one place while letting each entity decide which fields
are searchable.

Every `MedicalEntity` now implements `Searchable`; `Bill` implements `Payable`. Two further
interfaces were added: `BillingStrategy` and `AppointmentObserver`.

---

*[PR index](./README.md)*
