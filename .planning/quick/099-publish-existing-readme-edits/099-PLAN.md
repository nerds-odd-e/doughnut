# Publish edits to existing Readmes

Status: in progress (slice 1 delivered)
Source: [SEED-009 story 24](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-24)
Authority: planning only; no implementation or backlog claim yet.

## Goal and boundary

A notebook owner edits existing notebook/folder descriptions locally, publishes
them, sees them in Donut, and receives the exact authored files in a clean
second checkout. Preserve container/note identities and learning history.

Include same-path valid `type: Readme` modifications, Readme-only publication,
and a companion same-path note edit. Invalid typed Markdown must leave accepted
history and stored content unchanged. Existing authorization remains in force.
Use direct-child commits for the new examples without introducing a direct-child
restriction. Container creation, removal, moves, trash, divergence, performance,
and a new accumulated-history acceptance matrix are deferred.

## Existing solution and direction

PFE assessment (code inspection, 2026-09-15):

- `NotebookGitProposalTreeInspection` already classifies root/folder README
  files as containers. `NotebookGitProposalTreeShape.admitShape` admits only
  container additions; this is the evidenced gap.
- `NotebookGitProposalDocumentApplication` stores root Readme content;
  `NotebookGitProposalFolderMaterialization` resolves existing represented
  folders by path and stores their Readme content on the original rows. Extend
  this existing application responsibility to modifications. Do not create a
  second Readme writer or represent Readmes as notes.
- `NotebookGitProposalPublisher` owns validation, accepted-projection matching,
  document application and atomic acceptance. Its ordinary and relocation
  callers consume the admitted document shape. If that shape changes, align
  their meanings and preserve existing addition/relocation behavior; do not
  retain a misleading additions-only name for modifications. This caller
  maintenance does not add new structural-operation promises.
- `NotebookController` web Readme updates use the same container columns but
  do not own Git proposal acceptance. Reuse the Git application path rather
  than invoking web controllers or adding web-save synchronization.
- Installed CLI publish/pull and E2E checkout helpers already carry committed
  files, assert accepted HEAD, and read receiver files exactly. Extend existing
  scenarios/helpers only where an observation is missing. No new CLI command,
  API contract, schema, or frontend editing flow is anticipated.

Follow [One final publication result](../../NORTH-STAR.md#one-final-publication-result):
apply final content once in the existing transaction. No new direction topic.
[Accepted ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
owns container columns, typed Markdown, authored YAML preservation and lossless
round trips. [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md)
supports the existing validation outcome without speculative recovery.
[ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
keeps verification in disposable test environments. ADR 0002 remains Proposed.
No ADR conflict or new storage/infrastructure assumption was identified.

## Proof and execution rules

Tests drive the publication controller with real persisted fixtures and the
installed CLI against the E2E application. Avoid tests per internal class.
Use `snapshotCurrentPortableTree` to establish a consistent accepted baseline;
old root-rejection fixtures contain untyped text and cannot prove valid edits.
The existing folder-rejection fixture contains a note addition, not a note edit.
Replace obsolete blanket-rejection assertions while retaining genuine invalid
Markdown and unsupported-operation checks.

Commands during execution:

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_clone.feature
```

Backend rules require the full backend suite. Run both commands for each slice
that changes their covered behavior, after establishing the new failing example.
Do not substitute a seeded receiver state for an actual publish followed by pull.
No product tests were run while writing this plan; all proof below is pending.

Each slice targets about five minutes including focused work and verification.
Both are scrutinized 5–10 minute implementation/cleanup hypotheses, medium
confidence, because they cross admission, application and public observations.
The explicit timing exception is full-backend/E2E startup and runtime plus
required delivery gates; these waits can exceed ten minutes without implying
more behavior. Active implementation exceeding ten minutes requires finer
decomposition of the remaining work in this plan, not silent scope expansion.

Use dough-execute-plan's delivery contract when execution is authorized:
claim backlog work first; Jidoka; a fresh dough-post-change-refactor agent;
API generation only if signatures change; coordinator runs
`./scripts/run.sh pnpm format:changed` once per delivery; update this plan;
commit with the check-only hook; push and observe CI under that workflow.
Retain plan/proof through retrospective, then story wrap-up. Planning leaves
backlog placement and GSD STATE unchanged.

## Ordered slices

### 1. Publish an existing notebook description and receive it locally
Type: Behavior
Status: done

Behavior: Accepted A has a represented root `README.md`. The owner edits its
body and author-owned YAML, retaining `type: Readme`, commits B directly after A
and publishes. The same notebook displays the new description; a clean receiver
at A pulls B with the exact authored file.

Extend the existing admission/application path for container modifications;
root versus folder location is existing domain data, not a reason to add separate
publication modes. Naturally supported folder behavior need not be gated until
slice 2. Update affected old root rejection tests with valid baseline content.

Proof: a publication-controller test observes the original notebook ID, exact
stored content and accepted B. A root invalid-type variant with a valid companion
note edit observes rejection, unchanged accepted A and unchanged stored content
for both files. An E2E scenario in `cli_notebook_clone.feature` clones both
checkouts before editing, publishes through installed CLI, observes the notebook
Readme in Donut, pulls the receiver, and asserts clean B plus exact file content.
Use existing clone/commit/publish/pull and notebook Readme observation helpers.

Safe stop: owners can update notebook descriptions; folder proof remains pending.

### 2. Publish an existing folder description, alone or with a note edit
Type: Behavior
Status: planned

Behavior: Accepted A has `Recipes/README.md` and an existing note with learning
history. Publish an in-place folder Readme edit, alone or with a body edit to the
existing note. The same folder displays the new description and the same note
retains its learning associations. A receiver at A pulls the exact authored file.

Extend the same behavior/proof with folder data. Reuse represented-folder lookup
and ensure refreshed projection data participates in final acceptance. Preserve
the already-supported addition path when replacing the old folder rejection
test; do not introduce operation combinations beyond this story as new work.

Proof: controller cases cover Readme-only and companion same-path note edits;
assert original folder identity/exact content in the canonical case, and note
ID, retained tracker/history association and changed note body in the companion
case. A folder invalid-type case asserts validation rejection and no mutation.
Extend the E2E root journey to a folder variant (an outline if observations stay
clear), observing folder description through Donut and exact receiver bytes/HEAD.
Add only the missing folder UI observation to the existing page-object boundary.
Run the commands above; do not add a separate tests-only delivery slice.

Safe stop: the selected story's same-path description-edit workflow is covered.

## Promise ownership

| Promise | Owning slice / observation |
| --- | --- |
| Root Readme-only edit and authored YAML/bytes | 1: controller persistence + CLI receiver file |
| Description visible in Donut, same notebook | 1: UI observation + original ID |
| Folder Readme-only edit, same folder | 2: controller ID/content + folder UI |
| Companion note edit and learning preservation | 2: persisted original note/tracker/history |
| Clean receiver gets authored commit/content | 1 root and 2 folder: actual pull from A to B |
| Invalid typed content causes no partial write/head advance | 1 root and 2 folder: controller rejection |
| Existing authorization/admission behavior preserved | Both: full backend suite, update only obsolete Readme rejection expectations |

## Assessment

Two Behavior slices, no preparatory Structure slice. The cumulative model is
one classified container modification applied through existing content owners.
Root and folder examples exercise that model without a new recognizer per case.
No separate refinement pass was warranted by the inspected code: each slice
owns one publication journey and its public proof. Remaining uncertainty is
test-runtime overhead and the small folder UI observation helper, not new domain
scope. Reassess if either requires a larger change than described.

## Execution evidence

### Slice 1 — delivered (branch `099-publish-existing-readme-edits`)
Production: `NotebookGitProposalTreeShape.admitShape` now admits `CONTAINER`
`MODIFIED` (in addition to `ADDED`); `DELETED` stays reserved. `AdmittedShape`
renamed `additions` → `documents` (carries additions + container modifications);
deletion-mix guard now keys on `ADDED` kind only, so container *additions*
mixed with concept *removals* stay reserved while `MODIFIED` containers may
compose with note deletions. `NotebookGitProposalPublisher` callers updated to
`admitted.documents()` (`applyAdditionsUnderRelocatedDestination` →
`applyDocumentsUnderRelocatedDestination`); behavior preserved.
`NotebookGitProposalDocumentApplication` already overwrites root Readme content
unconditionally, so no application-path change was needed (javadoc only).

Accepted proof (inspected):
- `NotebookGitProposalTreeShapeControllerTest.acceptsAValidRootReadmeEditAsTheExactAuthoredCommit`
  — original notebook ID, exact stored `readmeContent`, accepted B via downloaded
  bundle head/tree equality.
- `NotebookGitProposalTreeShapeControllerTest.rejectsAnInvalidRootReadmeEditWithACompanionNoteEditWithoutMutatingAcceptedContent`
  — invalid typed Markdown + valid companion note edit → BAD_REQUEST, unchanged
  readme and note content.
- `NotebookGitProposalTreeShapeControllerTest.acceptsAValidRootReadmeEditAsTheSoleNotebookReadme`
  — root Readme edit, no notes.
- `NotebookGitProposalTreeShapeControllerTest.acceptsAValidFolderReadmeEditAlongsideANoteAdditionWithoutMutatingFolderIdentity`
  — folder Readme MODIFIED + note ADDED (naturally supported; folder proof proper
  remains slice 2).
- E2E `cli_notebook_clone.feature` "Publishing a committed root readme edit
  updates the notebook description and round-trips" — clones both checkouts,
  commits typed Readme edit, publishes via installed CLI, observes readme body
  in Donut, pulls receiver, asserts clean head + exact authored bytes.
Verification: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` (2406
tests, 0 failures) and `CURSOR_DEV=true nix develop -c pnpm cy:run --spec
e2e_test/features/cli/cli_notebook_clone.feature` (11 scenarios, 0 failing).
Refactor: `none — already clean`. Formatter: `./scripts/run.sh pnpm
format:changed` (spotlessApply + biome; mechanical javadoc reformat only).

Learning: `ExportReadmeMarkdown.assemble` wraps untyped `readmeContent` with
`type: Readme` frontmatter when building the portable tree, so a notebook
seeded with raw `readmeContent("readme original")` produces a typed accepted
`README.md` blob — making a subsequent typed proposal a MODIFIED (not ADDED)
container. This is why the only production gap was the `admitShape` guard.

Slice 2 remains planned.
