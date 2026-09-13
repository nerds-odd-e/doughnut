# Receive a web note rename locally

Status: in progress
Source: [SEED-009 story 22](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-22)
Created: 2026-09-13
Authority: execution authorized by the owner on 2026-09-13.

## Execution identity

- Originating checkout: `/Users/terryyin/git/doughnut`
- Originating branch: `main`
- Claim commit: `0934f3adfb5419d91029016b1314afbb326eeae7`
- Execution checkout: `/Users/terryyin/git/doughnut-worktrees/121-receive-web-note-rename`
- Execution branch: `quick/121-receive-web-note-rename`
- Integration target: `main`
- CI observation: unavailable for this execution branch; `.github/workflows/ci.yml`
  (`donut CI`) triggers pushes only on `main`.

## Goal and boundary

The owner frequently renames notes on the web and needs local pull to receive
those changes. From a synchronized notebook and clean checkout at A, rename
`Biology/Cells.md` on the web to `Cell structure`, appending accepted B. Pull
receives `Biology/Cell structure.md` and removes the old path, with authored
content retained and local HEAD clean at B. A remains an ancestor. The web
note ID, URL, and learning data remain unchanged during rename.

The story ends at pull. Local edit and publish afterward, local rename inference,
moves, folder/notebook rename, trash, concurrent local work, divergent history,
cross-notebook synchronization, drift repair, and performance work are deferred.
Do not change sibling scope or backlog order. Root and existing-folder placement
use the same rule; fixture counts and placements do not justify new gates.

Preserve existing title authorization and validation, reference-choice behavior,
body/YAML, and web behavior outside the promised synchronized starting state.
When rename rewrites same-notebook references, receive them in the same snapshot.
Existing web rewrites in other notebooks remain intact; publishing histories for
those notebooks is outside this delivery. No new rename option or local metadata.

## Architecture and PFE findings

Read the ADR index and record statuses: 0000, 0001, 0003–0007 are Accepted;
0002 is Proposed. No status conflicts or supersession were found. Relevant
constraints are [0001 — Ubiquitous language](../../../docs/adrs/0001-ubiquitous-language.md),
[0004 — Portable Markdown](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
(filename is title; authored body/YAML and reference meaning remain authoritative),
[0005 — Web routes](../../../docs/adrs/0005-web-routes-accepted.md)
(URLs retain server note IDs), [0003 — Scheduling](../../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md)
(retain tracker/log state), [0006 — Failure handling](../../../docs/adrs/0006-failure-handling-accepted.md)
(propagate unexpected failures), and [0007 — Isolation](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
(tests use disposable owned data). No ADR change or exception is proposed.
Append-only history is the owner's explicit direction, not authority inferred
from Proposed ADR 0002.

The established North Star topic in SEED-009 governs portable trash; it does
not require a new rename architecture. Its preservation principle applies.
PROJECT.md's older Git priorities do not supersede the current backlog or this
conversation. No new North Star file/topic is warranted: current structure
supports this bounded change.

Paths below are relative to the repository root.

| Responsibility | Evidence and selected use |
| --- | --- |
| Web rename semantics | `controllers/TextContentController.updateNoteTitle` owns reference-choice validation and dispatch; `services/WikiLinkRewriteService`, `TitleRenameWikiLinkRewrite`, and `WikiLinkRewriteSupport` own reference capture/rewrite and derived indexes. Move the title orchestration into the shared web note editing owner, preserving these collaborators. Do not copy rewrite logic or replace it with Git inference. |
| Accepted existing-note edits | `services/notebookGit/WebNoteContentSaveService` locks/reloads the notebook, checks accepted-tree consistency before mutation, applies the edit, suppresses unchanged tree commits, and appends a complete snapshot. Generalize this existing owner to `WebNoteEditService` for content and title operations; retain one private edit lifecycle, with small named entry points. Remove the obsolete content-only owner rather than leaving forwarding layers. |
| Snapshot persistence | Directly reuse `AcceptedSnapshotPersistence`, `NotebookGitBundleBuilder`, and `NotebookGitBundleWriter`: append to the accepted parent and save head/bundle together. No second history writer, event journal, rename metadata, or filesystem identity mapping. |
| Portable representation | Reuse `PortableTreeSnapshot` and `NotebookExportRows` for the complete post-edit notebook tree. A changed title naturally removes one old path and adds the new one. Snapshot all notes after reference rewriting; do not construct a rename-only patch or special referrer commit. |
| Note creation | `WebNoteCreationService` already shares snapshot persistence but changes note membership and checks represented destinations. Keep its construction lifecycle distinct; this story does not justify absorbing creation into an edit framework. Shared append logic remains in one owner. |
| Local receipt | `cli/src/commands/notebook/notebookPull.ts` downloads accepted history and fast-forwards ordinary ancestor checkouts. `cli/tests/notebookPull.fastForward.pathReceipt.ts` already exercises renamed paths, exact bytes, clean state, and retained ancestry through `run`. Reuse this behavior unchanged unless the real web-origin proof exposes a gap. No CLI rename detector. |
| Export and import alternatives | ZIP export shares the Portable representation but cannot advance the existing local Git history. `NotebookGitProposalImporter` infers local proposals; a web rename already has the server entity, so routing the web action through proposal inference is the wrong responsibility. |
| Outside-in proof | Existing title controller tests cover learning retention, reference choices, YAML rewriting and permission failures. Web content controller tests provide committed-transaction and downloaded-bundle helpers. CLI acquisition E2E provides actual installed CLI clone/pull and exact-file assertions. Reuse these boundaries, not helper-level mocks of the new lifecycle. |

### Cohesive lifecycle

The common domain rule is: apply an existing-note web edit to current notebook
state, then append the resulting Portable snapshot if the previously consistent
accepted tree changed. The operation owns its title/content business rules;
the shared lifecycle owns lock, accepted-state comparison, and snapshot timing.

1. Receive note/notebook IDs at the service boundary. Start the existing
   serializable transaction there, lock the binding, reload the authoritative
   note, and check authorization/membership. Do not edit the stale controller
   path-variable entity after locking. Remove the title controller's outer
   transaction when moving ownership so it cannot silently weaken isolation.
2. Import/validate accepted head and capture the pre-edit consistency result
   before changing title or referrers. Keep existing content-save behavior for
   missing bindings and pre-existing drift; no baseline repair or new rejection
   policy. Preserve web rename for unbound/drifted notebooks as well.
3. Apply existing rename/reference-choice semantics exactly once. Keep reference
   capture before title mutation. Build the response from the returned current
   note, through the existing NoteRealmService.
4. Build the complete post-edit snapshot and append through existing persistence
   only when the pre-edit accepted tree matched and the resulting tree changed.
   Same-title saves do not produce a tree-change commit. Same-notebook referrers
   participate through ordinary current-note rows, including YAML links.
5. Live edits, derived indexes, and accepted head/bundle share the transaction.
   No async publication, compensating rollback, retry protocol, or partial
   success response. Propagate unexpected failures under ADR 0006.

Use a private operation callback only if needed to express the two known edits;
no public mutation registry, operation hierarchy, configurable policies, or
speculative support for moves. Whole-notebook construction has existing cost;
do not introduce a cache or promise 10,000-note performance here.

Cross-notebook reference rewriting is an existing wider web behavior. Do not
restrict its candidate set to make the snapshot simpler. Only the selected
notebook's accepted history is newly advanced by this story. If implementation
evidence shows existing behavior cannot be retained with that boundary, return
the concrete conflict before adding multi-notebook lock/publication machinery.

## Proof and evidence limits

Code and test assertions were inspected; product tests were not run in planning.
The observed gap is concrete: `updateNoteTitle` currently mutates live notes
without calling accepted snapshot persistence. Existing CLI path-receipt tests
supply an already-published bundle, so they do not prove this web-origin journey.

Use the real web title action as the trigger. Snapshot fixture setup is allowed
only before cloning/renaming. Never invoke testability snapshot refresh after
rename: that would supply the missing product behavior on its behalf.

No new storage engine, schema, or transaction mechanism is proposed. Reuse the
existing JGit bundle and Spring/database transaction evidence; no separate
infrastructure experiment is needed. Verify real persistence via the controller
and downloaded bundle after clearing/reloading state. Do not mistake an outer
rollback-only test transaction for proof of the service's committed boundary.

| Promise | Proof owner |
| --- | --- |
| Real web rename reaches a clean local checkout; old path absent, exact authored content, retained ancestry, clean accepted HEAD | Slice 3 installed CLI/browser E2E |
| Appends from the previous accepted parent; stored and downloaded history agree | Slice 2 controller/downloaded-bundle assertions |
| Server note identity and learning data survive rename | Existing `TextContentControllerUpdateNoteTitleTests`; Slice 2 adds bound-notebook observation using the same fixture idiom |
| Same-notebook reference choice reaches local files, including authored YAML semantics | Slice 4 referenced-rename E2E plus existing inbound-reference controller tests |
| Missing required reference choice and unauthorized rename preserve state/history | Slice 5 bound controller rejection assertions; existing title tests retained |
| Same-title produces no changed accepted tree/head | Slice 5 bound controller no-op assertion |
| Content saves, unbound rename, and existing web reference semantics retained through restructuring | Slice 1 existing controller suite; Slice 2 focused bound/unbound/drift observations only where shared lifecycle changes coverage |
| No new local IDs or rename-specific transport | Existing CLI exact-tree tests and Slice 3 exact-file E2E; review production diff |

## Ordered slices

### 1. Share the existing web note edit lifecycle
Type: Structure
Status: done
Enables: immediately following web rename receipt Behavior.
Change: Generalize WebNoteContentSaveService to WebNoteEditService and keep
its existing content entry behavior. Extract only its known existing-note
lock/mutate/compare/snapshot sequence into a private shared operation, for
the immediately following title entry. Preserve missing-binding, drift,
unchanged-content and append-only behavior; update current callers and remove
the old owner. Keep AcceptedSnapshotPersistence and creation's lifecycle intact.
Proof: Existing content-save/history controller tests remain green under the
required full backend suite. No new helper-level tests or public test seams.
Sizing: 5 minutes active work hypothesis; full backend verification may exceed
the target as the explicit required-test wait exception. If extraction reveals
independent concepts or exceeds 10 active minutes, stop and refine this plan.
Safe stop: content saves behave as before; web rename still has existing behavior.
Execution proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed
after implementation and again after the independent refactor on 2026-09-13.

### 2. Append a web note rename to accepted history
Type: Behavior
Status: done
Behavior: Given a synchronized bound notebook, rename a learned ordinary note
with no inbound references using the web title action; accepted history appends
the complete renamed Portable tree from the previous accepted parent while the
note's server identity and learning data remain unchanged. This example does not
restrict rename to unreferenced notes.
Change: Move existing title orchestration into the shared editing owner; remove
its duplicate controller ownership, preserve rename collaborators, and let the
shared snapshot capture the result. Keep title/content API signatures unchanged.
Proof: Add one bound controller example of the persisted/downloaded renamed tree,
original accepted parent, and retained tracker/log data; use committed transaction
support, not a test-only snapshot after rename. Preserve author-owned YAML and H1
in the fixture bytes.
Sizing: 5 active minutes plus the required full backend-suite wait. Resume from
the parked backend production/controller-test portion of the oversized attempt;
do not repeat the already passing CLI regression proof because it does not own
this slice.
Safe stop: web renames durably advance accepted history; local pull receipt is
the immediately following behavior.
Execution proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed
with 2,418 tests after implementation and again after the independent refactor
strengthened before/after learning-data assertions on 2026-09-13.

### 3. Receive the accepted web rename through ordinary pull
Type: Behavior
Status: done
Behavior: Given a clean installed-CLI checkout at accepted A and the bound web
rename appended as B, ordinary pull yields the renamed file at B, removes the old
path, preserves exact authored content, retains A as an ancestor, and leaves the
checkout clean.
Change: Add one actual web rename → installed CLI pull scenario using existing
clone/rename/pull/exact-file steps. Reuse the backend behavior from Slice 2 and
the existing CLI pull unchanged unless the real journey exposes a product gap.
Preserve author-owned YAML and H1 in the fixture bytes.
Use `e2e_test/features/cli/cli_notebook_web_created_note.feature` alongside its
existing acquisition scenarios, broadening the feature title to web note changes
if needed; this assessed active spec avoids adding another E2E harness or changing
worktree spec routing. No local edit/publish step after pull.
Proof: The installed-CLI E2E owns the exact local tree, pulled HEAD, ancestry,
and cleanliness. Reuse the already passing focused CLI regression proof from the
oversized attempt while its boundary remains unchanged; rerun only if resumed
changes invalidate it.
Sizing: 5 active minutes plus the required E2E wait. Resume from the parked E2E
portion of the oversized attempt, whose last assertion correction remains to be
run. If further harness repair reaches 10 active minutes, stop and reassess the
story boundary rather than adding another receipt mechanism.
Safe stop: the selected journey is green end to end; leave no failing scenario
as a commit boundary.
Execution proof: `CURSOR_DEV=true nix develop -c pnpm cli:test --
tests/notebookPull.test.ts` passed with 448 tests in the oversized attempt and
remained reusable because no CLI source changed. `CURSOR_DEV=true nix develop -c
pnpm cy:run --spec e2e_test/features/cli/cli_notebook_web_created_note.feature`
passed all 7 scenarios after the restored fixture bytes were aligned with the
accepted Portable representation on 2026-09-13.

### 4. Receive the selected reference rewrite with the renamed note
Type: Behavior
Status: planned
Behavior: Given a synchronized notebook with a same-notebook referrer, rename
through the existing web action with a reference-handling choice; pull receives
the rename and rewritten authored references together with the selected display
semantics. Use a Scenario Outline for KEEP_VISIBLE_TEXT / UPDATE_VISIBLE_TEXT,
with body and YAML references in the same carrier document.
Change: Reuse the title operation and complete snapshot from Slice 2. Verify the
post-edit snapshot observes the mutated current referrer rows; correct that
boundary only if proof exposes stale state. Do not introduce a referrer-specific
publisher, per-reference commit, extra snapshot pass, or new reference policy.
Naturally correct behavior may require only the new executable examples.
Proof: Existing referenced-title controller tests own rewrite syntax/derived
indexes. Extend the same installed-CLI E2E spec with actual UI choice → pull →
exact reference-file assertions. The downloaded accepted commit contains both
path and reference changes; the local checkout stays clean at that commit.
No fixture snapshot after the trigger, and no subsequent local publication.
Sizing: 5 active minutes plus required E2E/backend waits if production changes.
Safe stop: referenced web renames are received under both existing choices;
existing broader web referrer behavior is retained.

### 5. Keep accepted history unchanged when no title change is accepted
Type: Behavior
Status: planned
Behavior: Given a bound notebook, an unchanged-title save or a title action
rejected by existing authorization/reference-choice rules leaves the accepted
head and bundle unchanged. A rejected action also retains live title/referrer
content. Unchanged-title timestamp semantics remain existing web behavior.
Change: Use the common pre-mutation checks and post-edit tree comparison. Correct
only an exposed ordering/transaction defect; add no catch/retry policy or history
entry for rejected/no-op requests. No need to invent new collision validation.
Proof: Extend bound title controller coverage with focused no-op, missing-choice,
and denied-owner examples, asserting each delta after reloading persisted state.
Reuse existing title validation tests instead of repeating their input matrix.
These business outcomes are distinct from testing an allowed loud exception.
Sizing: 5 active minutes plus the required full backend suite wait.
Safe stop: failed/no-op requests cannot publish a spurious rename; successful
receipt from earlier slices remains green.

## Verification and delivery

Repository commands for execution (not run by this planning task):

```bash
CURSOR_DEV=true nix develop -c pnpm backend:test_only
CURSOR_DEV=true nix develop -c pnpm cli:test -- tests/notebookPull.test.ts
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_web_created_note.feature
```

The backend rule requires the complete backend suite, not a selected test class.
Reuse existing CLI path receipt rather than duplicating it. E2E wrapper owns its
disposable stack; use isolated worktree resources if concurrent execution is
active. Required backend/E2E startup and runtime are explicit timing exceptions;
record active work separately from waiting, never waive a failing check.

For each executed slice follow dough-execute-plan: Jidoka, fresh independent
dough-post-change-refactor agent, API generation only if a public signature/type
actually changes, one coordinator `./scripts/run.sh pnpm format:changed`, plan
update, commit with check-only lint hook, push and asynchronous CI handling.
No routine second formatting pass or standalone lint:changed. Keep plan and
proof through retrospective/wrap-up. Do not modify the concurrently prepared
story-31 plan or its seed section. Recheck current shared code before execution.

## Refinement assessment and remaining concerns

Refined in place on 2026-09-13 after the first Slice 2 attempt exceeded the
10-minute active-work hard limit. The attempt stopped at about 11 active minutes,
excluding required test waits. Its combined backend publication and installed-CLI
receipt assumption proved false: these required independent controller and E2E
proof loops, and adapting both fixture boundaries caused the overrun. The exact
CLI regression command passed; the backend retry was blocked while the worktree
E2E runner owned test resources, and the final E2E assertion correction was not
rerun. All six attempt-owned files are safely parked in stash object
`b40094d7602aa4a31771a1d88b3603cc0814e1a7`.

Replaced the unfinished combined Slice 2 with separate accepted-history and
local-receipt Behaviors, followed by the existing reference-choice and
unchanged-history Behaviors. Five slices total, including completed Slice 1;
no story resplit is recommended. Proof mappings moved with their owning
behaviors, and the parked compatible work/proof must be reused rather than
repeated.

Cumulative assessment: all examples exercise one web existing-note edit
lifecycle and one complete Portable snapshot rule. Later slices must not add
recognizers or restrict the earlier examples' naturally supported behavior.
Slice 1 has an immediate consumer; no structure is reserved for deferred moves
or multi-notebook publication. Local pull remains ordinary Git fast-forward.

Slices 2 and 3 are now green and delivered from their separate proof loops. The
oversized attempt's compatible backend and E2E work has been assimilated; stash
`b40094d7602aa4a31771a1d88b3603cc0814e1a7` is no longer needed after Slice 3
delivery. Another hard-limit overrun would trigger story-boundary reassessment
rather than further routine subdivision. The later slices have no additional
specific decomposition concern identified by this assessment.

Cross-notebook reference histories remain outside the promised local receipt;
their existing web rewrite behavior must survive. Preserve the recorded boundary
unless concrete execution evidence requires returning a scope conflict. Product
execution still requires authorization; this plan does not supply it.
