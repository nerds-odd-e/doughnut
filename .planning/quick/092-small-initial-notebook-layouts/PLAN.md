# Publish three small initial notebook layouts

Source: [SEED-016 Story 6](../../seeds/SEED-016-initial-notebook-and-folder-readmes.md#story-6).
Status: in progress on `quick/092-small-initial-notebook-layouts`.
Checkout: `/Users/terryyin/git/doughnut-092-small-initial-notebook-layouts`.
CI: `donut CI` (`ci.yml`) is push-triggered on `main` only; feature-branch pushes have no workflow, so slice pushes are unobserved. Attach the mailbox observer to `main` before the merge push.

## Goal and scope

Allow an owner to publish each of three exact initial layouts as one authored
commit: notebook README plus three root Notes; two Notes in one implied root
Folder without README; notebook README plus one root Relationship. The seed owns
the examples and exclusions. All require no folders/live notes, an empty accepted
tree, and one direct-child commit. Preserve authored bytes, placement, exact Git
head/tree and atomicity. No complete jap1 import or combinatorial layout support.

## Current decisions and evidence

- Inspected at `93428635c9`. Local jap1 commit `9281fbc` adds 10,406 files onto an
  empty tree: 5,531 Notes, 4,873 Relationships and two Readmes. This is motivation,
  not a performance fixture or completion target. Do not mutate that checkout.
- Extend existing initial-composition handlers; retain one representation for
  each concept. Do not add a generic import engine or another handler per count.
- `NotebookGitProposalInitialNotebookReadmePublication.findWithRootNotes` and
  `NotebookReadmeWithRootNotes` now accept one, two, or three root Notes. Do not
  loosen ordinary type or root-path eligibility further.
- `NotebookGitProposalInitialComposition.findNotesInImpliedRootFolder` and its
  publication method materialize one implied Folder for one or two ordinary
  Notes with the same single root prefix; they reject sibling prefixes, deeper
  paths, or other types. Do not generalize nested folders or multiple prefixes.
- Relationship persistence already uses `AuthoredNoteDocument.fromContent` and
  `AuthoredNoteDocumentPersistence.persist`, including source-owned references.
  Add only the exact root README + one Relationship eligibility. Do not widen
  shared `requireOrdinaryNote` or `applyNotes` to admit Relationships everywhere.
- [ADR 0004 — OKF-compatible notebook Markdown profile](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md):
  preserve typed concepts, author YAML, README container storage and unresolved
  authored wiki references. [ADR 0006 — Failure handling](../../../docs/adrs/0006-failure-handling-accepted.md):
  retain loud handling of valid unimplemented compositions; do not test those
  loud failures or add a catch-all error mapper. Both records are Accepted;
  ADR 0002 remains Proposed. No conflict or exception required.

## Proof and execution gates

Use real `NotebookGitBundleController` publication/download boundaries with
existing `NotebookGitBundleControllerTestBase` fixtures and the real database.
Existing CLI E2E features cover clone, web-created notes, existing edits and folder
relocation, not these initial layouts. No new CLI transport or browser behavior
is promised; controller proofs exercise the publication boundary used by CLI.

For each Behavior, add/update its observable test, confirm the right red failure,
implement the minimal change, and confirm green. Backend rules require **all**
backend unit tests: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.
Do not substitute a selected test file. No schema/API change or generation expected.

Required slice delivery: Jidoka → fresh dough-post-change-refactor agent → API
generation only if signatures change → coordinator runs
`./scripts/run.sh pnpm format:changed` once → update this PLAN → commit with
independent lint hook → push and asynchronous CI observation via dough-execute-plan.
Keep unrelated work intact; do not update backlog order or STATE.

Sizing hypothesis: each leaf targets about 5 minutes of implementation,
verification and cleanup. The mandatory full backend suite and external wrap-up
waits may exceed that; this is an explicit test/external-wait exception, not
permission for extra implementation. At 5 minutes inspect hidden work; above
10 minutes of active work stop and refine in place. Repeated overruns require
story reassessment before more slicing. No timing guarantee or new storage
assumption is asserted.

## Ordered slices

### 1. Publish a notebook README with three root Notes
Type: Behavior
Status: done
Proof: `NotebookGitProposalInitialNotebookReadmeControllerTest#publishesInitialNotebookReadmeAndThreeRootNotesAsTheExactAuthoredCommit`
plus `unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL` then
`CURSOR_DEV=true nix develop -c pnpm backend:test_only`. Canonical three-note
round-trip: README bytes, three titled root Notes, no folders, exact download
head/tree. One- and two-note siblings keep count/content coverage only.

### 2. Publish two Notes in one implied root Folder
Type: Behavior
Status: done
Proof: `NotebookGitProposalInitialImpliedRootFolderNoteControllerTest#publishesTwoNotesInImpliedRootFolderAsTheExactAuthoredCommit`
plus `unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL` then
`CURSOR_DEV=true nix develop -c pnpm backend:test_only`. Canonical two-note
round-trip: one Folder, both authored Notes inside, no notebook/folder README,
exact download head/tree. One-note sibling keeps count/content coverage.

### 3. Reject an invalid contained Note without partial publication
Type: Behavior
Status: done
Proof: `NotebookGitProposalInitialImpliedRootFolderNoteControllerTest#rejectsALaterInvalidContainedNoteWithoutPartialImpliedFolderPublication`
plus `unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL` then
`CURSOR_DEV=true nix develop -c pnpm backend:test_only`. Later `note_level: 7`
rejected with path and property context; Folder/Note/wiki-reference rows and
accepted binding match the empty pre-call state. No production change: existing
publisher transaction already rolls back.

### 4. Publish a notebook README with one root Relationship
Type: Behavior
Status: planned
Proof: Controller publication/download preserves the exact head/tree and
Relationship Markdown (type, relation, source, target and body). The persisted
document exposes both authored references; no endpoint Notes are manufactured.

Behavior: Empty notebook + README and one root Relationship with absent A/B
endpoints → owner publishes → the relationship is accepted under existing
unresolved-reference semantics.

Update the root README controller test: replace only the now-obsolete single-root
Relationship rejection with success. Keep Readme/CustomType rejection and
Relationship rejection in other ordinary-Note layouts. Recognize the exact
two-file Relationship case and reuse existing root readme storage, note addition,
reference persistence, projection and binding acceptance. Avoid a global type
relaxation or endpoint resolver. Estimated active work: ~5 min, medium confidence;
if this needs new domain behavior, stop and revisit the assumption before editing.

## Promise ownership and readiness

| Promise | Owning proof |
| --- | --- |
| README + three root Notes | Slice 1 controller publication/download |
| Two Notes in one implied root Folder; no synthetic README | Slice 2 publication/download |
| Root README + one Relationship; preserved references, absent endpoints remain absent | Slice 4 publication/download and persisted reference observation |
| Authored bytes, exact commit and placements | Slices 1, 2, 4 success observations, canonical shape asserted once per distinct layout |
| Invalid-property publication is atomic | Slice 3 committed-transaction proof |
| Existing supported layouts and eligibility protections remain | Required backend suite after each slice; preserve existing relevant assertions |

Four single-outcome Behavior slices; no preparatory Structure slice is justified.
The atomicity check is separate from the two-note success proof to keep one proof
loop per leaf. All work is within existing publication/persistence boundaries.
Ready for direct execution; no additional slice-plan refinement required.

## Learnings

- Slice 1: three-note controller test is the canonical README+root-Notes
  round-trip; one/two-note siblings assert count and authored content only.
  Implementation ~6 min (suite wait excluded). Remaining slices unchanged.
- Slice 2: renamed `OneNoteInImpliedRootFolder` to `NotesInImpliedRootFolder`.
  Implementation ~8 min (suite wait excluded). Remaining slices unchanged.
- Slice 3: no production gap; publisher `REQUIRES_NEW` already rolls back Folder,
  first Note, and authored references. Implementation ~8 min.
