# Phase 5 — Foundation Layer

| | |
|---|---|
| **Owner** | Varun (interfaces) + Zubair (exceptions) + Sunil (constants) |
| **Depends on** | Phase 3 |
| **Blocks** | **Everything** — this is the synchronisation point |
| **Rubric** | Package Structure & Java Basics — **10 pts** |
| **Status** | ✅ Complete |

---

## Objective

Build the layer every other package depends on — constants, interfaces, exceptions — and fix
the package structure. Nothing else can start in parallel until this lands.

---

## Tasks

### Package structure fix ⚠️
- [x] **Rename `main.java.com.airtribe.meditrack.*` → `com.airtribe.meditrack.*`**
- [x] Re-point the IntelliJ source root from `src` to `src/main/java`
- [x] Give `Main.java` a package declaration (it had none)
- [x] Verify the base package matches the rubric exactly
- [x] Confirm `interface/` → `interfaces/` (reserved keyword)
- [x] Full clean compile after the rename

### `constants/Constants.java` — Sunil
- [x] Branding, tax rate, registration fee, surcharge rates
- [x] Domain rules (age bounds, clinic hours, slot length, daily cap)
- [x] Id prefixes and padding width
- [x] Date/time format patterns
- [x] File paths
- [x] CLI argument names
- [x] **Static initialisation block** resolving `MEDITRACK_DATA_DIR`
- [x] Private constructor throwing `AssertionError`

### `interfaces/` — Varun
- [x] `Searchable` — 1 abstract + 3 **default** methods
- [x] `Payable` — 3 abstract + 3 default + 1 **static** method
- [x] `BillingStrategy` — `@FunctionalInterface`
- [x] `AppointmentObserver` — with a default opt-out hook

### `exception/` — Zubair
- [x] `InvalidDataException` — field name + rejected value + chaining
- [x] `AppointmentNotFoundException`
- [x] `EntityNotFoundException` — for the generic store
- [x] `SlotUnavailableException`
- [x] `DataPersistenceException` — wraps `IOException`
- [x] All checked, all with `serialVersionUID`
- [x] Chaining constructors on every type

### Java basics (rubric)
- [x] **Access modifiers** — `private` fields, `protected` constructors, `public` API, package-private helpers
- [x] **Static vs instance scope** — contrasted explicitly in `MedicalEntity`
- [x] **Static blocks** — `Constants`, `MedicalEntity`
- [x] **Primitives and casting** — `int`/`double`/`boolean`, `Math.round(x * 100) / 100.0`
- [x] **Compile-time constants** — `static final`

---

## The package rename

This was the highest-risk change in the project: it touched every file.

**Before**
```java
package main.java.com.airtribe.meditrack.entity;   // path ≠ package
public class Main { ... }                          // no package at all
```

**After**
```java
package com.airtribe.meditrack.entity;
package com.airtribe.meditrack;
```

**Why it mattered:**

1. The rubric specifies base package **`com.airtribe.meditrack`** — marks were at stake.
2. Every service and util written next would have imported `main.java.com.airtribe...`,
   baking the mistake in permanently.
3. `Main.java` sat in the *default package* while living under `.../meditrack/` — inconsistent,
   and it would not have compiled under the corrected source root.

The old code did compile, because IntelliJ had `src` marked as the source root and path matched
package. That is exactly what made it easy to miss.

> **If IntelliJ reports package errors after pulling this**, it cached the old source root.
> **File → Reload All from Disk**.

---

## Interface design highlights

`Payable` carries all three method kinds a modern Java interface can hold:

```java
double getAmountDue();                              // abstract
default boolean isFullyPaid() { ... }               // default — built on the abstract ones
static String formatCurrency(double amount) { ... } // static — belongs to the contract
```

`Searchable` pushes the *matching rule* into a default method so it exists once, while letting
each entity decide *which fields* are searchable:

```java
String getSearchableText();                    // entity decides what is searchable
default boolean matches(String keyword) { ... } // rule lives here, once
```

That is Single Responsibility and Open/Closed working together on one small interface.

---

## Exception design

All five are **checked**. Bad input in a console app is expected and recoverable — checked
exceptions force the UI to catch and re-prompt rather than crash.

Every type supports chaining:

```java
catch (NumberFormatException e) {
    throw new InvalidDataException("age", ageText, "must be a whole number", e);
}
```

The user sees a domain message; `getCause()` still holds the original for debugging. Asserted in
`TestRunner`.

---

## Deliverables

| File | Lines | Status |
|---|---|---|
| `constants/Constants.java` | 143 | ✅ |
| `interfaces/Searchable.java` | 74 | ✅ |
| `interfaces/Payable.java` | 79 | ✅ |
| `interfaces/BillingStrategy.java` | 41 | ✅ |
| `interfaces/AppointmentObserver.java` | 42 | ✅ |
| `exception/` × 5 | ~230 | ✅ |

---

## Exit criteria

- [x] Base package is `com.airtribe.meditrack` throughout
- [x] Whole project compiles clean after the rename
- [x] All four interfaces defined with default/static methods
- [x] All five exceptions defined with chaining
- [x] Static blocks demonstrated and observable at runtime
- [x] **Varun and Sunil unblocked to start Phases 6 and 7 in parallel**

---

**Previous:** [Phase 4](./PHASE_04_DATA_DESIGN.md) ·
**Next:** [Phase 6 — Domain Model](./PHASE_06_DOMAIN_MODEL.md) ·
[Phase index](./README.md)
