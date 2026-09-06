# Publish several note changes in one commit

## Source and status

- Source: [SEED-009 Story 11](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-11).
- Status: executing; leaves 1–4 complete, leaf 5 next.
- Readiness: remaining leaves ready for direct execution after refinement.

## Goal and scope

An authenticated notebook owner uses `donut notebook publish <directory>` to
publish related note additions, optionally accompanied by edits, as one authored
Git commit. Donut displays the whole accepted change while preserving existing
note identities and giving additions fresh identities.

The checkout is clean, bound, on `main`, and exactly one single-parent commit
ahead of the current accepted head. Current remote Portable content matches
that accepted parent. Changed files are regular typed Markdown notes at the
notebook root or in folders already represented in accepted history. Retain
the existing ownership, filename, content-validation, collision, stale-head,
drift, retry, and lossless Portable-content policies from Stories 2 and 4.

Accept the exact commit and every note change together, or neither. A rejected
proposal identifies the reason and offending path where applicable, retains
local work, and does not change remote notes or accepted history. Already
accepted retries create no duplicate notes or commits. Existing clone/pull and
note views expose the accepted result.

**Excluded:** edits-only multi-note commits (the seed's conservative assumption),
deletions, renames/moves, new folders, README changes, attachments, multiple
unpublished commits, divergence/rebase/conflicts, web structural synchronization,
web-save batching, drift repair, direct standard-Git remotes, bulk-import
optimization, preview UI, partial publication, and automatic commit splitting.
Single-note edits/additions remain supported. Reject unsupported members rather
than skipping them. Do not introduce an arbitrary file-count limit.

## Outside-in examples

1. An eligible checkout has `Physics/` in its accepted tree. Commit valid
   `Physics/Force.md` and `Physics/Inertia.md` together; publish → both are fresh
   notes in that folder at the exact authored commit. A second clean checkout
   can pull both files.
2. An eligible checkout has a learned `Physics/Motion.md`. Commit a new Force
   note with an edit to Motion mentioning it; publish → both changes are visible,
   Motion retains its learning history, and Force has a fresh identity.
3. Add a third note missing `type` to that mixed proposal; publish → the invalid
   path/reason is reported, neither valid change is published, and the local
   commit remains available for correction.
4. Commit a valid root addition and `New Folder/Idea.md` without an accepted
   parent folder; publish → neither the folder nor either new note is created.

The learning checkpoint is a small related local editing session and one rejected
invalid member. The installed-CLI example and committed-state rejection proof
provide executable evidence. Trying a personal notebook is subsequent product
feedback, not a mandatory manual-testing or bulk-import workstream in this plan.

## Execution context and decisions

- CI observer: coordinator `/root`, checkout `/Users/terryyin/git/doughnut`,
  repository `nerds-odd-e/doughnut`, branch `main`; yielded cell `9`, PTY
  session `80533`. Initial sandbox launch exited before observer startup because
  the Nix fetcher cache was read-only; the escalated launch is active.
- Startup CI run `34029294495` attempt 1 failed at `ae291e3c8a` in the Codex
  lifecycle fixture: readiness was written before its SIGTERM handler, allowing
  shutdown to omit `github-request-stopped`. Current HEAD already contains
  repair `af65fb4d5f`, which registers the handler before readiness. Verified
  with `CURSOR_DEV=true nix develop -c node --test .agents/skills/execute-plan/scripts/ci-codex-lifecycle.test.mjs`
  (3 passed). No new repair needed; this disposition covers that fixture failure.

- Stable backend entry: `NotebookController.publishNotebookGitProposal`; observe
  notes through `NoteController.showNote` / `getNoteInfo`, and accepted Git state
  through `downloadNotebookGitBundle`. Keep the existing endpoint and response.
- `NotebookGitProposalTreeShape` already walks the complete two-tree diff, but
  returns exactly one change. Validate every changed path's extension, reserved
  README status, mode and kind before returning the supported collection. Keep
  delete/move rejection, no-change rejection, and the single-parent ancestry gate.
  A multi-note proposal must contain an addition under this story's boundary.
- `NotebookGitProposalPublisher.publish` already owns the notebook lock and one
  `REQUIRES_NEW`, `SERIALIZABLE` transaction. Preserve that ownership, its
  already-accepted early return, and the final single binding write. Do not
  introduce per-note transactions, compensating writes, or a new batch framework.
- Validate the accepted projection before applying changes. The existing
  `requireMatchingAcceptedTreeWithOneLiveNoteAtPath` combines the base check with
  identity lookup: calling it after an earlier note mutation would see the
  proposal's own work as drift. Separate those responsibilities for the immediate
  multi-note behavior. Resolve edited identities against accepted paths; include
  all newly created notes in the final proposed-tree comparison.
- Continue using `NoteFactory.create`, title validation, represented-parent
  lookup, and `AuthoredNoteDocumentPersistence.persist` for each relevant change.
  Apply authored-property validation to every changed document, not just the
  first. Do not infer identity from text or repair authored Markdown.
- [Accepted ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  supplies the unchanged Portable format. [Accepted ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md)
  permits unexpected failures to propagate; rollback is the explicit business
  outcome tested here. Add path context only where a per-note rejection otherwise
  cannot identify its member, preserving existing validation semantics. Proposed
  ADR 0002 remains Proposed; this plan follows the seed's recorded product scope.
- No schema, API signature, authentication, scheduling, link-resolution, or
  transaction-owner change is needed. No new storage experiment is indicated:
  the existing committed fixture and late-binding-save failure harness exercise
  the same publisher transaction. Reuse `NotebookGitBundleControllerTestBase`
  (`NOT_SUPPORTED`, isolated committed users) and `inCommittedTransaction` for
  post-failure observations. Do not substitute rollback-only outer fixtures.
- `proposalBundleBytes` takes the **whole proposed tree**, including unchanged
  notes and READMEs. Use `snapshotCurrentPortableTree` for positive and member-
  validation cases so synthetic projection drift cannot hide the intended result.
  Existing helpers already support several files; no new Git fixture framework.
- The CLI already sends the entire `main` bundle. Its transport, binding,
  readiness and ancestry logic need no feature extension. Its clone guidance in
  `cli/src/nonInteractiveCli.ts` and corresponding assertions mention one note;
  align them with supported scope by leaf 10. Until then the guidance conservatively
  describes the existing single-note workflow; it must not advertise pending cases.

## Ordered slices

### 1. Identify an unsupported changed path before rejecting its commit

Type: Behavior
Status: done
Evidence: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed
(2,206 tests, 47 seconds); reserved README path/reason and unchanged remote
state inspected. Independent refactor: no edits. About 3 minutes active work.
Proof: Publish a conformant tree with a valid addition and a changed README →
the rejection names the README and its reserved role, with unchanged remote state.

Behavior: A proposed commit contains an unsupported member among ordinary note
changes → the owner receives that member's path/reason instead of only the
generic multiple-files restriction.

In `NotebookGitProposalTreeShape`, apply the existing regular-note path checks
to every collected change before enforcing the current count restriction.
Extend the existing note-plus-README rejection with an assertion about the
actual offending member; retain the mode/deletion checks already in the walk.
Valid multi-note commits still reject. Slice 3 replaces only that count policy.
One tree-validation change and one controller proof loop; no projection changes.

### 2. Isolate the existing note-addition operation

Type: Structure
Status: done
Evidence: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed
(57 seconds). Retained root/folder creation, validation, identity and late-failure
observations inspected. Independent refactor: no edits. About 3 minutes active.
Proof: Existing root/folder additions, title/property rejection and addition
rollback cases remain green under the backend verification command.

Structure: Extract the currently inlined addition operation in
`NotebookGitProposalPublisher` so its current single-note caller uses a small
per-addition operation: validate/read the authored note, resolve its accepted
parent, create it and persist its document. Keep the accepted-base comparison
outside that operation and the final projection/binding write with the caller.
Retain the single-note gate and the existing single-edit path. This immediately
enables slice 3 to repeat addition application without duplicating creation or
mistaking earlier additions for drift. Do not build a general change framework
or refactor edited-note lookup yet.

### 3. Publish several additions as one atomic commit

Type: Behavior
Status: done
Evidence: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed
(47 seconds). Two-note views/exact downloaded head/tree/parent/bytes, committed
late-write rollback and conformant edits-only rejection inspected. Independent
refactor: no edits. About 5 minutes active; 70 seconds runner/startup.
Proof: One controller atomic-publication loop: a two-addition proposal appears
in the represented folder at the exact downloaded head/tree/parent; the existing
late-binding-save failure fixture with two additions leaves no attempted notes,
creators, references or binding changes in committed state.

Behavior: The owner publishes one direct-child commit adding several valid notes
in accepted locations → Donut accepts the entire authored addition set atomically.

Return the validated change list from the tree-shape gate. Permit a single
change or an additions-only set, keeping mixed and edits-only sets rejected.
Apply each addition with slice 2's operation, accumulate all created notes for
the final projection comparison, and write the binding once. Keep single-note
editing on its existing path. Use two added notes in the existing-folder
positive fixture; reuse root and lossless-content examples for unchanged policy.
The rollback fixture already accepts a list and counts all note-owned rows:
extend its data, not its transaction machinery.

Success and late-failure observations establish this one atomic acceptance
boundary in the same focused loop; neither can be postponed. Reuse the existing
bundle reader and committed fixtures rather than creating new assertion
infrastructure. Mixed rejection remains until slice 5; edits-only rejection
remains afterward. Add a conformant two-existing-note edits-only fixture so a
README or malformed baseline cannot accidentally supply that rejection.

### 4. Separate edited-note lookup from accepted-base validation

Type: Structure
Status: done
Evidence: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed
(47 seconds). First run (46 seconds) exposed the property fixture's Git note
without a live row; real note plus snapshot now exercises its existing field
error after the base check. All other assertions retained. No remaining defect.
Independent refactor: no edits. About 4 minutes active, 93 seconds Gradle total.
Proof: Existing single-note edits, slice 3's several additions, drift rejection
and late-failure cases remain green; mixed proposals remain rejected.

Structure: Separate the base comparison from
`requireMatchingAcceptedTreeWithOneLiveNoteAtPath` and retain a live lookup
that resolves the same note at the accepted path. Arrange the existing single
edit and repeated addition paths around one per-change application dispatch,
with accepted-base validation before mutation and one final comparison/write.
Keep the current shape policy. This immediately enables slice 5 to interleave
existing edits and additions; all extracted paths have current callers.

### 5. Publish a mixed commit while preserving learned-note identity

Type: Behavior
Status: planned
Proof: A controller mixed-publication loop observes one new note and two edits
at unchanged paths, with the learned note's same ID/tracker state. The retained
late-failure fixture, now also editing an existing note, observes unchanged
original content/timestamps/references and no attempted additions or binding change.

Behavior: The owner commits an addition and accompanying edits → the complete
mixed change becomes accepted while existing private identity stays attached
to the same notes.

Replace the additions-only multi-note policy with "contains an addition"; use
slice 4's already-live dispatcher. Replace
`asksTheAuthorToSplitAnAdditionAndEditIntoSeparateCommits` with positive mixed
proof, placing the added path between two edited paths to catch first-item-only
or repeated-base-check errors. Reuse one learned-note fixture and the existing
copy-isolation proof; do not repeat the private-association matrix. Adapt the
existing rollback data to include one existing edit in this same acceptance
loop, keeping its earlier multi-addition observation. Edits-only batches still
reject; no temporary mixed restriction or obsolete split-commit advice survives.

### 6. Reject the whole mixed commit when a note lacks its required type

Type: Behavior
Status: planned
Proof: The seed's valid mixed proposal plus a note missing `type` rejects with
its path/reason; committed state shows no additions, unchanged existing content
and the original accepted head/bundle.

Behavior: One document in a related set is invalid typed Markdown → none of the
commit's note changes are accepted.

Extend the current Markdown/addition validation controller family with this
one multi-note case. Whole-tree typed-Markdown validation already exists; keep
its current matrix rather than adding one new matrix per change kind. Existing
CLI submission-rejection proof supplies local-work preservation. No separate
property-diagnostic work is hidden in this leaf; that belongs to slice 7.

### 7. Identify an invalid authored property in a later changed note

Type: Behavior
Status: planned
Proof: A mixed proposal has valid earlier changes and a later note containing
`note_level: 7` → its path and existing `note_level` field error are reported,
and all remote state remains at the accepted parent.

Behavior: A changed note passes typed Markdown but violates the existing
authored-property rules → the owner can identify and correct that member
without any valid member being published.

Drive the controller using `NotebookGitProposalPropertyValidationControllerTest`
patterns and committed observations. `AuthoredNoteContent.assertValidForSave`
currently produces `ApiException` with a field error but no path. Enrich that
specific per-document publication error with the path in the existing
`ApiError.message`, retaining its error type, field errors and original cause.
Do not change the validator, global handlers, API shape or CLI transport.
The single-note property's existing rule remains the same.

### 8. Reject an addition whose parent is absent from accepted history

Type: Behavior
Status: planned
Proof: A valid root addition followed by `New Folder/Idea.md` rejects with the
unsupported path/reason; committed reads show no new notes/folder or binding change.

Behavior: One addition needs a parent outside the represented accepted tree →
the entire proposal is rejected, including the otherwise valid root addition.

Extend `NotebookGitFolderNotePublicationControllerTest` with the seed's example.
Reuse the existing represented-folder lookup and keep database-only empty
folders ineligible. Its current message suggests publishing a README: replace
that unsupported advice with guidance to use an existing represented location.
No folder creation, README authoring or broader drift repair is included.

### 9. Retry an accepted multi-note commit without duplicates

Type: Behavior
Status: planned
Proof: Publish a small mixed commit, capture committed note IDs/content/timestamps
and binding, then repeat the proposal → the same returned head and publication
state, without extra notes or rewritten bundle.

Behavior: The owner repeats a proposal that already succeeded → report that
existing accepted commit without applying its note changes again.

Extend the existing publication retry data and observation to the small set;
keep the already-accepted branch ahead of fresh-proposal validation. No retry
loop, token or new state-snapshot framework.

### 10. Explain the supported commit shapes in CLI guidance

Type: Behavior
Status: planned
Proof: The existing CLI clone test observes guidance describing additions,
mixed additions/edits, single-note edits, existing locations and one direct-child
commit. Align its existing E2E assertion to the same wording.

Behavior: The owner clones a notebook → the CLI explains which local commits
can be published without implying edits-only multi-note support.

Update `cli/src/nonInteractiveCli.ts` and its existing assertions. No new help
surface or copy-only test. The CLI test is this leaf's proof loop; the affected
installed feature is exercised by the immediately following fixture work.

### 11. Let the existing checkout fixture commit an explicit list of files

Type: Structure
Status: planned
Proof: The current installed-CLI feature, including its single-note edit/add
scenarios and revised guidance, stays green using the adapted helper.

Structure: Change `commitCliNotebookCheckoutNoteChange` and its sole page-object
caller in `notebookClone.ts` to pass an explicit file list; existing edit/add
wrappers pass one item. The task writes/stages that list and makes one commit,
returning its head through the existing alias. This immediately enables slice
12's multi-file scenario. Keep all helper paths exercised by existing callers;
do not add unused exports, a PTY, mode flags or a general scenario interpreter.

### 12. Publish a related change through the installed CLI

Type: Behavior
Status: planned
Proof: Clone the conformant notebook fixture, make one commit with two additions
and an existing-note edit, publish using the installed binary → the reported
head equals the local commit and all authored changes appear in Donut note views.

Behavior: The owner follows the existing local commit/publish workflow for
related additions and edits → Donut displays the whole accepted change.

Add one explicit multi-file step/scenario to
`e2e_test/features/cli/cli_notebook_clone.feature` using slice 11's working
list-based task and thin step/page-object glue. Reuse existing installation,
authentication, commit-head and content assertions. Keep the snapshot hook
only in Given setup, never after the local action under test. No new frontend
feature or second checkout harness. If this scenario needs multiple inner TDD
beats, keep it `@wip` until green; remove that tag before closing this leaf.

### 13. Receive the whole accepted commit in a clean second checkout

Type: Behavior
Status: planned
Proof: `run(['notebook', 'pull', directory])` against a real Git bundle with one
accepted multi-note commit yields its exact head/tree/file bytes and a clean
checkout without Portable metadata.

Behavior: A clean bound second checkout is at the accepted parent → pulling
receives every note change in the accepted commit together.

Extend the addition example in `cli/tests/notebookPull.fastForward.suite.ts`
with several changes in one commit. Reuse real-Git helpers and mock only download.
Slice 3 proves the backend's downloadable exact commit. No pull implementation
or full second E2E harness is expected.

## Proof ownership and reuse

Existing evidence means inspected tests retained for execution, not tests run
during refinement. Canonical assertions stay in one example; variations assert
their delta. No completed evidence exists to invalidate or discard.

| Final promise | Owning leaf and observable proof |
| --- | --- |
| Several additions at the root or in represented folders; exact authored commit/tree/bytes | 3: multi-note views and downloaded bundle; retained root/folder and lossless-content examples |
| Mixed edits all applied at unchanged identities | 5: two edited paths and existing learned-note tracker state |
| Fresh additions; copied text transfers no private associations | 3/5: distinct note IDs plus retained `NotebookGitCopyIdentityControllerTest` tracker/question/conversation observations through unchanged `NoteFactory` |
| All-or-nothing publication, including failure after writes | 3/5: positive atomic boundary plus adapted existing late-binding-save fixture and committed-state reads; required before acceptance closes |
| Invalid typed Markdown or authored property identifies its member and rejects all | 6: missing type; 7: later invalid property with preserved field semantics; existing validation matrix retained |
| Missing/unrepresented parent rejects even the valid root member | 8: no new notes/folder/binding; existing nested and README-only folder success/rejection cases retained |
| Unsupported changed members are never skipped | 1: path-specific rejection; 3/5 retain every per-path check and `NotebookGitProposalTreeShapeControllerTest` delete/move/mode/README rejection |
| Destination uniqueness, reserved/advisory names and no repair/normalization | 3/5 retain addition-validation, deleted-destination and advisory-name controller tests; exact proposed-tree comparison |
| Single-note operations remain; edits-only batches reject; temporary mixed rejection removed | 2–5 retain single-note examples; 3 installs explicit edits-only proof; 5 replaces only the mixed rejection and its obsolete advice |
| Retrying an accepted head does not duplicate or rewrite state | 9: repeated proposal and committed-state comparison |
| Owner, clean bound main, one direct-child commit; no stale/drift overwrite | 2–5 retain backend ancestry/drift/concurrency tests and CLI binding/readiness/ancestry suites; new data only if a guard boundary changes |
| Rejection keeps local work and exposes the server reason | 6–8 reuse `notebookPublish.submission.suite.ts` state/error observations and unchanged submission transport |
| Accurate CLI scope guidance | 10: existing clone test; 11: existing installed feature assertion |
| Existing Donut views and ordinary clone/pull expose the whole result | 3/5: view/download boundary; 12: installed publish/view; 13: real-Git receive |
| No Portable metadata or new transport/transaction owner | 2–5 retain endpoint, binding-write and transaction ownership; 3: exact tree; 11–13: existing client boundaries |

Interim restrictions are explicit: leaf 1 still rejects every multi-note
proposal; leaf 3 replaces that with additions-only acceptance; leaf 5 replaces
only the mixed restriction. Edits-only batches remain rejected. No temporary
policy, caller, test description or advice about splitting mixed commits
survives its replacing leaf.

## Verification and wrap-up

- Backend changes: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.
  Backend rules require the full backend unit suite; named classes above identify
  the owning observations, not permission to use a class-only runner.
- CLI guidance: `CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookClone.test.ts`.
- CLI receive: `CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookPull.test.ts`.
- Rerun CLI publish regressions if their source/contract changes:
  `CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookPublish.test.ts`.
- Installed CLI: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_clone.feature`.
  The existing feature is enabled; use its existing bundle/install setup.
- Reuse passing evidence until a later change invalidates the covered boundary.
  Do not add full-suite E2E, mutation testing, manual testing, or new framework
  tests. New safety proof belongs with its accepting behavior before that slice
  can close. Tests may be the only change when production already satisfies a
  later example; do not manufacture implementation work.
- On later authorized execution, use `execute-plan`: Jidoka, fresh
  post-change-refactor agent, API generation only for an actual signature change,
  coordinator `./scripts/run.sh pnpm format:changed` once, PLAN update, commit
  and push. No implementation or execution wrap-up is authorized by writing
  this PLAN alone.
- On completion, clean up spent planning history and reduce the home story to
  goal/scope. Do not modify sibling stories or start the next backlog item.

## Refinement assessment and readiness

Remaining leaves are **Ready** under the current evidence.
Structure leaves 2, 4 and 11 immediately precede their enabling Behaviors 3, 5
and 12. They retain live callers and existing outside-in proof. No new leaf is
an unverified implementation step or deferred safety test.

| Leaves | Sizing hypothesis including local cleanup and focused proof |
| --- | --- |
| 1 | About 3–5 minutes: relocate existing changed-path validation and tighten one existing rejection observation. |
| 2 | About 5 minutes: extract the current addition operation, retaining the single-edit path and current gate. |
| 3 | About 5 minutes excluding runner time: collect/traverse additions through the prepared operation and adapt the existing positive/failure data. Whole-tree fixtures and rollback observations already exist; no new storage or test harness. |
| 4 | About 5 minutes: separate base validation from edited lookup and reuse both live paths in the per-change dispatcher, without accepting mixed input. |
| 5 | About 5 minutes excluding runner time: switch the mixed policy and replace the existing mixed rejection with compact identity/atomicity examples using prepared traversal. |
| 6–9 | About 3–5 minutes each: one named rejection or retry variation; 7's small path-context enrichment is confined to publication's existing property error. |
| 10 | Under 5 minutes: existing guidance/assertion update. |
| 11 | About 5 minutes excluding runner time: one task and its sole caller become list-shaped; existing single-file scenarios prove unchanged behavior. |
| 12 | About 5 minutes excluding runner time: one explicit multi-file scenario using the already-green helper and existing assertions. |
| 13 | About 5 minutes excluding runner time: one real-Git receive data variation; no new production path. |

**Sizing exceptions:** the required full backend unit suite, installed-CLI
build/install/Cypress run, or external hook/CI/refactor waits may themselves
exceed five/ten minutes. Record measured wait time separately; the exception
does not cover fixture authoring, implementation, debugging or cleanup.
No runtime was measured during refinement, and no execution-time guarantee is
implied. Backend verification remains the full unit suite required by its rule.

Scrutinize a leaf after five minutes of non-exempt work. After ten, preserve
attempt-owned work and finer-decompose this PLAN under the repository's Learning
escalation rules. Do not disguise a repeated overrun by renumbering leaves.
A changed story boundary returns to Story 11 review; it does not authorize
extra synchronization capability.

## Learnings that changed refinement

- Every-path shape validation can become its own useful rejection outcome while
  the single-note restriction remains. The accepting leaf need not invent both
  member diagnostics and repeated projection in one step.
- Addition and mixed preparation have different immediate needs. Edited-note
  lookup's combined drift check can wait until mixed publication is next.
- The committed transaction fixtures and late-binding-save harness already
  support lists/row counts; expanding their representative data avoids a new
  multi-note test framework. Atomicity evidence stays with acceptance.
- Authored-property validation returns a field-specific `ApiException` without
  a path. That is a concrete diagnostic change, now separately owned instead
  of hidden behind a conditional instruction in the Markdown leaf.
- The E2E commit task has one caller. Adapting that exercised caller to a list
  can stay green before the multi-file scenario is introduced.
