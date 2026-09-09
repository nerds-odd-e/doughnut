# Publish the next small initial Readme-and-Note trees

Source: [SEED-016 Story 5](../../seeds/SEED-016-initial-notebook-and-folder-readmes.md#story-5).
Status: resumed and completed through slice 8. Next: Slice 9.

## Goal and scope

From a notebook with no folders or live notes and an empty accepted Portable
tree, publish each of these exact direct-child commit layouts:

1. `New Folder/First note.md`;
2. `Parent/Child/README.md`;
3. `Folder A/README.md` plus `Folder B/README.md`;
4. root `README.md` plus two root Notes;
5. `New Folder/README.md` plus two Notes directly inside that Folder; and
6. root `README.md`, `New Folder/README.md`, and one root Note.

Every path is a regular Markdown file. Every README contains valid nonblank
`type: Readme`; every ordinary Note contains valid `type: Note` and has a
valid filename-derived title. Create all implied Folders, preserve authored
bytes, create the Notes, and atomically accept the proposal's exact head and
tree.

Do not generalize acceptance beyond those six layouts and the already-supported
initial layouts. Exclude every other shape, more than three files, more than
two new Folders or Notes, deeper hierarchies, Relationship or unknown document
types, attachments, existing notebook content or README edits, multiple
unpublished commits, stale/divergent history, and bulk import. Valid unmatched
Readme/Note compositions continue to fail loudly; invalid input retains its
deliberate client outcome. No CLI or API contract changes are included.

## Current decisions

- Replace further ad hoc whole-tree branching with one internal initial
  Readme/Note composition model that can describe paths, roles, implied Folder
  prefixes, and the bounded layouts in this story. Keep exact eligibility at
  the behavior boundary; the model is not permission for general import.
- An ordinary Note path implies each Folder prefix needed to contain it even
  when no Folder README exists. A nested Folder README similarly implies its
  parent Folder. Blank Folder Readmes remain absent content, not synthetic
  files.
- Materialize parent Folders before children, and all Folders before Notes.
  Refresh Folder projection rows before resolving proposed Note destinations.
- Reuse `NotebookGitProposalNoteAddition` for Note validation and persistence
  and `NotebookGitProposalBindingPersistence` for the exact accepted bundle and
  head. Keep the publisher's SERIALIZABLE `REQUIRES_NEW` transaction as the
  atomicity owner.
- Keep new initial-composition behavior out of
  `NotebookGitProposalPublisher` and `NotebookGitProposalFolderAcceptance`,
  which are already near the 250-line review threshold. Prefer a cohesive
  initial-tree publication seam and extracted Folder materialization over
  enlarging either class.
- Accepted ADR 0004 defines root/folder README mapping, implicit directories,
  typed Markdown, and authored-byte preservation. Accepted ADR 0006 preserves
  loud failure for valid but still-unimplemented Readme/Note compositions.
  Accepted ADR 0001 supplies domain terminology; JGit remains the Git plumbing.

## Outside-in proof map

| Example | Observable result | Proof owner |
| --- | --- | --- |
| `New Folder/First note.md` | One new root Folder with absent Readme contains the authored Note; exact head/tree downloads | Slice 2 |
| `Parent/Child/README.md` | Parent and Child Folders exist in order; Child owns the authored Readme; exact head/tree downloads | Slice 4 |
| Two sibling Folder READMEs | Both root Folders and authored Readmes exist; exact head/tree downloads | Slice 5 |
| Notebook README + two root Notes | Notebook Readme and both authored root Notes exist; exact head/tree downloads | Slice 7 |
| Folder README + two contained Notes | One Folder owns its Readme and both authored Notes; exact head/tree downloads | Slice 8 |
| Notebook README + Folder README + root Note | Both container Readmes and the root Note exist at their authored paths; exact head/tree downloads | Slice 9 |
| Any late validation failure in an included layout | No Folder, Note, Readme, accepted head, or bundle change survives | Existing publication atomicity tests, retained as the shared transaction proof |
| A valid Readme/Note layout outside this list | Existing unmatched-composition path remains an uncaught failure | Existing ADR-0006 loud-failure behavior guarded throughout |

## Ordered slices

### 1. Recognize one Note in one implied root Folder
Type: Structure
Status: done

Learning: `OneNoteInImpliedRootFolder` on `NotebookGitProposalInitialComposition`
describes the single-depth layout; publisher consults it before ValidUnmatched
without accepting yet. Direct-child Note helpers live on
`NotebookGitProposalFolderCreationShape` (package-visible) to avoid duplication.

### 2. Publish one Note in an implied root Folder
Type: Behavior
Status: done

Learning: `NotebookGitProposalInitialCompositionPublication` accepts the
layout; `NotebookGitProposalFolderMaterialization` owns validated root Folder
create (with/without Readme) shared with FolderAcceptance. Controller proof:
`NotebookGitProposalInitialImpliedRootFolderNoteControllerTest`.

### 3. Materialize a bounded initial Folder hierarchy
Type: Structure
Status: done

Learning: `OneNestedFolderReadme` recognizes `Parent/Child/README.md`;
`FolderMaterialization.createNestedFolderWithChildReadme` builds parent-then-child
with child Readme and refreshed projection. Publisher still loud-refuses until Slice 4.

### 4. Publish one nested Folder Readme
Type: Behavior
Status: done

Learning: `acceptOneNestedFolderReadme` on InitialCompositionPublication uses
`createNestedFolderWithChildReadme`; controller proof
`NotebookGitProposalInitialNestedFolderReadmeControllerTest`.

### 5. Publish two sibling Folder Readmes
Type: Behavior
Status: done

Learning: `TwoSiblingRootFolderReadmes` + `tryAccept` on InitialCompositionPublication;
controller proof `NotebookGitProposalInitialSiblingFolderReadmesControllerTest`.
Publisher dispatches via `tryAccept` only.

### 6. Apply a bounded batch of initial Notes
Type: Structure
Status: done

Learning: `applyNotes` on InitialCompositionPublication batches NoteAddition without
binding accept; `NotebookReadmeWithRootNotes` (1–2 paths) replaced
`CreationWithRootNote`. Existing one-Note acceptors use `applyNotes`.

### 7. Publish a notebook Readme with two root Notes
Type: Behavior
Status: done

Learning: `findWithRootNotes` / `acceptWithRootNotes` accept README + 1–2 root
Notes via `applyNotes`. Controller proof added on
`NotebookGitProposalInitialNotebookReadmeControllerTest`.

### 8. Publish a Folder Readme with two contained Notes
Type: Behavior
Status: done

Behavior: Given an empty accepted notebook, when the exact proposed tree is one
valid root Folder README plus two valid Notes directly inside that Folder,
publication stores the Folder Readme and both contained Notes and accepts the
exact head and tree atomically.

Proof: A controller test observes one Folder with its authored Readme, exactly
two correctly placed Notes with authored titles/content, absent notebook
Readme, and downloaded head/tree equality. Existing shared validation and
transaction tests retain invalid-input and rollback ownership. Run the complete
backend unit-test suite.

Learning: `RootFolderAndContainedNoteCreation.notePath` widened to
`notePaths: List<String>` (1–2 entries). The shared private
`collectInitialAddedPaths` classifier (already used by the notebook-Readme
shapes) was generalized to accumulate 0..N note paths instead of rejecting a
second one, so all four `find*` shape-recognizers in
`NotebookGitProposalFolderCreationShape` now share one classification loop,
each applying only its own cardinality/prefix filter. No API/DTO changes.

### 9. Publish both container Readmes with one root Note
Type: Behavior
Status: planned

Behavior: Given an empty accepted notebook, when the exact proposed tree is a
valid notebook `README.md`, one valid root Folder `README.md`, and one valid
root Note, publication stores both container Readmes, places the Note at the
notebook root, and accepts the exact head and tree atomically.

Proof: A controller test observes notebook and Folder Readmes, the Folder's
root placement, the Note's root placement and authored content, and downloaded
head/tree equality. Existing shared validation and transaction tests retain
invalid-input and rollback ownership. Run the complete backend unit-test suite.

## Contract map

| Contract | Producer | Consumers | Proof owner |
| --- | --- | --- | --- |
| Bounded initial Readme/Note composition | initial composition parser | exact layout predicates and initial publication | Slice 1 regression suite; Slices 2/4/5/7/8/9 behavior proofs |
| Implied Folder prefixes | composition paths and Folder materializer | Note destination resolution and Folder projection | Slices 2 and 4 |
| Several new Folders in one proposal | Folder materializer | initial publication | Slice 5 |
| Several initial Notes in one proposal | bounded Note batch operation | initial publication and final projection | Slices 7 and 8 |
| Mixed root/container placement | initial publication orchestration | notebook, Folder, and Note projections | Slice 9 |
| Exact accepted commit | binding persistence inside publisher transaction | later download/publish | Every Behavior controller proof |
| Atomic rollback after validation or persistence failure | publisher transaction | notebook projection and binding | Existing publication atomicity tests retained throughout |
| Loud next-valid-shape signal | unmatched initial composition path | Failure-report lifecycle | existing ADR-0006 proof retained by every slice |

## Refinement record

Refined 2026-09-09 before execution. Slice 1's whole-plan composition model was
replaced by recognition limited to the immediately following one-Folder/one-Note
Behavior. Slices 3 and 6 now state the exact next increment they enable instead
of implying general hierarchy or batch support. Each Behavior proof was reduced
to its one positive controller loop; existing transaction and validation tests
retain the shared rejection/rollback contract rather than duplicating a second
proof loop in every slice.

The resulting plan remains nine slices: six Behavior and three immediately
enabling Structure slices. Each has one proof loop and a plausible five-minute
target-sized implementation hypothesis. No sizing exception, escalation, or
story resplit is indicated. Execution can start from Slice 1.
