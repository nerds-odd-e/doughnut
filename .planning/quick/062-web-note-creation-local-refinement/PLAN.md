# Web note creation followed by local refinement

## Source and status

[SEED-009, story 13](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-13)
— Create a note on the web and continue refining it locally.

Status: in progress. Slices 1–2 done; next is slice 3.
Sizing remains a hypothesis, not a time guarantee.

## Goal and scope

An owner creates an ordinary note on the web, finishes writing, pulls it into
a local Git checkout, edits it, and explicitly publishes onto the same Donut
identity without losing learning data or creating projection drift.

Include title-only creation, valid initial ordinary-note content, matching empty
notebooks, root and existing represented nested folders, immutable accepted
history, subsequent normal web saves, and the existing clean-main CLI workflow.
Creation and Git acceptance are one atomic outcome. Preserve existing files,
private associations, title validation/warnings, and local-work readiness gates.

Exclude new notebooks/folders, unrepresented destinations, README authoring,
restore/deleted-name reuse, relationship notes, external-data-assisted creation,
AI extraction, bulk import, attachments, web delete/rename/move, earlier drift
repair, divergent receipt across additions, multiple local commits, direct Git
transport, and commit batching. Unsupported existing web flows keep their current
behavior and do not silently advance accepted history. No new UI or API shape.

Key examples from the seed:

- Title-only root creation → web body save → pull → local edit/publish → same
  note and pre-publication tracker/history retained.
- First note in a matching empty notebook; nested represented placement without
  creating parents or READMEs.
- Invalid creation or late Git acceptance failure → no accepted creation.
- Dirty/divergent checkout → pull refuses and retains local work.
- Earlier projection drift → web creation does not absorb it into Git history.

## Execution context and current decisions

- Public entry point: `NotebookController.createNoteAtNotebookRoot` at
  `/api/notebooks/{notebook}/create-note`; folder selection is `NoteCreationDTO`
  data. Its current transaction calls
  `NoteConstructionService.createRootNoteWithWikidataService`.
- `NoteFactory.create` already supplies valid `type: Note` for an empty body,
  creates the NoteCreator, and validates reserved/deleted titles. Reuse it.
  Do not add Git side effects to this shared factory: local publication,
  extraction and other creation paths must not create extra commits.
- Model the included web path on `WebNoteContentSaveService`: lock with
  `NotebookGitStateLoader.findByNotebookIdForUpdate`, compare the pre-mutation
  projection to accepted history, create the note, build the complete Portable
  snapshot, append/write the bundle, persist the binding in one transaction.
  Capture eligibility before mutation. The loader's `liveNotes` list predates
  creation: the post-create snapshot must include the new persisted note.
- `NotebookController` already owns a transaction. A nested service annotation
  does not automatically upgrade its isolation. Make transaction ownership
  explicit for this entry point; acquire the binding lock before reading the
  accepted baseline or mutating the note. Follow the established writer lock
  protocol and rollback for checked as well as unchecked failures; do not add
  `REQUIRES_NEW` inside an existing controller transaction.
- Keep synchronization local to ordinary web creation. Gate eligibility on
  matching accepted content, destination representation and included note type;
  preserve legacy behavior for excluded inputs. Do not perform external-data
  work while claiming the ordinary single-note acceptance contract.
- Root support may land before folder support, but never accept an incomplete
  tree or enable a destination before its eligible path is proven. Folder
  representation follows accepted tracked descendants/README, not directory
  existence or the newly created file itself.
- Reuse `NotebookGitProjection`, `PortableTreeSnapshot`, bundle builder/writer
  and existing web-content save behavior. No generic mutation framework or
  schema migration is justified. Extract duplication only within this concept.
- Accepted ADR 0004 governs Portable bytes and path/title behavior;
  [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md) permits loud
  failures. Atomicity assertions verify the business outcome, not exception
  logging. Proposed ADR 0002 is direction, not an Accepted architecture mandate.
- Existing creator/reference side effects must share the creation transaction.
  A rollback assertion reads committed state after the failed public call, not
  a dirty persistence context or an enclosing rollback-only test transaction.

### Storage evidence and proof strategy

The repository uses MySQL 8.4 (`mysql84` in `flake.nix`). The accepted head and
bundle bytes are stored in MySQL alongside the projection; there is no second
persistent Git store to compensate. Existing
`NotebookGitPublicationAtomicControllerTest` proves late binding-save rollback
for mixed additions and web content updates using committed fixtures and
fresh committed readback. `NotebookGitBundleControllerTestBase` disables the
test's ambient transaction and supplies isolated committed users/cleanup.
Reuse that transaction/fixture pattern and its
`NotebookGitPublicationAtomicTestSupport` failure injection for creation.

This is matching evidence in the repository, not a new test run. No new engine,
DDL sequence, or REQUIRES_NEW visibility assumption needs an experiment now.
The new creation-specific proof belongs to leaf 2. If execution changes this
transaction model, stop and record the concrete assumption, engine/version,
literal isolated proof command, observed postcondition and result here before
relying on it. Never experiment on shared or production data.

## Refinement findings

No execution attempt or completed proof exists to preserve. Classification of
original leaves: **1 Refine** (integration plus input variations), **2 Refine**
(guards span several newly enabled paths), **3 Ready** (folder placement),
**4 Ready** (identity-preserving publication), **5 Refine** (three user actions
with separate useful checkpoints). No story escalation is needed.

The creation factory already provides canonical empty Markdown. Existing web
content saves already implement the append/write/persist sequence. Extract that
small sequence without changing transaction ownership; its immediate consumer
is the first creation Behavior. Do not extract a general mutation coordinator.

Existing E2E helpers include `forms/noteCreationForm.createNoteWithTitle`,
`noteSidebar`, the normal body editor, and
`step_definitions/cli_notebook_clone.ts` pull, commit, publish and exact-file
assertions. Reuse them rather than building new infrastructure. The inspected
`notebookPull.structuralHistory.suite.ts` has rename, delete/recreate, README,
mode and reversal cases, but no plain accepted addition; leaf 7 owns that gap.

## Ordered execution leaves

All leaves target about five minutes including their change, focused proof and
local cleanup. Required backend-suite and installed-CLI/browser runtime can
exceed that target; only measured test/external wait time is exempt from the
hard limit. No leaf budgets ten minutes of active implementation. Each proof
below is one bounded outside-in loop; fixture variations test the same outcome.

### 1. Reuse accepted snapshot persistence without changing web saves
Type: Structure
Status: done
Internal change: Extract only the existing append/write/binding-save sequence
from `WebNoteContentSaveService` into one transaction-participating collaborator
in the notebookGit package. Pass the actual accepted bundle, complete post-change
entries, binding, timestamp and message; retain lock acquisition, before/after
comparison and transaction ownership at the caller. No callback framework,
new storage, retry, asynchronous work or new transaction propagation.
Unchanged external behavior: Web content saves retain identical accepted
ancestry, no-op/drift behavior and rollback semantics.
Enables: Immediately following leaf 2 reuses this exact persistence operation.
Proof: Existing web-content save and late-binding-failure controller tests remain
green through the real save endpoint. No helper-only tests or new test harness.
Sizing: ~5 minutes, medium confidence; mechanical extraction of the inspected
append/write/save block with one caller, not a redesign of the service.
Safe stop: Existing web saves remain complete; the helper is already used in
production, and its only planned new consumer is the next Behavior.

### 2. Accept the first title-only root note atomically
Type: Behavior
Status: done
Pre-condition: Authorized owner, matching empty accepted Portable tree,
root destination, available title, absent initial content/external-data mode.
Trigger: Submit the normal web creation request.
Post-condition: Creation and its canonical accepted file succeed together or
leave neither a note nor an accepted revision on failure.
Execution path: Introduce the narrow ordinary-web orchestration at the existing
creation entry point. Keep one controller-owned transaction with explicit
SERIALIZABLE isolation and rollbackFor Exception; do not rely on a nested
annotation to upgrade it. Lock/load before comparison and creation, invoke the
existing construction operation, and pass the new note in the complete snapshot
to leaf 1's persistence collaborator. Guard all other inputs to retain their
existing web path and unchanged Git head. Do not modify the shared NoteFactory.
Proof: One atomic-creation controller proof family, using committed fixtures:
normal completion exposes `type: Note` at the requested root path, one immutable
child of the old head, and no metadata. The same transaction boundary with the
existing late-binding-save injection leaves no new note/creator rows or binding
change on committed readback. These are the two outcomes of atomic acceptance;
they ship together, never as a note-only commit followed by a safety patch.
Use existing invalid-title/authorization cases as regression proof; add only the
unchanged-head assertion where it is missing. Missing binding/excluded modes
must not acquire a new binding or enter the acceptance path.
Sizing: ~5 minutes, medium confidence after leaf 1 removes bundle persistence
work; one minimal eligibility branch and the existing transaction/failure harness.
Safe stop: First-note title-only capture synchronizes safely. Interim empty-tree
restriction is removed by leaf 3, no-initial-content restriction by leaf 4, and
root-only restriction by leaf 5. All other existing web behavior stays available.

### 3. Accept a root addition without absorbing earlier drift
Type: Behavior
Status: planned
Pre-condition: Existing notebook has accepted content and an available root
name; creation is still title-only and otherwise eligible.
Trigger: Create another ordinary root note.
Post-condition: Append only that new file when the pre-create projection matches;
otherwise retain the existing web result without advancing accepted history.
Execution path: Remove leaf 2's empty-tree restriction. Reuse the full pre-create
projection comparison and build the post-create tree from old live notes plus
the new persisted note. Do not derive the baseline after creation.
Proof: Controller cases with existing tracked content and with one earlier
unsynchronized change assert the accepted-tree delta (new file only, or none).
Existing blobs/commits retain their bytes/IDs; the drifted web creation is not
advertised as synchronized. Reuse leaf 2's atomicity proof unchanged.
Sizing: ~5 minutes, medium confidence; one baseline comparison/snapshot loop.
Safe stop: Ordinary title-only root additions work in matching notebooks; earlier
drift and missing-binding states remain outside synchronization.

### 4. Accept initial ordinary Markdown as the creation content
Type: Behavior
Status: planned
Pre-condition: Eligible root creation includes initial ordinary-note Markdown,
possibly authored frontmatter and a wiki reference.
Trigger: Create the note through the existing request.
Post-condition: The accepted new file contains the complete canonical persisted
creation content, with no intermediate empty-note commit.
Execution path: Remove leaf 2's absent-content restriction only for ordinary,
non-assisted note content. Reuse the existing canonical document preparation;
append after content and derived reference creation finish. Relationship types,
external-data-assisted content and other excluded operations retain their
existing behavior without being newly synchronized.
Proof: Controller creation/download compares exact canonical authored content.
Use the same late-save fixture variation to verify its newly introduced reference
row disappears on rollback; do not introduce another failure harness. Invalid
content and excluded type/mode cases verify the acceptance guard, not a second
implementation path. Existing validation and advisory-name behavior stays green.
Sizing: ~5 minutes, medium confidence; one initial-document eligibility and
snapshot loop, with reference-row delta added to the existing atomic family.
Safe stop: Valid initial ordinary content participates in the root workflow;
no unrelated assisted or relationship creation is silently enabled.

### 5. Accept creation in an existing represented folder
Type: Behavior
Status: planned
Pre-condition: Matching notebook, ordinary note, valid existing represented
folder, including nested placement without its own README.
Trigger: Create using the current folder selection.
Post-condition: The same atomic creation appears at its full accepted path
without creating or changing parent containers or their READMEs.
Execution path: Remove root-only eligibility using accepted-tree representation
and full folder-path mapping. Determine representation before adding the file;
never infer it from the creation itself. Keep unrepresented destinations on the
existing unsynchronized web path and preserve foreign-folder rejection.
Proof: Controller destination cases distinguish tracked descendants, README-only
content and unrepresented empty folders. Assert the actual accepted path or
unchanged head plus preserved containers. Reuse the atomic acceptance family.
Sizing: ~5 minutes, medium confidence; one existing path-resolution operation
and data-driven eligibility proof, without new folder semantics.
Safe stop: All story creation destinations work; no new folder/README capability.

### 6. Publish onto the web-created identity after a web content save
Type: Behavior
Status: planned
Pre-condition: Real accepted web creation, a later ordinary web body save, and
learning tracker/history associated with that identity.
Trigger: Publish a valid direct-child local content proposal at the same path.
Post-condition: Donut displays the refinement on the same identity and retains
its learning/private state.
Execution path: Use existing creation, TextContentController and publication
controllers with actual accepted bundle history and existing proposal fixtures.
No special publication route for web-created notes. Change product code only
for an observed incompatibility in this included round trip.
Proof: One controller-level publication loop asserts final same-id content and
representative tracker/schedule/history preservation, with existing notes intact.
Verify the creation and subsequent save remain separate accepted ancestors.
Reuse ordinary stale/invalid publication refusal tests as the compatibility gate.
Sizing: ~5 minutes, medium confidence; existing proposal/identity fixture pattern.
Safe stop: Full server-side round trip and private identity are established.

### 7. Preserve unpublished work when accepted history adds a note
Type: Behavior
Status: planned
Pre-condition: Clean bound main has one unpublished existing-note content edit;
accepted main advances through a plain addition of a different ordinary note.
Trigger: Run `donut notebook pull`.
Post-condition: Pull refuses the structural interval and retains the original
local branch, commit, index and files without publishing.
Execution path: Add the missing plain-addition row to the existing structural-
history table using `commitPortableFile`; drive `run` with real local Git and
mocked HTTP only. Preserve the current refusal implementation unless this
specific input exposes a defect.
Proof: Existing table's checkout-state and path-named refusal assertions for
this new variation. Readiness tests already cover dirty/untracked/unfinished
work; run them rather than cloning their assertions into this leaf.
Sizing: ~5 minutes, high confidence; one data variation in an inspected harness.
Safe stop: Receiving new web notes cannot silently expand divergent pull support.

### 8. Receive a title-only web-created note with the installed CLI
Type: Behavior
Status: planned
Pre-condition: Installed CLI and clean bound checkout of a matching notebook.
Trigger: Create a title-only root note through the normal web UI, then pull.
Post-condition: The checkout contains that note as valid canonical Markdown.
Execution path: Add one receipt scenario to `cli_notebook_clone.feature` using
the existing creation form/sidebar and installed pull/file assertions. Add at
most one thin ordinary-create step if absent; put busy waits in its page object.
Clone before the action. Never refresh the testability snapshot after creation.
Proof: One browser/installed-CLI receipt loop checks exact received file bytes.
Reuse CLI fast-forward repeat-pull/no-publication tests as the compatibility gate.
Sizing: ~5 minutes active work, medium confidence; existing creation and pull
helpers eliminate a new transport or browser harness. Install/runtime is exempt
only when measured; a new helper subsystem requires re-refinement.
Safe stop: Web capture can be consumed in local tools independently of publication.

### 9. Receive the completed web text after creation
Type: Behavior
Status: planned
Pre-condition: Normal web title-only creation is accepted after the checkout's
base revision, followed by an ordinary web body edit before any pull.
Trigger: Pull through the installed CLI.
Post-condition: Local Markdown contains the finished web text across both
accepted commits.
Execution path: Add the existing rich-body edit action to a second receipt
scenario, reusing leaf 8's minimal setup; do not change creation or save APIs.
Proof: One browser receipt loop asserts the final body from creation-plus-save
history. The earlier title-only scenario remains the canonical empty-body proof.
Sizing: ~5 minutes active work, medium confidence; one existing editor action
and a changed expected file, with browser runtime the only expected exception.
Safe stop: Owners may finish writing before switching tools, with no new save UI.

### 10. Publish the locally refined web capture through the installed CLI
Type: Behavior
Status: planned
Pre-condition: The checkout has received the web-created note and later web text.
Trigger: Commit a local content edit and explicitly publish using installed CLI.
Post-condition: Donut displays that locally refined text on the captured note.
Execution path: Extend leaf 9's finished-text journey with existing edit/commit,
publish and web-content assertion steps; keep its receipt assertion as prerequisite
proof. Do not add detailed private-data browser fixtures (leaf 6 owns them).
Proof: One installed-CLI publication loop with final web-visible content. Preserve
leaf 9's receipt proof in the extended scenario instead of duplicating a long
setup solely to retain an earlier scenario name.
Sizing: ~5 minutes active work, medium confidence; three existing steps and one
expected final result, plus measured installed-CLI/browser runtime.
Safe stop: Complete public story is demonstrated with real web actions and the
installed CLI, without a fabricated post-creation Git snapshot.

## Contract-to-proof map

| Contract | Owning leaves / observation |
|---|---|
| Existing web saves unchanged by extraction | 1: existing save/no-op/drift/rollback suite |
| Title-only first note, valid canonical bytes, no metadata | 2: controller note and downloaded root file; 8: UI receipt |
| Immutable accepted child and unchanged existing files | 2–3: actual bundle ancestry/tree |
| Atomic new note/creator/binding and initial-content reference rows | 2: committed late-failure readback; 4: reference-row fixture delta |
| Authorization and invalid/reserved/occupied titles | 2: existing creation refusals with unchanged-head delta |
| Earlier drift or missing binding remains unsynchronized | 2–3: guard outcome, unchanged head/bytes and existing web result |
| Initial ordinary content, canonicalization, advisory names | 4: authored-content bundle assertion and existing creation/UI validation proof |
| Relationship/assisted/extraction/restore flows not newly synchronized | 2/4: narrow entry/eligibility gates plus existing flow regression tests |
| Represented nested/readme-only placement; no new parents | 5: actual path, container identity/content, unrepresented refusal-to-sync |
| Creation followed by ordinary web saves | 6: real accepted ancestry; 9→10: received finished body |
| Same identity, private learning data and other notes preserved | 6: same-id readback and representative tracker/history |
| Accepted addition with local work remains refused | 7: public CLI run and complete retained checkout state |
| Dirty/untracked/unfinished operations remain protected | 7 gate: existing readiness suites |
| Clean receipt, repeat-pull no-op, pull never publishes | 8 gate: CLI fast-forward tests; 8–9: actual installed receipt |
| Stale/invalid local proposals refuse without overwrite | 6 gate: existing publication rejection proof; CLI gate at 7 |
| Complete web/local workflow | 8 → 9 → 10: create, receive finished text, explicitly publish |

No promises are dropped from the original five leaves. Original exclusion leaf
2 now belongs to the exact enabling boundaries in 2–5, with local-work protection
in 7. Original 3 maps to 5; original 4 to 6; original 5 to 7–10. No completed
proof was discarded. The temporary input restrictions in 2 are removed by
3–5 and must not remain as final product limitations.

## Verification commands and evidence reuse

No tests were run while writing this plan. During execution:

- Backend changes: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.
  Repository backend policy requires all backend unit tests, not one class.
  Use small controller assertions for the inner loop; backend suite runtime
  alone is an explicit sizing exception, not evidence of an oversized leaf.
- CLI compatibility gate (at leaf 7, or earlier if CLI code changes):
  `CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookPull.test.ts tests/notebookPublish.test.ts`.
  Reuse fast-forward, readiness, structural-history and publication rejection
  cases. Leaf 7 adds the inspected missing plain accepted-addition variation
  at `run` with real local Git and mocked HTTP only.
  Preserve already-green tests; do not add a separate CLI test harness.
- Browser checkpoint:
  `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_clone.feature`.
  The feature is not @ignore in the inspected file despite older generic CLI
  rule prose. Its tags build/install the CLI locally. Use an environment that
  supports installed CLI E2E; SEED-015's bounded no-external-service worktree
  workflow does not itself promise this CLI scenario is supported there.
- If backend controller signatures/DTOs change unexpectedly, run
  `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`; no such change is
  planned. Never edit generated files manually.
- Reuse valid proof from earlier leaves unless later edits invalidate it. Do
  not add a testing-only leaf for assertions already covered by these gates.

## Delivery and refinement gates

Ready for direct execution on a separate execution request. The ten leaves
have bounded known code paths and one acceptance/proof loop apiece; no unexplained
active-work path beyond the hard limit remains in this planning hypothesis.
Leaf 1 is the sole Structure and immediately enables leaf 2. Safety assertions
stay with each accepting behavior, not in a later testing or hardening slice.

The only sizing exceptions are measured required backend-suite runtime and
installed-CLI/browser execution time. A slow implementation, fixture redesign or
new helper subsystem is not an exception: refine that leaf in place. Each leaf
includes its change, proof and local cleanup. No elapsed execution time or new
product verification result is claimed by this refinement.

Keep each scenario green at its commit boundary. A multi-beat E2E may be @wip
locally until it passes; do not commit red alone, @focus/@only, or an uncompleted
proof as done. Reuse passed proof unless the covered behavior changes.

During execution: Jidoka at each boundary → fresh post-change-refactor agent →
API generation if necessary → coordinator runs
`./scripts/run.sh pnpm format:changed` once → update this PLAN → commit → push,
with asynchronous CI observation under execute-plan. Preserve unrelated work
and the active SEED-015 plan. At >5 minutes scrutinize; at >10 minutes finer-
decompose unless the documented focused-test runtime exception applies. Return
to story review if evidence changes product scope or sibling priority.

Keep resume state here, not in STATE.md. After delivery, reduce the story's
refinement to enduring Goal/Scope, update the backlog completion record, and
remove spent plan history only when the full outcome and proof are complete.

## Learnings

- Slice 1 extracted append/write/binding-save into `AcceptedSnapshotPersistence.persist`
  in the caller's transaction. Lock, comparison, document persist, and
  `@Transactional` stay on `WebNoteContentSaveService`. Other bundle write+save
  sites (proposal publish, folder acceptance, cutover) are different operations;
  do not fold them into this collaborator.
- Existing web-content save, folder save, projection-drift, late-binding-save
  rollback, and TextContentController update tests plus `pnpm backend:test_only`
  remained green through the real save endpoint. No helper-only tests.
- Slice 2 added `WebNoteCreationService` at `createNoteAtNotebookRoot` with
  controller-owned `SERIALIZABLE` / `rollbackFor = Exception`. Title-only root
  plus empty matching accepted tree is the only acceptance gate; folder,
  initial content, drift, and missing binding keep the existing web path.
  `NoteFactory` is unchanged. Lock/import overlap with web content save was
  left in place (not a generic mutation coordinator).
- Atomic proof: `NotebookGitNoteCreationAtomicControllerTest` (canonical
  `type: Note` child of old head; late binding-save rolls back note/creator/
  binding). Guard proof: `NotebookGitNoteCreationControllerTest`. Full
  `pnpm backend:test_only` green.
