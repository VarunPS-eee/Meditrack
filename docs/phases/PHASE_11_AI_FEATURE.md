# Phase 11 — AI Feature

| | |
|---|---|
| **Owner** | **Zubair** |
| **Depends on** | Phase 8 |
| **Rubric** | **Bonus C — AI Feature (10 pts)** |
| **Status** | ✅ Complete |

---

## Objective

Recommend a doctor from described symptoms, flag urgent cases, and suggest appointment slots —
using a rule-based expert system with no external dependencies.

---

## Tasks

### Symptom → speciality
- [x] Symptom keywords declared on each `Specialization` constant
- [x] `Specialization.scoreAgainst(String)` — keyword match count
- [x] `recommendSpecialities` — every speciality ranked
- [x] `recommendSpecialization` — the single best match
- [x] Fall back to `GENERAL_PRACTICE` when nothing matches
- [x] Null and blank input handled

### Urgency detection
- [x] Urgency keyword list (`severe`, `bleeding`, `unconscious`, …)
- [x] `isUrgent(String)`
- [x] Escalation banner in the triage report

### Doctor ranking
- [x] Weighted scoring across speciality, rating, experience, load
- [x] Experience **capped at 20 years** so seniority cannot dominate a speciality match
- [x] Load penalty so patients are spread across the roster
- [x] Unavailable doctors excluded
- [x] `DoctorRecommendation` record carrying score **and reason**

### Slot suggestion
- [x] `suggestSlots(doctor, appointments, max, days)` — searches forward day by day
- [x] Excludes booked slots and past times
- [x] `suggestEarliestAppointment` across all matching doctors

### Explainability
- [x] `buildTriageReport` — full printable report
- [x] Confidence percentage per speciality
- [x] Matched keywords shown, so every score is traceable
- [x] Per-doctor reason string

### Patient screening
- [x] `suggestScreeningPrompts(Patient)` — flags missing history, allergies, blood group
- [x] Senior-specific prompt

---

## Scoring model

```
score = 10.0 × specialityMatch          (dominant term)
      +  2.0 × rating                   (0–5 → 0–10)
      +  0.3 × min(experience, 20)      (capped at 6.0)
      -  0.8 × currentBookings          (load balancing)
```

### Why these weights

**Speciality match dominates.** A well-matched junior should outrank a mismatched veteran —
sending a chest-pain patient to a highly-rated dermatologist is the failure mode to avoid.

**Experience is capped at 20 years.** Uncapped, a 40-year generalist would outscore a
well-matched cardiologist purely on longevity.

**Load is penalised.** Without it, the highest-rated doctor in each speciality would receive
every single referral.

---

## Why rule-based, not machine learning

This is a deterministic **expert system**, not a learned model. That was a constraint (no
third-party libraries) but it is also the right answer here:

| Property | Rule-based | ML model |
|---|---|---|
| Explainable | ✅ "matched: chest pain, breathless" | ❌ Weights in a matrix |
| Auditable | ✅ Keywords are readable in source | ❌ Needs tooling |
| Deterministic | ✅ Same input, same output | ⚠️ Version-dependent |
| Needs training data | ✅ None | ❌ Labelled clinical data |
| Handles unseen phrasing | ❌ Keyword-bound | ✅ Generalises |

**For clinical triage, explainability is a feature.** A clinician can see exactly why a
recommendation was made and overrule it. A black box recommending a cardiologist with no stated
reason is not usable in a medical setting.

The honest limitation: keyword matching misses paraphrases. `"my chest hurts"` does not match
`"chest pain"`. Recorded in [Design_Decisions.md §12](../Design_Decisions.md#12-known-limitations).

---

## Keywords live on the enum

```java
CARDIOLOGY("Cardiology", 1200.0,
        "chest pain", "heart", "palpitation", "breathless", "bp", "blood pressure", "cholesterol"),
```

Keeping the mapping next to the speciality it describes — rather than in a distant lookup table —
means adding a speciality requires editing exactly one place.

---

## Sample output

```
  ========================================================================
   AI TRIAGE — rule-based recommendation
  ========================================================================
   Symptoms: severe chest pain and breathless

   ** URGENT ** These symptoms suggest emergency care. Escalate before booking a routine slot.

   Speciality match
   --------------------------------------------------------------------
   Cardiology           2 pts (100% confidence)  matched: chest pain, breathless

   Recommended doctors
   --------------------------------------------------------------------
   1. Dr. Anita Rao            score 23.6
      specialises in Cardiology; rating 4.8, 16y experience, 1 booked
   2. Dr. Rahul Mehta          score 14.4
      adjacent speciality (Orthopedics); rating 4.2, 22y experience, 0 booked
   3. Dr. Vikram Nair          score 11.5
      adjacent speciality (Neurology); rating 4.5, 11y experience, 1 booked

   Earliest slot: Dr. Anita Rao at 25 Jul 2026, 02:30 pm
  ========================================================================
```

Note the third-ranked doctor: Rahul Mehta has *more* experience (22y) and no bookings, yet Anita
Rao still wins on the speciality match. That is the weighting working as designed.

Try it: menu **6 → 1**.

---

## Deliverables

| File | Lines |
|---|---|
| `util/AIHelper.java` | 337 |
| Symptom keywords in `Specialization.java` | — |

Menu integration: AI Triage (option 6) with three sub-options.

---

## Verification

`TestRunner` suite *AIHelper — rule-based triage*.

Sample assertions:
- *"chest pain routes to cardiology"*
- *"unknown symptoms fall back to general practice"*
- *"urgency detected"*
- *"the cardiologist ranks first"*
- *"suggested slots are within clinic hours"*
- *"null patient yields no prompts"*

---

## Exit criteria

- [x] Symptoms map to the correct speciality across all six
- [x] Unknown symptoms degrade gracefully to General Practice
- [x] Urgent cases flagged
- [x] Doctors ranked with an explanation for each
- [x] Slots suggested within clinic hours, excluding booked times
- [x] Null and blank inputs handled everywhere

---

**Previous:** [Phase 10](./PHASE_10_PERSISTENCE.md) ·
**Next:** [Phase 12 — Streams & Concurrency](./PHASE_12_STREAMS_CONCURRENCY.md) ·
[Phase index](./README.md) · [Zubair's tasks](../team/ZUBAIR.md)
