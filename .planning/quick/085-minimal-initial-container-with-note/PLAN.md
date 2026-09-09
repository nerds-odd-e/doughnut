# Publish a minimal initial container with one note

Source: [SEED-016 Story 4](../../seeds/SEED-016-initial-notebook-and-folder-readmes.md#story-4).
Status: planned.

## Goal and scope

From a notebook with no folders or live notes and an empty accepted Portable
tree, accept either smallest two-file initial container-and-note commit:

1. root `README.md` plus one root ordinary Note; or
2. one root Folder's `README.md` plus one ordinary Note directly inside that
   Folder, with no notebook Readme.

Both files must be regular files in one direct-child commit. Readmes contain
valid nonblank `type: Readme` Markdown; Notes contain valid `type: Note`
Markdown and a valid filename-derived title. Preserve authored bytes, create
the required projection, and accept the exact commit atomically.

Once all supported shapes have been considered, another safe, typed,
role-correct initial Readme/Note composition must surface as an uncaught
failure under Accepted ADR 0006 rather than being converted into the misleading
`folder README, which is reserved` client error. Keep deliberate client
outcomes for malformed Markdown, wrong document types, unsafe paths,
non-regular files, stale/divergent history, authorization, and projection
drift.

Exclude acceptance of any third path, nested folders, relationships,
attachments, existing notebook content or README edits, multiple unpublished
commits, and bulk import. The observed 10,406-file repository is motivation,
not an acceptance example for this plan. No CLI or API contract changes are
included.

## Current decisions

- Treat the two layouts as variants of one minimal initial
  container-with-one-Note capability, not as general initial-tree import.
- Exact-shape recognition remains ahead of ordinary-note publication. Each
  variant requires exactly its two proposed paths and no accepted blobs.
- Reuse `NotebookGitProposalInitialNotebookReadmePublication` for the notebook
  Readme, `NotebookGitProposalFolderAcceptance` for Folder creation, and
  `NotebookGitProposalNoteAddition` for the Note; do not duplicate persistence
  or binding logic.
- Create a new Folder before resolving its contained Note against the proposed
  head, as in the delivered three-path initial shape.
- For loud failure, distinguish a valid initial composition from invalid input
  before the ordinary-note fallback. Do not catch or wrap the resulting
  unchecked exception as `ResponseStatusException`; `ControllerSetup` and
  `FailureReportFactory` remain the existing lifecycle owners.
- Accepted ADR 0004 owns Portable `README.md` roles and typed Markdown.
  Accepted ADR 0006 owns deliberate client outcomes versus propagated failures.
  Accepted ADR 0001 supplies the domain vocabulary; Git inspection remains in
  JGit.

## Outside-in examples

| Precondition and trigger | Observable result | Proof owner |
| --- | --- | --- |
| Empty accepted tree; publish `README.md` and `First note.md` | Authored notebook Readme and root Note are stored; exact proposed head/tree is downloadable | Slice 1 controller proof |
| Empty accepted tree; publish `New Folder/README.md` and `New Folder/First note.md` | Folder and contained Note are stored, notebook Readme stays absent, exact proposed head/tree is downloadable | Slice 2 controller proof |
| Empty accepted tree; publish valid `README.md`, `First note.md`, and `Second note.md` | Binding and projection stay unchanged; failure propagates to the existing Failure-report boundary instead of returning reserved-README 400 | Slice 4 loud-failure demonstration plus existing failure-report proof |
| Any included layout contains a wrong type or malformed Markdown | Whole proposal is rejected with the existing deliberate client error and no mutation | Slices 1–2 boundary proof and existing Markdown tests |

## Ordered slices

### 1. Publish an initial notebook Readme with one root Note
Type: Behavior
Status: planned

Behavior: Given an empty accepted notebook, when its direct-child proposal
contains exactly valid root `README.md` and one valid root ordinary Note,
publication stores both authored documents, creates the root Note, and accepts
the proposal's exact head and tree atomically.

Proof: Replace the existing controller rejection for a second path beside the
sole initial notebook Readme with a positive controller scenario. Observe the
stored notebook Readme, Note title and authored content, absence of Folders,
and downloaded head/tree equality. Retain a nearby wrong-type boundary that
proves rollback. Run the complete backend unit-test suite.

### 2. Publish an initial Folder Readme with one contained Note
Type: Behavior
Status: planned

Behavior: Given an empty accepted notebook with no notebook Readme, when its
direct-child proposal contains exactly one root Folder Readme and one ordinary
Note directly inside that same Folder, publication creates the Folder and
Note, preserves both authored documents, leaves the notebook Readme absent,
and accepts the exact head and tree atomically.

Proof: Add a controller scenario observing the Folder name and Readme, the
contained Note title and authored content, absent notebook Readme, and
downloaded head/tree equality. Retain a wrong-type boundary and the existing
Folder-only and three-path initial scenarios. Run the complete backend
unit-test suite.

### 3. Identify valid unmatched initial Readme-and-Note compositions
Type: Structure
Status: planned

Internal change: Give the publisher one cohesive way to distinguish an
added-only, empty-base initial composition made of safe regular Markdown paths
whose basename roles and authored types are valid, after every supported exact
shape has declined it. Preserve the existing external rejection temporarily.
This immediately enables Slice 4 to choose propagation without weakening
validation or teaching the ordinary-note classifier about container Readmes.

Proof: The complete backend unit-test suite remains green. Existing unsafe
path, file-mode, Markdown-format, wrong-type, projection-drift, and supported
publication tests continue to own their current outcomes.

### 4. Let the next valid initial composition fail loudly
Type: Behavior
Status: planned

Behavior: Given an empty accepted notebook and a safe, typed, role-correct
initial Readme/Note proposal that matches no supported exact shape, publication
leaves the projection and binding unchanged and propagates an unchecked
unimplemented-capability failure so the existing HTTP failure lifecycle records
it; it does not convert the first README path into a handled 400 response.

Proof: Use `README.md` plus two root Notes as the outside-in demonstration of
the next excluded shape. Per ADR 0006, do not retain a unit test whose sole
purpose is to assert an allowed loud failure. During the slice, demonstrate at
the controller boundary that the unchecked failure reaches the existing
`ControllerSetup` path and that transaction rollback preserves the binding and
projection; remove any temporary probe before commit. Retain the existing
`ControllerSetupTest`/`FailureReportFactoryTest` proof that an unchecked HTTP
failure creates a Failure report while `ResponseStatusException` does not.
Run the complete backend unit-test suite.

## Contract map

| Contract | Producer | Consumers | Proof owner |
| --- | --- | --- | --- |
| Exact notebook-Readme + root-Note shape | initial notebook publication recognition | proposal publisher | Slice 1 controller proof |
| Exact Folder-Readme + contained-Note shape | Folder creation recognition | proposal publisher | Slice 2 controller proof |
| Authored container and Note persistence | initial publication services | notebook projection and bundle download | Slices 1–2 controller proofs |
| Valid unmatched initial composition classification | initial proposal composition boundary | publisher fallback | Slice 3 regression suite |
| Uncaught failure reaches Failure-report lifecycle | publisher fallback and `ControllerSetup` | developer Failure report | Slice 4 demonstration plus existing failure-report tests |
| Invalid requests retain deliberate client outcomes | existing validators | CLI/API caller | existing validation suites, guarded by Slices 1–3 |

## Refinement assessment

The plan has four cohesive, sequential slices. Each Behavior owns one external
outcome and proof loop; the sole Structure slice immediately enables the loud
failure Behavior. No slice has an unexplained path beyond the repository's
ten-minute hard limit. The plan is ready for direct execution; a separate
slice-plan refinement pass is not needed.
