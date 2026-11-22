# Replace stale waiting recall questions

**Identity:** SEED-073#replace-stale-waiting-questions
**Source:** [Replace stale waiting recall questions](../../seeds/SEED-073-learning-refinement-and-retrieval.md#replace-stale-waiting-questions)
**Source review:** [source-review.md](source-review.md)

## Goal and scope

A due tracker with a stale waiting multiple-choice question can get a
replacement through the ordinary batch. Both the batch and on-demand recall
remove confirmed stale unanswered prompts once a current prompt is available.
The story's scope exclusions apply to the slices below.

## Current decisions

- Use the due window and eligibility rules in
  `QuestionGenerationBatchPlanningService` and `MemoryTrackerRepository`.
  Compare the projected note's update time with the time at which the
  generation input was read. Use a strict earlier-than comparison for the
  timestamp shortcut because MySQL stores note update time at second
  precision. Equal times, missing metadata, and a different focus note
  identity go through fingerprint confirmation. The timestamp comes from
  preparing the request, before provider generation and import.
- Keep the new input metadata alongside the existing private fingerprint.
  Extend `QuestionGenerationRequestBuilder` and carry the metadata through
  the on-demand result and existing batch request metadata. Read the same
  current focus used by the existing fingerprint check. Legacy questions
  with a known fingerprint can still match. An unknown fingerprint alone
  does not establish a content mismatch for deletion.
- Reuse `MemoryTrackerLifecycle` for prompt deletion, conversation detachment,
  and removal of unreferenced questions. Scope cleanup to the same tracker
  and its unanswered, uncontested multiple-choice prompts with a conflicting
  fingerprint. Preserve existing contests and previously recorded attempts.
- Use the existing accepted note projection and learning-data ownership in
  [ADR 0002](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md).
  Preserve answers, grades and scheduling state under
  [ADR 0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md).

## Baseline evidence

On 2026-11-22 in the preparation worktree at `ba636017ff`, an isolated
MySQL test-profile probe inside `MemoryTrackerBatchRecallPromptControllerTest`
rendered and imported a question for “nine,” published “ten” through the text
controller, and verified that the reloaded note's fingerprint differed.
Calling `findCandidateMemoryTrackersForBatchGeneration` still returned an
empty list for the due tracker. That final assertion was the only failure
among 25 tests. The temporary probe was removed, and the same command passed
all 24 maintained tests. The maintained batch test observed both stored
prompts after on-demand replacement. The deletion controller test exercised
shared question ownership and conversation retention.

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only \
  --tests '*MemoryTrackerBatchRecallPromptControllerTest' \
  --tests '*MemoryTrackerQuestionDeletionControllerTest' \
  --tests '*MemoryTrackerRecallPromptFreshnessControllerTest' \
  --tests '*QuestionGenerationBatchCandidateMemoryTrackersTest'
```

## Outside-in proof ownership

| Promise | Slice | Observable proof |
| --- | --- | --- |
| The timestamp describes the input read and survives generation and import | 1 | Inspect ordinary provider request metadata and reload the saved question. Stored focus identity, timestamp and fingerprint describe the rendered input. Existing prompt selection still works. |
| Due trackers with stale waiting questions get a batch replacement. A matching prompt avoids duplicate generation. | 2 | Run the ordinary planner for due trackers after accepted edits and inspect the planned request and its rendered input. Check edited, unchanged and restored content, a match alongside a stale prompt, and a legacy prompt with missing timestamp metadata. Preserve the existing due-window and pending-request exclusions. |
| A current batch replacement retires stale unanswered prompts. | 3 | Import the planned replacement through the batch importer, reload the tracker's waiting questions, and verify selection of the imported prompt and preservation of prior answers and grades. |
| Opening recall also retires stale unanswered prompts while preserving history. | 4 | Load the tracker through the ordinary recall controller after an accepted edit. Check the current choice, remaining waiting prompts, earlier answer and grade, due time, and a shared question record still referenced by history. |

Use the isolated backend MySQL test profile with stubbed provider boundaries.
Extend the maintained planning, batch controller and freshness controller cases.
Run the focused suite above for the affected slices. Batch rendering shows the
edited content sent to the provider, and recall uses the existing endpoint shared
by web and CLI.

## Ordered slices

### 1. Record when question generation read the focus note

Type: Structure
Status: done

Record the focus note identity and input-read time with the fingerprint
when preparing the provider request. Carry them through both generation paths
to persisted question metadata. This supplies the timestamp shortcut in
slice 2. **Proof.** Observe the prepared and persisted input identity and
time, private API output, and the same reuse behavior for a matching prompt.

**Accepted proof (2026-11-22).** `FocusNoteInput(noteId, readAt, fingerprint)`
is built in `QuestionGenerationRequestBuilder` (read time from
`TestabilitySettings`) and stored in the MCQ JSON (no migration). Batch rows
carry `focus_note_id` and `focus_note_read_at` (epoch millis) beside the
fingerprint; `QuestionGenerationBatchJsonlRenderer.focusNoteInputFrom` decodes
them on import, and a row without them imports fingerprint-only (kept for
batches spanning a deploy; slice 2 sends missing metadata to fingerprint
confirmation). `McqBuilder.generatedFrom` now records a full input, so a
legacy fingerprint-only question needs explicit setup in slice 2.
Observations: `QuestionGenerationBatchJsonlRendererTest` expected body;
`MemoryTrackerBatchRecallPromptControllerTest#recordsTheRenderedFocusInputWithTheImportedQuestion`;
`MemoryTrackerQuestionPersistenceControllerTest#preservesTheGenerationFocusWithThePersistedQuestion`
(including private-field absence); existing reuse and legacy cases. The
focused suite plus consumer-traced suites passed (89 tests), and the
builder's consumers passed after refactoring. API generation produced no change.

### 2. Prepare a batch replacement for an edited focus note

Type: Behavior
Status: done

A due tracker has only a waiting question for the older focus. During batch
planning, retain the cheap timestamp shortcut for likely current prompts.
For the remaining candidates, use the same fingerprint match as recall to
check all eligible waiting prompts before creating a batch request.
**Proof.** Reproduce the baseline through the planner and render the edited
focus. Check that a matching question, including a match after restoring the
content or alongside a stale prompt, keeps the tracker out of the batch.

**Accepted proof (2026-11-22).** The candidate SQL now excludes only a
waiting prompt without an MCQ; `QuestionGenerationBatchPlanningService`
drops trackers with a current waiting MCQ, first through
`FocusNoteInput.wasReadAfterLastUpdateOf(note)` and otherwise through the
recall fingerprint match `findUnansweredMcqForFocus`. Both queries keep the
contested-question exclusion. A waiting MCQ with no recorded fingerprint now
gets a batch replacement, as recall already replaces it on demand.
Observations: `MemoryTrackerBatchRecallPromptControllerTest#plansABatchReplacementFromTheEditedFocusBeforeRecall`
(failed before the change) and the `QuestionGenerationBatchCandidateMemoryTrackersTest`
cases for an older focus, a match beside a stale prompt, a read after the
last update, restored content, a legacy fingerprint-only match and a missing
fingerprint. The `QuestionGenerationBatch*` and `MemoryTracker*ControllerTest`
suites passed (228 tests), and the affected suites passed again after refactoring.

### 3. Remove stale waiting prompts after batch replacement

Type: Behavior
Status: done

After ordinary import supplies a current question, remove the tracker's
confirmed stale unanswered prompts through the lifecycle. Keep the imported
prompt and any answered history. **Proof.** Import a current replacement
for a tracker with a stale waiting prompt. Verify the remaining queue and
recall history after reloading the tracker.

**Accepted proof (2026-11-22).** `MemoryTrackerLifecycle.deleteStaleUnansweredRecallPrompts(tracker, fingerprint)`
removes uncontested unanswered MCQ prompts whose known fingerprint differs
(`RecallPromptRepository.findAllUnansweredMcqsForOtherFocus`; an unknown
fingerprint is kept) and shares conversation detachment and unreferenced
question removal with `deleteUnansweredRecallPrompts`.
`RecallPromptPersister.persistImportedRecallPromptForMcq` runs it only when
the imported question matches the current focus; slice 4 reuses the
lifecycle method. Observations: `MemoryTrackerBatchStalePromptRetirementControllerTest`
(remaining queue, imported prompt offered at recall, earlier answer and
grade, due time; the shared question survives) and the unchanged
"edit after rendering" case for "not before". The new case failed before the
change; the `QuestionGenerationBatch*`, `MemoryTracker*`, `RecallPrompt*` and
conversation suites passed (501 tests); the split batch test classes passed
after refactoring.

### 4. Remove stale waiting prompts when recall supplies a current question

Type: Behavior
Status: done

Opening recall after a focus edit selects or generates a current question
through the ordinary recall endpoint. Apply the same lifecycle cleanup once
that prompt is available. **Proof.** Check the current question and the
remaining waiting queue after recall opens. Compare earlier answers, grades
and due time, and confirm that history still holds any question it references.

**Accepted proof (2026-11-22).** `RecallPromptService.generateAQuestion`
passes the selected or newly generated current prompt to
`RecallPromptPersister.replaceStalePromptsWith`, the same lifecycle cleanup
the batch import uses; with no current prompt it deletes nothing.
Observations: `MemoryTrackerRecallStalePromptRetirementControllerTest`
(edited-focus question offered, cleanup after generation and after selecting a
waiting current prompt, no cleanup when generation is unavailable, earlier
answer with its choices and grade, due time). The two cleanup cases failed
before the change. The recall answer-after-renewal test was deleted
(obligation G1), and the edit-after-rendering batch case now expects only
the offered prompt. The `MemoryTracker*`, `RecallPrompt*`,
`QuestionGenerationBatch*`, `Recall*Controller*` and conversation suites
passed (618 tests), and 426 affected tests passed after refactoring.
Refactor follow-up for wrap-up: `MemoryTrackerRecallStalePromptRetirementControllerTest`
inherits the batch-specific `MemoryTrackerBatchImportControllerTestBase`,
whose batch setup it never uses.

## Plan review

Slice plan refinement was unnecessary. A short storage change supplies the
batch selector, and both replacement paths use one cleanup rule with separate
provider-boundary proof. No remaining slice-specific concern was identified.

## Story obligations

### G1. Answering a waiting card after recall removed its stale prompt
Reported: slice 4 — "Deleted MemoryTrackerRecallPromptFreshnessControllerTest#gradesTheIssuedPromptAgainstItsOriginalChoicesAfterRenewal: it answers the old waiting prompt after recall renewed the question, and that prompt is now deleted."
Story clause: "submission from a card whose prompt has just been removed"
Disposition: excluded "An edit during an in-flight batch, overlap between a batch and on-demand generation, and submission from a card whose prompt has just been removed"
