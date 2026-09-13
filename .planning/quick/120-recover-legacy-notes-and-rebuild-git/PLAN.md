# Recover retained deleted notes and rebuild notebook Git baselines

Status: in progress
Source: [SEED-009 story 31](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-31).

## Execution identity

- Originating checkout: `/Users/terryyin/git/doughnut` on branch `main`; claim commit `8205114639` (Taken-only).
- Execution checkout: `/Users/terryyin/git/doughnut-worktrees/120-recover-legacy-notes-and-rebuild-git` on branch `quick/120-recover-legacy-notes-and-rebuild-git`.
- Integration target: `main` (default).
- Backlog: story 31 moved to **Taken** in `PRODUCT-BACKLOG.md` (commit `8205114639`).
Authority: slice planning and optional refinement only. No implementation,
tagging, release, production read/copy/reset, or production migration is authorized.
Allocation: 120 follows allocated quick 119, now closed in Git history.

## Outcome and current decisions

An existing owner finds retained deleted notes in web trash, recovers them with
Move, and reuses former names. Note location replaces soft deletion completely.
The upgrade also replaces inconsistent Git history with a new root snapshot of
each live notebook's migrated Portable tree. The owner explicitly accepts loss
of old Git history; notebook data, note IDs, attachments, authored references,
and learning history remain intact during upgrade.
The replacement has exactly one root commit with no parent. Abandon the previous
base and every previous server commit. Do not append a reset commit, replay old
commits, merge histories, or preserve the old base inside the replacement bundle.

An accepted file deletion is permanent and removes the note's dependent data.
A recoverable deletion is an ordinary move beneath notebook-root `_trash`.
These are different user operations, not two persisted deletion states. New
files after permanent deletion get new identities. Web Trash remains recoverable;
do not turn its existing delete/undo callers into accidental permanent deletion.

Validate the actual upgrade and reset against isolated representative data in
this story. A later release, possibly after other stories, applies the upgrade
to production. No release tag, production backup, production reset, opt-in UI,
deployment workflow, or migration approval ceremony belongs to these slices.
Capture tested prerequisites and recovery instructions for the later release.

Deferred: Restore shortcut and automatic active-parent reconstruction (32),
folder Trash action (33), broader Git move/rename and web-to-local trash
synchronization (28), empty trash, permanent-delete UI, expiry, automatic old
checkout reconciliation, and 10,000-note performance commitments. Do not disable
deletion publication. Existing local checkouts are never erased by this work;
after the reset the supported starting point is a fresh checkout. Local-only
edits need separate preservation before an owner replaces their checkout.

## Architecture and reuse findings

The seed's **Portable trash — North Star and completion boundary** remains the
shared direction. No new architecture topic or independent state is needed.

| Responsibility | Existing solution and selected use |
| --- | --- |
| Trash placement and recovery | Reuse `FolderConstructionService.ensureTrashParentFor`, `NoteMotionService`, and `NoteController` trash/undo-trash. Extend their common placement rule only for demonstrated legacy collisions. |
| Availability | Retain `trashed_folder`, `Note` query constants and current-ancestry object checks; retire their legacy `deleted_at` branch after data conversion. Portable export includes trash; active search/learning does not. |
| Canonical tree and root bundle | Reuse `PortableTreeSnapshot`, `NotebookGitBundleBuilder`, and `NotebookGitBundleWriter`. `NotebookGitFleetCutoverBackfill` is the JDBC-before-JPA example, but its skip-existing-bindings selection does not reset inconsistent bindings. Do not alter committed migration V300000320 to repurpose it. |
| Binding replacement | Build the fresh single-root bundle before replacing the stored accepted head and bundle together. Do not use `AcceptedSnapshotPersistence`, which appends to an existing repository. The new repository is built only from current notebook data. |
| Permanent removal | Reuse existing FK cascade ownership and memory-tracker deletion evidence. Extend `DeletableEntityFkClosureTest` to the note root. Conversations have a restricting note FK; images currently use SET NULL, so neither is proof of complete note-dependent cleanup. |
| Outside-in evidence | Extend `NoteControllerTrashTests`, `NotebookGitDeletionPublicationControllerTest` and its atomic/container siblings, existing fleet-cutover tests, `StoredApiCollection` tests, and `note_deletion.feature`. Read back persisted state and downloaded Git bundles after clearing persistence context. |

Accepted [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md#trash)
owns location-based trash and retained Portable files;
[ADR 0005](../../../docs/adrs/0005-web-routes-accepted.md) owns identity URLs;
[ADR 0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md)
owns retained learning state;
[ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md) permits loud
failures instead of silent partial recovery;
[ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
requires disposable, isolated mutable test state. Relevant statuses agree with
the ADR index. ADR 0002 is Proposed. The owner's baseline-reset decision is a
one-time history replacement. Existing normal save behavior is unchanged, but
this story adds no separate post-reset commit-building capability.

## Execution and proof rules

Each leaf targets about 5 minutes including focused checks and local cleanup;
scrutinize work over 5 minutes and stop/finer-decompose before exceeding 10
minutes of implementation. Full required backend-suite runtime, service startup,
and isolated migration rehearsal runtime are explicit wait exceptions, not
permission for an unbounded implementation leaf. No elapsed timing proof exists
yet. Every slice must finish green, with the previous product path still usable.

Use test-first Behavior slices. Run all backend unit tests for backend changes:
`CURSOR_DEV=true nix develop -c pnpm backend:test_only`.
For migration changes run `CURSOR_DEV=true nix develop -c pnpm backend:verify`.
Frontend proof: `CURSOR_DEV=true nix develop -c pnpm frontend:test` with the
relevant existing test paths selected during execution. Browser proof:
`CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/note_deletion.feature`.
There is no requested manual testing.

For destructive removal fixtures, populate every table in the live FK closure,
including cascade descendants and restricting/SET NULL edges; assert unrelated
notes and their data survive. Inspect the current migrated schema, not baseline
SQL alone. Regenerate the ERD after schema changes. Preserve authored inbound
reference text; remove source-owned derived rows only with their deleted source.

Delivery follows dough-execute-plan: Jidoka, fresh independent
dough-post-change-refactor agent, API generation when needed, coordinator's one
`./scripts/run.sh pnpm format:changed`, plan update, commit, push, asynchronous
CI observation. Execution authority is separate. Keep the active plan and seed
for retrospective/wrap-up; do not clean them up after the last test alone.

## Ordered slices

### 1. Existing web removal uses recoverable trash
Type: Behavior
Status: done
Sizing: 5–8 minutes, medium confidence; backend/frontend suite wait exception.

Behavior: An owner uses an existing removal or undo-create flow; the note is
placed in trash through the current location rule, retaining the existing
reference-handling choice and supported Undo outcome. Adapt legacy callers in
`StoredApiCollection` to trash/undo-trash with prior placement where required.
Keep recovery of pre-migration soft-deleted data until the upgrade boundary.

Proof: Mounted/store entry-point examples for affected removal/Undo journeys;
controller reference-handling and trash examples preserve content and identity.
Do not silently map a recoverable web operation to Git permanent removal.
Safe stop: New web removals use trash; old data remains recoverable.

Learning: `StoredApi.deleteNote` had no production callers (components already
used `useNoteTrashFlow` → `trashNote`); the legacy soft-delete entry point was
already dead. Slice 1 removed it and routed `undoCreateNote` through `trashNote`.
Soft-delete recovery (`restoreDeletedNote` + `undoInner` "delete note" fallback)
is retained until slice 11. No backend production code changed; one focused
`NoteControllerTrashTests` example proves authored content survives trash/undo.

### 2. Note dependency ownership supports permanent removal
Type: Structure
Status: done
Sizing: 5–8 minutes, low confidence until the live FK closure is inspected.

Structure: Establish complete deletion ownership for note-dependent rows using
existing cascades and narrowly necessary explicit cleanup/FK changes. Inventory
conversation and image edges and all transitive dependents. Do not cascade into
other notes, notebook containers, users, or inbound reference sources. Enables
the immediately following Git deletion Behavior. Add note to the FK guard when
its deletion ownership can satisfy that guard; deliberate explicit cleanup must
be represented with an evidenced, reasoned exception rather than ignored edges.

Proof: Existing deletion/controller suite remains green, migrated schema matches
the intended closure, ERD regenerated for changed FKs. No permanent web-delete
UI or generalized deletion framework. Safe stop: Existing behaviors unchanged.

Learning: The live note FK closure had three edges the plan flagged as needing
human judgment. The owner chose CASCADE for all three — `conversation.note_id`
(note-dependent chat history, deletes with the note), `image.note_id` (1:1
note-owned uploads), and `recall_prompt.mcq_id` (removes the NO ACTION blocker;
recall_prompt is also CASCADE from memory_tracker). Migration
`V300000325__cascade_note_dependents_on_note_delete.sql` renames the constraints
(MySQL rejects re-adding a dropped name in the same ALTER) and preserves the
prior `ON UPDATE RESTRICT` on `image.note_id`. `note` added to
`HARD_DELETABLE_ROOTS`; `ALLOWED_RESTRICTING_FKS` stays empty. No JPA entity
changes were needed (plain `@ManyToOne`, no cascade conflicts).

### 3. Published file removal permanently deletes its note
Type: Behavior
Status: done
Sizing: about 5 minutes after slice 2, medium confidence; backend wait exception.

Behavior: A fresh valid Git proposal removes an ordinary note file; publication
removes that note and its complete dependent data, with its container and other
notes intact. Route the publisher to actual permanent removal instead of
`NoteService.destroy` soft deletion. Preserve LEAVE_DEAD_LINKS authored text.

Proof: Extend the Git publication controller example with the complete FK
fixture, read absence after a fresh transaction, and download the accepted tree
to observe file absence. Both learned and unlearned note shapes follow the same
rule; no recognition by fixture count. Safe stop: File deletion has its new
agreed meaning while retained legacy soft-deleted data still exists.

Learning: New `NoteService.permanentlyRemove` hard-deletes the note row (slice-2
CASCADE removes dependents); `NotebookGitProposalPublisher` DELETED branch now
calls it instead of `destroy`. `authored_note_reference` has no inbound target
FK to `note`, so source-owned refs are cascade-removed and inbound referrer text
survives as dead links (LEAVE_DEAD_LINKS). The retired "Git-deletion reserves the
deleted note's title" behavior (encoded by `NotebookGitDeletedDestinationControllerTest`)
was removed with the behavior change; slice 5 owns the positive same-path
recreation proof. Web `deleteNote`/`destroy`/`restore` soft-delete paths are
untouched (legacy recovery until slice 11).

### 4. Failed deletion publication leaves the accepted notebook intact
Type: Behavior
Status: done
Sizing: about 5 minutes, medium confidence; backend wait exception.

Behavior: A proposal containing a deletion fails an existing publication check;
the accepted tree, note, and dependent records remain unchanged.

Proof: Extend existing atomic controller cases with populated dependents;
observe persisted head/bundle and note data in a new transaction. These examples
extend the publisher's single transaction rule, not a deletion journal.
Safe stop: Permanent removal does not weaken existing publication atomicity.

Learning: Extended `NotebookGitDeletionPublicationAtomicControllerTest` with the
complete dependent fixture (memory_tracker, recall_prompt, mcq, image,
conversation) on the would-be-deleted notes (learned + unlearned shapes), captured
dependent counts before an injected binding-save failure, and asserted the counts
are unchanged after rollback — the CASCADE deletion rolled back with the
publication transaction. Updated the old `getDeletedAt() == null` assertions to
permanent-removal semantics (the note row survives the hard-delete rollback and was
never soft-deleted). The existing failure-injection point already fires after the
permanent-removal hard delete, so no new injection was needed. Refactor collapsed
the duplicated note-dependent count helpers into the shared
`NotebookGitBundleControllerTestBase`.

### 4a. Retrying an accepted deletion has no second effect
Type: Behavior
Status: done
Sizing: about 5 minutes, medium confidence; backend wait exception.

Behavior: A successfully accepted deletion proposal is retried through the
existing publication retry contract; it does not attempt a second deletion or
advance the accepted head again.
Proof: Extend the existing retry controller example, comparing head/bundle and
the absent note/dependents across the retry. Reuse accepted-proposal identity;
do not introduce a deletion tombstone or soft-delete marker for retry handling.
Safe stop: The publisher's existing retry contract works with permanent removal.

Learning: Extended `retriesAnAcceptedDeletionWithoutChangingHeadOrResurrectingTheNote`
with the complete dependent fixture (memory_tracker, recall_prompt, mcq, image,
conversation) on the deleted Target note. `publicationState` now carries
`DependentCounts`, so across the retry the test compares the full dependent
closure (equal before/after, and `== allAbsent()`), the accepted head/bundle
unchanged, and the retained note/container intact. Reused accepted-proposal
identity (same initialHead + same proposal bytes); no tombstone or new retry
mechanism. `DependentCounts`/`dependentCounts`/`countConversationMessagesByNoteId`
extracted to the shared `NotebookGitBundleControllerTestBase` (one representation
of the dependent-closure concept), trimming slice 4's committed test to use the
inherited helpers.

### 5. Recreating a permanently removed file creates a new note
Type: Behavior
Status: done
Sizing: about 5 minutes, medium confidence; backend wait exception.

Behavior: After accepted deletion, a later valid publication recreates the same
path; it creates a new note identity without the deleted note's learning state.
Proof: Publish/delete/recreate through existing controller helpers and read back
IDs and learning association. Do not add support for new rename/move shapes.
Safe stop: Path reuse cannot resurrect permanently removed data.

Learning: Added `recreatesSamePathWithAFreshIdentityAfterAcceptedPermanentRemoval`
to `NotebookGitCopyIdentityControllerTest` — the positive proof replacing the
retired `NotebookGitDeletedDestinationControllerTest`. No production change was
needed: slice 3's hard delete frees the path, and
`NoteTitlePlacementRules.requireNoSoftDeletedTitleAt` only matches soft-deleted
(`deletedAt IS NOT NULL`) notes, so same-path recreation works out of the box.
The new test asserts only its unique delta (same-path recreation → fresh id,
expected title/content, no inherited private associations); the deletion-
permanence post-condition is already owned by the sibling
`publishesALaterSameContentAdditionWithAFreshIdentityAfterAcceptedDeletion`.

### 6. A migrated notebook can receive a consistent new Git baseline
Type: Behavior
Status: done
Sizing: 5–8 minutes, medium confidence; backend wait exception.

Behavior: The upgrade operation receives a notebook with existing inconsistent
binding/history and retained active/trash content; it replaces head and bundle
with one new root commit whose Portable tree equals that database content.
Include folder/notebook Readmes and authored frontmatter through the existing
codec. Existing binding identity/ownership should be retained where possible;
no destructive delete-and-reinsert of user data.

Proof: Invoke the actual baseline-rebuild entry point on isolated database data,
then download/read its bundle and compare exact entries and parent count zero.
Prove retained database identities/data unchanged. Implement as migration-owned
JDBC-capable behavior usable before JPA; register no automatic upgrade yet.
Safe stop: Tested replacement operation exists; ordinary startup is unchanged.

Learning: Added `NotebookGitBaselineRebuild.rebuildNotebook`, a JDBC operation
that mirrors `NotebookGitFleetCutoverBackfill` but `UPDATE`s an existing
`notebook_git_binding` row (head + bundle + accepted_at) instead of inserting.
It is atomic: `setAutoCommit(false)`, commit on success, rollback on error,
restore auto-commit in `finally`. It reuses `PortableTreeSnapshot`,
`NotebookGitBundleBuilder`, and `NotebookGitBundleWriter` to build a fresh
single-root commit from current DB content (active and trashed notes,
folders, notebook Readme). `NotebookGitBaselineRebuildTest` proves an
inconsistent binding is replaced with a fresh single-root snapshot whose
tree equals current DB content, while notebook identity/ownership is
retained. Refactor extracted shared JDBC row-reading SQL/helpers
(`readNotebookReadme`, `readFolders`, `readNotes`, `readNullableInt`) into a
new `NotebookGitRows` class used by both `NotebookGitFleetCutoverBackfill`
and `NotebookGitBaselineRebuild`; full backend suite stayed green.

### 7. Baseline replacement fails and retries without partial binding state
Type: Behavior
Status: done
Sizing: 5–8 minutes, low confidence; MySQL 8.4 proof and suite wait exception.

Behavior: Failure during a multi-notebook rebuild leaves each persisted binding
internally consistent; retry finishes the intended reset. A completed Flyway
upgrade is not re-applied on normal restart.
Choose transaction/retry ownership at the migration boundary. Reuse Flyway's
completion lifecycle; no permanent reset flag or alternate Git authority.

Proof: Run the actual rebuild on isolated MySQL 8.4 with an induced middle-run
failure, compare head/bundle pairs and retained data, then retry and compare
final trees. The pre-JPA JDBC helper must explicitly roll back pending changes
on error; restoring auto-commit must not accidentally commit them. Normal
restart proof is completed with registered migration in slice 11.
Safe stop: Reset lifecycle has evidence before integration into startup.

Learning: Added a focused test
`aMiddleRunFailureLeavesEachPersistedBindingConsistentAndRetryFinishesTheReset`
to `NotebookGitBaselineRebuildTest` (test-only; no production change). Three
notebooks A/B/C are backfilled so each gets a binding, then C's binding row is
deleted. `rebuildNotebook(C)` builds C's bundle (the middle of the run) and
fails at the `UPDATE` step with 0 rows affected → `SQLException`. The test
proves A's and B's bindings are byte-for-byte unchanged (head, bundle,
`updated_at`) after C's failure — each binding stays internally consistent with
no partial new-bundle/old-head state, and the rollback plus
`finally { setAutoCommit(original) }` committed nothing. Retrying
`rebuildNotebook(A)` then `rebuildNotebook(B)` succeeds, replacing both with
fresh single-root baselines whose Portable trees equal each notebook's current
DB content (verified via bundle read-back). Per-notebook transaction ownership
was chosen over a `rebuildAllNotebooks` entry point: `rebuildNotebook` already
manages its own transaction and its only DB write is the `UPDATE`, so
per-notebook atomicity plus independent retry is the simplest consistent
contract. Normal-restart not-re-applied is deferred to slice 11. Refactor
extracted the shared bundle read-back + tree-equality check into
`assertBundleTreeEqualsCurrentContent`, now reused by both the happy-path and
failure/retry tests.

### 8. A legacy deleted note migrates into recoverable web trash
Type: Behavior
Status: planned
Sizing: 5–8 minutes, low confidence; isolated SQL proof wait exception.

Behavior: A representative old-schema notebook with a deleted learned note
upgrades its data to `_trash/<current-folder-path>/<title>` without changing
identity, content, attachments, authored references, or learning preferences.
Clear its old marker only after valid placement. All retained notes are covered,
including those in deleted notebooks without making their notebook active.

Proof: Run the candidate SQL against an isolated old-schema fixture, rebuild the
baseline via slice 6, and load through current controller/UI boundaries. Observe
trash visibility and direct access, learning exclusion, and Move recovery to an
existing active folder or notebook root. Use a fixture containing actual legacy
rows; injecting already-migrated rows does not prove migration.

Keep the candidate SQL outside the auto-discovered versioned Flyway directory
until its cases are complete. It is the actual upgrade recipe, not a second
runtime migration algorithm. Slice 11 registers the validated recipe immutably.
Safe stop: Validated candidate upgrade; deployed startup behavior unchanged.

### 9. Migration preserves existing trash and colliding legacy content
Type: Behavior
Status: planned
Sizing: 5–8 minutes, low confidence; isolated SQL proof wait exception.

Behavior: Existing trash is preserved while colliding incoming legacy notes
receive the first available basename suffix. A legacy note already under trash
keeps that location, without a second `_trash` prefix. Match root `_trash`
case-insensitively; preserve existing contents and Readmes.

Proof: Execute the same candidate SQL with occupied paths, existing suffixes,
mixed-case trash root, and a legacy marker already inside trash. Compare IDs and
payloads before/after and check every final path is unique. Inspect note/folder
namespace rules before adding an intermediate-path special case: use the
existing domain rule, and surface only a genuinely unresolved placement policy.
Safe stop: Candidate migration handles coexistence without loss or overwrites.

### 10. Consolidate the remaining legacy availability boundary
Type: Structure
Status: planned
Sizing: 5–8 minutes, low confidence due to fixture breadth; suite wait exception.

Structure: Narrow remaining live reads of note soft-deletion state to the
existing shared availability/legacy recovery boundaries so their final removal
is a small switch. Adapt ordinary inactive-note fixtures to use real trash;
retain old-marker fixtures only where they prove migration or interim recovery.
Account for startup backfill queries and title-conflict/undo callers. Preserve
historical Flyway ordering: historical SQL/helpers must still see the schema
version they were written for. Do not remove notebook deletion or edit committed
migrations. Enables the immediately following upgrade Behavior.

Proof: Full backend/frontend suites preserve current behavior; source inventory
enumerates remaining legacy reads/writes and their removal in slice 11. Avoid a
new availability abstraction, adapter registry, or a broad test rewrite.
Safe stop: Legacy data still works; final switch has a bounded inventory.

### 11. The real upgrade retires note soft deletion and resets Git once
Type: Behavior
Status: planned
Sizing: 5–8 minutes after preceding proof, low confidence; full migration/test
runtime exception. Reassess before starting if the remaining inventory exceeds
one proof loop or 10 minutes of implementation.

Behavior: Starting from the supported old database schema, the real application
upgrade migrates all retained deleted notes, rebuilds live notebooks' baselines,
and removes note `deleted_at` and its live model/undo/title-resurrection paths.
Search, recall, assimilation and wiki eligibility now follow location alone;
Portable snapshots still include trashed notes. New notes at old paths get new
identities; ordinary occupied-destination conflicts remain.

Register the validated SQL and JDBC reset using new versions above the current
highest migration at execution time; never edit a committed migration. Order
data placement, reset and schema removal so each retry can recover. Keep
legacy schema available to any historical migration that requires it. Add no
default-off placeholder or production gate: the owner's direct-migration
decision applies when this product is later released.

Proof: Actual Flyway old-to-new chain with populated old data, followed by fresh
JPA/controller load; backend:verify plus source/schema inventory proving no
live note soft-delete concept. Restart through Flyway and prove it preserves a
completed replacement unchanged. Regenerate API if changed and the database ERD.
Safe stop: Upgrade is fully usable and repeatable; production is untouched.

### 12. The rehearsed upgrade preserves all retained notebook data
Type: Behavior
Status: planned
Sizing: about 5 minutes of verification changes, medium confidence; backend
runtime exception.

Behavior: The actual isolated upgrade preserves all retained notebook data while
replacing the Git base and moving legacy deleted notes into trash.

Proof: Compare retained-data manifests before/after the upgrade, including complete dependent
data, not row counts alone. Do not obtain production data for this proof.
Read the new bundle to assert exactly one reachable root commit, with no parents
and no old history; compare its complete Portable tree to current notebook data.

Record commands, schema versions, fixture coverage, observed outcomes, and
limitations here during execution. Document that the later release must exclude
concurrent old-app writes and provide a fresh checkout; do not implement a new
maintenance-mode product feature or perform the release.
Safe stop: Validated upgrade and existing user journeys; all product data intact
except files explicitly deleted in the permanent-removal acceptance examples.

### 13. An owner recovers a migrated note through existing web navigation
Type: Behavior
Status: planned
Sizing: about 5 minutes, medium confidence; E2E runtime exception.

Behavior: After the real isolated upgrade, an owner browses a migrated note in
trash, opens its retained URL, and recovers it with Move. Its old active name
can belong to an independent new note; occupied destinations keep ordinary
conflict behavior. Move to root works when the original parent is absent.

Proof: Extend `note_deletion.feature` using legacy-data provenance from the real
upgrade fixture. Reuse existing navigation/Move steps and focused controller
examples for title reuse and learning preferences. Do not satisfy the migration
precondition by injecting already-trashed notes. The browser scenario owns the
recovery journey; data manifest and bundle shape stay owned by slice 12.
Safe stop: The migrated data is usable through the existing web product.

## Promise ownership and representative storage proof

| Promise | Owning slices / observable proof |
| --- | --- |
| Web removal and Undo remain recoverable with reference choices | 1; existing frontend/controller behavior |
| Real Git file removal deletes full dependency closure | 2–3; populated FK closure and accepted bundle |
| Failed/repeated deletion is atomic; recreation has new identity | 4, 4a and 5; committed publication controller observations |
| Replacement baseline includes exact active/trash Portable content | 6; downloaded root tree equality |
| No inconsistent binding after failure; no repeat reset after restart | 7 and 11; MySQL failure/retry and real Flyway restart |
| Upgrade retains legacy IDs, payloads, dependent records and preferences | 8–9 and 12; old-schema migration plus before/after manifests |
| Trash browsing, direct URL, Move, title reuse, availability | 8, 11 and 13; controller and real browser observations |
| Only note soft-delete machinery is retired | 10–11; live source inventory, schema and regression suite |
| Retained data survives the actual upgrade; no release performed | 12; isolated rehearsal and evidence report |

Engine assumption: MySQL 8.4 (repo `mysql84`) can execute the selected path
placement SQL and the chosen JDBC/Flyway retry boundary without partial loss.
Literal proof command: `CURSOR_DEV=true nix develop -c pnpm backend:verify`.
The suite must invoke the actual candidate/registered migrations against a
separate disposable schema containing the old data; migrating an empty test DB
is insufficient. Critical postconditions: retained-data manifests match,
placement is valid, head/bundle agree, and retry/restart has the documented
effect. Result: **not run; execution-owned in 7–9 and 11**. Failed representative
proof changes the plan before registering migrations or broad schema retirement.

## Construction assessment

One domain rule evolves throughout: location owns recoverability, file absence
owns permanent deletion, and Git is rebuilt from the migrated Portable tree.
The SQL recipe is migration-only because it runs before JPA, not another runtime
trash authority. The strongest smaller alternative—retaining soft deletion—was
explicitly rejected by the owner in favor of full retirement and a fresh baseline.

## Refinement assessment

Applied in place under the owner's optional-refinement request. Split the former
slice 4 rollback/retry pair into 4 and 4a, and the former slice 12 data/browser
proof into 12 and 13. Result: **14 slices**, all planned; none implemented.

Slice 9's cases exercise one placement rule at one migration boundary; they do
not justify separate production mechanisms or extra slices just for fixture
variations. The fresh base uses no old repository, parent commit, replay or merge.
The owner's simplification removed an extra post-reset content-commit journey.

Slices 1–9, 12–13 have bounded proof hypotheses, with the stated low-confidence
storage/closure points resolved by their actual representative checks. Slices
10–11 retain a concrete sizing concern: fixture/read-site breadth and the final
old-schema/new-runtime switch. Further arbitrary file-level subdivision would
not provide a safe boundary. Before execution of 10, use the then-current source
inventory and preceding migration evidence to refine 10–11 if they cannot fit
their 5–8 minute hypotheses. Neither may overrun the 10-minute implementation
limit under the test-runtime exception. No readiness certification is made for
those leaves on source inspection alone.

No further product decision is currently required. Remaining questions are
implementation evidence: live FK closure, intermediate-path collision fit, and
actual MySQL/Flyway failure/retry semantics. They do not authorize a schema flag,
a Git history compatibility layer, or production access. Execution has not been
requested; this plan and its isolated-validation work remain unexecuted.
