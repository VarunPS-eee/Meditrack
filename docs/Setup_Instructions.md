# Setup Instructions

> **Owner:** Sunil Kumar B A · **Phase:** 1 · **Rubric:** Environment Setup & JVM Understanding (10 pts)

Three ways to run MediTrack, in increasing order of isolation. Pick one.

> 🎬 **Prefer to watch someone else do it first?**
> [MediTrack — The Heist](../video/meditrack-the-heist.mp4) shows the build and the
> first run at **15:40**, then walks every menu. ([Contents](../video/README.md))

| Route | Needs | Best for |
|---|---|---|
| [A. Docker](#route-a--docker-fastest) | Docker only | Reviewers — zero Java setup |
| [B. Command line](#route-b--command-line-javac--java) | JDK 21 | Anyone with a terminal |
| [C. IntelliJ IDEA](#route-c--intellij-idea) | JDK 21 + IntelliJ | Day-to-day development |

---

## Prerequisites

| Tool | Version | Required for |
|---|---|---|
| JDK | **21 LTS** or newer | Routes B and C |
| Git | any recent | Cloning |
| Docker | 20.10+ | Route A |
| IntelliJ IDEA | 2023.1+ | Route C |

MediTrack has **no third-party dependencies** — Core Java only, no Maven or Gradle. Nothing is
downloaded at build time.

> **Screenshot placeholder** — `docs/images/00-prerequisites.png`
> *(Replace with a capture of your own `java -version` output.)*

---

## Step 1 — Install the JDK

MediTrack is developed and tested against **Eclipse Temurin 21 (LTS)**. Java 21 is required —
the code uses switch expressions, records, pattern matching for `instanceof`, text blocks and
`Math.clamp()`.

### Windows

1. Download the **Temurin 21 (LTS) MSI** from <https://adoptium.net/temurin/releases/?version=21>.
2. Run the installer. On the *Custom Setup* screen enable:
   - **Set JAVA_HOME variable**
   - **Add to PATH**
3. Open a **new** terminal and verify.

### macOS

```bash
brew install --cask temurin@21
```

### Linux (Debian / Ubuntu)

```bash
sudo apt update && sudo apt install -y temurin-21-jdk
# or: sudo apt install -y openjdk-21-jdk
```

### Verify the installation

```bash
java -version
javac -version
```

Expected — note that **both** must be present. If `javac` is missing you installed a JRE, not a
JDK:

```
openjdk version "21.0.11" 2026-04-21 LTS
OpenJDK Runtime Environment Microsoft-13877171 (build 21.0.11+10-LTS)
OpenJDK 64-Bit Server VM Microsoft-13877171 (build 21.0.11+10-LTS, mixed mode, sharing)
javac 21.0.11
```

> **Screenshot placeholder** — `docs/images/01-java-version.png`

### Confirm JAVA_HOME

```bash
# Windows (PowerShell)
$env:JAVA_HOME

# macOS / Linux
echo $JAVA_HOME
```

If empty, set it manually:

```powershell
# Windows — permanent, user scope
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-21", "User")
```

```bash
# macOS / Linux — add to ~/.zshrc or ~/.bashrc
export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which javac))))
export PATH=$JAVA_HOME/bin:$PATH
```

---

## Step 2 — Clone the repository

```bash
git clone https://github.com/VarunPS-eee/Meditrack.git
cd Meditrack
```

You should see:

```
Meditrack/
├── src/main/java/com/airtribe/meditrack/    <- all source
├── docs/                                     <- this file lives here
├── Dockerfile
├── docker-compose.yml
└── README.md
```

> **Screenshot placeholder** — `docs/images/02-project-structure.png`

---

## Route A — Docker (fastest)

No JDK needed. The image compiles the source and runs the test suite during build, so a
successful build is itself a green test run.

```bash
# Build (first run pulls the base images; subsequent builds are cached)
docker build -t meditrack:1.0.0 .

# Run the interactive app with demo data.
# -it is REQUIRED: the app is a console menu that reads stdin.
docker run -it --rm meditrack:1.0.0 --seedDemo

# Run only the test suite (exits non-zero on failure — usable as a CI gate)
docker run --rm meditrack:1.0.0 --runTests

# Persist data to the host between runs
docker run -it --rm -v "$(pwd)/data:/data" meditrack:1.0.0 --loadData
```

### Or with Docker Compose

```bash
docker compose run --rm meditrack              # interactive, demo data
docker compose run --rm meditrack --loadData   # restore from ./data
docker compose run --rm tests                  # test suite only
```

**Expected build tail:**

```
    Total  : 325
    Passed : 325
    Failed : 0
    Rate   : 100.0%
    ALL TESTS PASSED
```

> **Screenshot placeholder** — `docs/images/03-docker-build.png`

### Docker troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| App exits instantly, no menu | Missing `-it` | Add `-it`: the menu needs stdin |
| `₹` shows as `?` | Terminal not UTF-8 | Windows: `chcp 65001` before running |
| `Cannot connect to the Docker daemon` | Docker not started | Start Docker Desktop |
| Build fails at the test step | A test genuinely failed | Read the `[FAIL]` lines — the build is gating correctly |

---

## Route B — Command line (`javac` + `java`)

### Compile

```bash
# From the repository root
mkdir -p out
```

**Linux / macOS / Git Bash:**
```bash
javac -encoding UTF-8 -d out $(find src/main/java -name "*.java")
```

**Windows PowerShell:**
```powershell
$sources = Get-ChildItem -Path src\main\java -Filter *.java -Recurse | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -d out $sources
```

**Windows CMD:**
```cmd
dir /s /b src\main\java\*.java > sources.txt
javac -encoding UTF-8 -d out @sources.txt
```

A silent compile is a successful compile.

### Run

```bash
# Interactive menu with demo data (recommended first run)
java -cp out com.airtribe.meditrack.Main --seedDemo

# Empty clinic — enter your own records
java -cp out com.airtribe.meditrack.Main

# Restore previously saved CSV data
java -cp out com.airtribe.meditrack.Main --loadData

# Manual test suite
java -cp out com.airtribe.meditrack.test.TestRunner

# Usage help
java -cp out com.airtribe.meditrack.Main --help
```

> **Screenshot placeholder** — `docs/images/04-app-running.png`

### Build a runnable jar

```bash
jar --create --file meditrack.jar --main-class com.airtribe.meditrack.Main -C out .
java -jar meditrack.jar --seedDemo
```

### Windows and the `₹` symbol

If the console shows `?` instead of `₹`:

```powershell
chcp 65001                                    # switch the code page to UTF-8
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out com.airtribe.meditrack.Main --seedDemo
```

---

## Route C — IntelliJ IDEA

1. **File → Open** → select the `Meditrack` folder → **Trust Project**.
2. **File → Project Structure → Project**
   - *SDK*: your JDK 21 install
   - *Language level*: **21**
3. **File → Project Structure → Modules**
   - Confirm `src/main/java` is marked as **Sources** (blue folder icon).

   > This matters. The repo's `MediTrack.iml` already sets it, but if IntelliJ was open
   > before you pulled, use **File → Reload All from Disk** or the IDE will keep flagging
   > phantom package errors against the old `src` root.

4. **Run configurations** — create two:

   | Name | Main class | Program arguments |
   |---|---|---|
   | `MediTrack (demo)` | `com.airtribe.meditrack.Main` | `--seedDemo` |
   | `MediTrack Tests` | `com.airtribe.meditrack.test.TestRunner` | *(none)* |

5. Set the console encoding: **Help → Edit Custom VM Options** and add:
   ```
   -Dfile.encoding=UTF-8
   ```

> **Screenshot placeholder** — `docs/images/05-intellij-config.png`

---

## Step 3 — Verify your setup

Run all three and you are fully set up:

```bash
# 1. Tests — expect 325/325
java -cp out com.airtribe.meditrack.test.TestRunner | tail -8

# 2. App — expect the ASCII banner and the main menu
java -cp out com.airtribe.meditrack.Main --seedDemo

# 3. Persistence — expect data/*.csv to appear
#    In the app: 8 (Data & Settings) -> 1 (Save all to CSV) -> 0 (Exit)
ls data/
```

### Two-minute guided tour

Once the app is running with `--seedDemo`, try this sequence:

| Input | What it shows |
|---|---|
| `9` → `1` | Dynamic dispatch — one loop, three different `displayDetails()` |
| `9` → `2` | Deep vs shallow copy, side by side |
| `6` → `1` → `severe chest pain` | AI triage, with urgency escalation |
| `4` → `6` | Strategy pattern — one charge priced three ways |
| `7` → `2` | Streams analytics — average fee by speciality |
| `8` → `1` | Save everything to CSV |
| `0` | Exit |

---

## Troubleshooting

| Error | Cause | Fix |
|---|---|---|
| `'javac' is not recognized` | JDK not on PATH, or JRE installed instead of JDK | Reinstall Temurin **JDK**, tick "Add to PATH", open a new terminal |
| `error: invalid source release: 21` | Older JDK active | `java -version` — if below 21, fix `JAVA_HOME` |
| `Could not find or load main class` | Wrong classpath or missing package | Run from the repo root; use the fully qualified `com.airtribe.meditrack.Main` |
| `NoClassDefFoundError` after pulling | Stale `out/` from the old package layout | `rm -rf out` and recompile |
| Package errors only in IntelliJ | IDE cached the old `src` source root | **File → Reload All from Disk**, or re-mark `src/main/java` as Sources |
| `₹` renders as `?` | Non-UTF-8 console | `chcp 65001` (Windows), or pass `-Dfile.encoding=UTF-8` |
| App exits immediately in Docker | Missing `-it` | `docker run -it --rm meditrack:1.0.0 --seedDemo` |
| `AccessDeniedException` on `data/` | No write permission | Run from a writable directory, or set `MEDITRACK_DATA_DIR` |

---

## Generating JavaDoc (bonus)

Every public type in MediTrack is documented.

```bash
javadoc -d docs/javadoc \
        -encoding UTF-8 -charset UTF-8 \
        -windowtitle "MediTrack API" \
        -sourcepath src/main/java \
        -subpackages com.airtribe.meditrack

# Open docs/javadoc/index.html
```

---

## Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `MEDITRACK_DATA_DIR` | `data` | Where CSV and serialized state are written. Read once, in `Constants`' static block. |
| `JAVA_TOOL_OPTIONS` | *(unset)* | Set to `-Dfile.encoding=UTF-8` if your console mangles `₹` |

```bash
# Example: store data elsewhere
MEDITRACK_DATA_DIR=/var/meditrack java -cp out com.airtribe.meditrack.Main --loadData
```

---

*Part of the [MediTrack documentation set](./). See also
[JVM_Report.md](./JVM_Report.md) and [Design_Decisions.md](./Design_Decisions.md).*
