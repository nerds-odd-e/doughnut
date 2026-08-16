# 010 — Commissioned Tutor score 4 is FSRS-6 Good

Ad-hoc plan for SEED-004. Ordinary correct is already FSRS-6 Good; ordinary incorrect is Again (due +12h). Commissioned scores still use `DEFAULT_SPACES` for the “standard increment.” Score 4 is already “successful recall, standard growth” — that is FSRS **Good**.

Do **not** execute until asked. Do **not** Accept ADR 0003. Do **not** delete `DEFAULT_SPACES`. Do **not** change scores 5 / 3 / 2 / 1 / 0. Do **not** resolve Tutor 2 vs Hard (C2). Leave `quick/009` alone (Again pin tightening, including its ADR general-bullet slice).

## Current code

- `CommissionedLearningSessionFeedbackScheduling.recordFeedback` increments recall count, **then** sets `lastRecalledAt = now`, **then** `applyScore(stability, score)`. `applyScore` ignores elapsed time. Difficulty is never written.
- Score 4 from S=0 → **24h** (already matches D3 first-success S). Difficulty stays unset.
- Score 4 from S=24h → ladder +1 → **48h**. FSRS-6 Good on-time (D=5, elapsed=24h) → **102h**.
- `ForgettingCurve.succeeded(elapsed, null)` is Good SInc with no thinking-time tweak. `recalledSuccessfully` also writes Difficulty and advances the anchor — **too much for the Stability-only slice**. Do not call it until a Difficulty slice needs it.
- **Elapsed must be computed before advancing `lastRecalledAt`.** If `lastRecalledAt` is set first, elapsed is 0 and Good SInc is zero growth (same-hour rule).
- `HOLA4_GRACIAS1_REPORT` exists in `LearningSessionControllerTestBase` and is unused.
- E2E: `commissioned_learning_session.feature`. Memory Tracker already has `expectStability` / `expectDifficulty`. Commissioned row label is **Commissioned**. `openNoteLevelMemoryTracker` is only `'normal' | 'spelling'`.
- `V300000261` backfilled D=5 where `stability > 0 OR recall_count > 0`. Later commissioned scores never write D. Next Flyway: **V300000262**.

## Product lock (slice 1)

Tutor **4** = FSRS Good: same memory update as ordinary correct. Inherits overdue bounded extra. Due is `lastRecalledAt + stability` (no 12h retry). New score 4 still inits D=5, S=24h. Scores 5/3/2/1/0 stay on the Working-draft table.

## Pins (frozen default `w`)

| Fixture | Today (ladder) | FSRS Good |
|---------|----------------|-----------|
| New score 4 | S=24, D unset | S=24, D=**5** |
| On-time second score 4 (S=24, D=5, elapsed=24h) | S=**48** | S=**102** |
| Overdue second (elapsed=48h, same S/D) | (ladder ignores elapsed) | S=**146** (> 102) |
| Good next-D from 5 | unset | **5.001413f** (unit pin; do not E2E this float) |

## Commit size

One slice = one commit. Each Behavior slice asserts **only its unique post-condition**. Do not fold Difficulty, overdue, backfill, `applyScore` table edits, or gap-doc cleanup into the Stability-102 slice. Do not call `recalledSuccessfully` in the Stability-only slice (it writes D). Do not split one outcome across HTTP vs E2E as two slices (that is layers).

## Out of scope

- Settings requested-retention knob (B2), relearning steps, RecallLog, confusion off the ladder, FSRS Easy/Hard/Again for other Tutor scores, Accept of ADR 0003, 009 Again-bullet ADR qualification.
