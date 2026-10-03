# DearDough Process Findings

Retained material shared-process findings, reviewed 2026-09-30. Only an explicit
queued follow-up is planned work; other entries are open and unqueued. A retained
released response is not proof of effectiveness. Unknown provenance stays unknown.
[Response status](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md).
Full pre-trim evidence: `8830c682704aac3bbb34bf9b1204da8feba042ca:DearDough.md`. Older narratives live in Git, not a second archive.

- Highest allocated local number: 199. Removed local codes are never reused.

## ODF-034 — CI observer started for a feature branch that this project's workflow never triggers on

Former local code: DD-017.

CI observation validates workflow existence without checking target-branch trigger eligibility, then presents an impossible branch run as merely pending.

Follow-up: Open, unqueued.

- Execution: slice-plans/108-publish-notebook-edits-faster / a6fcddacad; Tool: Claude Code; Model: claude-sonnet-5; Open Dough release: 0.3.13.
- Execution: SEED-031 story 1 / slice-plans/147-resolve-deleted-failure-reports; Timestamp: 2026-09-18, ~20:00–22:00 +08:00 (all four slice deliveries); Tool: Claude Code; Model: claude-sonnet-5; Open Dough release: 0.3.25.
- Earlier occurrence details: 1 additional recorded rows in `8830c682704aac3bbb34bf9b1204da8feba042ca:DearDough.md`; these are historical evidence, not new occurrences.

## ODF-059 — Delegation guidance has no protocol for a subagent that dies mid-edit from an infrastructure error, leaving a silent partial change

Former local code: DD-062.

A host-terminated refactor agent leaves changes without a report, requiring recovery from the actual diff and unresolved proof.

Follow-up: Open, unqueued.

- Execution: SEED-009 story 43 / slice-plans/132-rebaseline-existing-notebooks / 301184431f; Timestamp: unknown (task-notification received 2026-09-17, exact time not captured; the error stated a session-limit reset at 4:50pm Asia/Singapore); Tool: Claude Code; Model: claude-sonnet-5; Open Dough release: 0.3.24.

## ODF-083 — A prior execution left the shared main checkout on its own feature branch with uncommitted work, blocking the next plan's execution setup

Former local code: DD-064.

An execution switches the shared checkout to its own feature branch and leaves a large uncommitted change, blocking another execution's setup.

Follow-up: Open, unqueued.

- Execution: SEED-009 story 44 / slice-plans/137-retire-notebook-rebaseline-migration; Timestamp: 2026-09-17, unknown exact time (diagnosed and repaired before this execution's delivery commit at 20:10:08 +08:00); Tool: Claude Code; Model: claude-sonnet-5; Open Dough release: unknown.

## ODF-084 — A concurrent session deleted an active Story Branch execution worktree and branch while a delegated subagent was mid-slice

Former local code: DD-072.

An execution worktree and branch are removed during an active delegated task, forcing recovery while another execution uses shared integration.

Follow-up: Open, unqueued.

- Execution: SEED-030 story 2 / slice-plans/145-reset-notebook-git-history; Timestamp: 2026-09-18, ~17:06-17:20 +08:00 (between the plan 146 claim commit `c619aba43a` at 17:05:27 +08:00 and slice 1's delivery at 17:30 +08:00); Tool: Claude Code; Model: claude-opus-5; Open Dough release: unknown.

## ODF-069 — The CI observer's fixed discovery-poll bound reports lost coverage for revisions whose CI run exists and later succeeds

Former local code: DD-076.

Registered revisions are reported uncovered during discovery and their later real verdicts are missed in the observed execution.

Follow-up: Open, unqueued.

- Execution: SEED-035 story 1 / slice-plans/148-cohesive-accepted-web-folder-changes / ea903668bb; Timestamp: unknown; Tool: Claude Code; Model: claude-sonnet-5; Open Dough release: unknown.
- Execution: SEED-039 story 4 / slice-plans/041-faster-frontend-unit-tests / c9347a9ee3; Timestamp: 2026-09-26, completion check started ~09:45+08:00; Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38. - Evidence: `complete-revision` for fe990eff58 on observer /tmp/dough-ci-501/watch-UaGJbW returned `unresolvedReason: timeout` with 70dd09501d, 8df4a87262 and fe990eff58 all `undiscovered`; `gh run list --branch story/faster-frontend-unit-tests` shows `donut CI` success for 8df4a87262 (run 36208396723, created 01:25:55Z), 70dd09501d (36207506131) and c9347a9ee3 (36206320436); fe990eff58 touches only `.planning/**`, which the workflow ignores. - Observed effect: a ten-minute wait ended without a verdict although every applicable run had already succeeded; the coordinator confirmed green with `gh run list`.
- Earlier occurrence details: 7 additional recorded rows in `8830c682704aac3bbb34bf9b1204da8feba042ca:DearDough.md`; these are historical evidence, not new occurrences.

## ODF-110 — A readiness replay observed only the plan's named seam, not the rest of the slice's journey

Former local code: DD-109.
Former local code: DD-163.
Former local code: DD-168.
Former local code: DD-171.

A replay resolves the named readiness seam without exercising the rest of the slice's promised journey, leaving a later operation to force a scope stop.

Follow-up: queued, not resolved: [Observe a planning premise through the operation that consumes it](../open-dough/.planning/seeds/SEED-059-observe-planning-premise-consumers.md#observe-premise-consumers) — SEED-059#observe-premise-consumers.

- Execution: SEED-035 story 14 / slice-plans/025-convert-raw-notebooks-to-lfs / 071d0e0861; Timestamp: 2026-09-24, ~15:35+08:00 (replay and readiness record 47df9474c2), failure observed ~16:05+08:00 (slice 3 E2E); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.37. - Evidence: research prompt scoped to "pull" risks only; its report noted "the publish check went only as far as the pointer blob being committed"; slice 3 E2E then failed at the second `donut notebook publish` ("Attachment at <commit> must be a Git LFS pointer…", `cli/src/commands/notebook/notebookPublishLfsSelection.ts`); plan recorded the stop in 1e2ed84c35. - Observed effect: one human round-trip and a scope change (CLI change, option A) that preparation could have surfaced before Take. - Inference: when resolving a readiness concern by observation, replay the slice's full promised journey (here pull, then publish), not only the mechanism the concern names; the replay's own "not covered" list was the signal.
- Execution: SEED-059#story-6 / slice-plans/056-change-or-clear-reading-mark / 7c9935b2c2; Timestamp: 2026-09-29 (slice 2 implementation, between 17:53 and 18:12 +08:00); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.47. - Evidence: plan *Decisive premises* "E2E setup and steps exist … Confirmed by grep"; slice 2 implementation report ("My first green run failed on `no book block should be marked` (found 1)"); new step "book block {string} should not be marked in the book layout" in 5cab6e8827. - Observed effect: one failed EPUB feature run and a new step; the plan's slice 2 behavior text was corrected during delivery. Small cost. - Inference: the premise checked that a step exists, not that it holds for the chosen fixture path. Related to DD-162 (a grep premise that did not reach the changed path).
- Execution: SEED-059#story-5 / slice-plans/053-pdf-smooth-scroll-after-choosing-block / 5989892325; Timestamp: 2026-09-29T21:45+08:00 through 22:15:52+08:00 (slice 1 implementation to commit); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.47. - Evidence: slice 1 hand-backs stopping at "the panel element never appears" (`panelShownBecauseScrolledPastContent` required `successor.id !== currentBlockId`) and then at "`read-from-here` covers `book-reading-mark-as-read`" (both overlays `absolute left-0 right-0 bottom-0 z-20`); owner question answered "Stack the two"; fix in 5989892325 (`ReadingOverlayDock.vue`). - Observed effect: two implementation stops, one owner decision on a scope the story had deferred (panel position), and slice 1 took about 30 minutes against a ~5-minute target. - Inference: reading `blockAwaitingConfirmation` and the overlay classes for the story's own "scroll past without marking" key example would have shown both at planning. Related to DD-162 and DD-163 (premises that stopped short of the consuming step).
- Earlier occurrence details: 1 additional recorded rows in `8830c682704aac3bbb34bf9b1204da8feba042ca:DearDough.md`; these are historical evidence, not new occurrences.

## ODF-112 — The CI observer delivered no failure for failed story-branch runs, so later slices were built on a red branch

Former local code: DD-112.

An attached observer delivers no failure for registered failing story-branch revisions, allowing dependent slices to continue on a red branch.

Follow-up: Response delivered; verification remains open. No new implementation queued.

- Execution: SEED-035 story 19 / slice-plans/029-remove-raw-file-storage / 0284ea7f52; Timestamp: 2026-09-25, runs failed ~09:40–10:03+08:00, found ~10:50+08:00; Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38. - Evidence: runs 36083055193 (0d84006f7d) and 36084271448 (9ad0b9bf9f) failed "Backend Unit tests"; mailbox `/tmp/dough-ci-501/watch-7llTFe/events` held only sequence 1 (`CI_DISCOVERY_DELAYED` for 0284ea7f52); both deliveries had reported `observation.state: reused`. A later `CI_MONITOR_UNAVAILABLE` (network error on `gh run list`) arrived during the repair. - Observed effect: slices 3 and 4 were implemented, refactored and published on a failing branch for about an hour; the repair then had to cover three failed revisions. - Later evidence: at completion, `complete-revision` for aba0dc2509 returned `unresolvedReason: timeout` with the revision still `undiscovered`, while `gh run list` showed that run `completed success`. - Inference: cause unverified (discovery after the delay advisory, per-revision registration, or hook delivery); a manual `gh run list` check before each delegation would have caught it one slice later.

## ODF-113 — A failure was called pre-existing by comparing against a revision that already contained this execution's earlier slices

Former local code: DD-113.

A failed previous-slice tip is used to label a regression pre-existing and out of scope, although the execution introduced it earlier.

Follow-up: Open, unqueued.

- Execution: SEED-035 story 19 / slice-plans/029-remove-raw-file-storage / 0284ea7f52; Timestamp: 2026-09-25T10:44+08:00 (slice 4 return; commit e9cbbc6547); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38. - Evidence: slice 4 report ("same thing happens on the unchanged base commit `9ad0b9bf9f`"); CI runs green for 0284ea7f52 and main 9d2d091d0a; heap dump later traced the leak to slice 2's large LFS test payloads retained by `InMemoryNotebookAttachmentContent`. - Observed effect: the first stop report told the owner the failure was pre-existing and out of scope; the correction came only after the CI check. - Inference: "pre-existing" needs a baseline from before this execution's first change (claim revision or last green CI), not the prior slice.

## ODF-120 — A not-ready assessment whose only reason was a satisfied start condition blocked queued startup

Former local code: DD-114.

A dependency lands but its dependent story retains a not-ready assessment whose only reason was that unmet dependency.

Follow-up: Open, unqueued.

- Execution: SEED-035 story 17 / slice-plans/034-book-source-as-notebook-file / 36eb15caaa; Timestamp: 2026-09-25, ~15:43+08:00 (refusal, then readiness commit 741dbf31ba); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38. - Evidence: `read-state` reasons "Start condition unmet: reuses naming helpers and the startup-move shape from the picture move (slice-plans/033, SEED-035#story-5), not yet on main."; story 5 merged at 72021b8efe; start receipt `{"status":"source-refused","error":"published preparation is needs-reassessment"}`; readiness recorded and pushed as 741dbf31ba. - Observed effect: about six extra calls and one extra commit on main; an execution coordinator performed a preparation assessment. - Inference: when a start condition names another story, that story's wrap-up (or the start command) could re-check dependents whose only blocking reason it resolves.
- Execution: SEED-050#story-1 / `.planning/slice-plans/020-validate-changed-markdown-once/PLAN.md` / f8b186cc7f; Timestamp: 2026-09-27, ~15:40+08:00 (refusal, then readiness commit 0584473e28); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.42. - Evidence: recorded reason "Waits for SEED-009#story-48 (plan 019) to land…"; story-48 wrapped up at 825560fe9e without reassessing SEED-050#story-1; start receipt `{"status":"source-refused","error":"published preparation is not-ready"}`; coordinator re-checked starting facts, recorded `ready`, pushed 0584473e28 (one rejected push, rebased over a sibling Take). - Observed effect: about ten extra coordinator calls and a commit on main before the Take; the coordinator had to find the preparation procedure itself.

## ODF-121 — One transient GitHub TLS timeout ended CI observation for the rest of the execution

Former local code: DD-115.

A reported GitHub transport failure ends observation and leaves later revisions without notification coverage.

Follow-up: Open, unqueued.

- Execution: SEED-035 story 17 / slice-plans/034-book-source-as-notebook-file / 36eb15caaa; Timestamp: unknown (event delivered between slice 6 commit 2026-09-25 16:44:12 +0800 and slice 7 commit 2026-09-25 16:58:36 +0800); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38. - Evidence: hook context `{"type":"CI_MONITOR_UNAVAILABLE",…,"reason":"Command failed: gh run list … TLS handshake timeout"}` for observer /tmp/dough-ci-501/watch-WnDwf2; slice 7 receipt `observation.state: unobserved`. - Observed effect: lost coverage for db13a2d99c and 96c756d531; manual CI checks replaced notifications. - Inference: a bounded retry for a transient network error before declaring the observer unavailable would likely have kept coverage.

- Execution: SEED-062#story-1 / slice-plans/006-reify-property / 508d4909b5; Timestamp: unknown (delivered at the coordinator boundary around 2026-09-30T20:24+08:00, after e2ad90297e and before 93eb7fd31d); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.50. - Evidence: hook context `{"type":"CI_MONITOR_UNAVAILABLE","branch":"claude/reify-a-property","reason":"… gh run list … error connecting to api.github.com"}` for observer /tmp/dough-ci-501/watch-N2C0GM; the next `deliver` (ec90308084) returned `observation.state: attached, reused: false` with a new observer /tmp/dough-ci-501/watch-DvvAnv. - Observed effect: results for 508d4909b5..e2ad90297e were no longer observed; coverage returned only at the next publication. - Inference: same transient-network ending as the first occurrence; managed delivery's re-attach limited the gap to already-published revisions.

## ODF-106 — Two concurrent executions allocated the same slice-plan number from different bases

Former local code: DD-118.

Concurrent unpublished plan allocation produces two quick plans with the same numeric selector, making a number-only execution request ambiguous.

Follow-up: Open, unqueued.

- Execution: slice-plans/037-share-backend-test-context / c7ea84e3a7; Timestamp: 2026-09-25T23:24:51+08:00 (plan commit 0f8709dfc1); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38. - Evidence: base 5c8bb75741 (22:09:45+08:00) lists plans 007, 035, 036; main's db601a2e42 (23:06:25+08:00) had already added plan `037-fold-picture-attach-step-into-upload`; 0f8709dfc1 added plan `037-share-backend-test-context`. Number 116 and 117 of this log were likewise allocated on main after the base, so this entry uses 118. - Observed effect: DearDough rows and `.planning/test-optimization-candidates.md` ("plan 037 cut the suite…") refer to "037" for two different executions once both plans are deleted at wrap-up; the retrospective's correction plan had to reword the candidate record. - Inference: the same stale-base allocation applies to DD numbers in this log, so a merge can also produce duplicate finding codes. Whether the coordinator fetched `origin/main` before planning is not recorded.

## ODF-124 — The coordinator told parallel agents a guidance rule did not exist after searching only SKILL.md files

Former local code: DD-120.

A negative claim about guidance searches skill entry files but omits their references, then misdirects delegated work.

Follow-up: Open, unqueued.

- Execution: SEED-039 story 4 / slice-plans/041-faster-frontend-unit-tests / c9347a9ee3; Timestamp: 2026-09-26, between 08:50 and 09:08+08:00 (between slice 1 and slice 3 commits); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38. - Evidence: grep over `.agents/skills/*/SKILL.md` returned nothing; `refactor-checks.md:128` "Shorten or split every checked file exceeding **250 lines**"; follow-up refactors split `RecallPage.answering.spec.ts` (400 → 225 + 221) and shortened `RichMarkdownEditor.propertyEntry.spec.ts` (268 → 242). - Observed effect: two extra refactor agents (~125k subagent tokens) and one failed commit; a plan learning had to be rewritten. - Inference: a negative claim about guidance needs a search of the whole skill tree, references included, before it is broadcast to agents.

## ODF-125 — Interim "agent has not reported yet" notifications repeatedly woke the coordinator with nothing to decide

Former local code: DD-122.

Delegated background tests generate report-pending completion notifications that repeatedly wake a coordinator with no decision to make.

Follow-up: Open, unqueued.

- Execution: SEED-035#story-11 / slice-plans/007-dissolve-merge-folders-with-files / 1a8b7abff7; Timestamp: 2026-09-26T05:32:17Z–05:37:01Z (10 notifications during slice 8), plus 2026-09-26T05:55:06Z (slice 9); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.40. - Evidence: session `fe371aa9-…` task-notifications for "Implement slice 8 of plan 007", each with the note "stopped with background work of its own still running" and the result "This agent has not reported yet"; 11 coordinator turns, each reading about 235k–240k cached input tokens, output 19–37 tokens. - Observed effect: about 2.6M cache-read tokens went on status-only turns. The final reports and the execution were unaffected. - Inference: asking delegated implementers to run focused tests in the foreground, or having the coordinator stay idle on an interim notification, would avoid this. Qualified: the host notification behavior is outside the project's control.
- Execution: SEED-046#story-9 / `.planning/slice-plans/010-responses-carry-no-orm-internals/PLAN.md` at d305df9c23 / 9365a11c7d; Timestamp: unknown (2026-09-27, between 09:50 and 10:02+08:00, slice 1); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.42. - Evidence: coordinator conversation: one task-notification for "Implement plan 010 slice 1" with "stopped with background work of its own still running" / "This agent has not reported yet", followed by the real hand-back. - Observed effect: one status-only coordinator turn; delivery unaffected.
- Earlier occurrence details: 1 additional recorded rows in `8830c682704aac3bbb34bf9b1204da8feba042ca:DearDough.md`; these are historical evidence, not new occurrences.

## ODF-155 — An implementer proved fails-first by putting HEAD versions back in the shared execution checkout

Former local code: DD-141.

An implementer temporarily replaces its shared-checkout files with HEAD versions to prove tests red, despite a supplied separate-baseline rule.

Follow-up: Open, unqueued.

- Execution: SEED-050#story-2 / `d3c9b4814d:.planning/slice-plans/021-relationship-notes-accepted-in-one-change/PLAN.md` / f8087d5845; Timestamp: unknown (2026-09-27, slice 5 work before commit 06ff81b916); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: unknown. - Evidence: slice 5 implementer return ("I put the HEAD versions of the resolver and the Vue file back temporarily ... restored my versions from temp copies"); delegation prompt's "never stash, reset, clean, checkout paths"; dough-execute-plan/references/delegation.md ownership bullet. - Observed effect: no damage; the coordinator checked the working tree afterwards and it held the intended diff. No other writer was active in the checkout at that time. - Inference: an implementer that writes code before its test finds the in-place restore the cheapest fails-first route; with a concurrent writer it could clobber or capture sibling work. Writing the test first, or a temporary worktree at HEAD, avoids it. Qualified: one occurrence; coordinator saw only the return.
- Execution: SEED-055#story-1 / `2509421236:.planning/slice-plans/012-public-api-cleanup/PLAN.md` / be9f44cd11; Timestamp: unknown (2026-09-29, between 08:35 and 08:55+08:00, slices 2–4); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.46. - Evidence: coordinator conversation: one or two task-notifications per slice for the slice 2, 3 and 4 implementers with "stopped with background work of its own still running" / "This agent has not reported yet", each followed by the real hand-back after the full backend suite finished. - Observed effect: four status-only coordinator turns; delivery unaffected.
- Earlier occurrence details: 1 additional recorded rows in `8830c682704aac3bbb34bf9b1204da8feba042ca:DearDough.md`; these are historical evidence, not new occurrences.

## ODF-074 — A plan said the changed script had no test, and nobody searched for one before delivery, so CI caught the stale test

Former local code: DD-126.
Former local code: DD-130.
Former local code: DD-134.
Former local code: DD-139.
Former local code: DD-147.
Former local code: DD-162.
Former local code: DD-167.

Concrete only-caller and host-state premises enter a plan without inspection, forcing a changed decision or stopped implementation when checked.

Follow-up: queued, not resolved: [Observe a planning premise through the operation that consumes it](../open-dough/.planning/seeds/SEED-059-observe-planning-premise-consumers.md#observe-premise-consumers) — SEED-059#observe-premise-consumers.

- Execution: SEED-043 story 1 / slice-plans/045-commit-gate-checks-committed-content / 574d61b52c; Timestamp: 2026-09-26T16:06:02+08:00 (CI step failure); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.40. - Evidence: plan "Current decisions" before 120753a097; CI run 36228685291 job "Other Unit Tests" failed `quality_changed.test` ("shared biome config selects every affected component": expected `pnpm frontend:lint`, got the install line after `ln` failed); repair 120753a097 updated and extended the test. - Observed effect: one red story-branch CI run, a stash/repair/restore cycle around slice 2, and two extra agents (repair ~49k and refactor ~48k subagent tokens). - Inference: a negative claim that code has no test needs a search of the test tree (here `grep -rl quality_changed scripts/test`) at planning or delegation; naming the stack skill for `scripts/` in the delegation would likely have surfaced it.
- Execution: SEED-059#story-3 / slice-plans/051-pdf-layout-from-bookmarks / 946e2a70e3; Timestamp: 2026-09-29 (slice 1); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.46. - Evidence: plan *Decisive premises* row "Backend tests attach fake PDF bytes" (`grep -rn "0x25, 0x50"`); slice 1 refactor report item 2 (`BooksControllerTest`, `NotebookGitWebAttachmentDeleteControllerTest` back to HEAD). - Observed effect: two files changed and reverted within one slice; small cost. - Inference: the premise matched a symptom (fake bytes) rather than the path (attach callers). Related to the planning-premise family (DD-128, DD-137).
- Execution: SEED-059#story-14 / `.planning/slice-plans/055-epub-resume-tests-and-rendered-view/PLAN.md` / 09ca632dea; Timestamp: 2026-09-29 (slice 2 first attempt; exact time unknown); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.46. - Evidence: slice 2 Decision paragraph in plan 055 at 0e9048812c; `epubSpinePathMatches` suffix rule in `frontend/src/lib/book-reading/epubHrefMatch.ts`. - Observed effect: one implementation-agent round of about 57k tokens returned no change, and the owner was asked one question. The recommended option was accepted and no work was lost. - Inference: reading the two matching rules while planning the correction (a few minutes) would have found the difference and taken the decision to the owner before the plan. Qualified: one occurrence.
- Earlier occurrence details: 4 additional recorded rows in `8830c682704aac3bbb34bf9b1204da8feba042ca:DearDough.md`; these are historical evidence, not new occurrences.

## ODF-147 — An implementer reasoned that a new test would fail instead of running it red, and one of its tests could not fail

Former local code: DD-127.

An agent reasons that new tests would fail instead of observing them against pre-change behavior; round-trip or wrong-boundary tests can pass without the fix.

Follow-up: Open, unqueued.

- Execution: SEED-035 story 24 / slice-plans/046-moves-to-another-notebook-reach-git / bc9a0ab229; Timestamp: 2026-09-26, ~17:30+08:00 (coordinator red check before slice 3 refactor); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.40. - Evidence: slice 3 return ("I did not run the new tests red first"); red run with `NotebookFolderController` and `FolderRelocationService` reset to HEAD: `movingAFolderBackAsUndoRestoresBothNotebooks` passed; `NotebookGitWebFolderCrossNotebookMoveControllerTest` in 3d5c1bf394 asserts Engineering's history grew by two commits. - Observed effect: two extra focused test runs by the coordinator; the delivered undo test now fails without the fix. - Inference: a round-trip test whose end state equals its start state passes when nothing happens; "red first if practical" in the delegation let the agent substitute reasoning for observation. Slice 2's implementer ran red and its undo test was meaningful.
- Execution: SEED-046#story-5 / `.planning/slice-plans/006-web-edit-changes-only-edit/PLAN.md` at 2a8fd844b4 / 5f71d4d237; Timestamp: between 2026-09-26T23:37:35+08:00 and 2026-09-26T23:43:24+08:00 (slice 5 quoted-rename test, before commit 2f2cd179cf) and between 2026-09-26T23:53:42+08:00 and 2026-09-27T00:00:58+08:00 (slice 8 flow-list test, before commit 06394ff704); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.41. - Evidence: the coordinator itself added `quotes a renamed key that YAML needs quoted` and `removeWikiLinksFromLeadingFrontmatterProperties_cutsOnlyTheEmptiedFlowListItem` and accepted them by reasoning ("without the fix it would emit `a: b: demo`") after only a green run; the slice implementers' reports also named no red runs. - Observed effect: both tests are plausibly meaningful, but neither was observed failing. - Inference: this time the coordinator, not an implementer, substituted reasoning for the red run; the delegation prompts again did not ask for one.
- Earlier occurrence details: 1 additional recorded rows in `8830c682704aac3bbb34bf9b1204da8feba042ca:DearDough.md`; these are historical evidence, not new occurrences.

## ODF-148 — Refined key examples promised link-rewrite outcomes that the existing rewrite rules do not produce

Former local code: DD-128.

Key examples promise new rewrite results while excluding changes to the current rewrite rules; execution drops a promise without an owner decision.

Follow-up: Open, unqueued.

- Execution: SEED-035 story 25 / `.planning/slice-plans/047-link-rewrites-in-other-notebooks-reach-git/PLAN.md` at 0ef2828d32 / 9861bc80c2; Timestamp: 2026-09-26, ~18:27+08:00 (slice 3 commit f63760e183 records the decision); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.41. - Evidence: refinement/plan 27673389c0 (seed key examples 2 and 4; plan slice 3 "Folder rename, move and dissolve"); `backend/src/main/java/com/odde/donut/algorithms/PortablePath.java` `withRenamedFolder` (early return when `notebookQualifier.isPresent()`); plan "Current decisions" in f63760e183; test expectations in `NotebookGitWebLinkingNotebookControllerTest` (`[[Science:/Force]]`, `[[Physics:Force|Science:Force]]`). - Observed effect: one owner-visible promise was dropped during execution by a plan note, without an owner decision or a seed edit. No extra slice was needed. The seed still promises the folder-rename case at closure. - Inference: when a story says an existing rule stays unchanged, check each key example's expected text against that rule (its code or tests) at refinement. Also, a promise dropped because it conflicts with an exclusion should reach the owner at completion, not only the plan. Qualified: the coordinator summary is the only process record, so how long the discovery took is unknown.

## ODF-149 — The "stays editable" boundary examples were all one-line bodies, so a check that refuses wrapped text shipped

Former local code: DD-129.

Minimal one-line editable examples miss ordinary hard-wrapped content, letting a style-only refusal ship against the story promise.

Follow-up: Open, unqueued.

- Execution: SEED-046 story 10 / `.planning/slice-plans/005-rich-editor-keeps-content/PLAN.md` / c5338213d0; Timestamp: 2026-09-26T22:19:00+08:00 (retrospective probe); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.41. - Evidence: plan "Current decisions" (planning, rendered-HTML comparison); `RichMarkdownEditor.bodyItCannotKeep.spec.ts` editable cases at 0e36045508; `richEditorKeepsBody.ts` normalizes only `/>\s*\n\s*</`; correction plan `008-hard-wrapped-note-stays-editable`. - Observed effect: a regression against the story's own style-only promise reached the published story branch; a correction story and plan were needed before integration. - Inference: when a story's promise is "ordinary content keeps working", its boundary examples need a realistic sample of that content (here, a hard-wrapped paragraph such as any file in this repo), not only minimal constructs. The seed's value note already asked how many real notes the editor cannot carry.

## ODF-139 — The coordinator accepted an implementer's reported gap as out of scope without checking the story, and the example test pinned the defect

Former local code: DD-132.

An implementer reports a loss that contradicts the story goal; acceptance labels it out of scope and preserves a test that pins it.

Follow-up: queued, not resolved: [Check reported gaps against the story before accepting a slice](../open-dough/.planning/seeds/SEED-058-accept-reported-story-gaps.md#accept-reported-story-gaps) — SEED-058#accept-reported-story-gaps.

- Execution: SEED-046#story-5 / `.planning/slice-plans/006-web-edit-changes-only-edit/PLAN.md` at 2a8fd844b4 / 5f71d4d237; Timestamp: 2026-09-26, before 23:25:41+08:00 (slice 2 acceptance; commit 61d400007d); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.41. - Evidence: slice 2 implementer return ("Gaps: Example 1's trailing newline is lost"); plan 006 Learnings "Slice 2" ("outside this story's promises"); retrospective probe output `"My First\n\nLast"`; correction `4ba13d691c:.planning/slice-plans/009-rich-edit-keeps-final-newline/PLAN.md` (SEED-046#story-14). - Observed effect: a defect against the story's own goal reached the published story branch; a correction story and plan are needed before integration. - Inference: a reported gap should be checked against the story's goal and exclusions list before it is filed as out of scope; the fix at slice 2 would have been a few lines in the same function. Qualified: as in DD-129, the key example was a file whose edited line was also its last line, which hid the effect.

## ODF-100 — Agents reported vue-tsc's exit code from a pipe into `tail`, so the coordinator had to rerun the typecheck

Former local code: DD-135.

A formatter piped through tail returns the final pipeline stage's success, allowing subsequent delivery steps after formatter failure.

Follow-up: Open, unqueued.

- Execution: SEED-033#story-2 / `9c8aca9bbd:.planning/slice-plans/012-read-note-context/PLAN.md` / 99aa915e22; Timestamp: 2026-09-27T09:52:41+08:00 (slice 1 acceptance, before commit 99aa915e22) and 2026-09-27T10:16:30+08:00 (slice 5 refactor acceptance, before commit 3712c94363); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.42. - Evidence: slice 1 implementer return ("The exit code I captured was the pipe's final `tail`, not vue-tsc's own"); slice 5 refactor return (same remark); coordinator reruns `vue-tsc --noEmit >/dev/null 2>&1; echo $?` → 0 both times. Later delegation prompts that said "report its real exit code (don't pipe it into tail)" got a correct exit code. - Observed effect: two extra typecheck runs, about a minute each; no wrong result was accepted. - Inference: a delegated command whose pass/fail matters should be given with its exit-code capture spelled out, since agents tend to trim long output with `tail`. Qualified: small cost, and the agents reported the problem honestly.
- Execution: SEED-056#story-1 / `6e23621c23:.planning/slice-plans/014-recall-half-day-refresh/PLAN.md` / 4b39b579e0; Timestamp: 2026-09-29T11:08:48+08:00 (slice 1 commit after acceptance); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.46. - Evidence: slice 1 implementer and refactor agents ran `vue-tsc --noEmit 2>&1 | tail -15; echo EXIT $?` / `| tail -5` and reported the typecheck clean; CI run 36515871965 (job 109237884287, `pnpm -C frontend build`) failed on `tests/pages/RecallPage.dueQueue.spec.ts(71,5): error TS2322`; fixed in 171e696e06. The slice 2 agent then wrongly concluded a standalone `vue-tsc --noEmit` misses test files. - Observed effect: unlike earlier rows, a wrong result was accepted: one failed published CI job and one repair; the delegation prompt did not spell out exit-code capture. - Inference: the coordinator should require an unpiped exit code (or reuse `pnpm -C frontend build`) in every delegation that asks for the typecheck.
- Earlier occurrence details: 2 additional recorded rows in `8830c682704aac3bbb34bf9b1204da8feba042ca:DearDough.md`; these are historical evidence, not new occurrences.

## ODF-150 — An implementer's slice proof ran only the specs it chose, missing consumers of the store method it changed

Former local code: DD-136.

Proof selection follows edited store areas rather than the changed operation’s whole caller flow, missing a consumer’s failure scenario.

Follow-up: Open, unqueued.

- Execution: SEED-047#story-1 / `edfd7ba92a:.planning/slice-plans/014-continue-to-neighboring-note-after-deletion/PLAN.md` / a41b1e0507; Timestamp: unknown (before slice 1 commit 2026-09-27T10:59:16+08:00); Tool: Claude Code; Open Dough release: 0.3.42 (VERSION in the execution checkout). - Evidence: coordinator summary to the retrospective (subagent transcripts not supplied): coordinator consumer check found 2 failing tests in `NoteMoreOptionsForm.trashNote.spec.ts`; fixed by an empty listing mock in `tests/notes/noteMoreOptionsTrashTestSupport.ts` (in a41b1e0507), then `tests/notes tests/store tests/toolbars` 353/353. - Observed effect: one coordinator repair before commit; no defect shipped. - Inference: a `grep` for callers of the changed method across `tests/` when choosing slice proof would have included the spec.
- Execution: SEED-059#story-16 / `dfec19ca03:.planning/slice-plans/058-current-block-same-in-pdf-and-epub/PLAN.md` / d3eec9db16; Timestamp: 2026-09-30T00:01:40Z (CI job log time of the failure); Tool: Claude Code; Model: claude-sonnet-5-5; Open Dough release: unknown. - Evidence: slice 2 removed the 40-point landing padding; its delegation named `book_browsing.feature` and `reading_record.feature`, and the report ran only those. CI run 36647846005 attempt 1 failed `phone_reading.feature` "Choosing a book block closes the book layout and moves the book there" (`expected ... to contain '2 /'`); the slice 3 agent ran `phone_reading` (7/7 locally at 390*844); the repair agent reproduced the failure only at 390*900 and repaired in 5206b08370. - Observed effect: one failed published E2E job, a paused slice 3 (stash, repair agent about 10 min, restore) and one test repair. - Inference: `phone_reading.feature` also chooses PDF blocks, so a search of `e2e_test/features/book_reading/` for scenarios that choose a block would have listed it. Same root cause as the entries above (proof chosen by edited area); the actor was the coordinator's delegation. The failure itself needed a taller-than-stock window, so local runs could not have shown it.
- Earlier occurrence details: 2 additional recorded rows in `8830c682704aac3bbb34bf9b1204da8feba042ca:DearDough.md`; these are historical evidence, not new occurrences.
- Execution: SEED-063#story-1 / slice-plans/007-track-property-values-separately / 8eaaf2b720; Timestamp: 2026-09-30T21:39:52+08:00 (slice 8 commit eead7350eb; failure reported after publication); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.50. - Evidence: slice 8 changed reduce from writing a suffixed `key 2` to appending a value; its delegation and plan named only backend tests (`RelationControllerReduce*`, `*Relation*`, ...). CI run 36723317159 attempt 1 failed `e2e_test/features/relationships/relationship_edit_and_remove.feature` "Reducing to source property uses a suffixed key when the property already exists" (expected the list content to include `a part of 2: '[[Mars]]'`); repaired in f07ff79425. - Observed effect: one failed published E2E job; slice 9 paused (repair stash, repair agent, refactor agent, restore). - Inference: a search of `e2e_test/features/` for the retired behavior (`suffixed`, `a part of 2`) when planning or delegating slice 8 would have listed the scenario; the plan's "Tests to extend" list covered unit and controller tests only.

## ODF-152 — The file-size rule conflicted with an approved staged simplification and mechanical callers

Former local code: DD-138.

An absolute changed-file size check conflicts with an approved staged decomposition and mechanical edits to pre-existing oversized callers.

Follow-up: Open, unqueued.

- Execution: SEED-049#story-1 / slice-plans/016-note-store-architecture / 3821dbd9c79c7ab25e68cd1ff96aa44061d6544f; Timestamp: unknown (2026-09-27, refactor1 and refactor6 handoffs); Tool: Codex; Open Dough release: 0.3.42. - Evidence: chat 01a0e0fe-e37c-7512-a6b3-b8d4c1318365, refactor1 questioned the 384-line intermediate store before planned slices4–6; refactor6 questioned useWikidataPropertyDialog after mechanical migration (300 lines at its base, then smaller). Rule: dough-post-change-refactor/references/refactor-checks.md, File size. Source: SEED-049 owner-approved single command/undo split; slice7 owns store size proof. - Observed effect: two applicability exchanges; no extra split was made. Final noteStore is 245 lines, noteUndo167, requests228, cache45. The unrelated Wikidata workflow was preserved. - Inference: clarify how the numeric check composes with approved intermediate states and the skill's requirement that a refactor address an introduced, exposed, or aggravated issue. Unlike ODF-124, the rule was found and acknowledged here.

## ODF-187 — The plan prescribed observations that execution had to drop: an absence check for removed UI and a case that could never fail

Former local code: DD-142.

Planning prescribes an absence assertion forbidden by removal policy and a scrolling case that passes regardless of the changed behavior.

Follow-up: Open, unqueued.

- Execution: SEED-043#story-1 / `62e33981f0:.planning/slice-plans/005-sidebar-full-row-reveal/PLAN.md` / 56505b78dd; Timestamp: 2026-09-28T11:16:07+08:00 (slice 1 commit) and between 2026-09-28T11:32:13+08:00 and 2026-09-28T11:37:08+08:00 (slice 3); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.44. - Evidence: plan at 791011100b, slices 1 and 3 Behavior bullets; slice 1 refactor return (removed the absence assertion); slice 3 implementer return ("The short-folder test passed before the change"); slice 3 refactor return ("cannot fail as written … `scrollTop` is always 0"); coordinator removed it before 68999d3505, recorded in the plan. - Observed effect: one assertion written then removed, one test written, run, and removed; two plan-text corrections during delivery. No behavior defect. - Inference: a planning check that each planned observation can fail on the pre-change code, and does not assert a removed feature's absence, would have caught both. Qualified: one execution; related to ODF-147 (an implementer's test that could not fail) but here the plan prescribed it and the implementer ran it red.

## ODF-189 — A four-line repository tip-over forced extracting an unrelated assimilation query block

Former local code: DD-144.

A small addition crosses the numeric file-size ceiling and triggers relocation and re-proof of a substantial previously untouched responsibility.

Follow-up: Open, unqueued.

- Execution: SEED-053#story-1 / `12c0f629ac:.planning/slice-plans/007-file-page-references/PLAN.md` / bc870053e8; Timestamp: 2026-09-28T12:37:00+08:00 through 2026-09-28T12:40:26+08:00 (Slice 2 refactor through delivery); Tool: Cursor; Model: gemini-3.8-flash; Open Dough release: 0.3.45. - Evidence: refactor transcript `ff698959-2f7d-4392-a127-0f3f0adcc8f8/subagents/2b60056d-ca4d-4cae-8a47-c79402587e9f` (decision pass: File size 254; learning "Slice 2's candidate query pushed NoteRepository over 250"); commit `bc870053` adds `NoteAssimilationQueries.java` and shrinks `NoteRepository.java`. Pre-Slice-2 `NoteRepository` at `c86eb7eba0` was 244 lines. - Observed effect: ~8 minutes of refactor time and an assimilation-focused re-proof (`AssimilationControllerTests`) for a tip-over caused by one new query. - Inference: the hard 250-line ceiling can force relocating a large untouched block when a small addition crosses it. Related in theme to ODF-152 (numeric check applicability), but here the agent performed the split rather than escalating a staged-simplification conflict.
- Execution: SEED-059#story-6 / slice-plans/056-change-or-clear-reading-mark / 7c9935b2c2; Timestamp: 2026-09-29T17:53+08:00 through 2026-09-29T18:12+08:00 (slice 2 implementation through refactor and delivery); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.47. - Evidence: `7c9935b2c2:backend/src/main/java/com/odde/donut/controllers/NotebookBooksController.java` has 248 lines; the slice 2 DELETE endpoint added 15. The slice 2 refactor report ("pushed `NotebookBooksController.java` to 263 lines") split out `NotebookBookReadingController` (124 lines) and changed five controller test files. Commit 5cab6e8827 carries the repo's only `@Tag(name = "notebook-books-controller")`, so the generated SDK class stays unchanged. - Observed effect: the refactor pass took about 25 minutes, against about 12 for the slice's implementation, and needed a wider backend re-proof. The seam it chose, the user's reading progress versus the book's attach and structure, is domain-meaningful. - Inference: same tip-over pattern; here the split landed on a real seam but introduced a new convention (a shared OpenAPI tag across two controllers) to avoid touching the frontend.
- Execution: SEED-063#story-1 / slice-plans/007-track-property-values-separately / 8eaaf2b720; Timestamp: 2026-09-30T20:44:32+08:00 (slice 2b commit 178ddce040); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.50. - Evidence: slice 2b added one parameter to `MemoryTracker.buildMemoryTrackerForProperty`, taking `MemoryTracker.java` from 256 to 258 lines (already over 250 before the slice); the refactor pass moved the unrelated grade-scheduling step `scheduleNextRecallFromStability` into `MemoryTrackerRecallDue` and reran `*MemoryTracker*`, `RecallsControllerTests` and `*Fsrs*` (174 tests). In slice 7, `MemoryTrackerService` at 249 lines led the implementer to move `updatePropertyKey` into a new `PropertyMemoryTrackerService`. - Observed effect: an unrelated recall-timing move and wider re-proof in slice 2b; slice 7's move landed on a real seam that slice 8 then reused for reduce re-homing. - Inference: same tip-over pattern; one of the two moves was unrelated to the change.

- Execution: SEED-064#story-6 / `f347df3d514feccaeaff8dbce062bce1992e0cff:.planning/slice-plans/007-add-property-draft-row/PLAN.md` / c05b83ea2b8a5ed3b0bd1534d720fcd08f8c9539; Timestamp: unknown (2026-10-01, slice 3 and 4 refactor handoffs before 0470d6be2d and cb7c47b618); Tool: Codex; Open Dough release: 0.3.52 (unchanged VERSION in the established execution checkout). - Evidence: coordinator conversation, refactor3 reported propertyEntry at 271 lines and moved the unchanged rename/body-refresh guard into propertyRenameGuard.spec.ts; refactor4 reported notePropertyLocationMethods at 281 lines and extracted layout assertions into notePropertyLayoutMethods.ts. Commits 0470d6be2d and cb7c47b618 contain those moves; dough-post-change-refactor/references/refactor-checks.md requires every changed file to fit 250 lines. - Observed effect: two responsibility extractions plus replacement mounted and layout E2E proof, each refactor reporting about 3 minutes. - Inference: same numeric tip-over pattern on test support; the seams were cohesive, but proof relocation added work beyond the small Cancel and touch-height outcomes. Consider anticipating file-size capacity during slice planning; no product defect or instruction change is implied.

## ODF-190 — The plan prescribed production observations whose access route or log source did not exist, and whose results could not change the approach

Former local code: DD-145.

Planning names an observation route or log source that is not available to the executing project, causing owner probing or loss of the intended proof.

Follow-up: Open, unqueued.

- Execution: SEED-051#story-1 / `f35fa810f1:.planning/slice-plans/009-retire-note-embeddings/PLAN.md` / 5003fbecc8; Timestamp: 2026-09-28T05:36:39Z–06:03:39Z (slice 1/3) and 2026-09-28T07:17:43Z–07:18:10Z (slice 10); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.45. - Evidence: four auto-mode denials while seeking a DB route (credential lookup, SSH to the app VM, probe edit to root, bucket IAM); owner: "But this wasn't needed uh, previously. Um, or can we skip this?"; coordinator's covering reasoning and skip recorded in d111968b62; slice 10 `gcloud logging read` found no Flyway lines, so sustained health became the D/P evidence (8e03ac5f5f). - Observed effect: about 27 minutes of owner-attended probing ended in skipping slice 1's SQL part and dropping slice 3; slice 10's named proof was replaced during delivery. No product defect. - Inference: planning could have asked, for each production observation, whether any result would change the approach, and whether the access route and log source exist (both checkable cheaply once `gcloud` auth worked). Related to DD-142 (prescribed observations dropped in execution), but here the cost was production access and owner time. Qualified: one execution; planning-time `gcloud` auth had failed.
- Execution: SEED-059#story-5 / slice-plans/053-pdf-smooth-scroll-after-choosing-block / 5989892325; Timestamp: 2026-09-29T21:45+08:00 through 22:10+08:00 (slice 1 attempts); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.47. - Evidence: plan 053's proof row "Manual observation … Chromium DevTools protocol or Playwright `mouse.wheel`" on the dev stack; the repo has no Playwright/Puppeteer, `.agents/agent-map.md` says linked worktrees refuse the persistent Development stack, and a throwaway Cypress spec sending CDP `mouseWheel` reached the DOM but scrolled 0 px in headless Electron and Chrome. - Observed effect: about 15 minutes of implementer time; the story's key example (every wheel step moves down) was delivered without its real-wheel observation. - Inference: same pattern in a local setting: the observation route could have been checked at planning from the agent map and `package.json`.

## ODF-192 — A two-hour UAT stopped at 56 minutes while cheap coverage gaps stayed open

Former local code: DD-156.

Exploration stops below its budget while cheap same-setup gaps remain, and the coordinator supplies an unsupported explanation that more time cannot help.

Follow-up: Open, unqueued.

- Execution: SEED-054#story-1 / `4f2f230505:.planning/slice-plans/011-book-reading-uat/PLAN.md` / 681768a71b; Timestamp: 2026-09-29T08:36+08:00 (second exploration part ends); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.46. - Evidence: seed `## UAT Findings` time list and `### Coverage gaps`; plan learnings for the two exploration parts; the coordinator's synthesis prompt asked for "the remaining gaps need other browsers, touch devices, or book sizes rather than more time". - Observed effect: 64 budget minutes unused; the size at which AI reorganization fails (defect 10) stays unknown. - Inference: exploration prompts could say to spend leftover budget on the cheapest open gaps before stopping, and the coordinator should not pre-write the report's justification. Qualified: one execution; the story calls the budget a limit, not a target.

## ODF-195 — The file-size check split an untouched block in one slice of an execution and was waived in a later slice

Former local code: DD-160.

Two refactor passes within one story treat barely touched, already oversized files differently, forcing an unrelated extraction in one and retaining another.

Follow-up: Open, unqueued.

- Execution: SEED-059#story-2 / slice-plans/050-read-a-book-on-a-phone / d4a47402af; Timestamp: 2026-09-29T12:00+08:00 through 2026-09-29T12:40+08:00 (slice 1 and slice 3 refactor passes); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.46. - Evidence: slice 1 refactor report ("`BookReadingBookLayout.vue` is in the diff and was 354 lines, over the 250-line limit"; new `useBookLayoutBlockPointerDrag.ts`, commit d4a47402af); slice 3 refactor report ("`BookReadingContent.vue` is 453 lines … It was 474 before this change … I left it for the owner to decide"). Pre-change size: `1e19f8224a:frontend/src/components/book-reading/BookReadingBookLayout.vue` has 354 lines. - Observed effect: about 15 minutes of refactor time in slice 1, plus a desktop re-proof (`reorganize_layout.feature`, `book_browsing.feature`) for code the story did not touch. Slice 3 took the other path, with no extraction. - Inference: `refactor-checks.md` "File size" does not say whether a file that was already over the limit, and that a slice barely touches, must be split. Agents resolve this differently, and the time cost follows whichever reading they pick. Related to DD-144 (a small addition tipping a file over the limit) and ODF-152; here the file was over the limit before the change.

## ODF-196 — An implementer reshaped a test fixture until the new scenario passed, and reported that no product change was needed

Former local code: DD-164.

An implementer moves a fixture into an already-supported shape, turns the test green and reports no product change despite the story's real example still failing.

Follow-up: queued, not resolved: [Check reported gaps against the story before accepting a slice](../open-dough/.planning/seeds/SEED-058-accept-reported-story-gaps.md#accept-reported-story-gaps) — SEED-058#accept-reported-story-gaps.

- Execution: SEED-059#story-1 / `e733844d01:.planning/slice-plans/049-epub-land-and-track-chosen-place/PLAN.md` / 485ed2eb49; Timestamp: unknown (2026-09-29, slice 4); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.46. - Evidence: slice 4 hand-back listing the moved cover and the gap "a cover in a spine file that no entry targets gets no block"; the coordinator's check of the real Alice EPUB (`wrap0000.xhtml` first in the spine, not targeted); the corrected slice with an extractor change in b2faf56d14. - Observed effect: one extra implementation round; without the check, the story's key example would have been reported as met while failing on the real book. - Inference: fixture changes that turn a failing scenario green should be checked against the story's real example, not only against the scenario. The implementer did name the gap, which made the check possible.

## ODF-197 — Plan edits by text replacement silently did nothing, and five slices' learnings never reached the plan

Former local code: DD-166.

Anchor replacements silently match nothing and chained later edits therefore fail to persist five slices' learnings in the resume record.

Follow-up: Open, unqueued.

- Execution: SEED-059#story-1 / `e733844d01:.planning/slice-plans/049-epub-land-and-track-chosen-place/PLAN.md` / 485ed2eb49; Timestamp: unknown (2026-09-29, from the first slice 2 refinement onward); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.46. - Evidence: `git show <commit>:…/PLAN.md | grep -c '^- \*\*'` was 3 at every commit b1746859a4..26c34d6649; the retrospective's outcome review found the missing learnings; they were restored from the conversation in the completion commit. - Observed effect: during execution the plan did not carry the slice 2 attempts, the CI repair, or the slice 3–6 causes, so a resumed execution would have lost them. No product effect. - Inference: asserting that each anchor is present before replacing would have stopped the first failed edit.

## ODF-144 — The CI stop hook re-announced an already-stopped lost observer at every coordinator stop

Former local code: DD-169.

The Claude Stop hook repeats an already-recorded lost-worker notice without acknowledgement, blocking every later turn end.

Follow-up: Response delivered; verification remains open. No new implementation queued.

- Execution: SEED-059#story-17 / slice-plans/059-reopen-epub-at-exact-paragraph / 946dccd30f; Timestamp: 2026-09-29T12:19:30Z–12:23:58Z (still repeating when the retrospective started); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.47. - Evidence: coordinator session `9516bae0-…`: 51 Stop-hook messages naming `/tmp/dough-ci-501/watch-MvVUKq`, the first before and the rest after the 12:19:47Z `ci-mailbox.mjs stop`; 49 coordinator turns answered "Handled; waiting". Those turns read about 7.7M cache-read tokens in total. The coordinator also filed host feedback about the loop. - Observed effect: token and turn waste only. Delivery, proof, and the later observer for b47d6eb4c8 were unaffected. - Inference: the hook's binding to the lost mailbox survives `stop`, so `lostWorkerMessage` (`ci-host-hook.mjs`) fires again at every stop until a new delivery rebinds. Qualified: the coordinator inferred this from reading the hook; there was no controlled check.
- Execution: SEED-059#story-19 / `.planning/slice-plans/060-designed-structure-for-reading-view/PLAN.md` / 821f5d617b; Timestamp: unknown (2026-09-29, between slice 3's publication a8cd47911c and slice 4's 16afb85941); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.47. - Evidence: mailbox `/tmp/dough-ci-501/watch-62k1ov`, killed by a full disk; after `ci-mailbox.mjs stop` returned `stopped`/`lost`, and after managed delivery attached a new observer, the Stop hook still returned `decision: block` with the lost-worker notice. - Observed effect: two blocked turn ends and the notice on every tool call; the coordinator deleted this session's binding file for the stopped mailbox to continue. - Inference: a second session confirms the binding to a lost mailbox survives both `stop` and a new observer's attachment.

## ODF-198 — A slice's premise that one code swap fixes a UAT defect was not tested first, and its spec passed before the change

Former local code: DD-173.

A slice assumes one code swap fixes a reported defect, but its new spec already passes and the original symptom remains unexplained.

Follow-up: queued, not resolved: [Observe a planning premise through the operation that consumes it](../open-dough/.planning/seeds/SEED-059-observe-planning-premise-consumers.md#observe-premise-consumers) — SEED-059#observe-premise-consumers.

- Execution: SEED-059#story-16 / `dfec19ca03:.planning/slice-plans/058-current-block-same-in-pdf-and-epub/PLAN.md` / f9a6f4a416; Timestamp: 2026-09-30 (slice 5); Tool: Claude Code; Model: claude-sonnet-5-5; Open Dough release: unknown. - Evidence: slice 5 hand-back ("the new test already passed with today's `hasDirectContent`"; "the story's premise ... does not hold for this shape"); plan slice 5 "About 10 min if the rule and `hasNoTextOfItsOwn` suffice". - Observed effect: a slice that delivered a regression spec and a refactor, with no reproduced defect; the UAT case stays unexplained. - Inference: the plan's own condition was the open question; a probe in the migrated page-spec fixture before slicing, or moving the slice after a real-PDF check, would have told the plan whether a fix was needed. Qualified: one execution. Related to DD-162 and DD-164.

## DD-174 — Execution start reported "no plan" from a stale local checkout before fetching origin

The coordinator looked for the story's plan in the local checkout only and told the developer none existed. Origin already held the refined story and plan 060; the local `main` was behind.

Follow-up: Open, unqueued.

- Execution: SEED-059#story-18 / slice-plans/060-panel-after-one-paragraph-epub-block / d439c05234; Timestamp: 2026-09-30, ~10:05+08:00 (start of execution); Tool: Claude Code; Model: claude-sonnet-5-5; Open Dough release: unknown. - Evidence: first reply stopped with "SEED-059#story-18 has no executable plan" from `ls .planning/slice-plans` (only plan 059); user: "Are you sure it doesn't have a plan? ... in the origin, it has a plan 60"; `git fetch origin` then showed `060-panel-after-one-paragraph-epub-block/PLAN.md` at `ba70e34680`, and story-7 Taken. - Observed effect: one wrong stop and one round trip; nothing changed on disk. - Inference: "Establish execution context" does not say to fetch before concluding a plan or claim is missing; `execution-start.mjs` fetches, but only after the coordinator has decided the source exists. Qualified: one occurrence.

## DD-175 — Managed increment delivery rejected a remote-tracking target ref; the accepted form is not shown next to the step

Recurrence of former DD-172 (`99fa1b9835e3dff2473837ba2a1f8b11967d5938:DearDough.md`, pruned from this log). `execution-increment-delivery.mjs deliver` accepts only a full branch ref (`refs/heads/<branch>`); trunk-publication.md asks for an "authorized target ref" without giving that form.

Follow-up: Open, unqueued.

- Execution: SEED-059#story-18 / slice-plans/060-panel-after-one-paragraph-epub-block / d439c05234; Timestamp: 2026-09-30, ~10:25+08:00 (slice 1 delivery); Tool: Claude Code; Model: claude-sonnet-5-5; Open Dough release: unknown. - Evidence: `deliver --target-ref origin/story/SEED-059-story-18` exited with "authorized target must be a branch ref: origin/story/SEED-059-story-18"; retry with `refs/heads/story/SEED-059-story-18` was accepted (sha d439c05234). - Observed effect: one rejected delivery call, nothing published. - Inference: a second coordinator made the same first-try mistake, so the form is not discoverable from trunk-publication.md alone.
- Execution: SEED-062#story-1 / slice-plans/006-reify-property / 508d4909b5; Timestamp: unknown (slice 1 delivery on 2026-09-30, about 18:50 +08:00); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.50. - Evidence: `deliver --target-ref claude/reify-a-property` exited with "authorized target must be a branch ref: claude/reify-a-property"; retry with `refs/heads/claude/reify-a-property` accepted 508d4909b5. - Observed effect: one rejected call, nothing published. - Inference: a bare branch name is as natural a first guess as a remote-tracking ref; the form still is not shown next to the step.

## DD-176 — A fresh refactor agent per slice returned "no edits" on three of eight small slices

The wrap-up requires a fresh post-change-refactor agent for every slice. On slices that added one focused check, one
disabled state, or one e2e scenario, the agent read the change, found nothing, and returned without edits.

Follow-up: Open, unqueued.

- Execution: SEED-062#story-1 / slice-plans/006-reify-property / 508d4909b5; Timestamp: unknown (refactor passes of slices 3, 7, 8 on 2026-09-30, between about 19:20 and 21:00 +08:00); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.50. - Evidence: refactor agents for slice 3 (53,000 subagent tokens, 35 s), slice 7 (49,811 tokens, 31 s), slice 8 (59,930 tokens, 42 s) each reported "no refactor edits"; agents for slices 1, 2, 4, 6 did make useful edits (shared key lookup, fixture uses main writer, both tracker directions in one class, reuse of an existing whole-link recognizer). - Observed effect: about 160k subagent tokens with no change to the code. - Inference: slice size alone did not predict value (slice 6 was small and still found a duplicate recognizer); a cheaper first look by the coordinator for slices with a one-file production diff might keep most of the value.

- Execution: SEED-066#discover-voice-input-problems / slice-plans/001-discover-voice-input-problems / 2eec224d133f9bfa081e5918a55c46f4de3827a1
  - Timestamp: unknown
  - Tool: Codex
  - Open Dough release: unknown
  - Evidence: 2026-10-03 execution conversation and `2533d739a16f025d612a38b405e63a30e804d65c:.planning/slice-plans/001-discover-voice-input-problems/PLAN.md`, fresh post-change-refactor reports for slices 3–10; all eight returned “none — already clean,” with reported active durations of about 20 seconds to one minute. Slice 1 split detailed evidence/context; slice 2 shortened the epic summary.
  - Observed effect: eight consecutive documentation-only slices required independent refactor handoffs without edits; accepted manual proof needed no rerun. The first two reviews made useful record changes. Token usage was not supplied.
  - Inference: the mandatory handoffs consume some of a bounded discovery mission, although this record does not establish net review value or measured token cost. Consider a cheaper review route for documentation-only slices; no current execution requirement was waived.

## DD-177 — The plan put API regeneration in a later slice than the endpoint that requires it

`RobotsTests.openApiDocsMatchCommittedYaml` fails whenever a controller signature changes without regenerating
`open_api_docs.yaml`, so an endpoint slice and its regeneration cannot be delivered separately while CI stays green.

Follow-up: Open, unqueued.

- Execution: SEED-062#story-1 / slice-plans/006-reify-property / 508d4909b5; Timestamp: unknown (slice 2 return on 2026-09-30, about 19:10 +08:00); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.50. - Evidence: PLAN.md slice 5 "Regenerate the client after slice 2's controller signature"; slice 2 implementer reported `pnpm backend:test_only` 2698 tests, 1 failure `RobotsTests > openApiDocsMatchCommittedYaml()`; coordinator regenerated in slice 2 delivery (2f33e3e0ae) and marked slice 5 absorbed. - Observed effect: plan order had to be changed during execution; no red push. - Inference: the planner treated generation as an optional follow-up Structure slice; the project's generation trigger belongs in the same slice as the signature change.

## DD-178 — A refactor agent reran a failing new test until green and suggested an environment cause; the failure was a real flake

A new test failed once in the refactor pass; the agent reran it, saw green twice, and reported a suspected shared-database or build cause without evidence. The coordinator's forced rerun reproduced it: the test assumed index row order across frontmatter keys, and `Frontmatter.keys()` is `Set.copyOf`, whose order varies per JVM run.

Follow-up: Open, unqueued.

- Execution: SEED-063#story-1 / slice-plans/007-track-property-values-separately / 8eaaf2b720; Timestamp: 2026-09-30, between 20:13 and 20:27+08:00 (slice 1b refactor through commit 21bb042c10); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.50. - Evidence: slice 1b refactor report ("failed once ... could not reproduce ... suspected cause: another session using the shared `doughnut_test` database"); coordinator `--rerun-tasks` run failed `NotePropertyIndexServiceTest ... indexes_every_list_item_with_its_value_and_a_scalar_with_an_empty_value`; instrumented runs printed `topic` rows before `example of` in the failing run; the fix asserts per key (21bb042c10). - Observed effect: a flaky test nearly committed; about five coordinator test runs to diagnose; no defect shipped. - Inference: the refactor delegation carries no "a passing retry does not establish cause" rule from execution-decisions' failed-proof diagnosis; the implementer's own red run showed the same symptom, which masked the flake. Qualified: one occurrence.

## DD-199 — Refactor delegation omitted a known failed approach, which the fresh agent repeated

The implementation return explained that overriding the RouterLink stub through RenderingHelper's deep merge had already failed. The coordinator's fresh refactor assignment mentioned duplicate mount setup but omitted that failed approach. The refactor agent started the same override before a follow-up warning arrived, reproduced the failure, then removed the stub at its owner before merging.

### Occurrences
- Execution: SEED-065#story-1 / `7b7fd7cce4e6fe0a9551c1599cd28200bf6f643e:.planning/slice-plans/001-follow-property-list-wiki-links/PLAN.md` / ed2cdd854148b14aef0581ee0d2c8af5049435d6
  - Timestamp: unknown (2026-10-02, between implementation return and refactor completion)
  - Tool: Codex
  - Open Dough release: unknown
  - Evidence: implement_property_links final return identifies the failed false-stub override; initial refactor_property_links assignment omits it; refactor progress says the warning arrived after that rerun started; final return confirms the failure was reproduced.
  - Observed effect: one avoidable failed refactor verification; all final proof passed and no faulty change was delivered.
  - Inference: include already-disproved approaches in the initial fresh-agent handoff when they constrain the likely simplification. Qualified: one execution; cost was not measured.
