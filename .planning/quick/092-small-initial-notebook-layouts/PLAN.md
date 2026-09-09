# Publish three small initial notebook layouts

Source: [SEED-016 Story 6](../../seeds/SEED-016-initial-notebook-and-folder-readmes.md#story-6).
Status: planned. Planning authorized 2026-09-09; implementation not yet requested.

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
  `NotebookReadmeWithRootNotes` currently cap root Notes at two. Extend that
  ordinary-Note range to three, preserving the root-path requirement.
- `NotebookGitProposalInitialComposition.findOneNoteInImpliedRootFolder` and its
  publication method already materialize one implied Folder. Extend coherently
  to one or two ordinary Notes under the same single root prefix; reject a match
  for sibling prefixes, deeper paths or other types.
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
Status: planned
Proof: Controller publication succeeds, three titled Notes are at root, authored
README/content survive, and a downloaded bundle has the exact proposed head/tree.

Behavior: Empty notebook + README and three root Notes in one direct-child commit
→ owner publishes → the complete authored root layout is accepted.

Extend the existing root-Note count contract coherently (recognition and record
invariant) without loosening ordinary type or root-path eligibility. Extend the
existing count-success test in `NotebookGitProposalInitialNotebookReadmeControllerTest`
using concise fixtures; retain one/two-note coverage without repeating canonical
round-trip assertions in every sibling. Estimated active work: ~5 min, medium confidence.

### 2. Publish two Notes in one implied root Folder
Type: Behavior
Status: planned
Proof: Controller publication produces one Folder with both authored Notes;
download preserves the exact proposed tree/head and contains no synthetic README.

Behavior: Empty notebook + `Folder/A.md` and `Folder/B.md` only
→ owner publishes → one implied Folder and its two Notes are accepted together.

Extend `NotebookGitProposalInitialImpliedRootFolderNoteControllerTest` and the
existing one-note recognition/materialization path to a cohesive list of one/two
Notes with identical direct-parent prefix. Rename singular internal concepts where
needed; reuse folder creation, `applyNotes`, and projection checks. No nested-folder
or multiple-prefix generalization. Estimated active work: ~5 min, medium confidence.

### 3. Reject an invalid contained Note without partial publication
Type: Behavior
Status: planned
Proof: Through a committed-transaction controller test, a two-note implied-folder
proposal whose later Note has invalid `note_level` is rejected with property
context; accepted head, Folder/Note rows and source-owned reference rows match the
pre-call state. Use the earlier valid Note's wiki reference to exercise index rollback.

Behavior: The second layout contains an invalid authored property
→ owner publishes → rejection leaves the notebook unchanged.

Reuse `NotebookGitPublicationAtomicControllerTest` / committed transaction helpers;
cover the actual publication boundary, not a test-owned outer rollback. Existing
publisher transaction should provide the outcome; fix only a demonstrated gap.
Estimated active work: ~5 min, medium confidence. This is an explicit handled
validation outcome, not a test of the ADR 0006 unimplemented-layout failure.

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

None from execution. On completion, reduce Story 6 to delivered Goal/Scope and
remove spent plan history under the repository lifecycle; preserve siblings.
