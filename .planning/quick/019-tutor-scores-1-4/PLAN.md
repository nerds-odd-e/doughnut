# Plan: Tutor Feedback scores 1–4

**Status:** in progress

**Goal:** Tutor Feedback scores are **1–4**, identical to FSRS G and to `product_outcome` (`1` Again, `2` Hard, `3` Good, `4` Easy). Request rubric, parser, recording, latest score, and RecallLog use that scale. Alias log rows (`SHRINK`, `AGAIN_ZERO`) rewrite to `HARD` / `AGAIN`. Live domain is `GOOD` | `EASY` | `HARD` | `AGAIN` | `CONFUSION`. ADR 0003 stays Proposed.

## Locked

- Score **1** → `AGAIN`, **2** → `HARD`, **3** → `GOOD`, **4** → `EASY`. `score = G`.
- Just review Yes → **3** (Good); No → **1** (Again).
- Rubric (ADR 0005):
  - **4** — mastered with full fluency
  - **3** — mastered with fluency
  - **2** — mastered but not fluent, or needed a reminder then showed mastery
  - **1** — needed several reminders, or could not reach the item even with help
- Valid report scores are **1, 2, 3, and 4**. Other integers are rejected report entries (same path as today’s out-of-range score).
- Persist named grades on RecallLog. Latest tutor feedback is **1–4** via `AGAIN→1`, `HARD→2`, `GOOD→3`, `EASY→4`.
- Alias rewrite: `recall_log.product_outcome` `SHRINK` → `HARD`, `AGAIN_ZERO` → `AGAIN`. Stability and Difficulty stay.
- `GOOD` / `EASY` / `HARD` / `AGAIN` / `CONFUSION` rows stay those grades (old digit **4** that was Good stays `GOOD`, shown as **3**).
- Flyway wrappers `V300000271`–`V300000278` stay. Helpers they call may use SQL literals `'SHRINK'` / `'AGAIN_ZERO'` so replay still selects those column values. Alias rewrite is ungated `V300000279`.
- Affirmative current state in ADR, tests, and code. No “used to be 0–5 / shrink” prose. Tests pin the 1–4 outcomes. Out-of-range rejection is current validation — pin that.
- Independent of `.planning/quick/018-last-recall-leftover-cohesion/` (different files).

## Out of this plan

- Accepting ADR 0003.
- E4 fitting.
- Replaying Stability/Difficulty from old shrink math.
- Squashing Flyway history.
- Confusion / overlap / just-review button count.

## Discoveries

- Historical backfills (271/272/277/278 helpers) keep `'SHRINK'` / `'AGAIN_ZERO'` in **their** SQL `IN` lists so Flyway replay still selects those column values. Live `mappedGradeSqlInList()` is the four grades.

## Slices

### 1. Lock 1–4 in ADR 0003 and 0005

- **Type:** Structure
- **Status:** done

Proposed ADR 0003/0005 Decision is the 1–4 identity (`score = G`). First-rating is one `G = score` rule (Again **5h** in the `S0(G)` list). Graded **2** is Hard. `product_outcome` listed as `GOOD` | `EASY` | `HARD` | `AGAIN` | `CONFUSION`. ADR 0005 rubric and latest-score map are **4/3/2/1**. FSRS gap / SEED-004 already point here; slice 5 still drops leftover 0–5 pins and `STATE.md`.

### 2. Tutor scores 1–4 are the four grades

- **Type:** Behavior
- **Status:** done

Request, parser, recording, and latest score are `score = G`. Graded **2** is Hard (`shrinkStability` gone). Parser rejects 0/5/6.

### 3. Alias RecallLogs are HARD and AGAIN

- **Type:** Behavior
- **Status:** done

Ungated `V300000279` / `AliasRecallLogGradeBackfill`: `SHRINK`→`HARD`, `AGAIN_ZERO`→`AGAIN`; S/D/due unchanged. Pins in `AliasRecallLogGradeBackfillTest`. Frequent-failure counts `AGAIN` only.

### 4. Live product_outcome is four grades plus CONFUSION

- **Type:** Structure
- **Status:** done

Live enum is `GOOD` | `EASY` | `HARD` | `AGAIN` | `CONFUSION`. OpenAPI regenerated. Historical helpers keep `'SHRINK'` / `'AGAIN_ZERO'` literals. `mappedGradeSqlInList()` is the four live grades (`ProductOutcomeTest`).

### 5. Trackers and leftover pins match 1–4

- **Type:** Structure
- **Status:** planned

`STATE.md` (include `V300000279`) plus leftover 0–5 / shrink pins in tests and fixtures. Frontend `tutor-feedback-score-4` (was 5) if still on the old digit. FSRS gap / SEED-004 already point at this plan from slice 1; keep remaining deferred **E4** + accept ADR 0003.

## SLICE PLAN WRITTEN
