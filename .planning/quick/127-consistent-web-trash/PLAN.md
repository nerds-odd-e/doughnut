# Receive web trash and recovery through one consistent accepted change

Status: in progress
Source: [SEED-009 story 28, first delivery](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-28)
Direction: [One complete accepted web change](../../NORTH-STAR.md#one-complete-accepted-web-change)
Authority: 2026-09-15 — `/dough-execute-plan 127` (planned Story Branch Mode).
Baseline inspected: `337a360f3e` (ordinary web moves merged into main).

## Execution identity

- Originating checkout: `/Users/terryyin/git/doughnut`
- Originating branch: `main`
- Claim commit: `4dc53bb603`
- Execution checkout: `/Users/terryyin/git/doughnut/.worktrees/127-consistent-web-trash`
- Execution branch: `quick/127-consistent-web-trash`
- Integration target: `main`

## Outcome and boundaries

A notebook owner trashes a learned note on the web, receives it locally under
`_trash`, recovers it with existing Move, and receives the active path again.
Content, note/tracker identity, recall history, scheduling and independently
removed tracking preferences survive. Existing immediate Undo also keeps the
accepted tree consistent; otherwise the new Trash commit would make that
existing recovery action introduce drift.

Start with one synchronized notebook and a clean checkout at an ancestor of
accepted main. Keep append-only history, canonical folder markers, existing
permissions, collision suffixes and reference choices. Examples are proof
commitments, not production gates on note type, count, history depth or folder
arrangement. The whole notebook result after each successful operation must
match its accepted tree; each operation has at most one new commit. Naturally
supported behavior is not rejected because a later example owns its proof.

Preserve current non-Git, no-op and pre-existing projection-drift behavior.
No repair of existing drift, cross-notebook Git synchronization, local-originated
trash publication, folder-subtree journeys, new Restore/Undo UI, repeated Undo
cleanup, permanent deletion changes, migration or performance target. Ordinary
same-path publication remains covered by the merged move work. Do not create a
local rename/identity mechanism for receiving already accepted web history.

## PFE findings and implementation direction

- `RelationController.webMove` now invokes `WebNoteEditService.edit`;
  `NoteMoveService` owns capture/place/rewrite and `NoteMotionService` owns
  placement. Content/title editing already shares the accepted-history owner.
  Keep one owner for lock, before-state eligibility, complete mutation, final
  projection, no-op detection and append. Move domain orchestration out of a
  controller when needed to keep transaction ownership coherent; do not copy
  an import/check/snapshot/persist block into Trash or Undo.
  `NoteTrashUndoService` now owns same-notebook Undo placement inside that
  owner; cross-notebook Undo stays on `NoteMotionService` only.
  `NoteTrashService` now owns the existing Trash recipe inside the same owner.
- `NoteController.trashNote` authorizes then `NoteTrashService.trash`, which
  applies reference choices, `FolderConstructionService.ensureTrashParentFor`,
  and shared placement inside `WebNoteEditService.edit`. Keep ordinary move
  reference rewriting distinct from trash reference choices and Undo's existing
  placement behavior.
- `NotebookGitStateLoader` loads folder DTO rows before mutation. Those rows
  cannot describe newly created folders. `WebFolderCreationService` already
  rereads folders after construction; `NotebookExportRows` and
  `PortableTreeSnapshot` own the canonical representation. Change the existing
  accepted edit owner to derive its final projection from current persisted
  state after the whole mutation, flushing when needed. Do not hand-patch a
  stale before-state list with specially recognized trash parents.
- `WebNoteCreationService` and `WebFolderCreationService` are additional
  accepted-tree writers; inspect and preserve their contracts when relocating
  shared projection helpers. Their special creation eligibility does not belong
  in note placement. Reuse/modularize the existing final projection responsibility
  where needed; a wholesale rewrite of creation orchestration is not necessary
  for this journey. No additional competing snapshot algorithm may be added.
- `AcceptedSnapshotPersistence` already appends a complete tree and stores
  bundle/head; it is not a low-level move listener. Snapshot only after folder
  construction, reference effects and placement are finished. Git's tree delta
  expresses old-path removal and new-path addition together.
- CLI `notebookPull` installs accepted trees by fast-forward. Publication has
  its separate, shared final-application responsibility under the other North
  Star topic. Neither requires a new trash protocol. ZIP export uses the same
  Portable representation but does not own accepted-history transactions.

Transaction contract: shared accepted-change service owns the actual transaction
and notebook writer lock. Controller outer transactions must not mask its
isolation. Reload mutation targets inside the boundary; do not continue with
stale request entities. Before-state eligibility must be checked before creating
trash folders or applying reference changes. Read the final complete state
inside the same transaction, then persist once. Ordinary Move recovery and Undo
must traverse the same consistency owner. Preserve existing fallback for an
unbound or already-drifted notebook; never resnapshot silently to make it eligible.

Relevant Accepted ADRs:
[0001](../../../docs/adrs/0001-ubiquitous-language.md) (paths versus identity),
[0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md)
(retained learning state),
[0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
(portable trash, reference format, `.keep`),
[0006](../../../docs/adrs/0006-failure-handling-accepted.md) (failure propagation),
and [0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
(disposable isolated test data). ADR 0002 remains Proposed. No exception or
new storage mechanism is proposed.

## Evidence and proof ownership

Existing evidence is a reuse baseline, not new Trash proof:

- Merged `NotebookGitWebNoteMoveControllerTest` drives the real move endpoint,
  downloads B and checks its parent/path/bytes, then publishes C derived from B
  and observes retained learning. The associated CLI feature exercises real
  clone/pull/publication; prior execution recorded four passing E2E scenarios.
- `NoteControllerTrashTests` covers retained identity, references, collisions
  and rejected Undo. `NoteTrashRecoveryLearningPreferencesTest` covers tracker
  state across Trash/Undo. Neither proves accepted Git trees.
- `NotebookGitConcurrentWriterTestSupport` queues real transactions behind the
  notebook binding lock; `NotebookGitPublicationConcurrencyControllerTest`
  observes committed parent ordering and stale-publication rejection.
- Existing web deletion E2E revisits trash and recovers using Move after reload.
  Reuse navigation from `note_deletion.feature`, installed CLI steps from
  `cli_notebook_web_note_moves.feature`, and original-note route observations.

New controller proofs use `NotebookGitWebContentControllerTestBase`,
`GitBundleTestReader`, real controller services and concise `makeMe` fixtures.
Seed/snapshot only before the action. Never repair the Git binding in an
intermediate Given after Trash to make recovery pass. Inspect downloaded bytes,
not only the stored head. Observe final state from a fresh persistence context
where committed atomicity matters. Test real behavior, not calls to helpers.

| Promise | Owning slice / observation |
| --- | --- |
| Shared owner, fresh final projection, preserved callers | 2: existing controller regressions and call-path review |
| Existing immediate Undo synchronized | 1: real Undo from synchronized trash → downloaded child; 4: actual Trash → Undo composition |
| Trash creates parents and publishes exact tree | 3: real Trash → download, original path absent, child commit, bytes and `.keep` |
| Web Trash → CLI pull → web Move → CLI pull | 4: installed CLI and original note route; no intermediate resnapshot |
| Retained note/tracker/recall identity, scheduling and preferences | 3–4: learned fixture after both actions, independently removed tracker, eligibility |
| Each reference choice included in the accepted result | 5–7: downloaded documents compared with existing domain outcomes |
| Collision preserves earlier trash | 8: first free suffix and unchanged earlier bytes |
| Rejected permission/destination actions preserve state | 9: real endpoints, fresh-context location/content/head/bundle |
| Concurrent writers retain both results | 10: queued real transactions and parent/tree observations |
| Pre-Trash proposal cannot overwrite accepted Trash | 11: stale publication rejected and B preserved |
| No-op/non-Git/drift fallback and existing edit/move behavior | 2 and full backend suite at each backend change; 9 adds Trash authorization guards |
| Clean pull and append-only ancestry | 4: existing CLI assertions at both pulls; no protocol changes |

## Ordered slices

All slices are sequential. Each is one evaluable behavior or immediately needed
structure; existing natural generality stays enabled throughout.

### 1. Keep existing immediate Undo on the accepted-change boundary
Type: Behavior
Status: done

A synchronized notebook already contains a trashed note at B (for example from
its current baseline). Existing same-notebook Undo appends C and restores its selected
prior location/title under current conflict rules. Use the same accepted-change
owner with existing Undo placement semantics, not ordinary Move's reference
rewrite recipe. Download C has B as parent and the recovered file; a checkout
at B can receive C directly. Preserve existing cross-notebook web behavior.

Proof: controller Undo → download from an initially synchronized trash fixture; reuse
existing learning/Undo coverage rather than repeat all canonical assertions.
Existing CLI fast-forward tests supply ancestor-receipt evidence; slice 4
will prove the browser/CLI round trip. No extra Undo UI or repeated-journey cleanup.
Safe stop: recovery of already-represented trash is synchronized. Deliver this
before advancing Git on Trash, so the existing shortcut remains consistent as
soon as slice 3 introduces that change. Slice 4 adds an actual Trash → Undo
continuation check without reseeding the binding between operations.
Sizing: 3–5 minutes plus suite.

Accepted proof:
- Promise: same-notebook Undo of already-represented trash appends C with
  parent B; downloaded tree has recovered path/bytes; former trash path absent.
- Boundary: `PATCH /api/notes/{note}/undo-trash` →
  `NoteTrashUndoService.undoSameNotebook` → `WebNoteEditService.edit` →
  `NoteMotionService.executePlacement`; accepted bundle download.
- Setup: `NotebookGitWebTrashUndoControllerTest.seedAlreadyRepresentedTrashUnderBiology`
  (Biology + `_trash/Biology/Cells.md`, `snapshotCurrentPortableTree` before
  Undo; no production Trash, no intermediate Git repair).
- Observations:
  `undoOfAlreadyRepresentedTrashAppendsAcceptedChildWithRecoveredPath`
  downloaded head == stored C; parentCount == 1 and parent == B;
  paths contain `Biology/Cells.md`; not `_trash/Biology/Cells.md`;
  blob bytes equal `CELLS_BODY`.
- Command: `unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL` then
  `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
- Result: pass (worktree unit DB; suite ~1m 10s). Existing
  `NoteControllerTrashTests` and `NoteTrashRecoveryLearningPreferencesTest`
  remain the identity/authorization coverage.

### 2. Read one complete final projection for accepted note changes
Type: Structure
Status: done

Keep existing content/title and same-notebook Move on one accepted edit owner.
Make its final snapshot/no-op comparison use current folders and authored notes
after mutation rather than pre-mutation DTO rows. Reuse existing export/state
loading owners; keep the accepted before-state comparison separate. This enables
slice 3's newly constructed ancestry. Preserve creation callers if a
shared helper is relocated, without moving their domain eligibility into it.

Proof: full backend suite, particularly content/title history, move variants,
creation projection and concurrency. Inspect that final projection has one
representation and the upcoming Trash recipe can finish before it is read.
Safe stop: all currently supported web behavior remains unchanged.
Sizing: 3–5 minutes plus required suite; low-medium confidence. Limit structural
work to the current projection/transaction owner, not a generic mutation framework.

Accepted proof:
- Promise: after-mutation no-op and snapshot use current persisted folders and
  live notes; before-state eligibility stays on the locked tree; existing
  edit/move/undo/creation behavior unchanged.
- Boundary: `WebNoteEditService.edit` (SERIALIZABLE + writer lock).
- Setup: existing controller fixtures; no new Trash Git scenario.
- Observations:
  - `acceptedTreeMatches(state, accepted)` still uses `LockedNotebookState`
    folders/liveNotes before `mutation.accept`.
  - `entityPersister.flush()` then one `foldersOf` + `liveNotesOf` pair feeds
    both `projection.matchesAcceptedTree` and `PortableTreeSnapshot.build`
    + `NotebookExportRows.notes`.
  - Mutation completes before that reread, so Trash can construct parents first.
  - `WebNoteCreationService` / `WebFolderCreationService` unchanged.
- Command: `unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL` then
  `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
- Result: pass. Newly created trash folders in the accepted tree remain slice 3.

### 3. Accept a web Trash with newly constructed parents
Type: Behavior
Status: done

Synchronized A has learned `Biology/Cells.md`, no `_trash`, and no referrers.
Trash uses its existing recipe inside the shared boundary and appends B once.
Download B: `_trash/Biology/Cells.md` has the original bytes, the active file is
absent, and an emptied Biology has the canonical marker. The original note and
learning data remain; location changes eligibility only. Use enough real recall
and tracker data to prove retention, including an independently removed tracker.

Proof: real NoteController Trash → bundle download and persisted learning
observations; assert B's sole parent is A. Include nested source ancestry in the
fixture so construction is real. No new target-preparation UI or CLI code.
Safe stop: web trash is represented completely; remaining recovery proofs stay
open. Keep current recovery behavior callable without a story-based restriction.
Sizing: 4–5 minutes plus suite, medium-low confidence; existing placement,
construction and snapshot owners are reused.

Accepted proof:
- Promise: Trash of learned nested `Biology/Cells.md` with no `_trash` appends
  one child B of A; downloaded tree has constructed `_trash/Biology/Cells.md`
  (original bytes), no active file, and `Biology/.keep`; note/tracker identity
  and recall survive; independently removed tracker stays removed; eligibility
  follows location.
- Boundary: `POST /api/notes/{note}/trash` → `NoteTrashService.trash` →
  `WebNoteEditService.edit` → existing reference/parent/placement recipe.
- Setup: `NotebookGitWebTrashControllerTest.seedLearnedCellsInBiologyOnlyWithoutTrash`
  (Biology with only Cells, no Readme, no `_trash`; learned tracker with
  recallCount 1; spelling tracker removedFromTracking; snapshot before Trash;
  `leaveDeadLinks`; no Git repair after).
- Observations: `trashOfLearnedNestedNoteAppendsAcceptedChildWithConstructedParents`
  downloaded head == B; parentCount == 1 and parent == A;
  paths containInAnyOrder `Biology/.keep`, `_trash/Biology/Cells.md`;
  trash bytes == CELLS_BODY; `.keep` empty; committed reload: same note id,
  isTrashed, learned tracker inactive, removed tracker still removed,
  recall_log count unchanged.
- Command: `unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL` then
  `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
- Result: pass. Recovery composition remains slice 4.

### 4. Receive Trash and ordinary Move recovery locally
Type: Behavior
Status: planned

Continue from actual accepted Trash B; ordinary Move to the existing active
folder appends C with parent B, without any resnapshot between actions. Download
and pull receive the recovered path with retained bytes and learning state.
Use the existing Move implementation and shared transaction owner; change only
what this real continuity exposes. Recovery must not enter the drift fallback.

Proof: extend a controller round trip with post-recovery learning invariants.
A sibling continuation uses actual Trash → Undo without an intermediate fixture
reset, confirming the already-delivered Undo boundary composes with Trash.
Add `e2e_test/features/cli/cli_notebook_web_trash.feature`: clone synchronized A,
web Trash, CLI pull and inspect trash path, reload/revisit, web Move, CLI pull,
inspect active path and original note route. Register with isolated CLI spec
selection. Assert clean accepted checkout and ancestry after each pull.
Safe stop: selected feedback journey works end to end.
Sizing: 4–5 minutes plus suite/E2E; reuse existing browser and CLI steps. A new
harness or protocol is not within this estimate and triggers reassessment.

### 5. Retain authored dead links when accepting Trash
Type: Behavior
Status: planned

An in-notebook referrer points to a note. Trash with leave-dead-links retains
its authored spelling in the downloaded tree while moving the target under
trash. Ordinary Move's rewrite policy must not leak into this recipe.

Proof: real Trash → download using the existing referrer fixture; assert the
referrer bytes, with target placement/parent shape already owned by slice 3.
Safe stop: sharing consistency has preserved the distinct Trash reference rule.
Sizing: 3–5 minutes plus suite; use the same production rule, not a new handler.

### 6. Accept the chosen removal from reference properties
Type: Behavior
Status: planned

Trash with REMOVE_FROM_PROPERTIES removes the selected target from referring
properties under existing semantics, retaining body text/dead links. The same
accepted tree contains the changed referrer and relocated target.

Proof: extend the existing remove-property controller fixture with an initial
Git binding and download after the real action; compare resulting authored
referrer content to the database. No snapshot between mutation and observation.
Safe stop: no reference edit is left outside the accepted result.
Sizing: 3–5 minutes plus suite; preserve current domain reference owner.

### 7. Accept existing relationship reduction as part of Trash
Type: Behavior
Status: planned

The existing eligible relationship Trash choice reduces it to the chosen source
property and retains the relationship in trash. Its source document and trash
file must appear together in the accepted tree. This preserves an existing web
choice; it introduces no local relationship-publication capability.

Proof: reuse `trashAppliesReduceToSourceReferenceChoiceWithoutSoftDeletingTheRelation`
setup, seed accepted state before acting, then inspect source property and
retained relationship file in the downloaded tree. Existing type/eligibility
rules remain with the domain operation.
Safe stop: all existing reference choices use the same complete snapshot rule.
Sizing: 3–5 minutes plus suite; no new relationship inference.

### 8. Preserve existing trash when choosing a collision suffix
Type: Behavior
Status: planned

Earlier `_trash/Biology/Cells.md` and `Cells (3).md` exist. Trash another active
Cells: the new accepted file uses `Cells (2).md`; earlier trash is unchanged.
Existing construction/title selection owns the rule; no collision-specific Git
branch is permitted. Existing empty folders retain canonical representation.

Proof: real Trash → download asserts incoming selected path and earlier bytes;
reuse existing web collision tests for UI behavior.
Safe stop: a valid collision cannot overwrite retained trash in either store.
Sizing: 3–5 minutes plus suite; expected data variation of slice 3's rule.

### 9. Keep rejected actions from changing accepted or live state
Type: Behavior
Status: planned

Existing authorization rejection for Trash and occupied-destination rejection
for recovery preserve note location/content and bundle/head. Exercise ordinary
Move and immediate Undo recovery with occupied destinations using existing
rules. Keep non-Git and already-drifted fallback behavior unchanged.

Proof: extend existing denial/conflict fixtures with a Git binding and observe
committed state in a fresh transaction. Reuse the real transaction helper; do
not treat the test's own rollback as production rollback evidence. Review the
actual shared transaction boundary and that all side effects are inside it.
No mock-induced tests of arbitrary loud infrastructure failures (ADR 0006).
Safe stop: rejected business operations preserve the accepted/live state.
Sizing: 3–5 minutes plus suite; reuse existing guard fixture patterns.

### 10. Serialize Trash with the next accepted writer
Type: Behavior
Status: planned

Queue a web edit behind Trash on the same notebook: the edit's accepted commit
has the Trash commit as parent and both results survive in the final tree.
This proves the new entry point uses the actual writer lock, without introducing
a new reconciliation policy.

Proof: `NotebookGitConcurrentWriterTestSupport` with real committed calls;
inspect resulting tree, parents and database state.
Safe stop: concurrent accepted changes keep both results in parent order.
Sizing: 4–5 minutes plus suite; reuse existing queued-writer harness.

### 11. Reject a proposal based on the pre-Trash head
Type: Behavior
Status: planned

An owner has a proposal based on A. Web Trash advances accepted history to B;
publication still expecting A is rejected by the existing stale-head rule,
leaving the complete Trash result intact. No reconciliation policy is added.

Proof: real Trash then real publication controller; reuse existing stale-head
fixtures/assertions with the new trigger. Observe unchanged B and note location
from committed state. Do not repeat queue scheduling already proved in slice 10.
Safe stop: all selected promises are proved, subject to required delivery/review.
Sizing: 3–5 minutes plus suite.

## Sizing, delivery and verification

Target about 5 minutes per leaf including focused tests; above 5 scrutinize,
above 10 stop and refine unless the excess is a required test/runtime wait.
The mandated full backend suite previously took about 1 minute; E2E startup/run
can take longer. Those measured runtime waits are explicit sizing exceptions,
not permission for more than 10 minutes of unresolved implementation work.
If projection extraction or browser setup exceeds assumptions, retain evidence
and refine this plan; do not silently expand the source story or add another
snapshot path. Reassess cumulative design, not just individual leaf size.

Required commands from the execution checkout:

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only
SUT_TIMEOUT_MS=360000 CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_web_trash.feature
```

Run the full backend suite for each backend change. Run focused E2E when its
behavior changes; reuse sufficient existing CLI evidence if CLI code is unchanged.
API generation only for changed wire signatures. During authorized execution,
follow dough-execute-plan: Jidoka, fresh independent post-change refactor,
coordinator's single `./scripts/run.sh pnpm format:changed`, plan update, commit
with check-only hook, push and asynchronous CI handling. Retain this plan and
source for retrospective and story wrap-up. Do not close the broader story 28
when only this first delivery is complete.

## Refinement assessment — 2026-09-15

Applied dough-slice-plan-refinement in place. The original reference-choice
slice became slices 5–7 because its three outcomes have distinct observable
rules. The original writer-order/stale-proposal slice became 10–11. Reordered
existing Undo before Trash so advancing Trash cannot strand the existing Undo
route outside accepted history. Placed final-projection Structure immediately before Trash, which needs new
folders; Undo reuses the existing edit owner directly. No completed slices or
proof were discarded.
Result: 11 slices, one Structure and ten Behavior; no story-resplit recommendation.

Cumulative design: one complete accepted-change boundary, shared current-state
projection and existing domain recipes. The later reference/collision examples
exercise that rule; they do not introduce per-example recognizers, wrappers or
commit mechanisms. The strongest smaller implementation—wrapping only Trash at
its controller—fails the approved cohesion/Undo-continuity requirements and the
fresh-folder snapshot requirement. A broader web-authoring rewrite is unnecessary.

No remaining slice requires an unexplained implementation path beyond 10
minutes on the inspected structure. Timing remains a hypothesis, especially
slice 2's extraction and slice 4's E2E adaptation; required backend/E2E runtime
waits are explicit exceptions above. If those assumptions fail, refine the same
plan rather than add special paths. No new product-scope decision is pending.

## Current decisions and learnings

- Undo to an existing prior folder does not create trash parents, so the
  current pre-mutation folder list is enough for slice 1. Slice 2 still owns
  rereading folders/notes after mutation for newly constructed Trash ancestry.
- Cross-notebook Undo stays outside `WebNoteEditService.edit`, matching
  ordinary Move.
- Same-notebook Undo orchestration lives in `NoteTrashUndoService` so
  `NoteController` stays under the file-size limit; it is not a second Git
  snapshot owner.
- Exposing `NotebookGitStateLoader.liveNotesOf` and flushing before the
  reread is enough for the edit owner. Folder creation still rereads folders
  only; do not merge those writers into one mutation framework.
- Keep note Trash and Undo as sibling recipes on the shared accepted-change
  owner. Folder trash stays on `NotebookController` until a later story.
