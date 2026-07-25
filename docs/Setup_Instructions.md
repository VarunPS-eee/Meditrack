# MediTrack — Setup Instructions

MediTrack is a console-based clinic management system built in plain Java (no frameworks, no build tool). This guide takes you from a fresh machine to a running application.

## 1. Prerequisites

You need a **JDK, version 17 or newer**. The codebase uses records, switch expressions and text-friendly `var`-era APIs, so Java 8/11 will not compile it. Verify your install with:

```bash
java -version
javac -version
```

Both should report 17+. If not, install one — [Eclipse Temurin](https://adoptium.net) is a good free choice. An IDE is optional; IntelliJ IDEA Community works well because the repo already contains IntelliJ project files.

## 2. Get the code

```bash
git clone https://github.com/VarunPS-eee/Meditrack.git
cd Meditrack
git checkout main
git pull origin main
```

Never commit directly to `main`. Create your own branch first:

```bash
git checkout -b yourname-dev
```

## 3. Project layout

```
Meditrack/
├── docs/                     # project documentation (this folder)
└── src/main/java/com/airtribe/meditrack/
    ├── Main.java             # console UI entry point
    ├── constants/            # shared configuration values
    ├── entity/               # Patient, Doctor, Appointment, Bill...
    ├── exception/            # custom checked exceptions
    ├── interfaces/           # Searchable, Payable, observer contracts
    ├── service/              # business logic layer
    ├── util/                 # DataStore, CSVUtil, Validator, AIHelper...
    └── test/TestRunner.java  # manual test suite
```

## 4. Compile and run (command line)

From the repository root:

```bash
# compile everything into an out/ folder
javac -d out $(find src/main/java -name "*.java")

# run the application
java -cp out com.airtribe.meditrack.Main
```

On Windows (PowerShell):

```powershell
Get-ChildItem -Recurse src\main\java -Filter *.java | ForEach-Object FullName | Out-File sources.txt
javac -d out "@sources.txt"
java -cp out com.airtribe.meditrack.Main
```

When the app starts it offers to load demo data — say `y` the first time so the menus have something to show.

## 5. Run in IntelliJ

Open the cloned folder, let IntelliJ index it, make sure the Project SDK (File → Project Structure) is a JDK 17+, then right-click `Main.java` → Run. To run the tests, right-click `TestRunner.java` → Run instead.

## 6. Run the test suite

```bash
java -cp out com.airtribe.meditrack.test.TestRunner
```

The runner prints one `[PASS]`/`[FAIL]` line per check plus a summary, and exits with code 1 if anything failed — so it can be used as a gate in scripts or Docker builds.

## 7. Data files

Choosing "Save all data to CSV" in the menu writes `patients.csv`, `doctors.csv` and `appointments.csv` into a `data/` folder next to where you launched the app. Set the `MEDITRACK_DATA_DIR` environment variable to store them somewhere else:

```bash
MEDITRACK_DATA_DIR=/tmp/meditrack-data java -cp out com.airtribe.meditrack.Main
```

The `data/` folder is git-ignored — never commit CSV data files.

## 8. Team workflow

Each member works only inside their assigned folders, on their own branch. Before starting new work, sync with main:

```bash
git checkout main
git pull origin main
git checkout yourname-dev
git merge main
```

Push your branch (`git push origin yourname-dev`) and open a pull request into `main` when your module is ready.

## Troubleshooting

**"invalid source release" or errors about records/switch** — your JDK is older than 17; check `javac -version`.

**`ClassNotFoundException: com.airtribe.meditrack.Main`** — you compiled to `out/` but forgot `-cp out`, or you're not in the repository root.

**Merge conflicts when syncing** — you probably edited files outside your assigned folders; ask in the group chat before force-resolving anything.

---
*Author: Anushtha (UI, Docs & Testing module)*
