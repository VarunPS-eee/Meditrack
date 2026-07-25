# Code Quality — SonarQube & Coverage

Real measurements, honestly reported, including what is **not** covered.

> All numbers on this page come from an actual run against SonarQube
> **26.7.0** (Community, local Docker) and **JaCoCo 0.8.12**, on Java 21.0.11.
> Reproduce them with the commands in [Running the analysis](#running-the-analysis).

---

## Current standing

| Metric | Value | Rating |
|---|---:|:---:|
| **Bugs** | **0** | 🟢 **A** |
| **Vulnerabilities** | **0** | 🟢 **A** |
| **Security hotspots** | **0** | 🟢 **A** |
| **Maintainability** | 276 code smells | 🟢 **A** |
| **Duplicated lines** | **0.0%** | 🟢 |
| Lines of code | 5,409 | — |
| Tests | 325 / 325 passing | 🟢 |
| Coverage (Sonar, line) | 44.4% | 🟠 |
| Coverage (JaCoCo, instruction) | 58.1% | 🟠 |

**Reliability went from E to A in this pass** — all 13 bugs were found and fixed.
Coverage is the honest weak spot and is discussed in full below.

---

## The 13 bugs, and what each one actually was

Every bug SonarQube reported was fixed. None were suppressed.

### Genuine defects (5)

| Rule | Where | The defect |
|---|---|---|
| **S2095** 🔴 *blocker* | `TestRunner:966` | `ExecutorService` shut down in a `finally`, but if `latch.await()` threw before reaching it the pool's threads leaked. Now a **try-with-resources** — `ExecutorService` is `AutoCloseable` as of Java 19 |
| **S2259** ×4 | `TestRunner` | `tryGet()` returns `null` on failure and `assertNotNull` **records** a failure without halting — so the next line threw `NullPointerException`, and the run died pointing at the symptom instead of the cause |
| **S8700** | `AppConfig:133` | Uptime measured by subtracting two `LocalDateTime`s. Wall clocks jump — NTP corrections, DST, manual changes — so this measured *the clock*, not elapsed time, and could go **negative**. Now `System.nanoTime()`, which is monotonic |
| **S8700** ×3 | `DateUtil` | Durations computed on zone-less `LocalDateTime`. Across a DST boundary 01:00→04:00 is three calendar hours but two real ones, so reminders would have been an hour out twice a year. Both operands now resolve through the system zone |
| **S2184** | `TestRunner:993` | `int * int` widened to `double` after the multiplication. Harmless at 10×100, but it would silently truncate if the thread counts were ever raised |

### False positives, fixed without weakening the test (3)

`S2159` ×2 and `S1764` flagged the **`equals`/`hashCode` contract tests**:

```java
assertFalse("different types are never equal", patient.equals(doctor));
assertFalse("null is never equal",             patient.equals(null));
assertTrue ("reflexive",                       patient.equals(patient));
```

Sonar is right that each result is statically provable — but *proving `equals`
returns false for an unrelated type is the contract*, and the test has to make
the call at run time to be worth anything.

Deleting them would have removed real coverage of the contract, and
`@SuppressWarnings` would have hidden the rule everywhere in the file. Instead
the calls now bind through `Object`-typed references, which keeps the runtime
check intact and states the intent plainly.

### A weakness the bugs exposed

Chasing the `S2259` findings turned up something Sonar did not report. The
`TestRunner` JavaDoc claimed *"exception capture so one failure does not stop the
run"* — but suites were invoked directly from `main()` with **no `try`/`catch`
around them**. A single escaped exception would abandon every remaining suite.

That is now true rather than merely documented:

```java
guard("Validator",  TestRunner::runValidatorTests);
guard("DateUtil",   TestRunner::runDateUtilTests);
// ... 20 suites
```

`guard` records an escaped throwable as one clean failure and carries on. A new
`assertPresent` helper asserts non-null, returns the value, and throws
`AssertionError` when absent — so a null now fails at the real cause rather than
cascading into an unrelated NPE.

---

## The 276 code smells in context

The headline number is misleading, and it is worth being precise about why.

| Rule | Count | Assessment |
|---|---:|---|
| **S106** — *"Replace System.out by a logger"* | **188** | **Not applicable.** MediTrack is a console application; printing to stdout is its user interface, and it has a hard zero-dependency rule so there is no logging framework to adopt. 68% of all smells are this one rule |
| **S1192** — duplicated string literals | 26 | **Worth fixing.** Genuine; mostly repeated format strings in menu code |
| **S8688** — specify a zone in `.now()` | 23 | **Partly worth fixing.** Correct in principle; most instances are in test setup where the system zone is intended |
| **S6204** — use `.toList()` | 17 | **Worth fixing.** Trivially mechanical Java 16+ modernisation |
| **S8694** | 6 | Minor |
| **S1181** — catch `Exception` not `Throwable` | 4 | **Deliberate.** `guard` and `tryGet` must catch `AssertionError`, which is an `Error`. Catching `Exception` would break them |
| **S107** — too many parameters | 3 | Entity constructors. Arguable |
| Others | 9 | Minor |

Strip the 188 inapplicable `S106` hits and the 4 deliberate `S1181` ones, and
**84 actionable smells** remain across 5,409 lines. Sonar already rates
maintainability **A**, with technical debt at ~45 hours against a codebase this
size.

These are **not** fixed yet. They are real, small, and worth a follow-up pass.

---

## Coverage — the honest picture

```
Overall        instruction  58.1%   (8,850 / 15,237)
               branch       39.0%   (416 / 1,066)

Excluding Main, DemoDataSeeder and TestRunner
               instruction  61.7%   (5,376 / 8,716)
               branch       46.6%   (365 / 783)
```

### Why 100% is not reachable here

**`Main` alone is 2,425 instructions at 0.6% coverage** — it is a `Scanner` loop
that blocks on human input. Driving it from a headless runner means piping
scripted stdin through every branch of nine menus, and several paths end in
`System.exit`, which terminates the JVM and takes the coverage data with it.

Chasing 100% would mean either:

- writing hundreds of brittle stdin-driving tests whose only purpose is to move a
  number, which tends to make a suite worse rather than better; or
- refactoring `Main` to inject its I/O — defensible, but a real architectural
  change that should be its own decision, not a side effect of a coverage target.

The three excluded classes are excluded honestly and for stated reasons:

| Excluded | Why |
|---|---|
| `Main` | Interactive `Scanner` loop; not drivable headlessly |
| `DemoDataSeeder` | Fixture data, not logic |
| `TestRunner` | The test scaffolding itself |

### Where the real gaps are

These are ordinary, worth closing, and have nothing to do with `Main`:

| Class | Instruction | Gap |
|---|---:|---|
| `ConsoleReminderObserver` | 0.0% | Never instantiated in any test |
| `BillingService` | 16.1% | Only the happy path is exercised |
| `NotificationService.ReminderTask` | 26.5% | The scheduled sweep is never triggered directly |
| `Bill.LineItem` | 32.7% | Accessors and formatting untested |
| `InsuranceBillingStrategy` | 39.4% | Boundary conditions missing |
| `Appointment` | 40.0% | Reschedule and cancel paths thin |
| `DataStore` | 45.6% | Iterator and delete paths partly untested |

**A realistic target is 85% instruction coverage excluding `Main`**, reachable by
adding suites for the classes above. That is a meaningful goal; 100% is a number,
not a goal.

---

## Running the analysis

Analysis runs through the standalone `sonar-scanner` CLI against pre-compiled
classes — MediTrack has no build tool by design, and JaCoCo attaches as a
`-javaagent` to the existing `TestRunner`. **No Maven migration, no JUnit, and
nothing added to the shipped artefact.**

```bash
# 1 · compile
javac -encoding UTF-8 -d out $(find src/main/java -name "*.java")

# 2 · run the suite under the JaCoCo agent
java -javaagent:.local/tools/org.jacoco.agent-0.8.12-runtime.jar=destfile=.local/jacoco.exec \
     -cp out com.airtribe.meditrack.Main --runTests

# 3 · turn the exec file into reports
java -jar .local/tools/org.jacoco.cli-0.8.12-nodeps.jar report .local/jacoco.exec \
     --classfiles out --sourcefiles src/main/java \
     --html coverage/html --xml coverage/jacoco.xml --csv coverage/jacoco.csv

# 4 · start SonarQube (first run only)
docker run -d --name sonarqube -p 9000:9000 sonarqube:community

# 5 · scan
sonar-scanner -Dsonar.token=$SONAR_TOKEN
```

Configuration lives in
[`sonar-project.properties`](../sonar-project.properties). The dashboard is at
<http://localhost:9000/dashboard?id=meditrack>, and the HTML coverage report at
`coverage/html/index.html`.

Getting the JaCoCo jars, if you do not have them:

```bash
mvn dependency:copy -Dartifact=org.jacoco:org.jacoco.agent:0.8.12:jar:runtime -DoutputDirectory=.local/tools
mvn dependency:copy -Dartifact=org.jacoco:org.jacoco.cli:0.8.12:jar:nodeps   -DoutputDirectory=.local/tools
```

> `sonar.java.binaries=out` matters. Without compiled classes Sonar's Java
> analyser cannot resolve types and most rules silently downgrade to
> syntax-only checks — you get a green board that means nothing.

---

## Summary

| | Before | After |
|---|---|---|
| Bugs | 13 | **0** |
| Reliability rating | **E** | **A** |
| Vulnerabilities | 0 | 0 |
| Tests | 325/325 | 325/325 |
| Suite-level failure isolation | claimed, absent | implemented |
| Coverage | 58.0% | 58.1% |

**Outstanding work**, in priority order:

1. Cover `BillingService`, `ConsoleReminderObserver` and the strategy boundaries
   → target 85% excluding `Main`
2. Clear the ~84 actionable code smells (`S1192`, `S6204` are mechanical)
3. Decide whether `Main` should have injectable I/O, on architectural merit
   rather than to move a coverage number
