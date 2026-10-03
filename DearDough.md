# DearDough Process Findings

Retained material shared-process findings, reviewed 2026-10-03. Only an explicit
queued follow-up is planned work; other entries are open and unqueued. A retained
released response is not proof of effectiveness. Unknown provenance stays unknown.
[Response status](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md).
Full pre-trim evidence: `8830c682704aac3bbb34bf9b1204da8feba042ca:DearDough.md`. Older narratives live in Git, not a second archive.

- Highest allocated local number: 203. Removed local codes are never reused.

## ODF-110 — A readiness replay observed only the plan's named seam, not the rest of the slice's journey

Former local code: DD-109.
Former local code: DD-163.
Former local code: DD-168.
Former local code: DD-171.

A replay resolves the named readiness seam without exercising the rest of the slice's promised journey, leaving a later operation to force a scope stop.

Follow-up: Open, unqueued. Responses 2c5ff71 (0.3.43) and fcc29fad (0.3.48) are released; the same class of unobserved premise is reported again on 0.3.51 and 0.3.52, so neither is shown to resolve it.

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

Follow-up: Open, unqueued. Responses 2c5ff71 (0.3.43) and fcc29fad (0.3.48) are released; the same class of unobserved premise is reported again on 0.3.51 and 0.3.52, so neither is shown to resolve it.

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

Follow-up: queued, not resolved: [Prove a slice through the consumers of what it changes](../open-dough/.planning/seeds/SEED-095-slice-proof-through-consumers.md#prove-slices-through-consumers) — SEED-095#prove-slices-through-consumers.

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

## ODF-190 — The plan prescribed production observations whose access route or log source did not exist, and whose results could not change the approach

Former local code: DD-145.

Planning names an observation route or log source that is not available to the executing project, causing owner probing or loss of the intended proof.

Follow-up: Open, unqueued.

- Execution: SEED-051#story-1 / `f35fa810f1:.planning/slice-plans/009-retire-note-embeddings/PLAN.md` / 5003fbecc8; Timestamp: 2026-09-28T05:36:39Z–06:03:39Z (slice 1/3) and 2026-09-28T07:17:43Z–07:18:10Z (slice 10); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.45. - Evidence: four auto-mode denials while seeking a DB route (credential lookup, SSH to the app VM, probe edit to root, bucket IAM); owner: "But this wasn't needed uh, previously. Um, or can we skip this?"; coordinator's covering reasoning and skip recorded in d111968b62; slice 10 `gcloud logging read` found no Flyway lines, so sustained health became the D/P evidence (8e03ac5f5f). - Observed effect: about 27 minutes of owner-attended probing ended in skipping slice 1's SQL part and dropping slice 3; slice 10's named proof was replaced during delivery. No product defect. - Inference: planning could have asked, for each production observation, whether any result would change the approach, and whether the access route and log source exist (both checkable cheaply once `gcloud` auth worked). Related to DD-142 (prescribed observations dropped in execution), but here the cost was production access and owner time. Qualified: one execution; planning-time `gcloud` auth had failed.
- Execution: SEED-059#story-5 / slice-plans/053-pdf-smooth-scroll-after-choosing-block / 5989892325; Timestamp: 2026-09-29T21:45+08:00 through 22:10+08:00 (slice 1 attempts); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.47. - Evidence: plan 053's proof row "Manual observation … Chromium DevTools protocol or Playwright `mouse.wheel`" on the dev stack; the repo has no Playwright/Puppeteer, `.agents/agent-map.md` says linked worktrees refuse the persistent Development stack, and a throwaway Cypress spec sending CDP `mouseWheel` reached the DOM but scrolled 0 px in headless Electron and Chrome. - Observed effect: about 15 minutes of implementer time; the story's key example (every wheel step moves down) was delivered without its real-wheel observation. - Inference: same pattern in a local setting: the observation route could have been checked at planning from the agent map and `package.json`.
- Execution: SEED-066#preserve-completed-speech / `2b34db5abb:.planning/slice-plans/001-keep-completed-speech/PLAN.md` / 4070f526bc; Timestamp: 2026-10-03T07:02:48Z–07:32:53Z (owner question), 07:32:54Z–07:57:43Z (primary checkout detached and restored); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.54 (execution-checkout VERSION, unchanged during execution). - Evidence: plan slice 1 states Development runs only from the primary checkout because linked worktrees refuse it, but slice 4 (the same paid journey on branch code) names no route; coordinator AskUserQuestion "Switch primary briefly / Defer / Drop"; primary detached at fd904bc2 and 1a981673, then restored to `main`, where `dev:restart` failed on a stale `dev.pid` (PID 597 reused by `accountsd`). - Observed effect: about 30 minutes waiting for the owner mid-execution, a shared checkout temporarily on branch code, and about 7 minutes restoring Development. - Inference: slice planning could have asked how a real-service proof reaches unmerged branch code and settled it with the owner before Take. Related to ODF-083 (shared checkout moved by an execution).

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

## ODF-202 — Managed Codex delivery left CI unobserved without a retained stream binding

Former local code: DD-201.

Follow-up: queued, not resolved: [Keep CI observed for Codex and Cursor executions](../open-dough/.planning/seeds/SEED-094-ci-observation-for-codex-and-cursor.md#observe-ci-on-codex-and-cursor) — SEED-094#observe-ci-on-codex-and-cursor.

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
