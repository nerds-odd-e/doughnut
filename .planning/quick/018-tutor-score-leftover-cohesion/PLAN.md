# Plan: Tutor score 1–4 leftover cohesion

**Status:** in progress (slices 1–2 done)

**Goal:** The shipped 1–4 identity (`score = G`) is pinned once per observable surface. Drop leftover duplicate tests from the old “score 4 leaves GOOD” special case and from Request-copy / parser / alias-rewrite outlines that re-assert the same post-condition.

## Locked

- Production map stays `1` Again, `2` Hard, `3` Good, `4` Easy. No schedule or parser behavior change.
- HTTP remains the pin for exact first-rating floats and for Request markdown copy (including the four rubric lines).
- E2E remains the pin for user-visible first-score hours and for recording latest tutor feedback **4**.
- `ProductOutcome.mappedGradeSqlInList()` stays the four live grades; `ProductOutcomeTest` stays (unique SQL-contract pin; backfill tests do not assert that string).
- Historical Flyway helpers keep `'SHRINK'` / `'AGAIN_ZERO'` SQL literals for replay. Do not collapse those into `mappedGradeSqlInList()`.
- ADR 0003 stays Proposed.

## Out of this plan

- Accepting ADR 0003.
- Changing `V300000279` or 271–278 helpers.
- Dropping HTTP first-rating `S0`/`D0` floats because E2E shows rounded Difficulty.
- Frontend `tutor-feedback-score-4` display pin (different boundary: renders an already-mapped number).
- Enum exhaustiveness `CONFUSION` branches in `scoreForProductOutcome` / `recordFeedback` (required by the switch; latest-tutor query already excludes CONFUSION).

## Discoveries

Inspection of the five 1–4 commits found **no production mapping bug**. Parser, `productOutcomeForScore`, recording, latest score, E2E, and ADR Decision agree.

Leftovers that wrap-up missed:

- HTTP `scoreThreeLeavesAGoodRecallLogWithoutAnswer` folded into `matchedScoreLeavesMappedRecallLog`.
- E2E `expectLearningSessionRequestIncludesRubric` now pins only `score from 1 to 4 per item` and `Hola: 4`; HTTP keeps the four mastery lines.
- Parser `acceptsScoreFromOneToFour` re-asserts the same parse shape for 1, 2, 3, and 4. Reject `0/5/6` already defines the range.
- `AliasRecallLogGradeBackfillTest` re-asserts Stability, Difficulty, and due for both aliases. `StillNewFirstRatingBackfillTest` still names a tracker `shrink`.

## Slices

### 1. One HTTP outline for score → named RecallLog

- **Type:** Structure
- **Status:** done

Folded leftover GOOD HTTP test into `matchedScoreLeavesMappedRecallLog` (`4/3/2/1` → `EASY/GOOD/HARD/AGAIN`). Canonical GOOD row still pins `answerId` null; siblings only the outcome. Dropped E2E `Recording tutor score 3 leaves a GOOD RecallLog`; just-review still uses the GOOD-log step.

Learning: step def stays — still used by `spaced_repetition.feature`.

### 2. E2E Request rubric is the 1–4 identity only

- **Type:** Structure
- **Status:** done

`expectLearningSessionRequestIncludesRubric` now asserts `score from 1 to 4 per item` and `Hola: 4` only. Four mastery lines stay in `LearningSessionRequestTests`.

Learning: helper lives in `recallLearningSessionMethods.ts`; Cypress not re-run (assertion trim).

### 3. Parser range is one accept plus reject-outside

- **Type:** Structure
- **Status:** planned

Structure: replace parameterized accept `1/2/3/4` with **one** in-range accept. Keep reject `0`, `5`, and `6` (`Score must be 1, 2, 3, or 4.`).

Unlocks: range is pinned as reject-outside, not four identical accepts.

### 4. Alias rewrite pins schedule once

- **Type:** Structure
- **Status:** planned

Structure: canonical alias row (`SHRINK`→`HARD`) asserts grade plus S/D/due unchanged. `AGAIN_ZERO` sibling asserts only `AGAIN`. Rename `StillNewFirstRatingBackfillTest` tracker `shrink` to the column-literal fixture (not a product concept).

Unlocks: historical alias tests match small-test “canonical shape once.”
