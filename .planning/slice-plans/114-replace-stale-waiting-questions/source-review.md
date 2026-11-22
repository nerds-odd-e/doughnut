# Source review: SEED-073#replace-stale-waiting-questions

Launch: 2026-11-22, fresh general-purpose reviewer (no conversation inheritance,
no fork or resume), instructed to use only the supplied source, read no project
files and change nothing; it used no tools. Input: the story's Goal, Scope, Key
examples and Outside delivery scope from
[SEED-073](../../seeds/SEED-073-learning-refinement-and-retrieval.md#replace-stale-waiting-questions),
verbatim, with no plan, slice, product or proof detail.

Unnumbered context returned:

- Reasons and framing: the Goal's motivation that the learner should get the
  replacement "before opening recall", and that stale prompts should stop
  "accumulating". C1, C4 and C6 cover the checkable parts.
- Settled baseline: the batch, its due window and its import already exist.
- Ambiguity, not a promise: in Example 2, the source does not say whether the
  older non-matching waiting question is deleted. It may fall under C4 if a
  reuse counts as a "current prompt is available". No rejection rule is derived.
- Outside delivery scope: an edit made while a batch is in flight; overlap
  between a batch and on-demand generation; submitting from a card whose prompt
  was just removed.
- Deferred to another story: edits to linked notes.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| C1 | Before the learner opens recall, the ordinary batch queues a replacement question built from the edited focus note, for a due multiple-choice tracker inside the existing batch due window whose only waiting question is stale (authored focus content changed after the question was made). Goal: "can get its replacement question from the ordinary batch before opening recall"; Scope: "Use update time to screen likely stale waiting questions for the existing batch due window… before queuing a replacement"; Example 1: "nine" → "ten", "The batch queues a replacement from the edited note" | slice 2 | observed: MemoryTrackerBatchRecallPromptControllerTest#plansABatchReplacementFromTheEditedFocusBeforeRecall: nine→ten published via text controller, ordinary local batch planned, rendered input contains "opens at ten" (failed before; s2-red) |
| C2 | When a due tracker already has a waiting question matching the current authored focus fingerprint, the batch reuses it and leaves the tracker out of the new batch, even when an older non-matching waiting question is also present. Scope: "Confirm reuse by comparing the authored focus fingerprint before queuing a replacement"; Example 2 | slice 2 | observed: QuestionGenerationBatchCandidateMemoryTrackersTest#excludesTrackerWhenAMatchingQuestionWaitsBesideAStaleOne and #excludesTrackerWaitingWithAQuestionReadAfterTheLastUpdate: tracker left out of candidates |
| C3 | If the note's update time marks a waiting question as possibly stale but the authored focus fingerprint has not changed, no replacement is queued. Update time only screens; the fingerprint decides. Scope: "Use update time to screen likely stale… Confirm reuse by comparing the authored focus fingerprint before queuing a replacement" | slice 2 | observed: QuestionGenerationBatchCandidateMemoryTrackersTest#excludesTrackerWhenTheFocusWasRestoredAfterTheQuestionWasRead: update time after read time, matching fingerprint, no candidate |
| C4 | Ordinary batch import deletes the tracker's confirmed-stale, unanswered prompts once a current prompt is available for it, and not before. Scope: "Both ordinary batch import… delete confirmed stale, unanswered prompts for that tracker once a current prompt is available"; Example 1: "removes the stale prompt after the replacement is available"; Goal: "stale unanswered prompts stop accumulating on the tracker" | slice 3 | observed: MemoryTrackerBatchStalePromptRetirementControllerTest#retiresOnlyTheUnansweredUncontestedPromptsFromAnotherFocus and #offersTheImportedReplacementAtRecall: after batch import of the "ten" replacement, the stale "nine" waiting prompts are gone and recall offers the imported prompt; MemoryTrackerBatchRecallPromptControllerTest#replacesTheImportedQuestionWhenAnEditLandsAfterRendering keeps prompts when the import is not current (not before) |
| C5 | If the learner opens recall before the batch has prepared a replacement, the server offers a question generated from the edited note, not the stale one. Example 3 | slice 4 |  |
| C6 | On-demand recall deletes the tracker's confirmed-stale, unanswered prompts once it has a current prompt. Scope: "…and on-demand recall delete confirmed stale, unanswered prompts for that tracker once a current prompt is available"; Example 3: "and removes stale unanswered prompts"; Goal | slice 4 |  |
| C7 | After stale prompts are deleted by either path, earlier answered attempts remain available, including their choices and grades. Scope: "Preserve answered attempts"; Example 3: "Earlier answers, their choices and grades… are still available" | slice 4 |  |
| C8 | Existing contests survive the cleanup. Scope: "Preserve… existing contests" | slice 4 |  |
| C9 | The tracker's schedule, including its due time, is unchanged by replacement and cleanup. Scope: "Preserve… the tracker's schedule"; Example 3: "and the due time are still available" | slice 4 |  |
| C10 | A question record is kept after cleanup as long as any prompt still references it. Scope: "A question record survives while any prompt still references it." | slice 4 |  |
