# Keep notebook-root attachments through local and web changes

Status: in execution. First story and slice plan refined.
Work item: **SEED-035#story-6**.
Source: [refined story](../../seeds/SEED-035-ai-workspace-supporting-files.md#story-6).
This reuses the original plan after its authorized resplit; nothing was delivered
or discarded. The complete redistribution is recorded below.

## Execution identity

- Mode: Story Branch Mode.
- Originating checkout: `/Users/terryyin/git/doughnut`, integration branch `main`.
  Queue claim commit: `d05a76bfbb`.
- Execution checkout:
  `/Users/terryyin/git/doughnut/.claude/worktrees/claude+260920-notebook-attachment-continuity`.
- Execution branch: `worktree-claude+260920-notebook-attachment-continuity`.
- Integration checkout/branch for later wrap-up: `/Users/terryyin/git/doughnut`, `main`.
- Authorized remote target: `origin`, execution branch (Story Branch Mode).
- CI: GitHub Actions, workflow `ci.yml`, display name `donut CI`, branch
  `worktree-claude+260920-notebook-attachment-continuity`.
  Observer directory: `/tmp/dough-ci-501/watch-aHFfBb`.
- Replanning permission: allowed (existing planning authority preserved; no
  `--no-replan` supplied).
- Checkout-bound skill runtime: `.agents/skills/dough-execute-plan`.

## Outcome and safe stopping point

Owners keep non-Markdown files at the notebook root, publish local changes,
continue ordinary web note work, and recover exact file bytes in another checkout.
Root files remain independent of referring notes. Existing note/folder operations,
ZIP export and history reset must preserve these root files. Private note identities
and learning data, Markdown validation, permissions and accepted-history rules stay
unchanged. Text and binary files use the same behavior.

Examples: publish `reference.json` and binary `diagram.png`, web-edit a typed note,
then pull a second clean checkout; its file bytes match. Publish only a file edit,
rename or removal, including an initial file-only tree; no dummy note is needed.
Invalid `AGENTS.md` plus a file change rejects the whole proposal. Removing a
referring note leaves the root file intact.

Nested attachment placement and attachment-bearing folder lifecycles belong to
[story 9](../../seeds/SEED-035-ai-workspace-supporting-files.md#story-9) and its
[mapped plan](../003-notebook-folder-attachments/PLAN.md). They are not prerequisites
for this usable root workflow. Until that story supplies containment safety,
retain the existing rejection of nested non-Markdown proposals atomically; never
accept files that later web operations can drop. This is an interim admission
boundary, removed by story 9, not a new permanent format or attachment type.
Existing Markdown folders and their operations continue unchanged.

No individual web attachment controls, image rendering/migration, assimilation
exclusion, AI behavior, new transport or broader rebase assistance is included.
Cross-notebook publication remains outside the current accepted-change owner's
coverage. No extension, MIME or IDE allowlist is introduced.

## Architecture and source evidence

Follow the three [North Star decisions](../../NORTH-STAR.md),
[ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md),
[ADR 0002](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md),
[ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md),
[ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md), and
[ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md).
Their index and in-file statuses were checked as Accepted. No new ADR decision
or North Star topic is needed. The narrower delivery does not redefine Attachment.

PFE findings carried forward and narrowed against current callers:

| Existing responsibility | Decision for this story |
| --- | --- |
| `PortableTreeEntry`, `NotebookGitAcceptedTree`, `NotebookGitBundleBuilder`, `NotebookZipBuilder` convert all blobs through String | Use exact byte content with value equality in the existing shared entry; decode Markdown only at its codec. Preserve unchanged blob reuse. |
| `PortableTreeSnapshot` and `NotebookExportRows` serve projection comparison, web commits, ZIP and cutover/reset | Extend the same snapshot inputs/loaders with root attachments. No per-caller file overlay. |
| Proposal inspection, note correspondence, document application and acceptance | Classify attachments once, keep them out of note correspondence, apply their final tip once within the existing transaction. Preserve complete raw-tree evidence for existing folder matching. |
| Existing `Attachment`/`Image`/`AttachmentBlob` own uploads and note images | Do not reuse their note/user ownership or upload routes. A notebook-owned projection is the distinct current responsibility; later image migration will use that model. |
| `NotebookGitCutoverService.resetHistory` rebuilds from current projection | Include root attachment bytes in that common source before admission ships. No new reset semantics. |
| CLI publish sends raw bundles; clean clone/pull uses native Git | Reuse these paths; no new client classifier or rebase recognizer. |

Use one small notebook-owned projection (`NotebookAttachment` is a suitable name)
with complete filename and bytes. Its SQL content is a projection of accepted Git,
not a second authority. Ordinary notebook ownership/FK cleanup is sufficient now;
folder containment, folder collision rules and subtree mutation wait for story 9.
Do not build a separate root-attachment entity/service or optional strategy that
must be discarded later. Extend this same entity with folder placement when needed.
No private ID becomes a Portable path, and no attachment identity matching is needed.

The same filename comparison must apply in SQL and the Git projection; distinct
Git paths must not be silently collapsed by database collation. Use the established
file path's exact spelling, not note-title normalization. Content-based byte
comparison must retain no-op detection. `.keep` remains structural under the
existing rules. All Markdown keeps existing type, Readme and UTF-8 validation.

For every proposal, compare accepted Git against the live projection before any
mutation; validate the whole tip; apply its final attachment set; compare the
complete result before persisting the accepted binding. Keep existing locking,
rollback and authorization. No special attachment transaction or head writer.
Changes to unrelated Markdown must preserve unchanged root files.

Taken plan 001 may change repository persistence. Reuse its owner if it lands
first; do not restore bundle-backed storage or create an alternative store.

## Refined slices

Target five active minutes including local cleanup; estimates below are 5–8
minutes with medium confidence. Over five requires scrutiny; over ten requires
stopping and finer decomposition. Full required suite/migration/E2E runtime is
the only elapsed-time exception, not coding or debugging. All slices are planned.

### 1. Preserve file bytes in the shared tree representation

Type: Structure. Status: **done**. Estimate: 5–8 active minutes; actual ~6.
Change the existing entry and its Git/ZIP readers, writers and equality to exact
bytes; keep Markdown construction/decoding explicit. No admission change. This
immediately enables slice 2's codec result.
Proof: full backend suite; existing Markdown bytes, Git object reuse, ZIP output,
`.keep`, projection comparisons and no-op saves remain unchanged. Adapt fixture
text accessors without decoding binary observation data.

Accepted proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`, pass.
Boundary: notebook Git publication/acceptance controllers, notebook export, Git
binding persistence. Inspected locations: `PortableTreeEntry.equals`/`hashCode`
(content-based value equality), `PortableTreeEntry.ofText` (the single UTF-8
encode point), `NotebookGitAcceptedTree.readEntries`, `NotebookGitBundleBuilder`
unchanged-blob reuse, `NotebookZipBuilder.writeEntry`,
`NotebookGitProjection.matchesAcceptedTree`,
`AcceptedWebChangeService.commitIfChanged`, `GitBundleTestReader.readTreeEntries`.

Delivered design: `PortableTreeEntry` is `record(String path, byte[] content)`
with overridden content-based equality; `ofText(path, text)` on the record is the
only place text becomes file bytes. Value equality is what preserves
projection-drift detection and no-op-save detection, which compare entry lists.
`NotebookGitBundleBuilder` reuses an unchanged blob via
`acceptedEntries.contains(entry)` on a `Set<PortableTreeEntry>`, still guarded by
the dircache path/file-mode check. `NotebookGitProposalFile.asProposal(entries)`
now owns the test-side proposal conversion that four relocation tests duplicated.

Learnings for later slices:

- Root attachments are built as `new PortableTreeEntry(path, bytes)` directly —
  no text path, no encode. Emit them in `PortableTreeSnapshot`'s root pass so
  they count toward the otherwise-empty-leaf `.keep` rule.
- `PortableTreeSnapshot` emits README, then notes, then subfolders. Only ZIP
  output is order-sensitive; `NotebookGitAcceptedTree.sorted` re-sorts the Git
  side by path.
- The test-side `NotebookGitProposalFile` already has a `byte[]` constructor for
  deliberately invalid UTF-8, so slice 2/4 fixtures can carry binary bytes end to
  end with no new test plumbing.
- Pre-existing size debt, untouched by this slice and not this story's work:
  `NotebookGitBundleControllerTestBase` (258 lines) and
  `NotebookGitProposalAncestryControllerTest` (355 lines) exceed the 250-line
  guidance, both already over at the branch point.

### 2. Serialize root attachments with the notebook tree

Type: Behavior. Status: **done**. Estimate: about 5 active minutes; actual ~5.
Given note/folder values plus named root attachments, canonical serialization
retains each complete filename and exact bytes. Extend the snapshot's attachment
input, with no nested-placement promise or MIME-specific branch.
Proof: `PortableTreeSnapshotTest` and `NotebookZipBuilderTest` inspect one mixed
serialized tree, including invalid UTF-8 binary bytes and an empty file. These
prove the codec only; they do not fabricate successful publication. Full suite.

Accepted proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`, pass.
Boundary: notebook export serialization. Inspected locations:
`PortableTreeSnapshotTest.keepsCompleteFilenamesAndExactBytesOfRootAttachmentsSortedAfterNotes`
(attachments supplied out of order, asserted in sorted canonical position between
notes and the subfolder; `FF FE 00 80` invalid-UTF-8 file and a zero-byte file
compared by exact bytes) and
`NotebookZipBuilderTest.writesRootAttachmentBytesUnchangedAlongsideNotes`
(raw ZIP entry bytes, never the UTF-8-decoding reader).

Delivered design: `ExportAttachmentRow(String filename, byte[] content)` joins the
`ExportFolderRow`/`ExportNoteRow` family. `PortableTreeSnapshot.build` and
`NotebookZipBuilder.build` take a fourth `List<ExportAttachmentRow> rootAttachments`.
Canonical order within a directory is README, notes, attachments sorted by
filename, then subdirectories; attachments are emitted in the root pass so they
count toward the otherwise-empty-leaf accounting. Attachment entries are built as
`new PortableTreeEntry(path, bytes)` — never through `ofText`, so no encode,
decode, MIME sniffing or extension rule exists anywhere. All four production
callers pass `List.of()`; no publication is fabricated and no projection seeded.
The refactor pass turned the static walker into a small instance with the grouped
maps as fields, cutting `collectDirectory` from 8 parameters to 4 and stating the
"null folder id means root" rule once.

Learnings for later slices:

- **Slice 3 uniqueness:** the codec does not deduplicate. Two rows with the same
  filename emit two entries at one path, which the Git side would silently
  collapse. Exact-filename uniqueness belongs to the schema/projection, with a
  case-sensitive binary collation per the plan's collation note.
- **Slice 3 load:** the natural shape is a `NotebookExportRows.attachments(...)`
  sibling of `folders(...)`/`notes(...)`. The four call sites to switch from
  `List.of()` are `NotebookExportService`, `NotebookGitProjection.matchesAcceptedTree`,
  `AcceptedWebChangeService.snapshot` and `NotebookGitCutoverService`. The second
  `matchesAcceptedTree(currentEntries, acceptedEntries)` overload needs no change.
  Those three sites fetch folders and notes side by side and will touch attachments
  too; consolidating that input shape is a cross-subsystem question for slice 3's
  design, deliberately not done during slice 2's refactor.
- **Ordering:** the repository query needs no `ORDER BY`; the snapshot sorts by
  filename and `NotebookGitAcceptedTree.sorted` re-sorts the Git side by path, so
  only ZIP output observes this order.
- `ExportAttachmentRow` holds a `byte[]` and so has identity equality. Harmless
  while only `PortableTreeEntry` is compared; a later slice needing no-op detection
  must compare entry lists rather than rows.
- **Story 9 readiness:** `attachmentsHere` is already per-directory with `List.of()`
  passed into subfolders, so nested placement becomes an `attachmentsByFolder` field
  symmetric with notes, without structural rework.

### 3. Include root files in the existing application projection

Type: Structure. Status: **done**. Estimate: 5–8 active minutes; actual ~8.
Add the minimal notebook-owned row/repository and schema, exact-filename uniqueness,
notebook-deletion cleanup, and one shared export-row load used by all snapshot
callers. No folder FK, upload migration, endpoint, or subtree behavior. Immediately
enables slice 4; root-only ownership removes the former containment uncertainty.
Proof: `backend:verify`, ERD and existing snapshot/cutover/export/drift tests.
Extend notebook FK-closure fixtures for the added dependent row. Existing notebooks
have no attachment rows; existing image uploads keep their behavior.

Accepted proof: `CURSOR_DEV=true nix develop -c pnpm backend:verify`, pass (schema
change, so `verify` rather than `test_only`), plus a regenerated `docs/database-erd.md`
whose diff is additive only. Boundary: notebook ZIP export, notebook Git
publication/acceptance, cutover/history reset, projection-drift detection.

Delivered design: `notebook_attachment` (migration `V300000335`) holds notebook id,
filename and `longblob` content, with `UNIQUE KEY (notebook_id, filename)` and the
filename column in `utf8mb4_bin`. The binary collation is what makes SQL uniqueness
the same comparison Git uses on path bytes, so `Diagram.png` and `diagram.png`
remain two rows instead of colliding under the default `utf8mb4_unicode_ci`; the
table's own default collation is unchanged. FK to `notebook` is `ON DELETE CASCADE`.
`NotebookAttachment` + `NotebookAttachmentRepository` expose one JPQL constructor
query returning `ExportAttachmentRow`.

The refactor pass then consolidated the snapshot inputs: `NotebookExportRows` is
replaced by a `NotebookLivePortableTree` service that owns the three repositories
and is the single production assembly point for a notebook's live Portable tree.
Its two `entriesOf` overloads are deliberate — one loads note rows by query, the
other maps `Note` entities a locked publication already holds, and collapsing them
would change which query runs. `NotebookZipBuilder.build` now takes the finished
entry list, so ZIP writing no longer knows about export rows. Places that must
change when a later slice writes rows or story 9 adds folder placement: 4 → 1.

Plan-versus-reality corrections recorded here:

- **"Extend notebook FK-closure fixtures" had no work to do.**
  `DeletableEntityFkClosureTest` has no fixtures; it is schema-driven from
  `information_schema` and walks children of `HARD_DELETABLE_ROOTS`
  (`memory_tracker`, `note`). `notebook_attachment` is a child of `notebook`,
  which is a parent of `note`, so it is outside that closure by construction.
  Verified in the test source. The `ON DELETE CASCADE` cleanup the plan actually
  wanted is in place. Making `notebook` a declared hard-delete root would be a
  separate decision — notebook deletion is currently soft (`deletedAt`) — and was
  not taken.
- **No product test asserts the new schema behavior yet**, by design: nothing
  writes rows, so every caller observes an empty attachment set. Uniqueness and
  collation currently rest on the migration applying plus a one-off catalog query.
  **Slice 4 must pin the collation with an executable guard** when the first real
  rows appear.

Learnings for slice 4:

- All four snapshot readers are already wired, so slice 4 only needs to *write*
  rows. Consequently `matchesAcceptedTree` will start failing drift checks the
  moment rows exist without a matching accepted tree — the intended coupling, but
  it means the final attachment set must be projected **before** the post-mutation
  comparison, inside the same transaction.
- The repository currently exposes only the read-only export-row projection. Slice 4
  needs a way to load and replace the live `NotebookAttachment` entities.
- Uniqueness is enforced in SQL. A final-set projection that inserts before deleting
  removed rows will hit `uk_notebook_attachment_notebook_filename`; delete-then-insert
  or flush ordering matters. A rename reusing a filename is a normal case, not a
  failure case.
- Rollback seam: `AcceptedWebChangeService.apply` is
  `@Transactional(isolation = SERIALIZABLE, rollbackFor = Exception.class)` and
  `commitIfChanged` persists the binding at the end; the committed-transaction
  failure seam sits around that persistence, after the attachment projection.
- Removing the initial nonempty-Markdown prerequisite needs no codec work: a
  file-only initial tree already serializes correctly, and only admission refuses it.
- Nested-file refusal is untouched; the entity has no folder column, which is what
  makes story 9 an extension of this row rather than a replacement.

Open naming note (not this story's work): ADR 0001 defines **Attachment** as "a
named supporting file owned by a notebook", which is what `NotebookAttachment` is,
but that name is held by the legacy note-image `Attachment` entity. Converging the
two is a future story, per the NORTH-STAR line about images adding presentation to
the same attachment model.

### 4. Accept root files through the existing publication boundary

Type: Behavior. Status: **done**. Estimate: 5–8 active minutes; actual ~9.
A valid root-file proposal becomes the exact accepted tip and remains intact in
the next web note save. Add one attachment classification and final-set projection
inside current acceptance. Remove the initial nonempty-Markdown prerequisite for
root files. Retain the existing nested-file refusal until story 9.
Proof: publication controller → web content controller → downloaded blob bytes
and parent IDs. Test initial file-only acceptance as the same rule. At this same
boundary, mixed invalid Markdown, stale heads and nested-file proposals must leave
head/projection unchanged. Extend the existing committed-transaction failure seam
for rollback after file projection, rather than relying on test rollback. Full suite.
Existing authorization/mode/path/concurrency tests remain the owning evidence.

Accepted proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`, pass
(2545 tests, 0 failures). Boundary: notebook Git publication controller, bundle
download controller, web content save. Inspected locations in
`controllers/NotebookGitRootAttachmentPublicationControllerTest`:
`publishedRootFilesAreTheExactAcceptedTipAndSurviveTheNextWebNoteSave`,
`anInitialPublicationMayBeFileOnlyWithNoNoteAtAll`,
`invalidMarkdownBesideAValidFileChangeLeavesTheHeadAndTheFilesUnchanged`,
`aStaleHeadLeavesTheAcceptedFilesUnchanged`,
`aNestedFileIsStillRefusedAndLeavesTheAcceptedFilesUnchanged`; plus
`controllers/NotebookGitPublicationAtomicControllerTest.lateBindingSaveFailureRollsBackTheProjectedRootFilesAndAcceptedBinding`
(real committed-transaction rollback after the attachment projection, not
test-transaction rollback).

The collation guard slice 3 deferred is now executable: the publication test uses
`Diagram.png` and `diagram.png` holding different invalid-UTF-8 bytes, and the
post-web-save tip is rebuilt from the live projection, so a collapsed collation
would fail either the unique key or the tip comparison.

Delivered design: `NotebookGitProposalTreeShape.isRootAttachment(path)` is
`!path.endsWith(".md") && path.indexOf('/') < 0` — one predicate, not two
implementations. `noteChangesFrom` skips empty-folder markers and root attachments,
so attachments carry no note identity, title or memory tracker, while remaining in
the inspected files and `conceptDocuments` so folder matching keeps complete
raw-tree evidence. `NotebookGitProposalAcceptance.projectRootAttachments` makes the
stored set exactly the proposal tip's root set (delete absent, update changed bytes
in place, insert new) as the first statement of `acceptMatchingProposedTree`, before
`requireMatchingProposedTree`, so the post-mutation comparison sees the complete
result. Deletes and inserts are disjoint by construction — a filename the tip keeps
is updated in place — so the per-notebook filename key cannot trip on a rename.
Admission for an initial publication now uses
`NotebookGitProposalTreeShape.carriesPortableContent`, keeping the Markdown-suffix
rule out of the publisher.

Accepted product decisions recorded here:

- **A root file named `.keep` is now an ordinary Attachment.** `isEmptyFolderMarker`
  matches only `"/.keep"`, so a root `.keep` never had structural meaning; it was
  simply refused before. `PortableTreeSnapshot` emits `.keep` only under a non-empty
  path prefix, so it round-trips as an ordinary root file. Chosen over a special
  case. Nested `.keep` is unchanged and still structural.
- **Initial-publication refusal message** changed from "Initial publication requires
  a nonempty Markdown tree." to "… requires nonempty Portable content." No test,
  frontend string or E2E step referenced the old text. An initial tree of only
  `Folder/.keep` is still refused.
- **The reserved-file rejection suite's root `note.txt` case moved to
  `Topic/note.txt`.** Root `note.txt` is legitimately an Attachment now; the nested
  path preserves the same "not a Markdown note" refusal evidence.
- The publisher's old clause refusing any non-Markdown initial path is gone, but
  nested non-Markdown is still refused on initial publication: the publisher's
  content check runs before `noteChangesFrom`, so both paths refuse. Verified by
  tracing both.

Learnings for slices 5–9:

- **Slice 5:** an attachment-only proposal reaches the publisher's early-return
  branch (`noteChanges` and `documents` both empty), which does the pre-mutation
  drift check and then `acceptMatchingProposedTree`, so the projection already runs
  there. Note that branch **skips `assertValidTypedMarkdown`**, so an attachment-only
  change to a tree whose Markdown is already invalid is accepted — pre-existing
  behavior for no-op shapes; slice 5 should decide whether to state it.
  Identical-byte writes are already skipped by an `Arrays.equals` guard.
- **Slice 6:** attachments stay in `conceptDocuments` and the inspected-file list, so
  folder shape and relocation still see them as raw evidence, and the relocation
  branches converge on the same `acceptMatchingProposedTree`. Web-side note/folder
  operations need no production change.
- **Slice 7:** `NotebookExportService` already reads through `NotebookLivePortableTree`,
  so a ZIP test can publish through the real boundary instead of seeding the
  projection. In-ZIP canonical order is README → notes → attachments by filename →
  subfolders, which differs from Git's byte-wise path order.
- **Slice 8:** `NotebookGitCutoverService.resetHistory` rebuilds from the same live
  tree, so it needs no production change — only its proof.
- **Slice 9:** `NotebookGitProposalFile` carries raw `byte[]` and
  `GitBundleTestReader.readTreeEntries` gives exact bytes; the E2E will need a real
  binary fixture because the text step helpers cannot express invalid UTF-8.
- **Story 9 readiness:** only `isRootAttachment`'s `indexOf('/') < 0` and the entity's
  missing folder column separate root from nested Attachments. The final set is
  already keyed by full Portable path, so nested placement is a column plus a wider
  predicate, not a restructure.

### 5. Publish attachment-only edits, renames and removals

Type: Behavior. Status: **done**. Estimate: about 5 active minutes; actual ~8.
Given accepted root files, a local file-only range produces exactly its final
root-file set; no Markdown edit or attachment rename correspondence is required.
Use the same final-set rule for edit, rename and removal, including identical-byte
files and a multi-commit rename/edit. Do not replay commits into live mutations.
Proof: parameterized publication-controller cases compare final paths/bytes and
original ancestry. Assert the removed path is absent, not just the new path present.
No notes or memory trackers are created for attachments. Full backend suite.

Accepted proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`, pass
(2550 tests, 0 failures). Boundary: notebook Git publication controller, bundle
download controller, live attachment projection. All observations in the new
`controllers/NotebookGitRootAttachmentLocalChangeControllerTest`:
`aFileOnlyProposalBecomesExactlyItsFinalRootFileSet` (4 parameterized cases —
edit, rename keeping bytes, removal, and renaming one of two identical-byte files)
and `aMultiCommitRenameAndEditRangeLandsAsItsFinalSetOnItsOriginalAncestry`.

**No production change was needed.** Slice 4's `projectRootAttachments` final-set
rule already covered edit, rename, removal, identical bytes and multi-commit
ranges; every case passed on the first run. Recorded as the result rather than
manufacturing a change.

Proof quality notes:

- The implementer mutation-tested the proof: removing `entityPersister.remove`
  from `projectRootAttachments` failed 4 of the 5 cases (the pure-edit case
  correctly stayed green, since it removes nothing), then restored the file. The
  proof is not vacuous.
- `assertFinalRootFileSet` derives the vanished names from the baseline rather
  than hard-coding them per case, and asserts they appear in **neither** the
  accepted tip **nor** the live projection. It also asserts four root files yield
  exactly one note (`Root Note`, the only `.md`) and zero memory trackers.
- The multi-commit test asserts the first-parent chain still runs through both
  intermediate commits back to the previous accepted head, and that the range's
  intermediate filename `interim.json` was never a live Attachment row — the
  sharpest available evidence that commits are not replayed into live mutations.
- This slice is the first evidence that an *accepted* attachment-only proposal
  works end to end: slice 4's attachment proposals were either mixed with Markdown
  or were refusals, so this is the first proof that
  `NotebookGitProposalFolderShape.requireExactOrCarried` does not mistake a
  root-file-only change for a folder relocation and that the publisher's
  early-return branch reaches acceptance for this shape.

Decision recorded — **the `assertValidTypedMarkdown` early-return skip stays
unchanged and untested.** An attachment-only proposal takes the publisher's
early-return branch, which skips whole-tip Markdown validation. This is safe by
induction, not a hole: that branch is reached only when no Markdown differs
between the accepted tree and the tip, so the tip's Markdown is byte-identical to
accepted Markdown, which was itself either Donut-generated or validated at its own
admission. The case is unreachable through the product's own boundaries, and
testing it would require seeding an unreachable accepted state and would cement
behavior that is only incidentally correct. The story's atomicity promise concerns
invalid Markdown *in the proposal*, which is a Markdown change and never takes this
branch — slice 4 owns that evidence. Residual, recorded: if Markdown validation is
ever tightened, previously accepted trees stay accepted through attachment-only
changes until something touches their Markdown. That is a general grandfathering
property of the design, not an Attachment defect.

Learnings for slices 6–9:

- `NotebookGitProposalFile.asProposal(entries)` builds a proposal from a
  `List<PortableTreeEntry>`, so declaring a case as its *final tree* and deriving
  the proposal from it removes a class of hand-ordering mistakes and cut the test
  class from 301 to 236 lines. It works for multi-commit ranges via
  `commitOnTopOf(repo, parents, asProposal(entries), message)`.
- Expected tip lists must be in Git's byte-wise path order (capitals before
  lowercase). This is **not** the ZIP order slice 7 observes (README → notes →
  attachments by filename → subfolders).
- The live projection's row order is insertion order, which drifts from filename
  order after a rename; compare it sorted by filename.
- Slice 8's `NotebookGitHistoryResetControllerTest` reads the binding's stored
  `getBundleBytes()` directly rather than going through `downloadNotebookGitBundle`,
  so this class's private `acceptedTip` helper would not serve it — do not promote it.

Deliberately not changed, reported instead: `assertFinalRootFileSet`'s explicit
absence block is logically implied by the exact list equality above it (because
`PortableTreeEntry` compares content with `Arrays.equals`). It is retained because
this slice's plan text explicitly requires asserting the removed path is absent;
removing it would be a plan dispute rather than a refactor.

### 6. Keep root files independent of note and folder operations

Type: Behavior. Status: **done**. Estimate: 5–8 active minutes; actual ~7.
Existing note changes, including a supported local note relocation and web note
removal, leave root-file ownership/content unchanged. Existing Markdown-only
folder placement/dissolve/deletion cannot collect unrelated root attachments.
This is one independence rule; no new folder-file behavior is implemented.
Proof: extend existing mixed-editing/private-association and web note/folder
lifecycle fixtures with an accepted root file. Assert its bytes after the operation;
retain current note identity/learning assertions where the note survives. Full suite.
Root files cannot be children of a folder in this increment.

Accepted proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`, pass
(2555 tests, 0 failures). Boundary: publication and bundle-download controllers,
`NoteController.trashNote`/`permanentlyDeleteNote`,
`NotebookFolderController.moveFolder`/`dissolveFolder`/`trashFolder`/
`permanentlyDeleteFolder`, and the live attachment projection. All five cases live
in `controllers/NotebookGitRootAttachmentIndependenceControllerTest`: local note
relocation, web removal of a note whose body *refers* to the file, web folder
placement, folder dissolve, folder trash + permanent delete.

**No production change was needed**, as slice 4 predicted: every web note/folder
operation rebuilds through the single `NotebookLivePortableTree` assembly point.

Proof quality notes:

- `assertRootFilesSurvived` pins the tip's first parent to the head captured
  immediately before the operation, so a web operation that silently skips its
  commit cannot pass vacuously.
- `filesAmong` filters every non-Markdown, non-`/.keep` entry **at any depth**, so
  a file swept into a folder changes its path and fails, and a dropped file fails.
  That tree-wide filter, not the mutation check, is what carries the
  "cannot collect, cannot drop" evidence.
- Mutation-checked: making `NotebookLivePortableTree` attachment-blind failed all
  5 cases, restored from a backup copy. The implementer correctly reported this as
  a *coarse* coupling — it cannot distinguish "dropped by the web operation" from
  "never projected at all".
- Where the note survives, the existing shown-content, note-identity and
  memory-tracker assertions are retained unchanged.

Judgement calls recorded rather than hidden: web *note* move into a folder is not
separately covered (same rebuild path; slice 4 already proved a web note save
preserves root files), and trashing is covered only as the first step of the two
removal cases rather than asserted on its own.

Refactor pass outcome: the live root-attachment projection read had become three
copies — two verbatim and one, slice 4's, silently missing the sort. It is now one
`controllers/NotebookLiveProjectionTestReader`, carrying the
insertion-order-so-sort-by-filename rule in a single documented home, and the
concept has one name (`committedRootAttachments`) across all three classes. A
fourth inline user, `NotebookGitPublicationAtomicControllerTest`, was deliberately
left alone because delegating would nest committed transactions and weaken its
atomicity proof. Raw lines net +18 (a new file's ceremony) against 3→1 copies of a
real gotcha — accepted as the better trade.

The bundle-tip-reading scaffolding duplicated across ~60 `NotebookGit*ControllerTest`
classes was deliberately **not** touched: a helper shared by 3 of ~60 would add a
fourth idiom, and the only genuine shared home is already over the size guidance
and inherited by ~120 classes. Recorded in
`.planning/test-optimization-candidates.md` as its own candidate rather than swept
into this story.

### 7. Export accepted root files in the notebook ZIP

Type: Behavior. Status: **done**. Estimate: about 5 active minutes; actual ~5.
Existing notebook export includes accepted root-file paths and bytes through the
shared snapshot. Proof: `NotebookExportControllerTest` publishes the fixture via
the real boundary and reads ZIP entry bytes. No post-publication projection seeding.
Full backend suite; this owns public export behavior beyond slice 2's codec proof.

Accepted proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`, pass
(2556 tests, 0 failures). Boundary: `publishNotebookGitProposal` → `exportNotebook`
→ `NotebookExportService` → `NotebookLivePortableTree` → `NotebookZipBuilder`.
Observation:
`controllers/NotebookExportRootAttachmentControllerTest.theExportedZipCarriesThePublishedRootFilesCompleteAndByteExact`
— complete entry names and canonical order, plus exact bytes for an invalid-UTF-8
file, a text root file and a Markdown note, all read as raw bytes.

**No production change was needed** — the third slice in a row. Slice 3's single
`NotebookLivePortableTree` assembly point already carried the files into export.

Observed in-ZIP order, confirming the canonical order and its difference from Git's:

```
README.md, Overview.md, Diagram.png, reference.json, Biology/README.md, Biology/Cells.md
```

Git's byte-wise order for the same tip is
`Biology/Cells.md, Biology/README.md, Diagram.png, Overview.md, README.md, reference.json`.

Mutation-checked twice, with the distinction that matters: making
`NotebookLivePortableTree` attachment-blind failed at the *publication* step
(409, never reaching the export assertion — coarse, proves nothing about export),
while filtering non-`.md` entries inside `NotebookExportService.exportNotebookAsZip`
failed exactly at the ZIP assertion. Only the second shows the proof observes the
export route rather than the shared plumbing beneath it. Both files restored from
backup copies.

Deviation from this slice's text, accepted: the proof lives in a **new** class
rather than extending `NotebookExportControllerTest`. That class extends
`NotebookControllerTestBase`, which runs in the default rolled-back test
transaction and has no Git publication fixtures; publishing through the real
boundary needs `NotebookGitBundleControllerTestBase`
(`@Transactional(NOT_SUPPORTED)`, committed fixture user,
`snapshotCurrentPortableTree`, `proposalBundleBytes`). Changing the existing
class's base and transaction semantics for its two existing tests would have been
the larger change. The promise — publish via the real boundary, read ZIP entry
bytes, no projection seeding — is met.

Unplanned extra evidence: the fixture publishes a root `README.md` in the same
proposal, so this is the first proof that a notebook README round-trips
byte-exactly *alongside* new root attachments. A one-byte difference would have
thrown 409.

Duplication finding corrected on measurement: the "publish-then-observe" fixture
shape is **two** copies, not four. Slices 4 and 5 use a materially shorter idiom
(snapshot an empty notebook, publish a literal tree) with no read-back at all.
Only slices 6 and 7 share the byte-identical 8-line `acceptedEntriesOf` helper,
and that helper *is* the bundle-tip read already recorded as a standalone
candidate — extracting it for 2 of 51 callers would start exactly the sweep this
story declined. The candidate entry's file count was corrected from "~60" to the
measured 51.

### 8. Preserve root files through existing history reset

Type: Behavior. Status: **done**. Estimate: about 5 active minutes; actual <5.
Existing authorized reset retains current root-file content in its new history.
Proof: `NotebookGitHistoryResetControllerTest` publishes files, resets, downloads,
and checks bytes in the new root. Keep current reset authorization/history rules;
ordinary publication still never rewrites history. Full backend suite.

Accepted proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`, pass
(2557 tests, 0 failures), rerun by the coordinator after the refactor — also pass.
Boundary: `publishNotebookGitProposal` → `resetNotebookGitHistory` → the stored
binding's bundle bytes. Observation:
`NotebookGitHistoryResetControllerTest.resetRestartsHistoryCarryingTheRootFilesTheNotebookCurrentlyHolds`
— `getParentCount() == 0` plus exact ordered list equality over the new root
(`Diagram.png` with invalid-UTF-8 bytes, `Overview.md`, `reference.json`), in Git
byte-wise path order.

**No production change was needed** — the fourth slice in a row. `resetHistory`
rebuilds through `NotebookLivePortableTree`, the assembly point slice 3 established.

Mutation-checked **sharply**, inside the reset path itself: filtering
`NotebookGitCutoverService.buildBundle` to `.md` only failed exactly 1 of 3 tests,
landing on the new assertion, while the two pre-existing reset tests stayed green.
That second fact matters — it rules out the vacuity trap this slice carried, since
the shared fixture's `snapshotCurrentPortableTree` *is* `resetHistory` and already
runs suite-wide, but only ever before attachments exist. Restored from a backup copy.

Confirmed unchanged: reset authorization (`deniedResetLeavesAcceptedHistoryUnchanged`
untouched and green), the parentless root commit, and "ordinary publication never
rewrites history".

Refactor pass outcome, and a correction to slices 6–7's reasoning: the three
byte-identical `acceptedEntriesOf` copies were collapsed — not into a new helper,
but into `testability/GitBundleTestReader.fetchTipTreeEntries`. That class was the
concept's real home all along (it already owned `fetchSingleParentCommit` and
`fetchAdvertisedHead` and is imported by all 51 callers), so no fourth idiom was
created and the over-size base class stayed untouched. Both earlier passes had
assumed the only candidate home was that base class, which is why they declined.
Adoption is complete at 3-of-3 for this concept: every other `readTreeEntries`
caller needs the tip *commit* — parent count, first-parent ancestry, author ident —
not just its entries, so they are a different concept and correctly keep their own
repository. Slice 8's own test keeps an inline block for the assertion half,
because it asserts `getParentCount()` alongside the entries. Net −11 lines.

Server-side coverage is now complete: the invalid-UTF-8 bytes survive publication,
acceptance, the live projection, a web note save, note and folder operations, ZIP
export, and history reset. Only the on-disk second-checkout hop remains (slice 9).

Learnings for slice 9:

- Reset produces a parentless root commit, so a receiver that already has the old
  history needs a fresh clone rather than a pull. If the E2E exercises reset, the
  **pull case must come before it**, not after.
- A CLI checkout lists files in filesystem/locale order, which is neither Git's
  byte-wise order nor the ZIP canonical order — both of which have already bitten
  twice. Sort explicitly rather than assuming either.

### 9. Recover files in a real second checkout

Type: Behavior. Status: planned. Estimate: 5–8 active minutes.
Extend `cli_notebook_publish_to_clean_clone.feature`: installed CLI publication
of root text/binary files → web note edit → clean receiver pull and fresh clone.
Observe exact on-disk bytes, filenames, accepted ancestry and clean worktree.
Include a file-only follow-on edit. Reuse native Git transport and existing tasks;
use a byte fixture/read where text helpers cannot express the observation.
Proof: the existing E2E spec with real backend acceptance, not a mocked receipt.
Run CLI tests if CLI code/tests change. No rebase-recognizer changes.

## Proof coverage and refinement assessment

| Promise | Owner |
| --- | --- |
| Exact bytes and complete names, unchanged Markdown codec | 1–2 |
| Every snapshot caller sees the same root files | 3; external evidence 4, 7, 8 |
| First file-only acceptance; next web save; atomic refusals | 4 |
| Local file lifecycle and submitted history | 5 |
| Private note identity and independent root-file lifetime | 6 |
| ZIP and existing reset do not drop current files | 7 and 8 respectively |
| Usable receiver and fresh checkout | 9 |

Refinement replaced the old broad projection slice with root ownership only and
moved nested materialization/subtree obligations to story 9. It folded the separate
late rejection slice into admission, so atomicity is proved when files become
accepted. Byte-codec Structure immediately enables its serialization Behavior;
projection Structure immediately enables publication. There are no remaining
low-confidence folder-policy or recursive-merge dependencies in this first plan.
The nine slices are **ready for direct execution** as planning hypotheses; execution
still needs separate authorization. No tests or implementation have been run.

The cumulative model remains one attachment concept and one content authority.
The interim placement admission is removed by story 9, which extends the same
projection. It must not survive as competing root/nested implementations.

## Redistribution of the original 15 slices

Numbers in this table refer to the original unsplit plan, not current headings.
No slices were completed and no execution evidence was lost.

| Original slice / promise | New owner |
| --- | --- |
| 1 byte representation | This plan 1 |
| 2 mixed-tree codec | This plan 2 root bytes; plan 003 input 1 nested placement/markers |
| 3 projection and containment | This plan 3 root ownership; plan 003 input 1 folder ownership |
| 4 root publish/web continuity and initial file-only tree | This plan 4 |
| 5 attachment-only folders | Plan 003 input 1 |
| 6 attachment-only local changes | This plan 5 root lifecycle; plan 003 input 2 nested lifecycle |
| 7 mixed note/folder correspondence | This plan 6 independent root files; plan 003 input 2 contained files |
| 8 independence from note removal | This plan 6; plan 003 input 1 retains it for nested files |
| 9 web folder placement | Plan 003 input 3; this plan 6 preserves unrelated root files |
| 10 dissolve/merge | Plan 003 input 4 |
| 11 permanent folder deletion | Plan 003 input 5 |
| 12 ZIP export | This plan 7; plan 003 input 6 extends to nested files |
| 13 history reset | This plan 8; plan 003 input 6 extends to nested files |
| 14 atomic rejection | This plan 4; plan 003 inputs 1–5 add their mutation-specific evidence |
| 15 installed CLI recovery | This plan 9; plan 003 input 7 extends to nested paths |

## Verification and delivery

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only
CURSOR_DEV=true nix develop -c pnpm backend:verify
CURSOR_DEV=true nix develop -c pnpm export:database-erd
CURSOR_DEV=true nix develop -c pnpm cli:test
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_publish_to_clean_clone.feature
```

Run the full backend suite for backend leaves, `backend:verify` for schema changes,
and regenerate the ERD. Tests use disposable environments per ADR 0007. At every
stop the represented behavior must be green; delivery is incomplete until the
real checkout loop and current snapshot callers are covered. Shared native-storage
work may require adapting source locations, not changing these promises.
Future authorized execution follows Jidoka → fresh post-change-refactor agent →
conditional API generation → one coordinator format pass → plan update → authorized
commit/push/CI. This resplit authorizes none of those actions. Leave edits uncommitted.
