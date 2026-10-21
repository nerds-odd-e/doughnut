# Donut Retrospective Findings

Findings specific to Donut’s product, repository tooling, or local conventions.
Findings about shared Open Dough skills, supporting guidelines, and skill scripts
remain in [DearDough.md](DearDough.md), including those observed in this project.
Existing finding identifiers and occurrence evidence are preserved when moved.
Resolved problems are removed; Git history keeps their evidence.

A finding is Donut’s when the remaining problem concerns Donut’s product,
scripts, or Donut-authored guidance. A repaired product incident alone does not
establish a defect in published Open Dough guidance. Shared planning, proof
acceptance, and delivery failures remain in DearDough when the observation
concerns those responsibilities, even after their product symptom is repaired.

## Ownership and priority assessment

Reviewed 2026-10-12 against Donut `8ac439b16b` and published Open Dough
`v0.3.60` (`e72ed2aaea475c7fbe8427a5c66bc877dda51275`). The installed
[execution wrap-up](https://github.com/terryyin/open-dough/blob/v0.3.60/src/skills/dough-execute-plan/references/wrap-up.md)
and [refactor checks](https://github.com/terryyin/open-dough/blob/v0.3.60/src/skills/dough-post-change-refactor/references/refactor-checks.md)
match that release. Public refactor guidance retains negative assertions when
absence is the actual promise and defers to project test style. Donut’s
principle 7 and `unit-testing` skill supply the stricter removal convention.

| Group | Distinct recorded executions | Impact and disposition |
| --- | --- | --- |
| MinerU venv recovery | One | About 28 minutes of mixed environment and stack work; recovery remains unqueued. |
| Stale Development classes | One; later probes healthy | Failed start, unknown cause; unqueued, not confirmed resolved. |

The earlier 2026-10-21 local-log selection at Donut `55007dab3d` queued no
new stories. The owner's subsequent product reassessment at
`b46b36c9c1abdd6e31802b7ec2dac14330b583cb` also read the current backlog and
canonical candidates, recent completed-story reviews, and relevant recall
source/tests. It selects two new leading product stories: recover a failed or
lost answer response, then retry a failed question load or explicitly choose
existing self-review. Both preserve learning history and the current direction;
the existing four queued stories retain their order and Taken remains empty.
These source-backed assessments establish failure paths, not live occurrence
counts, frequency or measured cost. The formal contest candidate
[SEED-073#story-6](.planning/seeds/SEED-073-learning-refinement-and-retrieval.md#story-6)
remains unqueued with its policy decisions intact.

The single MinerU recovery remains below the current learning/refinement
direction. The stale Development-class cause remains unestablished after the
healthy probes below, so no specific remedy is selected for either older
finding. Their original evidence and uncertainty remain available.

Shared findings retained in DearDough include changed-boundary proof acceptance
(DD-218; public wrap-up step 2), decisive planning premises (DD-224; public
`dough-slice-planning`), proof-table coverage, sizing, file-size rules, and CI
observation/delivery. Their remaining claims concern the shared workflow, not
an unrepaired Donut helper or voice-control defect. Successful local practice
is separated below from open problems and has no backlog entry.

## Queued product improvements: Recall request recovery

### DD-317 — A failed or lost answer response has no complete recovery journey

**Current source assessment:** At Donut
`b46b36c9c1abdd6e31802b7ec2dac14330b583cb`,
`frontend/src/components/recall/SpellingQuestionDisplay.vue` sets `submitted`
before emitting the answer and disables Answer. Its parent
`RecallPromptCard.vue:onSpellingAnswer` reacts only on success and provides no
failure completion that restores submission. A rejected answer request therefore
leaves that mounted spelling attempt unable to submit again.

For multiple choice, `RecallPromptComponent.vue:submitAnswer` re-enables input
on an API error; its existing `RecallPromptComponent.spec.ts` test "allows
retrying on API error" proves the mocked fail-then-success path only.
`AnswerService.createAnswerForQuestion` calls `Answer.buildAnswer`, which
rejects an existing answer with "The question is already answered" before
scheduling. Consequently, when an answer commits and its response is lost,
retrying the current operation cannot recover the stored result. This prevents
a duplicate answer, but leaves the learner without a complete in-place recovery.
The spelling grading path likewise rejects an existing answer
(`services/SpellingRecallGrading.java`).

**Ownership / limits:** Donut owns these UI/API states and learning effects.
This is separate from shared proof and delivery processes. These are current
source/test observations, not reproduced live incidents; production occurrence
count, transport conditions and frequency remain unknown. Existing overlap
spelling retry concerns a returned learning outcome, not a failed request, and
does not cover this seam. No active story covers it.

**Queued response:** [Recover a recall answer after a failed or lost submission
response](.planning/seeds/SEED-081-recover-recall-request-failures.md#story-1)
(`SEED-081#story-1`). Preserve the original attempt and learning history, recover
a recorded result after response loss, and allow deliberate retry when the
attempt was not recorded. Not resolved; implementation and adoption are pending.
**Evaluation:** Inject a failure before acceptance and a response loss after
acceptance for both answer formats. Recover through the same card; inspect the
persisted answer, recall logs and schedule to establish one set of effects and
unchanged session position until a real result is recovered.

### DD-318 — A failed question load falls through to an unintended recall surface

**Current source assessment:** At Donut
`b46b36c9c1abdd6e31802b7ec2dac14330b583cb`,
`useRecallPromptFetching.ts:fetchNextRecallPrompts` writes an `undefined` cache
entry on an API error. `RecallPromptCard.vue` treats key presence as fetched and
renders `JustReview` for that falsy prompt. The existing
`frontend/tests/recall/RecallPromptCard.spec.ts` test "should show ContentLoader,
not JustReview, when navigating to a memory tracker that previously failed"
expects self-review after the initial failure and recovers only after changing
the current tracker away and back. No direct question retry is offered there.
`MemoryTrackerController.getRecallPrompt` returns 503 when question generation
produces no prompt; the server does not identify that as successful self-review.
Global error handling can show a toast, but does not supply a question-recovery
choice on the card.

The spelling branch loads its question separately in
`SpellingQuestionDisplay.vue:fetchSpellingQuestion`; after an error it clears
loading without a prompt, leaving an empty stem and the answer form available.

**Ownership / limits:** These are Donut-owned failure presentations and learner
choices, distinct from question-context quality and shared execution process.
The source-backed paths were not exercised live in this assessment; production
frequency, latency and cost are unknown. There is no active covering story.

**Queued response:** [Retry a failed recall-question load or choose self-review
explicitly](.planning/seeds/SEED-081-recover-recall-request-failures.md#story-2)
(`SEED-081#story-2`). Retain explicit existing MCQ self-review while making the
failed current question recoverable; do not add a spelling self-grading policy.
Not resolved; implementation and adoption are pending.
**Evaluation:** Fail the current question request and a future prefetch, then
retry on the same recall surface. The same tracker and progress remain; a
loaded spelling question is required for answering, and no Grade or schedule
change occurs until the learner submits an answer or deliberately grades an
explicit self-review.

## Open, not queued: Rebuilding a stale MinerU virtual environment

One retained execution; no repeat or automatic venv-recovery correction is
confirmed. Kept unqueued below the recurring groups. The pinned version and
held-stack parts were delivered (`f70984790f`, `ee414fbdf1` through `1ae39ec64f`)
and are outside this remaining finding.

### DD-161 — A `.venv-mineru` whose Python was garbage-collected had to be rebuilt by hand

A `.venv-mineru` whose Python lived in a garbage-collected Nix store path must
be rebuilt by hand. The plan's manual slice needed real MinerU to `/attach` real
PDFs through the CLI.

#### Occurrences

- Execution: SEED-059#story-3 / slice-plans/051-pdf-layout-from-bookmarks / 946e2a70e3; Timestamp: 2026-09-29 (slice 6, after 6f36cb2952); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.46.
  - Evidence: slice 6 agent report (1,692 s, about 197k tokens, 96 tool uses); plan premise "`.venv-mineru/bin` has no `python`"; venv rebuilt with Homebrew Python 3.12, `mineru[pipeline]==3.4.5` and `six`.
  - Observed effect: about 28 minutes for one manual slice, most of it environment repair and stack scaffolding rather than observation.
  - Inference: a supported way to rebuild the pinned MinerU environment after its Python disappears could reduce this remaining setup cost. The recorded 28 minutes also includes stack scaffolding, so it is not a measurement of venv recovery alone.

## Open, not queued: Development startup on stale compiled classes

### DD-159 — The Development stack failed to start on stale compiled backend classes in the default checkout

`pnpm dev` from the default checkout failed with `No qualifying bean of type NotebookGitCutoverService`: `backend/build/classes` still held classes from before that service was removed. Deleting `backend/build/classes` let the next start succeed. Its cause is unknown.

#### Occurrences

- Execution: SEED-054#story-1 / `4f2f230505:.planning/slice-plans/011-book-reading-uat/PLAN.md` / 681768a71b; Timestamp: 2026-09-29T07:25:42+08:00; Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.46.
  - Evidence: `dev.log` "APPLICATION FAILED TO START" with the missing-bean message; `git grep NotebookGitCutoverService -- backend/src` found nothing; the next `pnpm dev` was healthy.
  - Observed effect: one failed start and a short diagnosis before the UAT setup could continue.
  - Inference: the Development start's incremental build did not drop classes whose sources were deleted. Qualified: cause not investigated further.
- Investigation (2026-10-03, SEED-067#stacks-survive-other-builds slice 2): not reproduced. In the default checkout, a compiled `@Service` and its injecting consumer were deleted with the stack stopped, then `pnpm dev` started healthy and Gradle removed the stale class; the same held with a concurrent `classes --rerun-tasks` build and with a build-cache restore. Linked-worktree probes also removed the classes. No writer found; the cause remains unknown. Owner decides whether to keep watching or drop.

## Useful project practice (not an open problem)

The following is a successful Donut-specific observation technique. It is not
an unresolved defect and contributes neither to issue frequency nor priority.

### DD-230 — An environment-only Flyway callback with a MySQL named lock held a real startup migration open for full-stack proof

To observe the runner during a migration, the execution supplied `beforeMigrate.sql` through `SPRING_FLYWAY_LOCATIONS` from a directory outside the checkout; the file waited on a named lock that a separate session held and released. No product or test code and no delay option was added.

#### Occurrences
- Execution: SEED-067#start-e2e-after-migrations (first implementation commit f297a916c5)
  - Timestamp: 2026-10-11T09:18:00+09:00
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.58
  - Evidence: plan 066 Learnings "Slice 3" and "Slice 4" at `af62c7cd0a`; `sut.log` lines 148 to 234 in the execution worktree (`Executing SQL callback: beforeMigrate`, `Successfully applied 41 migrations`)
  - Observed effect: both `pnpm e2e:hold` and `pnpm cy:run` were observed returning 503 during the held migration and resetting test data only after the first 200; each slice took about five minutes including startup
  - Inference: useful practice: a timing fault was proved at the real boundary with a release barrier instead of a sleep or a stand-in response; it depends on the runner passing its environment to the backend, which was confirmed with `ps eww`

## Reopening

Reopen a project finding when a new occurrence contradicts its actual correction,
link that evidence to the existing story or correction if still active, and
otherwise queue a bounded recurrence story. A previously recorded resolution
without supporting correction evidence is insufficient to close a finding.
