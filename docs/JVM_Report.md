# JVM Report — How MediTrack Actually Runs

> **Owner:** Sunil Kumar B A · **Phase:** 1 · **Rubric:** Environment Setup & JVM Understanding (10 pts)

This report explains the JVM by tracing what happens when you run MediTrack, rather than
reciting definitions. Every section ties back to code you can point at in this repository.

---

## 1. JDK vs JRE vs JVM

The three are nested, and the distinction is not academic — our [Dockerfile](../Dockerfile)
depends on it.

| Layer | Contains | Used for | In this project |
|---|---|---|---|
| **JVM** | The execution engine | *Running* bytecode | Inside both Docker stages |
| **JRE** | JVM + core class libraries | Running Java apps | Docker **runtime** stage (`21-jre-alpine`) |
| **JDK** | JRE + `javac`, `jar`, `javadoc`, `jdb` | *Developing* Java | Docker **build** stage (`21-jdk-alpine`) |

Our multi-stage build compiles with the JDK and ships only the JRE. The compiler never
reaches the production image — that is why the runtime image is 286 MB rather than ~450 MB,
and why an attacker landing in the container has no `javac` to work with.

```
Meditrack.java  --javac-->  Meditrack.class  --JVM-->  running program
   (source)                    (bytecode)              (native execution)
   JDK only                    portable                JRE is enough
```

---

## 2. Class Loader Subsystem

Class loading happens in three ordered stages: **Loading → Linking → Initialisation**.

### 2.1 The delegation hierarchy

Java uses **parent delegation**: a loader asks its parent before trying itself.

```
        Bootstrap ClassLoader      (native code; loads java.lang.*, java.util.*)
                 ▲ delegates up
        Platform ClassLoader       (loads JDK modules like java.sql, java.logging)
                 ▲ delegates up
        Application ClassLoader    (loads OUR classes from the classpath)
                 │
    com.airtribe.meditrack.entity.Patient
```

**Why delegation matters.** If we wrote our own `java.lang.String`, the Application loader
would ask the Platform loader, which would ask the Bootstrap loader, which would return the
*real* `String`. Ours would never load. This is a security property, not a convenience: it
makes core classes impossible to spoof.

### 2.2 The three stages, observed in our code

**Loading** — bytecode is read and a `Class` object is created in the Method Area.

**Linking** — three sub-steps:
- *Verification* — checks the bytecode is well-formed and type-safe. This is why a corrupted
  `.class` file fails at load, not mid-execution.
- *Preparation* — static fields get **default** values (`0`, `null`, `false`). Note: defaults,
  not the values you wrote. `MedicalEntity.totalEntitiesCreated` is briefly `null` here.
- *Resolution* — symbolic references become direct references.

**Initialisation** — static initialisers and static blocks run, in source order, exactly once.

MediTrack makes this observable. Run the app and the very first lines are:

```
[Constants] Static block executed — data directory resolved to: data
[AppConfig] Eager singleton constructed at 2026-07-25 14:16
[IdGenerator] Lazy singleton constructed on first use.
[MedicalEntity] Class loaded and initialised at 2026-07-25T14:16:26
```

Each line comes from a static block in [Constants.java](../src/main/java/com/airtribe/meditrack/constants/Constants.java),
[AppConfig.java](../src/main/java/com/airtribe/meditrack/util/AppConfig.java),
[IdGenerator.java](../src/main/java/com/airtribe/meditrack/util/IdGenerator.java) and
[MedicalEntity.java](../src/main/java/com/airtribe/meditrack/entity/MedicalEntity.java).

**The ordering proves lazy loading.** `MedicalEntity`'s message appears *after* the banner,
because no entity exists until demo seeding runs. The JVM did not load it at startup — it
loaded it at first active use. That is the exact mechanism `IdGenerator`'s
initialisation-on-demand holder idiom exploits for a thread-safe lazy singleton.

---

## 3. Runtime Data Areas

The JVM partitions memory into five regions. Two are shared across all threads; three are
per-thread.

```
┌──────────────────────── JVM Memory ────────────────────────┐
│                                                            │
│   SHARED ACROSS ALL THREADS                                │
│  ┌──────────────────────┐  ┌────────────────────────────┐  │
│  │        HEAP          │  │      METHOD AREA           │  │
│  │  All objects and     │  │  (Metaspace, Java 8+)      │  │
│  │  arrays.             │  │  Class metadata, static    │  │
│  │  GC operates here.   │  │  fields, runtime constant  │  │
│  │                      │  │  pool, method bytecode.    │  │
│  └──────────────────────┘  └────────────────────────────┘  │
│                                                            │
│   PER THREAD (main thread + meditrack-reminder daemon)     │
│  ┌────────────┐ ┌──────────────┐ ┌─────────────────────┐   │
│  │ JVM STACK  │ │ PC REGISTER  │ │ NATIVE METHOD STACK │   │
│  │ frames,    │ │ address of   │ │ for JNI calls       │   │
│  │ locals,    │ │ current      │ │                     │   │
│  │ operands   │ │ instruction  │ │                     │   │
│  └────────────┘ └──────────────┘ └─────────────────────┘   │
└────────────────────────────────────────────────────────────┘
```

### 3.1 Heap — where our objects live

Every `new Patient(...)`, every `ArrayList`, every `String` we build lands here. It is
generational:

- **Young Generation** (Eden + two Survivor spaces) — most objects die here. The `StringBuilder`
  inside `Bill.getFormattedBill()` is allocated, used, and collected within one method call.
- **Old Generation** — long-lived objects. Our `DataStore<T>` maps and the entities they hold
  survive for the whole session and get promoted here.

**Why our `AtomicInteger` counters matter for the heap.** They are *shared mutable state on
the heap*, reachable from two threads (main and `meditrack-reminder`). Heap memory has no
thread isolation — which is precisely why `IdGenerator` cannot use a plain `int`.

### 3.2 Stack — where our method calls live

Each thread gets its own stack. Every method call pushes a **frame** holding local variables,
an operand stack and a reference to the constant pool.

```java
// From TestRunner — three frames deep at the point of the assertion:
main()                          <- frame 1
  runCopySemanticsTests()        <- frame 2
    original.clone()              <- frame 3
```

Stack memory is per-thread and automatically reclaimed on return — no GC involvement. This is
also the source of `StackOverflowError`: unbounded recursion pushes frames until the stack
is exhausted.

**Primitives vs references.** In `Patient patient = new Patient(...)`:
- the `Patient` **object** is on the heap;
- the `patient` **reference** (8 bytes) is on the stack;
- `int age` inside the object is on the heap *as part of the object*, but a local `int i` in a
  loop lives directly on the stack.

This is exactly why shallow copy is dangerous. `super.clone()` copies the *reference* in the
new object's field, so both objects' fields point at one heap list — the failure our
[Patient.shallowCopy()](../src/main/java/com/airtribe/meditrack/entity/Patient.java)
demonstrates deliberately.

### 3.3 Method Area (Metaspace)

Holds per-class data: the runtime constant pool, field and method metadata, and **static
variables**. Our `Constants.TAX_RATE`, `MedicalEntity.totalEntitiesCreated` and
`AppConfig.INSTANCE` live here — one copy each, regardless of how many objects exist.

Since Java 8 this is **Metaspace**, allocated in native memory rather than the heap. The old
PermGen had a fixed size and famously threw `OutOfMemoryError: PermGen space` on redeploys;
Metaspace grows dynamically.

### 3.4 PC Register

One per thread, holding the address of the instruction currently executing. When the OS
suspends our `meditrack-reminder` daemon thread mid-sweep and resumes it later, the PC
register is what lets it continue from the right instruction.

### 3.5 Native Method Stack

For methods implemented in C/C++ via JNI. We use it indirectly — `System.currentTimeMillis()`
and `Object.hashCode()` are native.

---

## 4. Execution Engine

Bytecode reaches the CPU through three cooperating components.

### 4.1 Interpreter

Reads bytecode instruction by instruction and executes it immediately.

- **Advantage:** starts instantly, no compilation delay.
- **Disadvantage:** re-interprets the same instruction on every pass. A method called 10,000
  times is decoded 10,000 times.

### 4.2 JIT Compiler

The JVM profiles execution and, once a method or loop crosses a threshold (~10,000 invocations
for C2), compiles it to **native machine code** and caches it. Subsequent calls run at native
speed.

HotSpot uses **tiered compilation**:

| Tier | Compiler | Behaviour |
|---|---|---|
| 0 | Interpreter | Immediate start, collects profiling data |
| 1–3 | C1 (client) | Fast compile, light optimisation |
| 4 | C2 (server) | Slow compile, aggressive optimisation |

JIT optimisations that apply to our code:
- **Method inlining** — `patient.getName()` is a one-line getter; C2 replaces the call with a
  direct field read, eliminating call overhead entirely.
- **Loop unrolling** — the slot-generation loop in `DateUtil.generateSlotsForDay()`.
- **Escape analysis** — an object proven not to escape its method can be stack-allocated or
  scalar-replaced, skipping the heap and GC.
- **Dead code elimination**.

**Observable in our test suite.** `runConcurrencyTests()` generates 1,000 ids across 10
threads. The first few hundred run interpreted; by the end `IdGenerator.nextId()` is JIT-compiled
native code. Same method, same bytecode, different execution strategy.

### 4.3 Garbage Collector

Reclaims heap objects that are no longer reachable. Java 21 defaults to **G1GC**, a
region-based collector targeting predictable pause times.

Reachability, not reference counting, decides collection. Our `DataStore.deleteById()` removes
the map entry; if nothing else references that `Patient`, it becomes unreachable and G1 will
collect it — but we cannot say *when*. `System.gc()` is a suggestion the JVM may ignore, which
is why our persistence layer uses **try-with-resources** rather than relying on `finalize()`.

### 4.4 Interpreter vs JIT — side by side

| | Interpreter | JIT Compiler |
|---|---|---|
| Unit of work | One bytecode instruction | Whole method / loop |
| Startup | Immediate | Compilation delay first |
| Steady-state speed | Slow | Near-native |
| Memory | Minimal | Code cache required |
| Re-execution | Re-decodes every time | Compiled once, reused |
| Used for | Cold code, startup | Hot paths |

They are not alternatives — HotSpot runs both, simultaneously, and switches per-method based
on measured behaviour. That is the "Hot Spot" the JVM is named for.

---

## 5. Write Once, Run Anywhere

### 5.1 The mechanism

`javac` targets the **JVM**, not a CPU. The output is platform-neutral bytecode; the
platform-specific part is the JVM implementation itself.

```
                    MediTrack source (.java)
                              │
                           javac
                              │
                    MediTrack bytecode (.class)   <- identical everywhere
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
   JVM for Windows       JVM for Linux         JVM for macOS
        │                     │                     │
   x86-64 machine code   ARM64 machine code    ARM64 machine code
```

### 5.2 Proven in this repository

We did not argue this — we tested it. The identical `.class` files produced by `javac` on
**Windows 11 / x86-64** were copied into an **Alpine Linux** container and ran unmodified:

| Environment | Java | OS | Result |
|---|---|---|---|
| Host | Temurin 21.0.11 | Windows 11 | 325/325 tests pass |
| Container | Temurin 21.0.11 | Alpine Linux | 325/325 tests pass |

No recompilation, no `#ifdef`, no platform branches in our source.

### 5.3 Where the abstraction leaks

WORA is not absolute, and MediTrack hit two real cases:

1. **Character encoding.** The app prints `₹`. Windows defaults to CP-1252 and Alpine to POSIX;
   both mangle it. We fixed this explicitly in the Dockerfile:
   ```dockerfile
   ENV LANG=C.UTF-8 \
       JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8"
   ```
2. **File paths.** We use `java.nio.file.Paths` and forward slashes throughout rather than
   hard-coding `\` — `Paths.get()` resolves the separator per platform.

The lesson: bytecode is portable, but *environment assumptions* are not. WORA is a property
of the JVM you must not undermine from your own code.

---

## 6. End-to-end trace: `docker run meditrack --seedDemo`

1. **JVM starts.** Heap, Metaspace and thread stacks are allocated.
2. **Bootstrap loader** loads `java.lang.*`.
3. **Application loader** loads `com.airtribe.meditrack.Main` from the jar.
4. **Linking** verifies bytecode; static fields get default values.
5. **Initialisation** runs `Constants`' static block → the `[Constants]` line prints.
6. `main()` frame is pushed onto the main thread's stack.
7. `AppConfig.getInstance()` triggers eager singleton construction → `[AppConfig]` line.
8. `IdGenerator.getInstance()` first touches `Holder` → `[IdGenerator]` line. **This is the
   lazy-loading proof.**
9. Seeding allocates `Patient`/`Doctor` objects on the heap; `MedicalEntity` initialises →
   `[MedicalEntity]` line.
10. `Timer` spawns the `meditrack-reminder` **daemon** thread with its own stack and PC register.
11. The menu loop runs interpreted; hot methods are JIT-compiled as thresholds are crossed.
12. On exit, the daemon thread does not block shutdown — that is what `daemon = true` buys.
13. JVM terminates; the OS reclaims all memory.

---

## 7. Verifying this yourself

```bash
# Which JVM, and is it running mixed mode (interpreter + JIT)?
java -version

# Watch classes load in order — proves lazy loading empirically
java -verbose:class -cp out com.airtribe.meditrack.Main --runTests | head -40

# Confirm the default collector and heap sizing on this machine
java -XX:+PrintFlagsFinal -version | grep -E "UseG1GC|MaxHeapSize|MetaspaceSize"

# Watch JIT compilation decisions on a hot path
java -XX:+PrintCompilation -cp out com.airtribe.meditrack.test.TestRunner | head -40

# Inspect the bytecode javac actually produced
javap -c -p out/com/airtribe/meditrack/util/IdGenerator.class | head -50
```

---

## 8. Summary

| Concept | Where MediTrack demonstrates it |
|---|---|
| Class loading order | Four static-block messages, in dependency order, on startup |
| Lazy loading | `IdGenerator`'s holder class initialises only on first `getInstance()` |
| Method Area / statics | `Constants.TAX_RATE`, `AppConfig.INSTANCE`, entity counters |
| Heap | Every entity; the `DataStore<T>` maps that outlive method calls |
| Stack | Per-thread frames; why shallow copy aliases heap objects |
| Thread-shared heap | `AtomicInteger` in `IdGenerator` — 1,000 ids, 10 threads, 0 collisions |
| PC Register | The reminder daemon resuming mid-sweep |
| JIT | `nextId()` transitioning from interpreted to compiled during the load test |
| GC | try-with-resources everywhere instead of relying on collection timing |
| WORA | Same bytecode passing 325 tests on Windows and Alpine Linux |

---

*Part of the [MediTrack documentation set](./). See also
[Setup_Instructions.md](./Setup_Instructions.md) and [Design_Decisions.md](./Design_Decisions.md).*
