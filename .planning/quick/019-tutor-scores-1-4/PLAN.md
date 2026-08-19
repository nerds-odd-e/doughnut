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
- Flyway wrappers `V300000271`–`V300000278` stay. Helpers they call may use SQL literals `'SHRINK'` / `'AGAIN_ZERO'` so replay still selects those column values. Next migration after `V300000278`.
- Affirmative current state in ADR, tests, and code. No “used to be 0–5 / shrink” prose. Tests pin the 1–4 outcomes. Out-of-range rejection is current validation — pin that.
- Independent of `.planning/quick/018-last-recall-leftover-cohesion/` (different files).

## Out of this plan

- Accepting ADR 0003.
- E4 fitting.
- Replaying Stability/Difficulty from old shrink math.
- Squashing Flyway history.
- Confusion / overlap / just-review button count.

## Discoveries

- Request copy and parser live in `LearningSessionRequestMarkdownBuilder` / `LearningSessionReportParser` (range **0–5** today). Example line is `Hola: 5`.
- Recording map and reverse map: `CommissionedLearningSessionFeedbackScheduling`. Score **2** on `S > 0` calls `shrinkStability` (80% S, D unchanged). New is already Hard for **2**.
- E2E `commissioned_learning_session.feature`: record **5**/**1**; first-score outline includes **0–5**; second-score **4/5/3**; “score **4** leaves GOOD”.
- HTTP pins the same numbers in `LearningSessionRecordTutorFeedbackTests` / `RecallLogTests` / `NoteControllerNoteInfoTests`.
- `latestTutorFeedbackScore` is derived, not stored.
- Frequent-failure count includes `AGAIN_ZERO`; after rewrite, `AGAIN` is enough.
- `ProductOutcome.mappedGradeSqlInList()` is used by still-New / ungraded / removed backfills. After the live enum is four grades, those **historical** helpers must keep selecting `'SHRINK'` and `'AGAIN_ZERO'` in their own SQL (replay at that version).

## Slices

### 1. Lock 1–4 in ADR 0003 and 0005

- **Type:** Structure
- **Status:** done

Proposed ADR 0003/0005 Decision is the 1–4 identity (`score = G`). First-rating is one `G = score` rule (Again **5h** in the `S0(G)` list). Graded **2** is Hard. `product_outcome` listed as `GOOD` | `EASY` | `HARD` | `AGAIN` | `CONFUSION`. ADR 0005 rubric and latest-score map are **4/3/2/1**. FSRS gap / SEED-004 already point here; slice 5 still drops leftover 0–5 pins and `STATE.md`.

### 2. Tutor scores 1–4 are the four grades

- **Type:** Behavior
- **Status:** planned

**Pre:** commissioned trackers (New and already graded). **Trigger:** open a Learning Session Request; record a Report with scores **1–4**. **Post:** Request rubric is the four lines above (example `Hola: 4`); each score writes the matching `product_outcome` and schedule (New first-rating `S0`/`D0`; graded **2** is Hard next S/D and due from `I`); latest tutor feedback is that 1–4 score.

Capability tests (extend existing, one outline where the path is the same):

- E2E `commissioned_learning_session.feature`: Request instruction; record **4**/**1** (latest **4**); first-score outline **4/3/2/1** with Easy/Good/Hard/Again hours; “score **3** leaves a GOOD RecallLog”; second-score outline **4→484**, **3→284**, **2→193** (same numbers as today’s **5/4/3**).
- HTTP `LearningSessionRequestTests` / `LearningSessionRecordTutorFeedbackTests` / `RecallLogTests` / `NoteControllerNoteInfoTests`: same map. Graded **2** pin is Hard next S/D (replace the 80% S pin).
- Parser: valid **1–4**; **0**, **5**, and **6** rejected (“Score must be 1, 2, 3, or 4.”).

Implementation: identity `productOutcomeForScore` / `scoreForProductOutcome`; `recordFeedback` **2** → `recalledHard`; delete `shrinkStability` / `MemoryTrackerShrinkStability` when unused. OpenAPI / generated client stay until slice 4 if the enum still has unused values for one deploy.

### 3. Alias RecallLogs are HARD and AGAIN

- **Type:** Behavior
- **Status:** planned

**Pre:** a commissioned tracker with a `SHRINK` or `AGAIN_ZERO` RecallLog (S/D already set). **Trigger:** apply the new Flyway rewrite. **Post:** those rows are `HARD` / `AGAIN`; Stability, Difficulty, and due unchanged; latest tutor feedback is **2** / **1**.

Ungated Java (or SQL) migration after `V300000278`. Controller-level pin via `makeMe` logs + run the backfill (same style as other recall-log rewrites). Frequent-failure count uses `AGAIN` only.

### 4. Live product_outcome is four grades plus CONFUSION

- **Type:** Structure
- **Status:** planned

`ProductOutcome` = `GOOD`, `EASY`, `HARD`, `AGAIN`, `CONFUSION`. OpenAPI + `pnpm generateTypeScript`. Historical backfill helpers keep SQL literals `'SHRINK'` / `'AGAIN_ZERO'` in **their** `IN` lists (replay). Tests that fixture those column values for 271/272/277 use the same literals. `mappedGradeSqlInList()` is the four live grades.

### 5. Trackers and leftover pins match 1–4

- **Type:** Structure
- **Status:** planned

`STATE.md` plus leftover 0–5 / shrink pins in tests and fixtures. Frontend `tutor-feedback-score-4` (was 5) if still on the old digit. FSRS gap / SEED-004 already point at this plan from slice 1; keep remaining deferred **E4** + accept ADR 0003.

## SLICE PLAN WRITTEN
