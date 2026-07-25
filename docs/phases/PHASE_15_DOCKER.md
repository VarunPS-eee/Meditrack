# Phase 15 — Dockerization

| | |
|---|---|
| **Owner** | **Sunil Kumar B A** |
| **Depends on** | Phase 14 |
| **Blocks** | Phase 16 |
| **Rubric** | Beyond the brief — deployment engineering |
| **Status** | ✅ Complete — image builds and runs |

---

## Objective

Package MediTrack so a reviewer can run it with **no Java installed**, and so the test suite
gates the artefact.

---

## Tasks

### Dockerfile
- [x] Multi-stage build — JDK for compiling, JRE for running
- [x] Stage 1: compile all sources with `javac`
- [x] Stage 1: **run the test suite** — a failure fails the build
- [x] Stage 1: package an executable jar with `Main-Class` in the manifest
- [x] Stage 2: JRE-only runtime, copying just the jar
- [x] OCI labels (title, version, authors, source)
- [x] UTF-8 environment so `₹` renders
- [x] **Non-root user** (`meditrack`)
- [x] `VOLUME ["/data"]` for persistence
- [x] `ENTRYPOINT` + overridable `CMD`
- [x] `MEDITRACK_DATA_DIR` pointed at the volume

### `.dockerignore`
- [x] Exclude `out/`, `build/`, `*.class`, `*.jar`
- [x] Exclude `data/` — the container uses its own volume
- [x] Exclude `.git/`, `.idea/`, `*.iml`
- [x] Exclude `docs/` — not needed to build or run
- [x] Exclude Docker's own files

### `docker-compose.yml`
- [x] `meditrack` service — interactive, with `stdin_open` and `tty`
- [x] `tests` service — one-shot, usable as a CI gate
- [x] Host volume mount for `./data`
- [x] Environment variables set

### Verification
- [x] Image builds from a clean context
- [x] Tests pass **inside** the container
- [x] Interactive menu works with `-it`
- [x] UTF-8 renders correctly in Alpine
- [x] Data persists across runs via the volume
- [x] Non-root user confirmed

---

## Why multi-stage

| Stage | Base | Size | Contains |
|---|---|---|---|
| build | `eclipse-temurin:21-jdk-alpine` | ~450 MB | Compiler, sources, test classes |
| runtime | `eclipse-temurin:21-jre-alpine` | **286 MB** | The jar, nothing else |

Everything the build needs but the app does not is discarded at the stage boundary. The shipped
image has no `javac`, no source and no test classes — smaller, and a smaller attack surface.

This is also the clearest practical illustration of the **JDK vs JRE** distinction from the JVM
report: you compile with one and run with the other.

---

## Tests gate the build

```dockerfile
RUN set -eux; \
    find src/main/java -name '*.java' > sources.txt; \
    javac -encoding UTF-8 -d out @sources.txt; \
    java -Dfile.encoding=UTF-8 -cp out com.airtribe.meditrack.test.TestRunner
```

`TestRunner` exits non-zero on failure, so **the image cannot be built from failing code**.
Build output:

```
#13 4.278     Total  : 325
#13 4.278     Passed : 325
#13 4.278     Failed : 0
#13 4.278     ALL TESTS PASSED
#13 DONE 4.3s
```

---

## Two things that would silently break

### 1. `-it` is required

```bash
docker run -it --rm meditrack:1.0.0 --seedDemo   # correct
docker run --rm meditrack:1.0.0 --seedDemo       # exits immediately
```

The app is a console **menu** reading stdin. Without a TTY it sees EOF and quits at the first
prompt. Documented prominently in the README and Setup Instructions.

### 2. UTF-8 must be set explicitly

Alpine's default locale is POSIX, which renders `₹` as `?`:

```dockerfile
ENV LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8"
```

This is the WORA leak described in [JVM_Report §5.3](../JVM_Report.md#53-where-the-abstraction-leaks):
bytecode is portable, environment assumptions are not.

---

## Security choices

```dockerfile
RUN addgroup -S meditrack && \
    adduser -S -G meditrack -h /app meditrack && \
    mkdir -p /data && chown -R meditrack:meditrack /data /app
USER meditrack
```

A console app has no reason to hold root. Combined with the JRE-only runtime, an attacker
landing in this container has neither root nor a compiler.

---

## Usage

```bash
# Build
docker build -t meditrack:1.0.0 .

# Interactive, demo data
docker run -it --rm meditrack:1.0.0 --seedDemo

# Tests only (CI gate)
docker run --rm meditrack:1.0.0 --runTests

# Persist data to the host
docker run -it --rm -v "$(pwd)/data:/data" meditrack:1.0.0 --loadData

# Compose
docker compose run --rm meditrack
docker compose run --rm tests
```

---

## Verified

| Check | Result |
|---|---|
| Image builds clean | ✅ |
| Final size | 286 MB |
| Tests inside container | ✅ 325/325 |
| Interactive menu | ✅ |
| UTF-8 (`₹`) in Alpine | ✅ |
| Volume persistence | ✅ |
| Runs as non-root | ✅ |
| Same bytecode as Windows host | ✅ — the WORA proof |

---

## Exit criteria

- [x] Multi-stage build with JDK → JRE separation
- [x] Test suite gates the image
- [x] Runs as a non-root user
- [x] Data persists via a volume
- [x] UTF-8 output correct
- [x] Compose file for both interactive and CI use
- [x] Fully documented in the README

---

**Previous:** [Phase 14](./PHASE_14_TESTING.md) ·
**Next:** [Phase 16 — Documentation](./PHASE_16_DOCUMENTATION.md) ·
[Phase index](./README.md)
