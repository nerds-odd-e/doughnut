# CONTEXT: RecallLog leftover cohesion

Inspection of the shipped RecallLog work (`.planning/quick/004-recall-log/`, migrations `V300000263`–`V300000268`). This plan does **not** reopen ADR 0003/0005 accept, add a log source column, or restore `session_item`.

## Meaningful findings

### Prompt history Correct/Incorrect after reload is unpinned

Memory Tracker history (`GET` recall-prompts → `RecallPromptHistoryItem.answer.correct`) shows Correct/Incorrect in `MemoryTrackerPageView.vue` when `correct != null`.

`Answer.getCorrect()` is a recovery ladder: overlap `outcome` → transient `correct` (same request only; `@Transient`, column dropped in `V300000268`) → walk `answer.recallLogs`. Ordinary MCQ/spelling persist no `AnswerOutcome` (enum is only `OVERLAP` / `ACCIDENTAL_MATCH`). After a new load, history depends on the linked RecallLog.

No controller test grades through the API, `flushAndClear`s, then asserts `getRecallPrompts(…).answer.correct`. History tests that use `RecallPromptBuilder.answerChoiceIndex` never write a log, so they cannot pin this.

Live grade responses still set the transient, so the submit response is fine. The gap is **reloaded history**.

### Redundant tests (same entry point, same claim)

Canonical shapes already pinned:

- Just-review Yes → one GOOD log, elapsed, `recorded_at`, null `answer_id` (`successfulMarkAsRecalledLeavesOneGoodRecallLog`)
- MCQ correct → GOOD + `answer_id` (`correctAnswerLeavesAGoodRecallLogLinkedToTheAnswer`)
- MCQ incorrect → AGAIN + `answer_id`
- Overlap → no log (`overlapAnswerDoesNotWriteARecallLog`)
- E2E just-review Yes/No logs (`spaced_repetition.feature`)

Overlaps to remove or slim are listed in the PLAN slice.

### Dead production code in stats

`RecallStatsService.compute` uses the JPQL projection `findAnsweredRecallAnswerRows`. `aggregate(List<RecallPrompt>)`, `rowsFrom`, and `productOutcomeFromLogs` exist only so `RecallStatsTestFixtures` can feed in-memory entities. Tests should build `RecallAnswerRow` and call `aggregateRows`.

## Looked at and not planning

| Finding | Why not |
|---------|---------|
| `getRecallCount()` walks lazy `recallLogs` | `enable_lazy_load_no_trans` is on; E2E reads count over HTTP. A `@Formula` would disagree with in-memory `addRecallLog` before flush. No failing pin. |
| Tutor Java mapping vs `V300000266` CASE | Do not edit committed Flyway. |
| `latestTutorFeedbackScore` set in `NoteController.getNoteInfo` | Works; shotgun, not user-visible. |
| `PATCH mark-as-recalled` on commissioned | Due queue excludes `COMMISSIONED`; product path is Tutor Feedback. |
| Deleted backfill controller tests | Later `V300000268` dropped `answer.correct`; replay tests cannot be restored as they were. |
| LearningSession `record` relies on dirty-check | E2E asserts Stability after record; schedule persists. |
| Frontend vs E2E GOOD log rendering | Different surfaces; keep the mounted-page GOOD/AGAIN render tests, drop only the unused `recall-log-answer-id` testid pin. |

## ADRs

- [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md) (Accepted): RecallLog glossary.
- [ADR 0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy.md) (Proposed): log shape. This plan does not change it.
