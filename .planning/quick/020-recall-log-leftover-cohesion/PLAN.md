# Plan: RecallLog leftover cohesion

**Status:** in progress

Tighten leftovers from `.planning/quick/004-recall-log/` (see `CONTEXT.md`). Do not change RecallLog shape, revive `session_item`, or accept ADRs.

## Design

- History Correct/Incorrect after reload is the one user-facing gap: pin `GET` recall-prompts after `flushAndClear`. Smallest production fix only if that test is red for the right reason. If the fix would add stored `AnswerOutcome` values beyond `OVERLAP` / `ACCIDENTAL_MATCH`, **Jidoka-stop**.
- Collapse tests that re-assert a canonical RecallLog shape already pinned on the same writer. Keep one canonical pin per writer outcome.
- Stats unit tests should drive `RecallAnswerRow` / `aggregateRows` (the production path), not a RecallPrompt adapter.

---

### 1. Prompt history shows Correct/Incorrect after reload

- **Type:** Behavior
- **Status:** done
- **Pre-condition:** A memory tracker has a graded recall prompt (ordinary correct MCQ, ordinary incorrect MCQ, or overlap spelling).
- **Trigger:** Caller loads prompt history after a new persistence context (`flushAndClear` then `GET` recall-prompts).
- **Post-condition:** History `answer.correct` is `true` for correct MCQ and overlap, `false` for incorrect MCQ (badge can render; not `null`).
- **Pin:** `MemoryTrackerRecallPromptHistoryCorrectnessControllerTest` — grade through the answer controller, `flushAndClear`, assert only `correct`. Existing submit-response `getCorrect()` stays the live-grade pin.
- **Learnings:** Already recovered by `Answer.getCorrect()` (RecallLog for ordinary MCQ, persisted `outcome` for overlap). No production change. Tests live in their own class so `MemoryTrackerRecallPromptsControllerTest` stays under 250 lines.

---

### 2. One canonical pin per RecallLog writer

- **Type:** Structure
- **Status:** done
- **What:** Removed overlapping tests/assertions. No production change.
- **Learnings:** Deleted `markAsRecalledIncrementsRecallCount`, `unansweredPromptDoesNotWriteARecallLog`, spelling `correctAnswerLeavesARecallLogLinkedToTheAnswer`, and the unused frontend `recall-log-answer-id` absence pin. Slimmed MCQ/spelling validate tests, tutor score-4 log, accidental-match log, overlap schedule, and `LearningSessionRecordTests` to delta-only. Renamed validate tests to `shouldValidateTheAnswer` after dropping tracker-update assertions.
- **Keep** (unique claims still live): just-review GOOD shape; just-review AGAIN as second log; MCQ GOOD+`answer_id`; MCQ AGAIN+`answer_id`; overlap writes no log; confusion CONFUSION on matched tracker; tutor score mapping parameterized test; unmatched title writes no log; property-tracker credit vs note-level; E2E just-review logs; frontend GOOD elapsed-hours render and two-log AGAIN.

---

### 3. Stats tests drive RecallAnswerRow

- **Type:** Structure
- **Status:** planned
- **What:** `RecallStatsTestFixtures.answered` builds `RecallAnswerRow`. Call `aggregateRows`. Delete `RecallStatsService.aggregate(List<RecallPrompt>)`, `rowsFrom`, and `productOutcomeFromLogs`.
- **Verify:** `RecallStatsServiceTest` (and any fixture callers) still pass; `compute` / JPQL path unchanged.

---

## Out of scope

- Accept ADR 0003 or ADR 0005
- RecallLog source column; revive `session_item` / `learning_session`
- Edit committed backfill SQL; restore deleted backfill tests
- Reject just-review API on commissioned trackers
- `@Formula` / COUNT for `getRecallCount()` (lazy walk works with `enable_lazy_load_no_trans`; Formula fights in-request `addRecallLog`)
- Show `answerId` on `RecallLogs.vue`
