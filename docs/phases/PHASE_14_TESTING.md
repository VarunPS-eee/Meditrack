# Phase 14 — Testing & Quality

| | |
|---|---|
| **Owner** | **Sunil Kumar B A** |
| **Depends on** | Phase 13 |
| **Blocks** | Phase 15 |
| **Rubric** | Testing (manual runner, no JUnit) |
| **Status** | ✅ Complete — **325/325 passing** |

---

## Objective

Build a manual test runner — no JUnit, as the assignment requires — that actually proves the
system works, and can gate the Docker build.

---

## Result

```
  ============================================================
    TEST SUMMARY
  ============================================================
    Total  : 325
    Passed : 325
    Failed : 0
    Rate   : 100.0%
  ============================================================
    ALL TESTS PASSED
  ============================================================
```

---

## Tasks

### Test framework
- [x] `assertTrue` / `assertFalse` / `assertNotNull`
- [x] `assertEquals(String, Object, Object)` and a `double` overload with epsilon
- [x] `assertThrows(description, ExpectedType.class, action)`
- [x] `assertNoThrow(description, supplier)`
- [x] `tryGet` — converts checked exceptions into failures
- [x] `ThrowingRunnable` / `ThrowingSupplier` functional interfaces
- [x] Suite grouping with headers
- [x] Pass/fail tally and failure list
- [x] **`System.exit(1)` on failure** — makes it usable as a CI gate

### Suites (20)
- [x] Validator — 22 assertions
- [x] DateUtil — 14
- [x] Entities — inheritance, encapsulation, identity — 24
- [x] Cloning — deep vs shallow — 14
- [x] BillSummary immutability — 11
- [x] Enums and state machine — 19
- [x] Singletons — 16
- [x] DataStore<T> — 21
- [x] Generics and Iterator contract — 12
- [x] PatientService — 15
- [x] DoctorService — 15
- [x] AppointmentService — 18
- [x] Billing, Factory, Template Method — 27
- [x] Strategy — 12
- [x] Observer — 15
- [x] AIHelper — 22
- [x] Exceptions and chaining — 14
- [x] CSV round-trip — 20
- [x] Concurrency — 10
- [x] Streams and lambdas — 14

### Integration
- [x] `--runTests` flag on `Main`
- [x] Runs inside the Docker build — a red suite fails the image
- [x] Real filesystem round-trip in the CSV suite

---

## What the suite actually proves

These are the assertions that would catch a real regression, not just exercise a getter.

### Deep vs shallow copy

```java
original.addMedicalHistoryEntry("NEW ENTRY");
assertEquals("deep copy is unaffected", 1, deep.getMedicalHistory().size());
assertEquals("shallow copy shares the list and changed too", 2, shallow.getMedicalHistory().size());
```

### Immutability, verified by reflection

Rather than trusting that `BillSummary` is immutable, the test checks:

```java
assertTrue("BillSummary class is final",
        Modifier.isFinal(BillSummary.class.getModifiers()));

boolean allFieldsFinal = Arrays.stream(BillSummary.class.getDeclaredFields())
        .filter(f -> !f.isSynthetic() && !Modifier.isStatic(f.getModifiers()))
        .allMatch(f -> Modifier.isFinal(f.getModifiers()));
assertTrue("every instance field is final", allFieldsFinal);

boolean hasSetters = Arrays.stream(BillSummary.class.getDeclaredMethods())
        .anyMatch(m -> m.getName().startsWith("set"));
assertFalse("no setter methods exist", hasSetters);
```

If someone adds a setter later, this fails.

### Concurrency

```java
// 10 threads × 100 ids
assertEquals("AtomicInteger issued 1000 unique ids with no collisions", 1000, ids.size());
```

Fails intermittently with a plain `int++` — which is what makes it worth having.

### Exception chaining

```java
try {
    Validator.validateAge("abc");
    fail("expected InvalidDataException");
} catch (InvalidDataException e) {
    assertTrue("chained cause is NumberFormatException",
            e.getCause() instanceof NumberFormatException);
    assertEquals("field name captured", "age", e.getFieldName());
}
```

### State machine

```java
assertFalse("PENDING cannot jump straight to COMPLETED",
        AppointmentStatus.PENDING.canTransitionTo(AppointmentStatus.COMPLETED));
assertThrows("cannot cancel a COMPLETED appointment", InvalidDataException.class,
        () -> service.cancelAppointment(id));
```

---

## Two bugs the suite caught — in the tests

The first run was 321/323. Both failures were **test bugs**, and both are worth recording
because the reasoning matters more than the fix.

### 1. `assertNoThrow` treats `null` as failure

```java
assertNoThrow("null email accepted (optional field)", () -> Validator.validateEmail(null));
```

`validateEmail(null)` **correctly** returns `null` — email is optional. The helper's "returned
null" check made correct behaviour look like a bug.

**Fixed the test, not the code**, and left a comment saying so:

```java
// validateEmail(null) legitimately returns null, so assert on the value, not on
// assertNoThrow — which treats a null return as a failure.
assertEquals("null email accepted and returns null (optional field)",
        null, tryGet(() -> Validator.validateEmail(null)));
```

### 2. An assertion placed after the state it depended on had changed

```java
assertTrue("free slots exclude the booked one", !service.getAvailableSlots(...).contains(slot));
```

This ran *after* the appointment had been completed. A `COMPLETED` appointment is terminal, so
the system correctly **releases** its slot back to availability. The code was right; the
assertion was in the wrong place.

**Fixed by splitting it into two assertions** that each test the real behaviour:

```java
// before the lifecycle transitions:
assertFalse("a live booking removes its slot from availability", ...);

// after completion:
assertTrue("a completed appointment releases its slot back to availability", ...);
```

**The lesson:** tests are wrong at least as often as code. Both times the instinct was to "fix"
the product — and both times that would have introduced a real bug.

---

## Why no JUnit

The assignment requires a manual runner. Building one is also instructive: it makes clear what a
framework actually provides — assertion vocabulary, isolation between tests, failure reporting,
and an exit code. Ours implements all four in about 100 lines.

---

## Docker integration

```dockerfile
RUN javac -encoding UTF-8 -d out @sources.txt; \
    java -Dfile.encoding=UTF-8 -cp out com.airtribe.meditrack.test.TestRunner
```

Because `TestRunner` exits non-zero on failure, **a red suite fails the image build**. Tests gate
the artefact rather than merely reporting after it.

---

## Manual verification performed

Beyond the automated suite:

- [x] Full menu walkthrough — every option exercised
- [x] Demo seed → book → confirm → complete → bill → pay
- [x] CSV save, exit, reload with `--loadData`, verify state
- [x] Comma-containing field round-trip verified in the file and on screen
- [x] Strategy comparison output verified for all three policies
- [x] AI triage verified across all six specialities
- [x] UTF-8 (`₹`) verified on Windows and in Alpine
- [x] Docker: build, test, interactive run
- [x] Piped/EOF input terminates cleanly

---

## Exit criteria

- [x] 325 assertions across 20 suites
- [x] 100% pass rate
- [x] Every rubric feature covered by at least one assertion
- [x] Edge cases: null, empty, boundary, illegal transitions
- [x] Non-zero exit code on failure
- [x] Wired into the Docker build

---

**Previous:** [Phase 13](./PHASE_13_CONSOLE_UI.md) ·
**Next:** [Phase 15 — Docker](./PHASE_15_DOCKER.md) ·
[Phase index](./README.md) · [Sunil's tasks](../team/SUNIL.md)
