# MediTrack

A console-based clinic management system built in Core Java (no frameworks). Manage patients, doctors, appointments and billing from a menu-driven UI, with CSV persistence and a rule-based symptom checker that recommends a doctor.

## Features

- Patient registration with medical history, allergies and insurance status
- Doctor roster with specializations, fees, experience and ratings
- Appointment booking with double-booking prevention, confirm/cancel/complete/reschedule, and free-slot lookup
- Billing with bill types (consultation/procedure/emergency), tax, insurance and senior-citizen pricing, partial payments
- Search across patients, doctors and appointments
- AI symptom checker: describe symptoms in plain words, get a specialization and doctor recommendation
- Reports: revenue, appointments per doctor, status breakdowns
- Save/load everything to CSV files

## Quick start

Requires JDK 17+.

```bash
git clone https://github.com/VarunPS-eee/Meditrack.git
cd Meditrack
javac -d out $(find src/main/java -name "*.java")
java -cp out com.airtribe.meditrack.Main
```

Say `y` to the demo-data prompt on first run so the menus have data to show.

Run the test suite (82 assertions, exits non-zero on failure):

```bash
java -cp out com.airtribe.meditrack.test.TestRunner
```

Or with Docker:

```bash
docker compose up --build
```

## Project structure

```
src/main/java/com/airtribe/meditrack/
├── Main.java          # console UI
├── constants/         # shared config
├── entity/            # Patient, Doctor, Appointment, Bill hierarchy, enums
├── exception/         # custom checked exceptions
├── interfaces/        # Searchable, Payable, observer/strategy contracts
├── factory/           # BillFactory
├── strategy/          # pricing strategies
├── observer/          # appointment notifications
├── service/           # business logic
├── util/              # DataStore (generics), CSVUtil, Validator, AIHelper...
└── test/TestRunner.java
```

More detail in [docs/Setup_Instructions.md](docs/Setup_Instructions.md), [docs/Design_Decisions.md](docs/Design_Decisions.md) and [docs/JVM_Report.md](docs/JVM_Report.md).

## Team

| Member | Module |
|---|---|
| Varun | Entities & interfaces |
| Zubair | Services & exceptions |
| Sunil | Utils & data storage |
| Anushtha | UI, file I/O, docs & testing |
