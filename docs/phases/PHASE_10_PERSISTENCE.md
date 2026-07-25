# Phase 10 — Persistence & File I/O

| | |
|---|---|
| **Owner** | **Sunil Kumar B A** |
| **Depends on** | Phases 4, 7 |
| **Rubric** | **Bonus A — File I/O & Persistence (10 pts)** |
| **Status** | ✅ Complete |

---

## Objective

Make data survive a restart, via CSV and Java serialization, with try-with-resources throughout.

---

## Tasks

### `CSVUtil`
- [x] `escape` / `unescape` — commas, newlines, nulls
- [x] `joinList` / `splitList` for multi-valued cells
- [x] `writeLines` / `readLines` with header handling
- [x] `toCsvRow(Patient)` / `patientFromCsvRow`
- [x] `toCsvRow(Doctor)` / `doctorFromCsvRow`
- [x] `toCsvRow(Appointment)` / `appointmentFromCsvRow` with re-linking
- [x] **`String.split(",")`** used as the assignment requires
- [x] **try-with-resources** on every stream
- [x] Malformed rows return `null` and are skipped — one bad row cannot abort an import
- [x] Parent directories created on demand
- [x] UTF-8 explicitly, so `₹` and non-ASCII names survive

### Serialization
- [x] Every entity implements `Serializable`
- [x] Explicit `serialVersionUID = 1L` on each
- [x] `DataStore.serializeTo(String)`
- [x] `DataStore.deserializeFrom(String)`
- [x] `transient` on `Bill.billingStrategy` — behaviour, not data
- [x] `IOException` / `ClassNotFoundException` wrapped in `DataPersistenceException`

### Command-line integration
- [x] **`--loadData`** restores from `data/`
- [x] `--seedDemo`, `--runTests`, `--help`
- [x] Menu options for save and load
- [x] `MEDITRACK_DATA_DIR` environment override

### Integrity
- [x] Load order enforced: doctors → patients → appointments
- [x] Foreign keys resolved against in-memory maps
- [x] Orphaned rows skipped rather than loaded with nulls
- [x] Id counters synced after import

---

## try-with-resources

Every file operation uses it:

```java
try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
    writer.write(header);
    for (String row : rows) { writer.write(row); writer.newLine(); }
} catch (IOException e) {
    throw new DataPersistenceException(filePath, "Failed to write CSV", e);
}
```

The resource closes automatically, in reverse declaration order, on **both** the success and
exception paths. This replaces the `finally { if (w != null) w.close(); }` pattern that was
routinely written incorrectly — and which itself could throw, masking the original exception.

---

## Escaping, and why it is not optional

The assignment requires `String.split(",")`. A medical history entry like
`"Migraine, recurring"` would split into two columns and shift every field after it.

| Character | Stored as |
|---|---|
| `,` | `\|\|` |
| `\n`, `\r` | space |
| list separator | `;` |
| `null` | empty string |

**Verified end to end.** The written file:

```csv
PAT-0002,Meera Joshi,34,9812345671,A+,true,Migraine|| recurring,
```

Reloaded and displayed:

```
[PAT-0002] Meera Joshi  |  Age: 34 (ADULT)  |  Blood: A+  |  Insured: YES
        History  : Migraine, recurring
```

The comma survived.

---

## Referential integrity on load

CSV cannot enforce foreign keys, so the application does:

```java
Patient patient = patients.get(unescape(f[1]));
Doctor  doctor  = doctors.get(unescape(f[2]));
if (patient == null || doctor == null) {
    return null;   // orphaned row — skipped
}
```

Skipping is deliberate. Loading an appointment with a null patient would produce a
`NullPointerException` later, far from the actual cause.

---

## `transient` on the billing strategy

A strategy is **behaviour, not data**, and is often supplied as a lambda — which is not
`Serializable`. Without `transient`, serializing a bill throws `NotSerializableException`.

Marked transient, the bill keeps its already-computed totals, and `applyStrategy()` falls back
to identity so a reloaded bill never silently reprices itself.

---

## CSV vs serialization

| | CSV | Serialization |
|---|---|---|
| Human-readable | ✅ | ❌ |
| Survives class changes | ✅ | ❌ `serialVersionUID` mismatch |
| Handles object graphs | ❌ needs flattening | ✅ automatic |
| Cross-language | ✅ | ❌ Java only |

CSV is primary — a reviewer can `cat data/patients.csv`.

---

## Verified round-trip

```
$ ls data/
appointments.csv   doctors.csv   patients.csv

$ java -cp out com.airtribe.meditrack.Main --loadData
  Loaded 5 patients, 6 doctors, 4 appointments from data/
```

---

## Verification

`TestRunner` suite *CSV — round-trip and try-with-resources*, including a real
write-read-delete cycle against the filesystem.

Sample assertions:
- *"comma inside a field survived escaping"*
- *"malformed row returns null"*
- *"reading a missing file returns empty, not an exception"*
- *"speciality survived"*

---

## Exit criteria

- [x] All three entity types round-trip through CSV
- [x] Commas inside values survive
- [x] `--loadData` restores a full session
- [x] try-with-resources on every stream
- [x] Malformed rows skipped, not fatal
- [x] Foreign keys re-linked; orphans dropped
- [x] Id counters synced so new ids do not collide with imported ones

---

**Previous:** [Phase 9](./PHASE_09_PATTERNS.md) ·
**Next:** [Phase 11 — AI Feature](./PHASE_11_AI_FEATURE.md) ·
[Phase index](./README.md)
