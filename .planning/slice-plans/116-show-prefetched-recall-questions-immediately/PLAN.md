# Show prefetched recall questions immediately

**Identity:** SEED-073#show-prefetched-recall-questions-immediately
**Source:** [Show prefetched recall questions immediately](../../seeds/SEED-073-learning-refinement-and-retrieval.md#show-prefetched-recall-questions-immediately)
**Source review:** [source-review.md](source-review.md)

## Goal and scope

A learner who advances to a multiple-choice card in web recall sees the prompt
its background prefetch loaded, or, while that prefetch is in flight, the
result of that same request. The card makes no display-time request. Initial
and background loading, Retry, Self-review, spelling loading and OVERLAP
recovery, answer submission against the displayed prompt ID, the shared
recall-prompt endpoint, backend freshness selection and CLI recall stay as
they are. `docs/recall-question-loading.md` is deleted and its
still-applicable guidance moves into existing documentation.

## Current decisions

- One rule replaces the two-step selection in
  `frontend/src/components/recall/useRecallPromptFetching.ts`: a successful
  load stores `loaded` for any tracker, and `fetchRecallPrompts` requests
  only trackers that have no load entry. Delete the `prepared` status, the
  `needsSelection` branch and the `prepared` check in
  `RecallPromptCard.vue`. The `foreground` flag keeps choosing
  `apiCallWithLoading` for the first card, Retry and the spelling restart.
- A successful off-screen Retry is a load like any other: returning to that
  tracker shows the prompt the Retry loaded.
- Documentation destinations for the deleted page's surviving guidance:
  - "Question ownership and lifetime" (ownership, fingerprint reuse, edits
    after presentation) → a new "Question ownership and reuse" section in
    `docs/recall-results.md`, beside the question loading section.
  - "Shared due list order" → a new "Due list order" section in
    `docs/recall-backlog-spreading.md`, beside the ADR 0003 scheduling
    policy it already cites.
  - "Question load states", CLI loading and spelling OVERLAP recovery → a new
    "Question loading and recovery" section in `docs/recall-results.md`,
    which already describes multiple-choice and spelling cards. The web
    paragraph says the prefetched prompt is shown as loaded; the display-time
    request is not described.
  - Retarget `DonutRetrospectiveFindings.md` line 8's "Current behavior"
    link to the new recall-results section.
- No North Star topic bears on this frontend loading change. ADR 0003 and
  ADR 0006 links move with the guidance that cites them.

## Decisive premises

| Premise | Consuming operation | Observation | Result |
| --- | --- | --- | --- |
| The display-time request lives only in `useRecallPromptFetching` and the card's `prepared` check; no other production code consumes `prepared`. | Slice 1 change | `grep -rn -e prepared -e startRecallAttempt -e fetchRecallPrompts -e recallPromptLoads frontend/src` at `23dc504c9c` | Only `RecallPromptCard.vue` lines 8, 86–127 and the composable. |
| The tests that consume the deleted behavior are exactly those that fail under the planned change. | Slice 1 proof | Scratch application of the decision above at `23dc504c9c`, then `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend exec vitest run tests/recall tests/pages/RecallPage tests/notes/NoteMoreOptionsForm.removalLanding.spec.ts`; scratch reverted. | 18 files, 92 tests: 3 failed, 89 passed. Both cases in `tests/recall/RecallPromptCard.activation.spec.ts` (expected the extra request for tracker 2) and `RecallPage.questionRecovery.spec.ts` "selects the current question when returning after an offscreen retry succeeded" (expected the re-fetched Question 3). All other recall, spelling, recovery and Self-review cases passed with the change. |
| No E2E scenario depends on a re-fetch at display time. | Slice 1 scope | `grep -rln -i "recall\|question" e2e_test/features | xargs grep -l -i "edit\|update the note\|change the"`, then the matching recall features read at `23dc504c9c`. | No recall scenario edits a note or changes the server's prompt between a background prefetch and reaching that card, so the prefetched response equals what the removed request returned; a page revisit remounts the card with empty load state. Remainder: hosted CI runs the E2E suite after publication. |
| Only `DonutRetrospectiveFindings.md` and the seed link to `docs/recall-question-loading.md`. | Slice 1 documentation | `grep -rln recall-question-loading --exclude-dir=node_modules --exclude-dir=.git .` at `23dc504c9c` | Those two files. The seed's mention is the story text itself. |

## Outside-in proof ownership

| Promise (story key example) | Slice | Observation |
| --- | --- | --- |
| Completed prefetch: advancing shows B's choices immediately, no request for B, lookahead continues | 1 | Rewritten activation spec: after B's background request resolves, `setProps({ currentIndex: 1 })` shows B's stem with no content loader and no further `getRecallPrompt` call for tracker 2; tracker 3 is requested. Fails on the current code (loader shown, extra request). |
| In-flight prefetch: loader, then that request's result, no second request | 1 | Activation spec: advance before B's request resolves; loader shows; resolving the original request shows its prompt; tracker 2 was requested once. |
| Edit after prefetch: answer graded against the prefetched prompt | 1 | Activation spec: the server's prompt changes after prefetch; the card shows the prefetched choices and `RecallPromptController.answer` is called with the prefetched prompt ID. |
| Failed prefetch offers Retry and Self-review; Retry makes one foreground request | 1 | Existing `RecallPage.questionRecovery.spec.ts` "keeps a failed future prefetch local…" and Self-review cases, unchanged and passing. |
| Successful off-screen Retry is shown on return | 1 | `RecallPage.questionRecovery.spec.ts` returning case asserts the Retry's prompt is shown and no new request is made for that tracker. |
| First card loads in the foreground | 1 | Existing `RecallPromptCard.spec.ts` and `RecallPage.queueProgress.spec.ts` cases, unchanged and passing. |
| Guidance survives in its new homes and links resolve | 1 | Reading of the three destination sections and the retargeted link. |

Local proof command (form observed in the premise run):

```sh
env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend exec vitest run tests/recall tests/pages/RecallPage tests/notes/NoteMoreOptionsForm.removalLanding.spec.ts
```

The one-time removal sweep at acceptance is a reading, not a test: search the
product, tests and docs for `prepared` load status, `needsSelection`,
`recall-question-loading` and wording about selecting or requesting the
current prompt when the card is reached.

## Ordered slices

### 1. Show the prefetched prompt when the learner reaches its card

Type: Behavior
Status: done
Proof: the rewritten activation spec and the adjusted question-recovery case
fail before the change and pass after; the rest of the local proof command
stays green. Documentation reading as above.

Behavior: a recall session whose next multiple-choice tracker has a completed
or in-flight background prefetch → the learner advances to it → the card shows
that prefetch's prompt (after its loader, if still in flight), makes no other
request for that tracker, and answers submit against that prompt ID.

Rewrite `RecallPromptCard.activation.spec.ts` to the three examples above,
adjust the question-recovery returning case, then apply the composable and
card change from Current decisions. In the same slice, delete
`docs/recall-question-loading.md`, add the three destination sections and
retarget the retrospective link.

Accepted proof (slice 1, uncommitted candidate on `92aa3d067f`): the focused
command passed, 18 files and 93 tests, with `vue-tsc --noEmit` passing, after
the refactor pass; the rewritten activation cases and the adjusted
question-recovery case failed against the base production files first.
Acceptance sweep found no `prepared` load status, `needsSelection` or
`recall-question-loading` outside planning records. Source review
observations are in [source-review.md](source-review.md).

Learnings: `startRecallAttempt` stays because the spelling restart watcher
uses it. makeMe's auto-numbered IDs can collide with fixed prompt IDs in a
spec file, which makes Vue reuse a keyed `ContestableQuestion`; give fixed
prompts distinct IDs. The refactor pass shared one `recallPromptRequestsFor`
reader in `frontend/tests/helpers/recallDataTestSupport.ts`. Optional
follow-up from that pass: the "keeps a failed future prefetch local…" case
asserts a fixed call position and could use that reader.

## Execution complete

Product advice: no change. The web card now shows the prefetched prompt with
no display-time request, matching the story; no queued story's priority or
assumption is affected. Process review is deferred into wrap-up with the
intent coverage check, because this execution rewrote existing recall tests.
The trunk amendment at `0e54051423` moved "Question ownership and reuse" to
`docs/recall-results.md` and removed
`docs/question-generation-batch-operations.md`; the branch carries both.

## Plan review

Refinement not needed: one cohesive change with a single proof loop, and the
scratch run bounded its consumers to three test cases. This review identified
no remaining slice-specific concern.
