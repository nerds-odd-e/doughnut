# Publish two related notes inside one new folder

Source: [SEED-016 Story 8](../../seeds/SEED-016-initial-notebook-and-folder-readmes.md#story-8).
Status: postponed — not execution-ready. No implementation begun.

## Hold: structural design correction first

The developer postponed execution and inserted
[SEED-017 Story 9](../../seeds/SEED-017-cohesive-design-corrections.md#story-9)
at highest priority. That story documents example-shaped production restrictions
and is now refined in its canonical SEED-017 home. Complete that prerequisite, then revisit Story 8 and
revise or retire this plan before execution. Do not automatically resume it.

The scope and slices below are retained as prior planning context, not current
implementation instructions. In particular, exact file-count and layout gates
must be reconsidered rather than treated as approved domain rules.

## Goal and scope

Accept exactly notebook README plus two Notes and one Relationship directly under
one implied root Folder on an otherwise empty notebook. Preserve authored bytes,
titles, placement and exact Git head/tree; resolve the Relationship's links under
existing Portable-path semantics; reject invalid authored properties atomically.
No folder README, deeper nesting, extra concepts, existing-notebook editing or
full jap1 import. The seed owns the complete examples and exclusions.

## Evidence and implementation constraints

- Inspection base `edf3c0512a`, which merges delivered quick/094. Its
  `NotebookGitProposalInitialNotebookReadmePublication.findWithRootRelationship`
  still requires all concepts at root. `findNotesInImpliedRootFolder` supports
  only one/two ordinary Notes and no notebook README. Neither recognizes this
  four-file composition.
- Reuse existing root mixed-composition role classification and implied-Folder
  materialization. Do not globally relax ordinary-Note checks or accept arbitrary
  counts/depths. A match requires all additions, root notebook README, exactly two
  Notes and one Relationship, and a shared single-segment folder prefix.
- `NotebookGitProposalFolderMaterialization.createRootFolderWithoutReadme`
  already validates and creates the required Folder. Load refreshed Folder rows
  after creation for note placement **and final projection matching**. The current
  root publication helper uses `state.folders()`; passing that stale pre-creation
  list would invalidate this new path. Keep this change coherent with existing callers.
- Reuse empty-notebook validation, notebook Readme storage, `applyNotes`,
  `applyRelationship`, final projection proof and binding acceptance within the
  existing transaction. Avoid cyclic service dependencies or another import engine.
  Small helper/signature adjustments belong to the first Behavior, not an
  independent preparatory architecture project.
- Reuse `NotebookGitProposalInitialRootRelationshipControllerTest` for mixed
  publication/readback and show-note patterns,
  `NotebookGitProposalInitialRootRelationshipRejectionControllerTest` for rollback,
  and `NotebookGitProposalInitialImpliedRootFolderNoteControllerTest` for Folder
  placement and committed-transaction assertions. Tests remain capability-named.
- Relevant Accepted constraints are unchanged:
  [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  for typed Portable content, implied folders and authored references;
  [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md) for deliberate
  validation outcomes versus loud unimplemented cases;
  [ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md) for
  disposable test data. ADR 0002 remains Proposed. No exception is required.

## Proof and delivery gates

Use the real publication/download controllers and database via
`NotebookGitBundleControllerTestBase`; navigation proof uses `NoteController.showNote`.
The CLI transport is unchanged; existing CLI E2E scenarios do not cover this exact
initial composition. No new UI/CLI contract is promised.

Before implementing each new behavior, establish a failing controller test for
the right reason. If later reference/rollback proof already passes, retain the
proof without manufacturing a code change. Backend rules require all backend
unit tests: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.
Use the owning disposable Unit Test database, never Development, production or
the actual jap1 notebook. Stop if environment ownership is unclear. No novel
storage assumption or schema/API change is expected.

Each leaf follows dough-execute-plan: Jidoka → fresh dough-post-change-refactor
agent → API generation only if needed → coordinator runs
`./scripts/run.sh pnpm format:changed` once → update PLAN → commit with independent
lint hook → push and asynchronous CI observation. Preserve unrelated work;
do not reorder backlog or update STATE as part of this plan.

Target ~5 minutes per leaf including verification and cleanup. Scrutinize work
over 5 minutes; active work over 10 minutes requires stopping and finer decomposition.
Mandatory full-backend-suite runtime and external delivery waits are the explicit
timing exception, not extra implementation allowance. Record disproved sizing
assumptions; repeated overruns trigger story reassessment. Estimates are hypotheses.

## Ordered slices

### 1. Publish the connected set inside one implied Folder
Type: Behavior
Status: planned
Proof: Controller publication of the seed's exact four-file layout yields one
Folder and three concepts assigned to it, preserves notebook Readme and authored
content, and downloads the exact proposed head/tree with no generated Folder README.

Behavior: Empty notebook + root README and two Notes/one Relationship under Topic
→ owner publishes → the complete grouped composition is accepted.

Extend the existing composition/persistence path as described above, including
refreshed Folder rows. Use typed role classification independent of path order.
Keep existing root and implied ordinary-note layouts working. Do not introduce
new refusal tests for intentionally loud unsupported compositions (ADR 0006).
Estimate: ~5 min active work, medium confidence; refreshed projection data is the
main integration risk. If adapting shared helpers becomes a second concept or
requires broad restructuring, park attempt-owned work and refine rather than expand.

### 2. Resolve the Relationship's paths to the new contained Notes
Type: Behavior
Status: planned
Proof: Publish the same shape and show the Relationship through NoteController;
its `Topic/A` and `Topic/B` wiki links identify the newly created Note IDs.
Use a Relationship path sorting before the endpoints. Avoid repeating slice 1's
canonical content/tree checks or asserting only internal reference-row counts.

Behavior: Relationship and endpoints arrive together inside Topic
→ owner opens the Relationship → both authored Portable-path links resolve.

Reuse current document persistence, reference indexing and viewer resolution.
No new linking pass or endpoint IDs. If new reference semantics are needed,
stop and revisit the stated assumption. Estimate: ~5 min active work, medium confidence;
existing behavior may need only proof. The story remains incomplete until this passes.

### 3. Reject invalid content without leaving a partial Folder publication
Type: Behavior
Status: planned
Proof: In committed-transaction testing, publish the layout with invalid
Relationship `note_level`; observe deliberate property-context rejection and
unchanged Readme, Folder/Note rows, source-owned reference rows and accepted head.
Give an earlier valid Note a wiki reference to cover index cleanup if writes have
occurred. Do not rely on a test-owned outer rollback or force writes if validation
legitimately runs before persistence.

Behavior: The grouped proposal contains an invalid authored property
→ owner publishes → nothing from the proposal remains accepted or persisted.

Reuse the existing root-Relationship rejection proof with the new Folder footprint.
Fix only a demonstrated transaction gap. Estimate: ~5 min active work, medium confidence.

## Promise ownership and readiness

| Promise | Owning proof |
| --- | --- |
| Four-file grouped publication, root Readme, titles and placements | Slice 1 |
| Exact authored bytes/head/tree; no synthetic Folder README | Slice 1 download |
| Same-commit contained endpoint navigation | Slice 2 show-note |
| Invalid property leaves no partial state | Slice 3 committed transaction |
| Existing layouts and validation behavior retained | Required backend suite after each leaf |

The prior three-slice breakdown is superseded as an execution recommendation by
the hold above. Readiness must be reassessed after Story 9 and revision of this
plan. Implementation has not begun.

## Learnings and completion

No execution evidence yet. Record only findings affecting remaining scope or proof.
After delivery, reduce Story 8 to delivered Goal/Scope and remove spent plan history,
preserving unfinished siblings and enduring behavior in code/tests.
