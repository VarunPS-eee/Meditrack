# MediTrack — JVM Report

This report explains how the Java platform runs MediTrack, using our own classes as the examples.

## 1. JDK vs JRE vs JVM

The **JVM** (Java Virtual Machine) is the program that actually executes our compiled code. The **JRE** (Java Runtime Environment) is the JVM plus the standard class library (`java.util`, `java.nio`, `java.time` — everything CSVUtil and DateUtil import). The **JDK** (Java Development Kit) is the JRE plus developer tools, most importantly `javac`. As developers we install the JDK; someone who only *runs* MediTrack would need just a JRE.

## 2. From source to execution

When we run `javac` on the project, each `.java` file becomes a `.class` file containing **bytecode** — instructions for the JVM, not for any physical CPU. `Main.java` becomes `com/airtribe/meditrack/Main.class`, and the JVM on Windows, macOS or Linux runs the identical file. This is the "write once, run anywhere" property, and it's why our four-person team can develop on different operating systems without changing a line.

At runtime the JVM does not interpret bytecode forever: the **JIT (Just-In-Time) compiler** watches for "hot" methods and compiles them to native machine code on the fly. In MediTrack, methods like `DataStore.findById` or `CSVUtil.escape` that run on every menu action are exactly the kind of code the JIT optimises.

## 3. Class loading

Classes are loaded lazily, the first time they are referenced, by the class-loader hierarchy (bootstrap → platform → application). Our project makes this visible: `Constants` has a `static` initializer block that resolves the data directory and prints a message. That message appears exactly once, at the moment some class first touches `Constants` — not at program start. Static state such as `MedicalEntity`'s total-entities counter and the `IdGenerator` singleton also live from class-load until the JVM exits.

## 4. Runtime memory areas

**Heap.** Every object lives here: each `Patient`, `Doctor`, the `ArrayList`s inside `DataStore`, every `String` read by the Scanner. The heap is shared by all threads and managed by the garbage collector.

**Stack.** Each thread gets a stack of frames, one frame per method call, holding local variables and references. When `Main.patientMenu()` calls `PatientService.addPatient()` which calls `Validator.validateName()`, that is three frames; each pops when its method returns. Deep recursion would overflow it (`StackOverflowError`).

**Metaspace.** Class metadata — the structure of `Patient`, method bytecode, static fields — lives here, in native memory (it replaced the old "PermGen" in Java 8).

A useful distinction from our code: in `Patient p = new Patient(...)`, the *reference* `p` is on the stack, the *object* is on the heap.

## 5. Garbage collection

Java frees memory automatically: an object becomes garbage when nothing reachable refers to it any more. When `CSVUtil.readPatients` builds temporary `String[]` arrays for every row, those arrays are unreachable as soon as the method moves to the next row, and the GC reclaims them. Modern collectors (G1 is the default) are generational — most objects die young (our per-row parse arrays), and the few that survive (entities held in a `DataStore`) get promoted to an old region that is collected rarely.

The practical consequence for our design: we never "free" anything by hand, but we must not *accidentally hold on* to references. This is why `DataStore.findAll()` returns a copied list and why `BillSummary` is immutable — no hidden references that keep dead objects alive.

## 6. Threads

The JVM starts our `main` thread, plus its own background threads (GC, JIT). MediTrack's `NotificationService` can start a reminder scheduler thread; because that thread would keep the JVM alive, `Main` shuts it down (`stopReminderScheduler()`) before exiting — a JVM only terminates when all non-daemon threads finish.

## 7. Useful flags we tried

```bash
java -Xmx256m -cp out com.airtribe.meditrack.Main   # cap the heap at 256 MB
java -Xss512k ...                                    # smaller per-thread stacks
java -verbose:gc ...                                 # log collections as they happen
java -XX:+PrintFlagsFinal -version | grep MaxHeap    # show the default heap cap
```

Even with demo data loaded, MediTrack sits comfortably under a few tens of megabytes of heap — object counts in the hundreds, not millions.

---
*Author: Anushtha (UI, Docs & Testing module)*
