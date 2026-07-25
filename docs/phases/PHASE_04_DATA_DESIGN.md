# Phase 4 — Data & Persistence Design

| | |
|---|---|
| **Owner** | **Sunil Kumar B A** |
| **Depends on** | Phase 3 |
| **Blocks** | Phase 10 |
| **Status** | ✅ Complete |

---

## Objective

Design the data model and file formats. MediTrack has no database, so this phase does the
equivalent work: entity relationships, key strategy, file schemas, referential integrity, and
the SQL migration path if it ever outgrew CSV.

---

## Tasks

### Data model
- [x] ER diagram covering all entities and relationships
- [x] Cardinality decided for each relationship
- [x] Class diagram mapping the Java hierarchy
- [x] Composition vs association decided (`LineItem` is composed into `Bill`)

### Keys
- [x] Format chosen: `PREFIX-####` (`PAT-0001`)
- [x] Alternatives evaluated and rejected (UUID, bare integers, name-based)
- [x] Prefix per entity type
- [x] Thread-safe generation via `AtomicInteger`
- [x] Counter sync after import, so loaded ids do not collide with generated ones
- [x] Monotonic sync — a lower id must never move the counter backwards

### File schemas
- [x] `patients.csv` columns, types and nullability
- [x] `doctors.csv` columns, types and nullability
- [x] `appointments.csv` with foreign keys
- [x] Escaping rules for commas, newlines and lists
- [x] Enum storage: **name**, not display name (stable across UI changes)
- [x] Serialization format as a secondary option

### Integrity
- [x] Foreign key validation on load
- [x] Load order fixed: doctors → patients → appointments
- [x] Orphan handling: skip the row rather than load nulls
- [x] Business rules listed with their enforcement point
- [x] Appointment state machine specified

### Storage
- [x] `LinkedHashMap` chosen for deterministic iteration
- [x] Complexity analysis per operation
- [x] Data volume estimates

### Migration path
- [x] Full SQL DDL written
- [x] Java → SQL translation notes
- [x] `double` → `NUMERIC(12,2)` flagged as a real change production would need

---

## Key decisions

### Why `PREFIX-####` over UUID

A clinic receptionist reads ids aloud over a counter. `PAT-0042` works; a 36-character UUID
does not. The prefix also makes the id self-describing — `1` is ambiguous, `PAT-0001` is not.

### Why appointments store ids, not nested objects

```csv
APT-0001,PAT-0001,DOC-0001,2026-07-26 10:00,PENDING,chest pain;breathless,
```

Nesting a full patient record inside every appointment row would duplicate data and make updates
inconsistent — the CSV equivalent of failing to normalise. The object graph is flattened to
foreign keys on write and re-linked in memory on read.

This decision is also what makes the SQL migration path straightforward: the CSV is already
relational in shape.

### Why load order matters

```
1. doctors.csv       (no dependencies)
2. patients.csv      (no dependencies)
3. appointments.csv  (depends on both — re-linked against the maps)
```

Loading appointments first would leave every foreign key unresolvable.

### Escaping

`String.split(",")` is required by the assignment, so commas must not reach the file. Values are
escaped on write (`,` → `||`) and reversed on read.

Verified end to end: `"Migraine, recurring"` is stored as `Migraine|| recurring` and restored
with its comma intact.

---

## Deliverables

| Deliverable | Location |
|---|---|
| ER diagram | [DB_Design.md §1](../DB_Design.md#1-entity-relationship-model) |
| Class diagram | [DB_Design.md §2](../DB_Design.md#2-class-hierarchy-the-schema-in-java-terms) |
| Key strategy | [DB_Design.md §3](../DB_Design.md#3-primary-key-strategy) |
| File schemas | [DB_Design.md §4](../DB_Design.md#4-file-schemas) |
| Integrity rules | [DB_Design.md §5](../DB_Design.md#5-referential-integrity) |
| SQL DDL | [DB_Design.md §9](../DB_Design.md#9-sql-migration-path) |

---

## Exit criteria

- [x] Every entity and relationship modelled
- [x] Key format decided with alternatives recorded
- [x] File schemas fully specified before any I/O code was written
- [x] Referential integrity strategy defined
- [x] Migration path documented

---

**Previous:** [Phase 3](./PHASE_03_LLD.md) ·
**Next:** [Phase 5 — Foundation](./PHASE_05_FOUNDATION.md) ·
[Phase index](./README.md)
