# DearDough Process Findings

Retained material shared-process findings, reviewed 2026-10-06. Only an explicit
queued follow-up is planned work; other entries are open and unqueued. A retained
released response is not proof of effectiveness. Unknown provenance stays unknown.
[Response status](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md).
Full pre-trim evidence: `8830c682704aac3bbb34bf9b1204da8feba042ca:DearDough.md`. Older narratives live in Git, not a second archive.

- Highest allocated local number: 217. Removed local codes are never reused.

## ODF-110 — A readiness replay observed only the plan's named seam, not the rest of the slice's journey

Former local code: DD-109.
Former local code: DD-163.
Former local code: DD-168.
Former local code: DD-171.

A replay resolves the named readiness seam without exercising the rest of the slice's promised journey, leaving a later operation to force a scope stop.

Follow-up: queued, not resolved: [Observe decisive planning premises through the full promised journey](../open-dough/.planning/seeds/SEED-108-planning-observations-cover-promised-journeys.md#observe-promised-journey) — SEED-108#observe-promised-journey. Responses 2c5ff71 (0.3.43) and fcc29fad (0.3.48) are released; the same class of unobserved premise is reported again on 0.3.51–0.3.56, so neither is shown to resolve it.

- Execution: SEED-035 story 14 / slice-plans/025-convert-raw-notebooks-to-lfs / 071d0e0861; Timestamp: 2026-09-24, ~15:35+08:00 (replay and readiness record 47df9474c2), failure observed ~16:05+08:00 (slice 3 E2E); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.37. - Evidence: research prompt scoped to "pull" risks only; its report noted "the publish check went only as far as the pointer blob being committed"; slice 3 E2E then failed at the second `donut notebook publish` ("Attachment at <commit> must be a Git LFS pointer…", `cli/src/commands/notebook/notebookPublishLfsSelection.ts`); plan recorded the stop in 1e2ed84c35. - Observed effect: one human round-trip and a scope change (CLI change, option A) that preparation could have surfaced before Take. - Inference: when resolving a readiness concern by observation, replay the slice's full promised journey (here pull, then publish), not only the mechanism the concern names; the replay's own "not covered" list was the signal.
- Execution: SEED-059#story-6 / slice-plans/056-change-or-clear-reading-mark / 7c9935b2c2; Timestamp: 2026-09-29 (slice 2 implementation, between 17:53 and 18:12 +08:00); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.47. - Evidence: plan *Decisive premises* "E2E setup and steps exist … Confirmed by grep"; slice 2 implementation report ("My first green run failed on `no book block should be marked` (found 1)"); new step "book block {string} should not be marked in the book layout" in 5cab6e8827. - Observed effect: one failed EPUB feature run and a new step; the plan's slice 2 behavior text was corrected during delivery. Small cost. - Inference: the premise checked that a step exists, not that it holds for the chosen fixture path. Related to DD-162 (a grep premise that did not reach the changed path).
- Execution: SEED-059#story-5 / slice-plans/053-pdf-smooth-scroll-after-choosing-block / 5989892325; Timestamp: 2026-09-29T21:45+08:00 through 22:15:52+08:00 (slice 1 implementation to commit); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.47. - Evidence: slice 1 hand-backs stopping at "the panel element never appears" (`panelShownBecauseScrolledPastContent` required `successor.id !== currentBlockId`) and then at "`read-from-here` covers `book-reading-mark-as-read`" (both overlays `absolute left-0 right-0 bottom-0 z-20`); owner question answered "Stack the two"; fix in 5989892325 (`ReadingOverlayDock.vue`). - Observed effect: two implementation stops, one owner decision on a scope the story had deferred (panel position), and slice 1 took about 30 minutes against a ~5-minute target. - Inference: reading `blockAwaitingConfirmation` and the overlay classes for the story's own "scroll past without marking" key example would have shown both at planning. Related to DD-162 and DD-163 (premises that stopped short of the consuming step).
- Execution: SEED-066#preserve-completed-speech / `2b34db5abb:.planning/slice-plans/001-keep-completed-speech/PLAN.md` / 4070f526bc; Timestamp: 2026-10-03T06:48:59Z (slice 2 stop) and 2026-10-03T07:41:26Z (slice 4 first run); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.54 (execution-checkout VERSION, unchanged during execution). - Evidence: original plan premises confirmed the mid-speech path by reading `audioProcessingScheduler.ts` and `SRTProcessor` separately, never the multipart binding between them or a real SRT; slice 2 hand-back ("`isMidSpeech` form field ... never reaches the backend", `WebDataBinder` probe bound false); slice 4 run 1 on note 13730 wrote every tail because the real SRT ends with `\n\n\n`; slice 1's probe already logged every request and response (empty end timestamps) but was asked only for user-visible results. - Observed effect: one slice 2 stop and replan, one extra paid real-service run, a repair commit (1a9816737d) and a second primary-checkout restart. - Inference: asking the slice 1 probe to check the request flag, the returned end timestamp and the raw SRT shape against the plan's premises would likely have exposed both before slices 2–3, at no extra paid call. Qualified: one execution.
- Earlier occurrence details: 1 additional recorded rows in `8830c682704aac3bbb34bf9b1204da8feba042ca:DearDough.md`; these are historical evidence, not new occurrences.

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

## ODF-074 — A plan said the changed script had no test, and nobody searched for one before delivery, so CI caught the stale test

Former local code: DD-126.
Former local code: DD-130.
Former local code: DD-134.
Former local code: DD-139.
Former local code: DD-147.
Former local code: DD-162.
Former local code: DD-167.

Concrete only-caller and host-state premises enter a plan without inspection, forcing a changed decision or stopped implementation when checked.

Follow-up: queued, not resolved: [Observe decisive planning premises through the full promised journey](../open-dough/.planning/seeds/SEED-108-planning-observations-cover-promised-journeys.md#observe-promised-journey) — SEED-108#observe-promised-journey. Responses 2c5ff71 (0.3.43) and fcc29fad (0.3.48) are released; the same class of unobserved premise is reported again on 0.3.51–0.3.56, so neither is shown to resolve it.

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

Follow-up: delivered, unreleased: SEED-095#prove-slices-through-consumers (recoverable at `56ed987b:.planning/seeds/SEED-095-slice-proof-through-consumers.md`). Response `cca9bff4` is on main; no release tag contains the complete response as of 2026-10-06.

- Execution: SEED-047#story-1 / `edfd7ba92a:.planning/slice-plans/014-continue-to-neighboring-note-after-deletion/PLAN.md` / a41b1e0507; Timestamp: unknown (before slice 1 commit 2026-09-27T10:59:16+08:00); Tool: Claude Code; Open Dough release: 0.3.42 (VERSION in the execution checkout). - Evidence: coordinator summary to the retrospective (subagent transcripts not supplied): coordinator consumer check found 2 failing tests in `NoteMoreOptionsForm.trashNote.spec.ts`; fixed by an empty listing mock in `tests/notes/noteMoreOptionsTrashTestSupport.ts` (in a41b1e0507), then `tests/notes tests/store tests/toolbars` 353/353. - Observed effect: one coordinator repair before commit; no defect shipped. - Inference: a `grep` for callers of the changed method across `tests/` when choosing slice proof would have included the spec.
- Execution: SEED-059#story-16 / `dfec19ca03:.planning/slice-plans/058-current-block-same-in-pdf-and-epub/PLAN.md` / d3eec9db16; Timestamp: 2026-09-30T00:01:40Z (CI job log time of the failure); Tool: Claude Code; Model: claude-sonnet-5-5; Open Dough release: unknown. - Evidence: slice 2 removed the 40-point landing padding; its delegation named `book_browsing.feature` and `reading_record.feature`, and the report ran only those. CI run 36647846005 attempt 1 failed `phone_reading.feature` "Choosing a book block closes the book layout and moves the book there" (`expected ... to contain '2 /'`); the slice 3 agent ran `phone_reading` (7/7 locally at 390*844); the repair agent reproduced the failure only at 390*900 and repaired in 5206b08370. - Observed effect: one failed published E2E job, a paused slice 3 (stash, repair agent about 10 min, restore) and one test repair. - Inference: `phone_reading.feature` also chooses PDF blocks, so a search of `e2e_test/features/book_reading/` for scenarios that choose a block would have listed it. Same root cause as the entries above (proof chosen by edited area); the actor was the coordinator's delegation. The failure itself needed a taller-than-stock window, so local runs could not have shown it.
- Earlier occurrence details: 2 additional recorded rows in `8830c682704aac3bbb34bf9b1204da8feba042ca:DearDough.md`; these are historical evidence, not new occurrences.
- Execution: SEED-063#story-1 / slice-plans/007-track-property-values-separately / 8eaaf2b720; Timestamp: 2026-09-30T21:39:52+08:00 (slice 8 commit eead7350eb; failure reported after publication); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.50. - Evidence: slice 8 changed reduce from writing a suffixed `key 2` to appending a value; its delegation and plan named only backend tests (`RelationControllerReduce*`, `*Relation*`, ...). CI run 36723317159 attempt 1 failed `e2e_test/features/relationships/relationship_edit_and_remove.feature` "Reducing to source property uses a suffixed key when the property already exists" (expected the list content to include `a part of 2: '[[Mars]]'`); repaired in f07ff79425. - Observed effect: one failed published E2E job; slice 9 paused (repair stash, repair agent, refactor agent, restore). - Inference: a search of `e2e_test/features/` for the retired behavior (`suffixed`, `a part of 2`) when planning or delegating slice 8 would have listed the scenario; the plan's "Tests to extend" list covered unit and controller tests only.

## ODF-152 — The file-size rule conflicted with an approved staged simplification and mechanical callers

Former local code: DD-138.

An absolute changed-file size check conflicts with an approved staged decomposition and mechanical edits to pre-existing oversized callers.

Follow-up: Open, unqueued.

- Execution: SEED-049#story-1 / slice-plans/016-note-store-architecture / 3821dbd9c79c7ab25e68cd1ff96aa44061d6544f; Timestamp: unknown (2026-09-27, refactor1 and refactor6 handoffs); Tool: Codex; Open Dough release: 0.3.42. - Evidence: chat 01a0e0fe-e37c-7512-a6b3-b8d4c1318365, refactor1 questioned the 384-line intermediate store before planned slices4–6; refactor6 questioned useWikidataPropertyDialog after mechanical migration (300 lines at its base, then smaller). Rule: dough-post-change-refactor/references/refactor-checks.md, File size. Source: SEED-049 owner-approved single command/undo split; slice7 owns store size proof. - Observed effect: two applicability exchanges; no extra split was made. Final noteStore is 245 lines, noteUndo167, requests228, cache45. The unrelated Wikidata workflow was preserved. - Inference: clarify how the numeric check composes with approved intermediate states and the skill's requirement that a refactor address an introduced, exposed, or aggravated issue. Unlike ODF-124, the rule was found and acknowledged here.

## ODF-189 — A four-line repository tip-over forced extracting an unrelated assimilation query block

Former local code: DD-144.

A small addition crosses the numeric file-size ceiling and triggers relocation and re-proof of a substantial previously untouched responsibility.

Follow-up: Open, unqueued.

- Execution: SEED-053#story-1 / `12c0f629ac:.planning/slice-plans/007-file-page-references/PLAN.md` / bc870053e8; Timestamp: 2026-09-28T12:37:00+08:00 through 2026-09-28T12:40:26+08:00 (Slice 2 refactor through delivery); Tool: Cursor; Model: gemini-3.8-flash; Open Dough release: 0.3.45. - Evidence: refactor transcript `ff698959-2f7d-4392-a127-0f3f0adcc8f8/subagents/2b60056d-ca4d-4cae-8a47-c79402587e9f` (decision pass: File size 254; learning "Slice 2's candidate query pushed NoteRepository over 250"); commit `bc870053` adds `NoteAssimilationQueries.java` and shrinks `NoteRepository.java`. Pre-Slice-2 `NoteRepository` at `c86eb7eba0` was 244 lines. - Observed effect: ~8 minutes of refactor time and an assimilation-focused re-proof (`AssimilationControllerTests`) for a tip-over caused by one new query. - Inference: the hard 250-line ceiling can force relocating a large untouched block when a small addition crosses it. Related in theme to ODF-152 (numeric check applicability), but here the agent performed the split rather than escalating a staged-simplification conflict.
- Execution: SEED-059#story-6 / slice-plans/056-change-or-clear-reading-mark / 7c9935b2c2; Timestamp: 2026-09-29T17:53+08:00 through 2026-09-29T18:12+08:00 (slice 2 implementation through refactor and delivery); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.47. - Evidence: `7c9935b2c2:backend/src/main/java/com/odde/donut/controllers/NotebookBooksController.java` has 248 lines; the slice 2 DELETE endpoint added 15. The slice 2 refactor report ("pushed `NotebookBooksController.java` to 263 lines") split out `NotebookBookReadingController` (124 lines) and changed five controller test files. Commit 5cab6e8827 carries the repo's only `@Tag(name = "notebook-books-controller")`, so the generated SDK class stays unchanged. - Observed effect: the refactor pass took about 25 minutes, against about 12 for the slice's implementation, and needed a wider backend re-proof. The seam it chose, the user's reading progress versus the book's attach and structure, is domain-meaningful. - Inference: same tip-over pattern; here the split landed on a real seam but introduced a new convention (a shared OpenAPI tag across two controllers) to avoid touching the frontend.
- Execution: SEED-063#story-1 / slice-plans/007-track-property-values-separately / 8eaaf2b720; Timestamp: 2026-09-30T20:44:32+08:00 (slice 2b commit 178ddce040); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.50. - Evidence: slice 2b added one parameter to `MemoryTracker.buildMemoryTrackerForProperty`, taking `MemoryTracker.java` from 256 to 258 lines (already over 250 before the slice); the refactor pass moved the unrelated grade-scheduling step `scheduleNextRecallFromStability` into `MemoryTrackerRecallDue` and reran `*MemoryTracker*`, `RecallsControllerTests` and `*Fsrs*` (174 tests). In slice 7, `MemoryTrackerService` at 249 lines led the implementer to move `updatePropertyKey` into a new `PropertyMemoryTrackerService`. - Observed effect: an unrelated recall-timing move and wider re-proof in slice 2b; slice 7's move landed on a real seam that slice 8 then reused for reduce re-homing. - Inference: same tip-over pattern; one of the two moves was unrelated to the change.

- Execution: SEED-064#story-6 / `f347df3d514feccaeaff8dbce062bce1992e0cff:.planning/slice-plans/007-add-property-draft-row/PLAN.md` / c05b83ea2b8a5ed3b0bd1534d720fcd08f8c9539; Timestamp: unknown (2026-10-01, slice 3 and 4 refactor handoffs before 0470d6be2d and cb7c47b618); Tool: Codex; Open Dough release: 0.3.52 (unchanged VERSION in the established execution checkout). - Evidence: coordinator conversation, refactor3 reported propertyEntry at 271 lines and moved the unchanged rename/body-refresh guard into propertyRenameGuard.spec.ts; refactor4 reported notePropertyLocationMethods at 281 lines and extracted layout assertions into notePropertyLayoutMethods.ts. Commits 0470d6be2d and cb7c47b618 contain those moves; dough-post-change-refactor/references/refactor-checks.md requires every changed file to fit 250 lines. - Observed effect: two responsibility extractions plus replacement mounted and layout E2E proof, each refactor reporting about 3 minutes. - Inference: same numeric tip-over pattern on test support; the seams were cohesive, but proof relocation added work beyond the small Cancel and touch-height outcomes. Consider anticipating file-size capacity during slice planning; no product defect or instruction change is implied.

- Execution: SEED-066#preserve-existing-content / `979cac31fc19f756bdfc480d9b24f1ab7dfeec34:.planning/slice-plans/001-preserve-existing-content/PLAN.md` / 64173ad25fbbe7457705aeea972a959d9d3f8dd4; Timestamp: unknown (2026-10-03, slice 2 refactor); Tool: Codex; Open Dough release: 0.3.54 (VERSION unchanged from claim through implementation). - Evidence: refactor_append decision and return; noteStore was 252 lines before append, 265 after; processing spec reached 267. Commit 6c2ed996f7 extracts noteTextEditing and splits audio preservation tests. - Observed effect: the mandatory ceiling prompted an 80-line text-edit extraction, a test split, and expanded proof from 38 to 63 frontend tests plus integrated E2E; refactor reported about seven active minutes. - Inference: the seams were relevant and coherent, but the numeric gate expanded verification beyond the append change. This occurrence does not establish a net cost or an unrelated production move.

- Execution: SEED-066#join-dictated-passages / slice-plans/001-join-dictated-passages / ba66d94ad393c23015ae855375133c7a67779a5a
  - Timestamp: unknown (2026-10-05, slice 2 refactor)
  - Tool: Codex
  - Open Dough release: unknown
  - Evidence: `refactor_language` decision/return in the execution conversation; preservation spec was 249 lines at ba66d94ad3, grew to 304, then e769c06a99 extracted language cases and shared saved-body test support.
  - Observed effect: approximately four active refactor minutes, two new test files, and replacement proof of 27 mounted tests plus typecheck; production code stayed unchanged.
  - Inference: the numeric ceiling prompted a cohesive test split and extra proof. Consider test-file capacity while planning; net review value and token cost were not measured.

## ODF-190 — The plan prescribed production observations whose access route or log source did not exist, and whose results could not change the approach

Former local code: DD-145.

Planning names an observation route or log source that is not available to the executing project, causing owner probing or loss of the intended proof.

Follow-up: queued, not resolved: [Observe decisive planning premises through the full promised journey](../open-dough/.planning/seeds/SEED-108-planning-observations-cover-promised-journeys.md#observe-promised-journey) — SEED-108#observe-promised-journey.

- Execution: SEED-051#story-1 / `f35fa810f1:.planning/slice-plans/009-retire-note-embeddings/PLAN.md` / 5003fbecc8; Timestamp: 2026-09-28T05:36:39Z–06:03:39Z (slice 1/3) and 2026-09-28T07:17:43Z–07:18:10Z (slice 10); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.45. - Evidence: four auto-mode denials while seeking a DB route (credential lookup, SSH to the app VM, probe edit to root, bucket IAM); owner: "But this wasn't needed uh, previously. Um, or can we skip this?"; coordinator's covering reasoning and skip recorded in d111968b62; slice 10 `gcloud logging read` found no Flyway lines, so sustained health became the D/P evidence (8e03ac5f5f). - Observed effect: about 27 minutes of owner-attended probing ended in skipping slice 1's SQL part and dropping slice 3; slice 10's named proof was replaced during delivery. No product defect. - Inference: planning could have asked, for each production observation, whether any result would change the approach, and whether the access route and log source exist (both checkable cheaply once `gcloud` auth worked). Related to DD-142 (prescribed observations dropped in execution), but here the cost was production access and owner time. Qualified: one execution; planning-time `gcloud` auth had failed.
- Execution: SEED-059#story-5 / slice-plans/053-pdf-smooth-scroll-after-choosing-block / 5989892325; Timestamp: 2026-09-29T21:45+08:00 through 22:10+08:00 (slice 1 attempts); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.47. - Evidence: plan 053's proof row "Manual observation … Chromium DevTools protocol or Playwright `mouse.wheel`" on the dev stack; the repo has no Playwright/Puppeteer, `.agents/agent-map.md` says linked worktrees refuse the persistent Development stack, and a throwaway Cypress spec sending CDP `mouseWheel` reached the DOM but scrolled 0 px in headless Electron and Chrome. - Observed effect: about 15 minutes of implementer time; the story's key example (every wheel step moves down) was delivered without its real-wheel observation. - Inference: same pattern in a local setting: the observation route could have been checked at planning from the agent map and `package.json`.
- Execution: SEED-066#preserve-completed-speech / `2b34db5abb:.planning/slice-plans/001-keep-completed-speech/PLAN.md` / 4070f526bc; Timestamp: 2026-10-03T07:02:48Z–07:32:53Z (owner question), 07:32:54Z–07:57:43Z (primary checkout detached and restored); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.54 (execution-checkout VERSION, unchanged during execution). - Evidence: plan slice 1 states Development runs only from the primary checkout because linked worktrees refuse it, but slice 4 (the same paid journey on branch code) names no route; coordinator AskUserQuestion "Switch primary briefly / Defer / Drop"; primary detached at fd904bc2 and 1a981673, then restored to `main`, where `dev:restart` failed on a stale `dev.pid` (PID 597 reused by `accountsd`). - Observed effect: about 30 minutes waiting for the owner mid-execution, a shared checkout temporarily on branch code, and about 7 minutes restoring Development. - Inference: slice planning could have asked how a real-service proof reaches unmerged branch code and settled it with the owner before Take. Related to ODF-083 (shared checkout moved by an execution).
- Execution: SEED-066#keep-every-transcribed-sentence / `9ad1cedcc0:.planning/slice-plans/008-keep-every-transcribed-sentence/PLAN.md` / d68937a0d2; Timestamp: 2026-10-04 (slice 4, after 6ecad492f1 was pushed); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.56 (execution-checkout VERSION, unchanged during execution). - Evidence: plan 008 Commands section names a local `pnpm cy:run --spec ...record_live_audio...` and slice 4 names the real-service feature run as proof; `scripts/isolated-cypress-spec-selection.mjs` refuses live-OpenAI specs in linked worktrees (`scripts/isolated-cypress.test.mjs` asserts the refusal). The owner authorized "Run the feature only"; the implementer's `cy:run` was refused, it then made two direct whisper-1 calls on `lecture.wav` (one output lost to a macOS `cat -A` pipe error) and the feature proof moved to the CI shard. - Observed effect: one owner round-trip, a refused run, two paid calls the owner had not specifically authorized, and a slice 4 proof that rests on CI plus an owner-pending manual run. - Inference: third SEED-066 occurrence of the same planning gap; the plan could have named CI as the branch route for the live spec. Qualified: the direct calls were small (3 s clip) and confirmed the open premise.

## ODF-195 — The file-size check split an untouched block in one slice of an execution and was waived in a later slice

Former local code: DD-160.

Two refactor passes within one story treat barely touched, already oversized files differently, forcing an unrelated extraction in one and retaining another.

Follow-up: Open, unqueued.

- Execution: SEED-059#story-2 / slice-plans/050-read-a-book-on-a-phone / d4a47402af; Timestamp: 2026-09-29T12:00+08:00 through 2026-09-29T12:40+08:00 (slice 1 and slice 3 refactor passes); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.46. - Evidence: slice 1 refactor report ("`BookReadingBookLayout.vue` is in the diff and was 354 lines, over the 250-line limit"; new `useBookLayoutBlockPointerDrag.ts`, commit d4a47402af); slice 3 refactor report ("`BookReadingContent.vue` is 453 lines … It was 474 before this change … I left it for the owner to decide"). Pre-change size: `1e19f8224a:frontend/src/components/book-reading/BookReadingBookLayout.vue` has 354 lines. - Observed effect: about 15 minutes of refactor time in slice 1, plus a desktop re-proof (`reorganize_layout.feature`, `book_browsing.feature`) for code the story did not touch. Slice 3 took the other path, with no extraction. - Inference: `refactor-checks.md` "File size" does not say whether a file that was already over the limit, and that a slice barely touches, must be split. Agents resolve this differently, and the time cost follows whichever reading they pick. Related to DD-144 (a small addition tipping a file over the limit) and ODF-152; here the file was over the limit before the change.

## ODF-200 — Managed increment delivery rejected a remote-tracking target ref; the accepted form is not shown next to the step

Former local code: DD-175.

Recurrence of former DD-172 (`99fa1b9835e3dff2473837ba2a1f8b11967d5938:DearDough.md`, pruned from this log). `execution-increment-delivery.mjs deliver` accepts only a full branch ref (`refs/heads/<branch>`); trunk-publication.md asks for an "authorized target ref" without giving that form.

Follow-up: Open, unqueued.

- Execution: SEED-059#story-18 / slice-plans/060-panel-after-one-paragraph-epub-block / d439c05234; Timestamp: 2026-09-30, ~10:25+08:00 (slice 1 delivery); Tool: Claude Code; Model: claude-sonnet-5-5; Open Dough release: unknown. - Evidence: `deliver --target-ref origin/story/SEED-059-story-18` exited with "authorized target must be a branch ref: origin/story/SEED-059-story-18"; retry with `refs/heads/story/SEED-059-story-18` was accepted (sha d439c05234). - Observed effect: one rejected delivery call, nothing published. - Inference: a second coordinator made the same first-try mistake, so the form is not discoverable from trunk-publication.md alone.
- Execution: SEED-062#story-1 / slice-plans/006-reify-property / 508d4909b5; Timestamp: unknown (slice 1 delivery on 2026-09-30, about 18:50 +08:00); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.50. - Evidence: `deliver --target-ref claude/reify-a-property` exited with "authorized target must be a branch ref: claude/reify-a-property"; retry with `refs/heads/claude/reify-a-property` accepted 508d4909b5. - Observed effect: one rejected call, nothing published. - Inference: a bare branch name is as natural a first guess as a remote-tracking ref; the form still is not shown next to the step.
- Execution: SEED-066#author-controlled-titles / slice-plans/002-keep-titles-under-author-control / aa093fffeb; Timestamp: unknown (slice 1 delivery on 2026-10-03, shortly after 14:55 +08:00); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.54. - Evidence: `deliver --target-ref claude/keep-note-titles-under-the-author-s-control` exited with "authorized target must be a branch ref"; retry with `refs/heads/...` accepted aa093fffeb. - Observed effect: one rejected call, nothing published. - Inference: third occurrence; the coordinator had read the `--help` usage, which also says only `--target-ref REF`.
- Execution: SEED-067#stacks-survive-other-builds / slice-plans/005-stacks-survive-other-builds / 4122c07c06; Timestamp: 2026-10-03, ~18:45+08:00 (slice 1 delivery); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.56. - Evidence: after `--help` showed only `--target-ref REF`, the coordinator read `execution-increment-delivery.mjs` and `publication-git.mjs` (`targetBranchName` requires `refs/heads/`) and also had to infer that Story Branch Mode targets the execution branch, not `main`; first call with `refs/heads/claude/start-and-keep-local-app-stacks-on-current-backe` was accepted. - Observed effect: no rejected call, but three extra source-reading calls before delivery. - Inference: fourth occurrence; avoiding the rejection still cost source reading, so the form and the story-branch target belong next to the step.
- Execution: SEED-066#keep-every-transcribed-sentence / slice-plans/008-keep-every-transcribed-sentence / d68937a0d2; Timestamp: 2026-10-04 (slice 1 delivery); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.56. - Evidence: coordinator ran `deliver --help` (only `--target-ref REF`), then read `execution-increment-delivery.mjs` and grepped `publication-git.mjs` `targetBranchName` before the first accepted call with `refs/heads/claude/keep-every-transcribed-sentence-when-dictated-te`. - Observed effect: no rejected call; two extra source-reading calls. - Inference: fifth occurrence; same as the fourth.
- Execution: SEED-069#observe-branch-code-against-real-services / slice-plans/004-hold-worktree-e2e-stack / ee414fbdf1; Timestamp: 2026-10-05, ~12:16+09:00 (slice 1 delivery); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.56. - Evidence: coordinator ran `deliver --help` (only `--target-ref REF`), then grepped `targetBranchName` in `publication-git.mjs` before the first accepted call with `refs/heads/claude/observe-unmerged-branch-code-against-real-servic`. - Observed effect: no rejected call; two extra lookup calls. - Inference: sixth occurrence; same as the fourth and fifth.

## ODF-141 — A fresh refactor agent per slice returned "no edits" on three of eight small slices

Former local code: DD-176.

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

- Execution: SEED-066#author-controlled-titles / slice-plans/002-keep-titles-under-author-control / aa093fffeb
  - Timestamp: unknown (slice 1 refactor on 2026-10-03, between 14:53 and 14:55 +08:00)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.54
  - Evidence: slice 1 (one composable removal, two specs, one doc) refactor agent reported "no refactor edits" (52,715 subagent tokens, 30 s); slice 2's refactor agent did find useful residue (inlined the last `executeWithTool` overload, made `maxOutputTokens` non-nullable).
  - Observed effect: one of two refactor passes changed nothing.
  - Inference: again, diff size did not predict value: slice 2 was a pure deletion and still left residue.

- Execution: SEED-039#internal-mocks-to-real-modules / slice-plans/009-frontend-specs-run-real-internal-modules / a70529a3fb
  - Timestamp: unknown (refactor passes on 2026-10-04, between about 09:20 and 10:15 +08:00)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.56 (execution-checkout VERSION, unchanged during execution)
  - Evidence: 13 test-only slices. Refactor agents for slices 4, 7, 10, 13 returned "no refactor edits" (54,847 + 50,344 + 55,406 + 60,542 subagent tokens). The other nine made edits later slices reused: shared `answerOnlyPendingPopup`, `productionRouterAt` replacing four local router builders, `countHistoryEntriesAdded` shared by three specs, toast readers moved into the toast helper.
  - Observed effect: about 221k subagent tokens on passes without edits; nine of thirteen passes changed code.
  - Inference: in a test-only story that grows shared helpers slice by slice, the passes mostly paid off; the no-edit passes came on slices that only applied helpers already in place. Qualified: one execution.

- Execution: SEED-039#specs-share-production-router / slice-plans/002-specs-share-production-router / 98728709f1
  - Timestamp: unknown (refactor passes on 2026-10-05, between about 06:40 and 06:58 +09:00)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.56 (execution-checkout VERSION, unchanged during execution)
  - Evidence: 4 test-only slices. Refactor agents for slice 1 (one-line reset plus a new spec; 50,313 subagent tokens) and slice 4 (three files plus one skill sentence; 53,730 tokens) returned "no refactor edits". Slice 2's pass moved imports to the `@tests/helpers` barrel and dropped a redundant `router.push("/")`; slice 3's pass removed a redundant `$route` mock.
  - Observed effect: about 104k subagent tokens on passes without edits; two of four passes changed code.
  - Inference: as in the previous row, passes on slices that only applied an established pattern found nothing. Qualified: one execution.

- Execution: SEED-039#one-production-router-builder / slice-plans/003-one-production-router-builder / 20f253b39d
  - Timestamp: unknown (slice 1 refactor on 2026-10-05, shortly before 10:45 +09:00)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.56 (execution-checkout VERSION, unchanged during execution)
  - Evidence: one-slice test-support correction (one extracted router builder, deletion of an unused render path and fixture). The refactor agent returned "none — already clean" (50,844 subagent tokens, about 20 s); the implementer had already removed the leftovers the plan named.
  - Observed effect: the only refactor pass changed nothing.
  - Inference: when the plan itself is a cleanup correction that lists exact leftovers, the separate pass has little left to find. Qualified: one execution.

- Execution: SEED-069#observe-branch-code-against-real-services / slice-plans/004-hold-worktree-e2e-stack / ee414fbdf1
  - Timestamp: unknown (refactor passes on 2026-10-05, between about 12:12 and 12:38 +09:00)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.56 (execution-checkout VERSION, unchanged during execution)
  - Evidence: 4 slices of runner scripts and guidance. Refactor agents for slice 1 (new hold entry, one session seam; 54,309 subagent tokens) and slice 3 (one reset call, one passed field; 55,041 tokens) returned no edits. Slice 2's pass shared a `withEnv` test fixture across three callers and split `scripts/e2e-hold.mjs` out of an over-limit file; slice 4's pass linked the guidance to its one detailed home and added a missing pointer in `docs/development-setup.md`.
  - Observed effect: about 109k subagent tokens on passes without edits; two of four passes changed files.
  - Inference: as in earlier rows, diff size did not predict value. Qualified: one execution.

- Execution: SEED-066#join-dictated-passages / slice-plans/001-join-dictated-passages / ba66d94ad393c23015ae855375133c7a67779a5a
  - Timestamp: unknown (2026-10-05, slice 1 refactor)
  - Tool: Codex
  - Open Dough release: unknown
  - Evidence: `refactor_segments` returned “none — already clean” and `## REFACTOR COMPLETE`, reporting approximately three active minutes, no edits, and no proof reruns; ba66d94ad3 carries the reviewed slice.
  - Observed effect: one of this execution's two mandatory independent refactors produced no edits; slice 2's review made the test split recorded under ODF-189.
  - Inference: this bounds handoff cost but does not establish that the independent review lacked value. No execution requirement was waived.

- Execution: SEED-066#recover-failed-transcription / `212e428968:.planning/slice-plans/005-recover-failed-transcription/PLAN.md` / 9eb06ed0bd
  - Timestamp: unknown (refactor passes on 2026-10-05, between about 14:40 and 14:58 +09:00)
  - Tool: Claude Code
  - Open Dough release: 0.3.56 (execution-checkout VERSION, unchanged during execution)
  - Evidence: coordinator summary to the retrospective (subagent transcripts not supplied). Slice 1's pass found a real defect: `stop()` awaited the raw processing promise, which could reject; the catch moved onto the promise and a test was added (in 9eb06ed0bd). Slice 2's pass made no edits. Slice 3's pass split `NoteAudioTools.retry.spec.ts` out for file size and removed a duplicated `ServiceMocker` stub builder (in ab773157bb). Token counts not supplied.
  - Observed effect: one of three passes changed nothing; one caught a defect the slice's own tests had not.
  - Inference: the defect-finding pass came on the slice that changed a promise's failure path; as in earlier rows, diff size did not predict value. Qualified: one execution.

- Execution: SEED-066#prompt-dictation-results / `a4c70254e7:.planning/slice-plans/007-see-submitted-dictation-promptly/PLAN.md` / dd875812e5
  - Timestamp: unknown (2026-10-06; slice 2 refactor between about 09:30 and 09:36 +09:00)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.56 (execution-checkout VERSION, unchanged during execution)
  - Evidence: slice 2's refactor agent (70,273 subagent tokens, about 2 minutes) made one rename, `stubTranscription` → `stubStopTranscription` in `e2e_test/start/mock_services/openAiService.ts`, in dd875812e5. The coordinator ran no refactor agent for the records-only commits 99962fbf99 (plan measurement table) and f50c7dcef6 (plan status and docs/voice-input.md), and reported that deviation to the retrospective.
  - Observed effect: one pass made a small naming edit; two required passes were skipped without a waiver.
  - Inference: the coordinator treated records-only slices as needing no independent refactor, the cheaper route earlier rows suggest, but the wrap-up has no such route, so the skip is a deviation rather than an allowed path. Qualified: one execution.

## ODF-202 — Managed Codex delivery left CI unobserved without a retained stream binding

Former local code: DD-201.

Follow-up: delivered, unreleased: SEED-094#observe-ci-on-codex-and-cursor (recoverable at `0dc71704:.planning/seeds/SEED-094-ci-observation-for-codex-and-cursor.md`). Response `abeb79f9` arms and reuses the Codex stream; no release tag contains it as of 2026-10-06. Native evaluation remains pending.

The managed delivery CLI can declare the Codex bridge ready, while the documented yielded adapter launches its own stream observer. This execution did not establish a supported binding between those paths and published without observation.

### Occurrences
- Execution: SEED-066#preserve-existing-content / `979cac31fc19f756bdfc480d9b24f1ab7dfeec34:.planning/slice-plans/001-preserve-existing-content/PLAN.md` / 64173ad25fbbe7457705aeea972a959d9d3f8dd4
  - Timestamp: unknown (2026-10-03, slice 1 and 2 managed delivery)
  - Tool: Codex
  - Open Dough release: 0.3.54 (unchanged execution-checkout VERSION)
  - Evidence: coordinator had functions.exec/notify/yield_control and exec/write_stdin available, but invoked deliver without `--codex-bridge-available`; both receipts reported unobserved. `ci-host-bridge.mjs` tests that flag and describes binding as retained by caller; `ci-mailbox.mjs stream` creates a mailbox. Guidance prohibits a separate observer start for managed ordinary increments.
  - Observed effect: commits 64173ad25f and 6c2ed996f7 were accepted without a live observer, failure notifications, or a CI completion verdict. Local proof passed; remote CI success was never claimed.
  - Inference: the unavailable receipt reflects the omitted flag, not proof that host tools were unavailable. Clarify or provide one supported managed-delivery-to-yielded-stream binding before treating that flag as notification readiness. This is a process/integration gap, not a product defect; no workaround or guidance edit was made here.

## ODF-208 — The CI observer's worker exited without a terminal result, with no recorded cause

Former local code: DD-202.

Mid-execution, the delivery hook reported that the observer's worker had exited without recording a normal terminal result. No transport error or other cause was reported.

### Occurrences
- Execution: SEED-066#preserve-completed-speech / `2b34db5abb:.planning/slice-plans/001-keep-completed-speech/PLAN.md` / 4070f526bc
  - Timestamp: 2026-10-03T07:50:52Z (hook notice)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.54 (execution-checkout VERSION, unchanged during execution)
  - Evidence: PostToolUse hook "CI observer lost its worker for this coordinator: /tmp/dough-ci-501/watch-ZXmzAO (CI observer worker exited without recording a normal terminal result)"; coordinator then read results with `gh run list --branch claude/keep-completed-speech-as-dictation-continues`.
  - Observed effect: notification coverage ended for the published slices; the coordinator confirmed CI manually. No failure was missed.
  - Inference: the cause is unknown and is not established as the transient-network ending of ODF-121. The stale `dev.pid` seen minutes later (see ODF-190) suggests process churn on the machine, but no link is shown. Qualified: one occurrence.

## ODF-213 — Refinement planned a search to reproduce a flaky mock, but the project's test rules already forbade that mock

Former local code: DD-204.

A story about an intermittent CI failure in a module mock planned a bounded
search to reproduce it ("no fix without a reproduction"; mock changes excluded).
Nobody first checked whether the mocked module should be mocked under the
project's own test rules. The `unit-testing` skill says to mock only external
dependencies, and the mocked `useRecallData` is in-process state with setters.

### Occurrences
- Execution: SEED-039#mainmenu-mock-flake / `8ab270cae5:.planning/slice-plans/006-automocked-specs-pass-reliably/PLAN.md` / 74b20fdcac
  - Timestamp: 2026-10-03T18:54:53+08:00 (plan committed) through 2026-10-03T22:00:06+08:00 (owner rescope committed)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.56 (execution-checkout VERSION, unchanged during execution)
  - Evidence: plan premises and scope excluded "a change to the mocks without a reproduced cause". Slice 1 ran four owner-approved reproduction rounds: local instrumentation, a standalone suite with 1,940 runs, a Linux container with 23 shard runs, and a Vitest version diff. None reproduced the failure (Learnings in 99089600b4). The owner then asked "what is the feature it is testing? Does it have to be implemented in this way?" and said "this test is wrong from the very beginning". `.agents/skills/unit-testing/SKILL.md` already says "Do not mock unless external or exceptional". Slices 2–4 removed the mocks in about 30 minutes of agent work (74b20fdcac, c7944701f2, 330cd1820b).
  - Observed effect: about 107 minutes and about 630k tokens of reproduction-agent work (1,465 s + 674 s + 4,256 s by the hand-back records), three owner round-trips, and Docker cleanup, before a fix that needed no reproduction.
  - Inference: when refining a story about a failing test mechanism, check the failing mechanism against the project's test rules first. A mechanism the rules forbid is a removal story, not a reproduction search. Qualified: one execution. The reproduction search was owner-approved at each round, so the cost comes from how the story was framed, not from a broken execution step.

## ODF-214 — A slice plan told the implementer to replace a push/replace assertion with a current-location check, which drops the replace

Former local code: DD-206.

The plan said to assert the router's current location instead of a captured
`replace`/`push`. A location check cannot tell a replace from a push, so
following the plan weakened an assertion the same plan forbade weakening.

### Occurrences
- Execution: SEED-039#internal-mocks-to-real-modules / slice-plans/009-frontend-specs-run-real-internal-modules / a70529a3fb
  - Timestamp: unknown (slice 10 acceptance on 2026-10-04, about 09:55 +08:00)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.56 (execution-checkout VERSION, unchanged during execution)
  - Evidence: plan slice 10: "`AddRelationship.spec.ts` and `WikidataAssociationDialog.titleActions.spec.ts` assert `replace`/`push`; assert the current location instead." Plan Goal and scope: "without weaker assertions". The implementer reported the loss as a gap; the coordinator returned the slice, and the implementer added a history-position check (later `countHistoryEntriesAdded`), shown to fail for a push. Commit f7cc9a2293.
  - Observed effect: one extra implementation round (about 3 minutes, 76,592 subagent tokens in total for the slice's implementer). No weakened assertion was delivered.
  - Inference: when a plan swaps a mock observation for a real one, check that the real observation still tells apart every case the mock did. Qualified: one occurrence.

## ODF-110 — A plan's proof for a CLI key example checked only that the server answered, not the authenticated step the example needed

Former local code: DD-207.

The story's key example (DD-161) had an agent point the CLI at the held app and attach a PDF. The plan's proof row asked only that a CLI command get an answer rather than "Donut service is not available", and slice 4's guidance named the base URL but no access-token route.

### Occurrences
- Execution: SEED-069#observe-branch-code-against-real-services / slice-plans/004-hold-worktree-e2e-stack / ee414fbdf1
  - Timestamp: 2026-10-05, between about 12:33 and 12:36 +09:00 (slice 4 acceptance)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.56
  - Evidence: `b7ae807ce5:.planning/slice-plans/004-hold-worktree-e2e-stack/PLAN.md` proof row "The CLI reaches the held app"; slice 4 implementer report listed "No CLI token route" as a gap after using curl `generate-token` by hand; the coordinator returned the slice once, and `1ae39ec64f` adds the token route to `.agents/agent-map.md`.
  - Observed effect: one extra implementation round (about 49 s); caught only by reading the return's named gaps against the key examples.
  - Inference: a proof row that checks reachability can pass while the example's real shape (an authenticated write) stays unguided. Qualified: one occurrence.

## ODF-074 — A slice made the conversion step reject on failure; the existing catch also covered a save the story excluded

Former local code: DD-209.

The plan's Goal excluded "a failure while saving the note after conversion
succeeded", but slice 1 said only that the conversion callback rejects on
failure. In the code, one `try` covered both the conversion request and the
note save, so the plain reading made a save failure reject too.

### Occurrences
- Execution: SEED-066#recover-failed-transcription / `212e428968:.planning/slice-plans/005-recover-failed-transcription/PLAN.md` / 9eb06ed0bd
  - Timestamp: unknown (slice 1, before commit 2026-10-05T14:48:15+09:00)
  - Tool: Claude Code
  - Open Dough release: 0.3.56 (execution-checkout VERSION, unchanged during execution)
  - Evidence: coordinator summary to the retrospective (subagent transcripts not supplied): the first implementation rejected for any failure, including a save failure after a good conversion; the coordinator returned the slice. Plan at 212e428968, slice 1: "The conversion callback rejects on failure instead of resolving `undefined`"; Decisive premises read `useNoteAudioProcessing.ts` for the toast, not for what its catch covered. Delivered code in 9eb06ed0bd adds an inner `try` around `appendDictatedText`; plan Learnings record "Only a failed conversion rejects".
  - Observed effect: one extra implementation round; the rejected version would have let audio already joined to the note be sent and joined again after a save failure.
  - Inference: when a slice changes what a catch does, the plan could name which excluded paths that catch also covers. Qualified: one occurrence; the coordinator caught it at acceptance.

## ODF-215 — E2E startup fails on unformatted files, but implementers are told not to format

Former local code: DD-211.

The execution wrap-up formats once, by the coordinator, after the refactor.
The frontend dev server's checker (`frontend/vite.config.ts`, `checker({ biome: true })`)
runs Biome when the E2E stack starts, so an implementer whose slice proof is
an E2E run cannot run it on unformatted files.

### Occurrences
- Execution: SEED-066#recover-failed-transcription / `212e428968:.planning/slice-plans/005-recover-failed-transcription/PLAN.md` / ab773157bb
  - Timestamp: unknown (slice 3, before commit 2026-10-05T14:58:50+09:00)
  - Tool: Claude Code
  - Open Dough release: 0.3.56 (execution-checkout VERSION, unchanged during execution)
  - Evidence: slice 3 implementer learning, recorded in the plan's Learnings at ab773157bb: "E2E stack startup runs a Biome format check, so an unformatted frontend file stops `cy:run` before any scenario runs"; CLAUDE.md "implementers/refactorers run neither it nor standalone `lint:changed`".
  - Observed effect: the slice 3 E2E proof was blocked until files were formatted; extra time not recorded.
  - Inference: the rule and the project tooling conflict for any slice whose proof is an E2E run; the agent map or the slice delegation could say that formatting the touched files before `cy:run` is allowed. Qualified: one occurrence; how the implementer resolved it was not supplied.
- Execution: SEED-066#understandable-first-dictation / `0662bac730:.planning/slice-plans/008-complete-a-first-dictation-with-understandable-controls/PLAN.md` / 31b9b7b7bb
  - Timestamp: unknown (slice 1, before commit 2026-10-06T09:18:48+09:00)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.56 (execution-checkout VERSION)
  - Evidence: slice 1 implementer return: the first mocked-journey run failed on a Biome line-length error in `NoteToolbar.panels.spec.ts`; it was rewrapped by hand. The coordinator then told later implementers they may run `biome check --linter-enabled=false` on their files, and the slice 3 and slice 4 E2E runs passed first time.
  - Observed effect: one failed E2E run in slice 1; extra time not recorded.
  - Inference: a read-only format check of touched files before `cy:run` avoided a repeat without breaking the coordinator-only formatting rule. Qualified: the delegation prompt supplied it, not the guidance.
- Execution: SEED-066#no-record-while-stopping / `b63b742f8a:.planning/slice-plans/010-keep-the-dictation-result-true-while-stop-finishes/PLAN.md` / 2c30afdd26
  - Timestamp: 2026-10-07T13:47:02+09:00 (SUT readiness failure in `sut.log`)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.57 (execution-checkout `.agents/skills/dough-update/VERSION`)
  - Evidence: slice 1, implemented by the coordinator; first `pnpm cy:run --spec e2e_test/features/note_creation_and_update/record_live_audio.feature` stopped with "SUT readiness failed" because `frontend:sut` reported "Formatter would have printed the following content" for `NoteAudioTools.retry.spec.ts`; after `biome format --write` on the three touched frontend files the rerun passed (2 scenarios). The plan's proof commands did not mention it; the plan's Learnings now do.
  - Observed effect: one failed E2E stack start and a log read before the rerun; about one minute.
  - Inference: the third occurrence in three SEED-066 executions; the guidance or the agent map stating "format touched frontend files before `cy:run`" would remove it. Qualified: the coordinator, not a delegated implementer, hit it here.

## ODF-216 — Landing retired the execution workspace while its final slice still needed a continuation

Former local code: DD-212.

Landing and story completion were correctly distinguished in the response, but
the unfinished execution's saved workspace and branch were removed. The story
remained Taken while its terminal session later stopped.

### Occurrences
- Execution: SEED-069#reliable-development-stack-lifecycle / `404892b54af9d40804a0309c8bd92204d41c3e7d:.planning/slice-plans/006-stop-and-restart-development-stack/PLAN.md` / 4101f03be4
  - Timestamp: 2026-10-05T17:05:29+09:00 (retirement)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: unknown
  - Evidence: native session `26f67388-7040-4fff-b68a-786f5f7e7b95` landed `77df78ed15` after the owner's `/dough-land`, then received `worktree: removed`, `branch: removed`, `remoteBranch: removed`. Its completion message explicitly retained slice 4 and wrap-up; the saved session cwd and dashboard start still name the removed worktree. Dashboard `doneAt` is 2026-10-05T21:52:08+09:00; native job state is `stopped`. The owner subsequently asked why the Taken story had neither a worktree nor an attached session.
  - Observed effect: an investigation and takeover were needed to recover the remaining real-stack check and closure. Implementation was preserved on main; no code or data was lost.
  - Inference: when an approved proof runs after landing, the workflow could retain a usable continuation checkout and its remaining obligation before retiring the execution workspace. Qualified: one execution; who marked the dashboard session done is not established.
- Execution: SEED-066#prompt-dictation-results / `a4c70254e7:.planning/slice-plans/007-see-submitted-dictation-promptly/PLAN.md` / dd875812e5
  - Timestamp: 2026-10-06T11:12:06+09:00 (owner's merge b86f649322, pushed to `main` and the story branch)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.56
  - Evidence: PLAN.md slice 3 was "after landing on `main`" (Development measurement). After slice 2 the coordinator stopped, reported the execution unfinished, and named `/dough-land` as the next step. The owner answered "not sure landing is the right action. Just sync to main." A merge-and-push to `main` was then refused by the session's permission classifier, and the owner ran it by hand. Slice 3 continued in the retained worktree and was published as f50c7dcef6.
  - Observed effect: one extra stop, one owner question, one refused command, and one manual owner command. The workspace was kept, so there was no takeover.
  - Inference: the same gap as the first row, avoided this time by the owner. The workflow names no step that puts a story's code on trunk while keeping the execution workspace for a post-integration slice. Matching uncertainty: here the plan, not landing, put a slice after integration.

## DD-213 — A worktree landing's default-checkout refresh stopped as diverged because the owner committed there during the landing

A preparation keep landed from a worktree while the owner's own session
committed on the default checkout's `main`. Publication succeeded; the refresh
correctly stopped (`diverged`) and left the owner's commit untouched, and the
owner's session then had to rebase and push it. Every skill script in the
landing session returned `ok`; no script failed. The owner read the attention
message as a script failure.

Follow-up: Open, unqueued.

### Occurrences
- Execution: SEED-066#create-with-spoken-title / preparation keep through Dough Land / 2e3a630c1a
  - Timestamp: 2026-10-06T09:17:24+09:00 (owner commit 5ec0d443f4 on the default checkout), 2026-10-06T09:18:28+09:00 (landing commit, pushed and accepted before 09:19), 2026-10-06T09:20:04+09:00 (owner's rebase onto 2e3a630c1a, now a2bf875430 on `main`)
  - Tool: Claude Code
  - Model: claude-fable-5-1
  - Open Dough release: 0.3.56
  - Evidence: `preparation-assignment.mjs release`, `agent-commit.mjs`, `queued-closure-check.mjs` (`clear`), `git push`, and `worktree-retirement.mjs` (`ok: true`) all succeeded; refresh inspection found the default checkout clean on `main` with 5ec0d443f4 not contained in fetched `origin/main` and `origin/main` not contained in `HEAD`; the default checkout's reflog shows `rebase (start): checkout origin/main` at 09:20:04. The dashboard report was `completed` with that refresh as the attention message. Side note: `dashboard-completion.mjs --help` answers "Completion delivery was not acknowledged: Malformed reporting arguments" and exits 1; it has no usage text.
  - Observed effect: one owner rebase and push, and one attention message that was read as a failure. No work was lost and nothing was retried.
  - Inference: the refresh rule behaved as designed; the cost came from two writers on the same `main` within three minutes. The attention wording could say plainly that the stop is expected when the owner has a local commit, and what the owner's next command is. Qualified: one occurrence.

## DD-214 — The coordinator accepted an interim "Record shows again during Stop" note; a later slice made it a wrong-result race

An implementer named an interim behavior as a learning. It was accepted as
harmless without checking it against the story's examples, and later slices
built the result status on top of it.

### Occurrences
- Execution: SEED-066#understandable-first-dictation / `0662bac730:.planning/slice-plans/008-complete-a-first-dictation-with-understandable-controls/PLAN.md` / 31b9b7b7bb
  - Timestamp: unknown (slice 1 acceptance, before commit 2026-10-06T09:18:48+09:00)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.56 (execution-checkout VERSION)
  - Evidence: slice 1 return "while Stop is still finishing, the main action already shows Record again", recorded in that plan's Learnings. At 9f8a52c7, `NoteAudioTools.vue` `startRecording` clears the passage saves, and the pending `stopRecording` `finally` overwrites `phase`. The correction was planned as `b63b742f8a:.planning/slice-plans/010-keep-the-dictation-result-true-while-stop-finishes/PLAN.md`.
  - Observed effect: none in use; the retrospective found it in the code. Pressing Record during "Turning your speech into text…" can report "No speech was turned into text." for added text, or hide Stop while recording.
  - Inference: proof acceptance reads named gaps against the goal. A named interim that later slices depend on needs the same reading when those slices are accepted. Qualified: one execution. In the same execution the coordinator did return slice 3's self-declared superseded-save gap for a same-slice fix.

## DD-215 — The coordinator told the owner that paid runs were already authorized when they were not

The plan required the owner's go-ahead for every paid run. After two
separately approved batches, the coordinator wrote that the next batch was
"within the go-ahead you already gave".

Follow-up: Open, unqueued.

### Occurrences
- Execution: SEED-066#prompt-dictation-results / `a4c70254e7:.planning/slice-plans/007-see-submitted-dictation-promptly/PLAN.md` / dd875812e5
  - Timestamp: unknown (2026-10-06, between about 09:00 and 09:30 +09:00, before dd875812e5)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.56
  - Evidence: the owner approved slice 1's ten calls and then a fifteen-call model probe, each through its own question. After delegating slice 2, the coordinator's message said it would measure slice 2 "within the go-ahead you already gave". On the implementer's return it noticed the error, said so, and asked; the owner approved before any slice 2 paid call was made.
  - Observed effect: no unapproved call was made; one corrected statement to the owner.
  - Inference: a per-run approval rule is easy to stretch across batches in a long session. Qualified: one occurrence, caught by the coordinator itself.

## DD-216 — Managed delivery stopped with a misleading rebase error when given an abbreviated previously-published base

`execution-increment-delivery.mjs deliver --previously-published-base`
compares SHAs as strings. An abbreviated SHA never equals the fetched remote
tip, so delivery tried a no-op rebase and stopped with "rebase left the
pre-rebase SHA as the candidate".

Follow-up: Open, unqueued.

### Occurrences
- Execution: SEED-066#prompt-dictation-results / `a4c70254e7:.planning/slice-plans/007-see-submitted-dictation-promptly/PLAN.md` / dd875812e5
  - Timestamp: 2026-10-06, shortly after 11:18:17+09:00 (commit f50c7dcef6)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.56
  - Evidence: `deliver --previously-published-base b86f649322 --target-ref refs/heads/claude/see-submitted-dictation-promptly` failed. The coordinator read `execution-increment-publication.mjs` (`remoteTip !== previouslyPublishedBase` makes `reconcileOnto` run; the guard at line 75 throws). A rerun with the full SHA was accepted as f50c7dcef6.
  - Observed effect: one failed call, nothing published, and three source-reading calls.
  - Inference: the usage text says only `--previously-published-base SHA`. Resolving the argument to a full SHA, or naming the cause in the error, would avoid the lookup. Qualified: one occurrence; earlier deliveries in this execution passed full SHAs from receipts.

## DD-217 — Tooling slices sized at 5–8 minutes ran 10.5–12 minutes because real proof needed fresh disposable worktrees

Plan sizing counted the code change but not the required real proof. Each
real hook proof created and prepared a disposable worktree, then ran several
real hook invocations. The slices converged with complete proof, so they were
recorded as overruns rather than refined.

Follow-up: Open, unqueued.

### Occurrences
- Execution: SEED-070#fast-warning-free-commit-hook / `b1dab1d7ed:.planning/slice-plans/011-fast-warning-free-commit-hook/PLAN.md` / f53bee8004
  - Timestamp: 2026-10-07, about 21:20–22:25+09:00 (slices 1 and 6 delegations)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: unknown
  - Evidence: slice 1 implementation agent took 628 s and slice 6 took 714 s by the host task duration (plan budget 5–8 min, hard limit 10). Slice 6 found a pre-existing lockfile rewrite during its real negative proof and fixed the hook too. Slices 4 and 7 with similar proof took about 5–6 min.
  - Observed effect: two hard-limit overruns, recorded in the plan; no retry or revert.
  - Inference: when a slice's proof needs a fresh prepared worktree and many real tool runs, sizing could count that setup separately. Qualified: one execution; the slice 6 overrun also included an unplanned defect fix.
