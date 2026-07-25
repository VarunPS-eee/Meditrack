# Contributing to MediTrack

Thanks for taking an interest. MediTrack is an open-source, dependency-free
clinic management system written in Core Java 21, and contributions of every
size are welcome — from a typo in the docs to a new billing strategy.

New to open source? Issues tagged
[`good first issue`](../../issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22)
are scoped to be completable in an evening.

---

## Table of contents

- [Ground rules](#ground-rules)
- [Getting set up](#getting-set-up)
- [Running the tests](#running-the-tests)
- [Where things live](#where-things-live)
- [Coding conventions](#coding-conventions)
- [Commit messages](#commit-messages)
- [Pull request process](#pull-request-process)
- [Reporting bugs](#reporting-bugs)

---

## Ground rules

Three rules are non-negotiable, because they define what this project *is*:

1. **Zero third-party runtime dependencies.** The JDK standard library only. If
   you find yourself wanting a library, that is usually a signal the feature
   belongs in a different project. Test-scope and analysis-only tooling
   (JaCoCo, SonarQube) is fine — it never ships in the artefact.
2. **The test suite stays green.** `TestRunner` exits non-zero on failure and
   the Docker build runs it, so a red suite cannot produce an image. Add
   assertions for what you change.
3. **No real patient data. Ever.** Not in code, not in fixtures, not in issues,
   not in screenshots. Use `DemoDataSeeder` or invent something obviously fake.

---

## Getting set up

You need **JDK 21 or newer**. Nothing else — no Maven, no Gradle.

```bash
git clone https://github.com/VarunPS-eee/Meditrack.git
cd Meditrack
```

**Compile** (Linux / macOS / Git Bash):

```bash
javac -encoding UTF-8 -d out $(find src/main/java -name "*.java")
```

**Compile** (Windows PowerShell):

```powershell
$s = Get-ChildItem src\main\java -Filter *.java -Recurse | % { $_.FullName }
javac -encoding UTF-8 -d out $s
```

**Run:**

```bash
java -cp out com.airtribe.meditrack.Main --seedDemo
```

`--seedDemo` gives you a populated clinic, which is by far the easiest way to
explore. See the [User Manual](docs/USER_MANUAL.md) for the full menu tour.

Prefer containers? `docker build -t meditrack:dev . && docker run -it --rm meditrack:dev --seedDemo`.
The `-it` is required — it is an interactive menu, and without a TTY it reads
EOF and exits immediately.

---

## Running the tests

```bash
java -cp out com.airtribe.meditrack.Main --runTests
```

MediTrack uses a **hand-written test runner**, not JUnit — a deliberate choice
so the project keeps its zero-dependency property. `TestRunner` provides an
assertion vocabulary, suite grouping, per-test exception capture, a failure
list, and a non-zero exit on failure.

Adding a test means adding assertions to an existing suite in
[`TestRunner.java`](src/main/java/com/airtribe/meditrack/test/TestRunner.java),
or adding a new suite method and calling it from the runner.

Write assertions that would catch a real regression, not ones that merely
exercise a getter. The suite's best existing examples:

- immutability verified **by reflection**, so it fails if someone adds a setter
- 10 threads × 100 IDs asserting 1000 unique, which fails intermittently with a
  plain `int++`
- deep vs shallow copy asserted to actually diverge after a mutation

---

## Where things live

```
src/main/java/com/airtribe/meditrack/
├── constants/    Tax rates, clinic rules, ID prefixes, file paths
├── entity/       Domain model — Person, Patient, Doctor, Appointment, Bill…
├── exception/    Five checked exceptions, all supporting cause chaining
├── factory/      BillFactory — which Bill subclass to build
├── interfaces/   Searchable, Payable, BillingStrategy, AppointmentObserver
├── observer/     SMS, audit log and console reminder observers
├── service/      Use-case orchestration — the only layer that coordinates
├── strategy/     Standard, senior-citizen and insurance pricing
├── test/         TestRunner and all suites
├── util/         DataStore, validation, singletons, CSV, dates, AI triage
└── Main.java     Console menu — deliberately thin
```

The layering rule: **services orchestrate, entities hold behaviour, utils are
stateless helpers, `Main` only reads input and prints.** Validation belongs in
`Validator`, storage in `DataStore`, formatting on the entity. If you find
yourself validating inside a service or a menu handler, that logic wants to
move.

---

## Coding conventions

Match the surrounding code. Concretely:

- **Java 21**, 4-space indent, no tabs
- **Braces on the same line**, always braces even for one-line `if`
- **`final` by default** on fields; immutable types get `final` classes
- **Checked exceptions** for domain failures, so the compiler forces a handler
- **`try-with-resources`** for anything closeable, no exceptions
- **JavaDoc on every public type and method.** Explain *why*, not *what* — the
  signature already says what
- **`java.time`**, never `java.util.Date` or `SimpleDateFormat`
- **No `System.out` outside `Main`, `TestRunner` and the observers.** Services
  return values; they do not print
- Prefer **composition and interfaces** over deep inheritance

### Adding a new billing strategy

The most common contribution. Implement `BillingStrategy`, then wire it into
`BillFactory.chooseStrategy()`. Note that the strategy is derived from patient
attributes — the factory decides, the strategy prices, and neither re-decides
the other's job. Add a suite to `TestRunner` covering the discount maths and at
least one boundary case.

---

## Commit messages

Imperative subject under ~72 characters, then a blank line, then a body that
explains **why** the change is right — the diff already shows what changed.

```
Add late-cancellation fee to the billing strategies

A cancellation inside the 24-hour window still consumes the slot, so the
clinic bears the cost. StandardBillingStrategy now applies a flat fee...
```

Please do **not** add AI-assistant co-author trailers to commits.

---

## Pull request process

1. **Open an issue first** for anything beyond a typo, so we can agree on the
   approach before you spend time on it
2. **Branch from `main`** — `feature/short-description` or `fix/short-description`
3. **Compile and run the full suite** before pushing
4. **Fill in the PR template** — especially the "why" and the testing sections
5. **One logical change per PR.** A drive-by reformat buried in a behaviour
   change is very hard to review
6. **Update the docs** in the same PR if you changed behaviour

A maintainer will review within a week. PRs need a green test run and one
approval to merge.

---

## Reporting bugs

Use the [bug report template](../../issues/new?template=bug_report.yml). The
single most useful thing you can include is the **exact menu path** — for
example "menu 2 → 3, entered a date of 2020-01-01" — because it makes the
problem reproducible in seconds.

Please check [SECURITY.md](SECURITY.md) first if the issue has security
implications. Those go through private disclosure, not the public tracker.
