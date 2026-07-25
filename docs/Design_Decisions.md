# MediTrack — Design Decisions

This document records the main design choices in MediTrack and the reasoning behind them. The project was split into four modules — entities (Varun), services & exceptions (Zubair), utils & storage (Sunil), and UI/docs/testing (Anushtha) — and the decisions below are what let those modules connect cleanly.

## 1. Layered architecture

The code flows in one direction: **UI → services → utils/storage → entities**. `Main.java` only reads input and prints output; it never touches a `DataStore` directly. All rules (age limits, slot clashes, billing maths) live in the service layer, and all mechanics (storage, CSV, id generation, validation) live in utils. The payoff is that the console UI could be replaced with a web API tomorrow without rewriting any business logic — and it made the four-way work split possible, because each layer is a contract the next one codes against.

## 2. OOP model

**Inheritance where there is true "is-a"**: `MedicalEntity` (abstract, owns the id and audit timestamps) → `Person` (abstract, adds name/age/contact) → `Patient` and `Doctor`. `Appointment` and `Bill` extend `MedicalEntity` directly — they are records of events, not people.

**Abstract methods force consistency**: every entity must implement `displayDetails()` and `getEntityType()`, so the UI can print any entity polymorphically without `instanceof` checks.

**Interfaces for capabilities**: `Searchable` is implemented by everything that can be found by keyword; `Payable` only by things money can be collected against. Capabilities-as-interfaces avoids forcing unrelated classes into one inheritance tree.

**Encapsulation with defensive copies**: entities never expose internal lists directly. `Patient.getMedicalHistory()` returns a copy, and constructors copy incoming lists. This was a deliberate guard against the classic bug where two parts of the program silently share and mutate one list. `BillSummary` goes further and is fully immutable — a snapshot meant for reports should never change after creation.

## 3. Enums that carry behaviour

`Specialization`, `AppointmentStatus` and `BillType` are enums with fields and methods, not bare constants. The standout decision is `AppointmentStatus.canTransitionTo(...)`: the legal state machine (PENDING → CONFIRMED → COMPLETED, cancellations terminal) lives *inside* the enum. Services cannot accidentally revive a cancelled appointment because the type itself refuses. Similarly, `Specialization` owns its base fee and symptom keywords, which is what makes the AI helper possible without a database.

## 4. Generics for storage

`DataStore<T extends MedicalEntity>` is one generic, type-safe, in-memory repository reused for all entities. The bound (`extends MedicalEntity`) is what lets the store call `getId()` on any element. One implementation, four repositories (`DataStore<Patient>`, `DataStore<Doctor>`, ...), zero copy-pasted CRUD code — this was the clearest win of generics in the project.

## 5. Design patterns

**Singleton — `IdGenerator`.** Ids must be unique across the whole application, so exactly one counter object may exist. It also demonstrates why singletons are used *sparingly* — they are effectively global state.

**Factory — `BillFactory`.** Callers say *what kind* of bill they need (`BillType.EMERGENCY`); the factory decides *which class* to build (`EmergencyBill` with its surcharge). New bill kinds mean a new subclass plus one factory case — no caller changes.

**Strategy — `BillingStrategy`.** Standard, senior-citizen and insurance pricing are interchangeable objects, chosen per patient at billing time, instead of an if/else ladder inside `Bill`.

**Observer — `NotificationService`.** When an appointment is booked or cancelled, registered observers (console reminder, SMS simulation, audit log) are notified. The service layer doesn't know or care who is listening — the UI registers whichever observers it wants at startup.

## 6. Exceptions: checked and custom

We created custom checked exceptions (`InvalidDataException`, `AppointmentNotFoundException`, `SlotUnavailableException`, `EntityNotFoundException`, `DataPersistenceException`) rather than throwing `RuntimeException` with string messages. Checked was a deliberate choice: these are *recoverable, expected* failures (user typo, taken slot), and the compiler forces the UI to handle them. The exceptions also carry structured data (which field was rejected, which id was missing), so the UI can print a precise message instead of parsing text.

## 7. Persistence: CSV over serialization

Data saves to plain CSV files, one per entity type. We chose CSV over Java serialization as the primary format because the files are human-readable, diffable in git during debugging, and openable in a spreadsheet — at the cost of writing our own escaping (commas become `||`, lists join on `;`). Two consequences shaped `CSVUtil`:

1. **Relationships are stored as ids.** An appointment row stores `patientId`/`doctorId`, not the whole objects, and is re-linked to the loaded objects at read time. Duplicating the person data inside every appointment row would let the two copies drift apart.
2. **Corrupt rows are skipped, not fatal.** One damaged line in a file should not destroy a clinic's whole dataset, so readers log and skip bad rows.

## 8. Testing without a framework

`TestRunner.java` is a plain `main` method with `check(description, condition)` helpers — no JUnit. This was partly a constraint (no build tool, no dependencies) and partly pedagogical: writing the assertion helpers by hand shows what a test framework actually does. The runner exits with code 1 on any failure so it can gate the Docker build. Tests cover each layer: entity behaviour (defensive copies, the status state machine), utils (validator edge cases, CSV round-trips including a name containing a comma and a deliberately corrupt row), and services (booking clashes, billing maths, every custom exception actually being thrown).

## 9. Trade-offs we accepted

In-memory storage means data is lost unless the user saves — acceptable for a teaching project, and it kept the storage layer simple. The console UI re-reads all input as text and re-validates in the services (double validation is deliberate: the UI validates for friendliness, services validate for safety). And the "AI" recommendation is keyword scoring, not machine learning — honest about what it is, but it exercises streams, records and enum data nicely.

---
*Author: Anushtha (UI, Docs & Testing module)*
