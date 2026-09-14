# Trash and recover a folder

Status: planned; no implementation started. Authority: refinement and slice
planning only. Source: [SEED-009 story 33](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-33).

## Goal and scope

An owner can trash one active folder as a retained subtree, revisit it, and
recover it using ordinary Move. Preserve folder Readmes and metadata, nested
empty folders, note content/identity, learning history, and independent tracking
preferences. Trash changes availability through location, not another lifecycle.

Mirror the selected folder's ancestors beneath notebook-root `_trash`, then
place the selected folder there. Reuse ancestor path folders. If the selected
folder's destination name is occupied, suffix the incoming folder as a whole
with the first available ` (2)`, ` (3)`, etc. Never merge it into earlier trash.
Root matching stays case-insensitive; new trash roots use lowercase `_trash`.

One confirmation on the existing folder Settings surface explains the subtree
effect, retained references, and recovery through Move. Preserve all authored
reference spelling; no bulk property removal or relationship reduction. After
Trash, navigate to the former parent or notebook root and refresh the existing
sidebar listings. Move recovery uses an existing active destination or notebook
root in the same notebook; existing Move conflict/explicit merge behavior stays.

Excluded: event folders/metadata, operation history, folder Undo, Restore and
missing-active-parent reconstruction, bulk selection, permanent deletion, expiry,
new Git/local compatibility, schema changes, performance projects, and reference
repair. Existing note Trash/Undo and supported moves remain working. Deferred
cases are not new rejection rules. Empty folders use the ordinary rule.

## Architecture and PFE decisions

Follow the seed's [portable-trash North Star](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#portable-trash--parent-problem-former-stories-26-and-27):
location owns membership; ordinary placement owns entering/leaving trash; no
second trash state, journal, resolver, or cache. Preserve one domain owner for
each responsibility and remove any replaced implementation within its slice.

The shared [North Star](../../NORTH-STAR.md#one-final-publication-result) governs
publication, not this web story. Respect its placement/content ownership without
editing publication orchestration or building its deferred Git capabilities.
No new North Star topic is needed for this extension of existing owners.

| Responsibility | Inspected existing solution and decision |
| --- | --- |
| Trash root and ancestor construction | `FolderConstructionService.ensureTrashParentFor(Note)` already finds the case-insensitive root and mirrors the path. Evolve this owner to accept the notebook and containing-folder trail; keep note Trash using it. `FolderTrailSegments.ancestorsFromRootToParent(Folder)` already supplies the folder trail. Do not create a second path builder. |
| Place an existing subtree | `FolderMoveRelocation` owns validation and ordinary reparenting; `FolderRelocationService` exposes it. Factor its current non-merge placement into a shared operation used by ordinary Move and Trash. Keep reference rewriting with ordinary Move orchestration. Trash must not call rename/move wrappers that rewrite links and then try to undo those edits. Do not copy the placement sequence into a folder-trash service. |
| Destination collisions | `FolderSiblingNameValidation` owns folder sibling matching; `NoteTitlePlacementRules.firstAvailableTitleAt` owns existing numbered note names. Share the small numbered-name selection algorithm when adding folder suffixes, with each owner supplying its own occupancy and name-length rules. Do not share note/folder uniqueness semantics or introduce a naming framework. |
| Availability and references | `Folder.isTrashed`, `Note.isAvailable`, and the existing read-only ancestry query boundary derive availability. `AuthoredNoteReferenceInboundFacade` resolves candidates live and omits inactive referrers. Retain authored indexes when content is unchanged; no recursive index deletion/rebuild or tracker toggling for a location-only change. |
| Web interaction and recovery | `FolderPage`, `FolderSettings`, `useFolderAdmin`, and `folderAdminMutations` already own folder actions, loading, navigation, refresh, and Move conflict handling. Extend these owners. Reuse existing Move for recovery; do not add another recovery endpoint or UI. |
| Other product boundaries | CLI/MCP do not own this web trash placement. Git folder correspondence/materialization serves publication and must not become a second web trash engine. `NoteLegacyTrashMigration` is migration-time code, not a live placement owner; do not rewrite historical migration behavior to share runtime helpers. |

Relevant Accepted ADRs: [0001](../../../docs/adrs/0001-ubiquitous-language.md)
for Folder/Readme meaning; [0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
for location-based trash, retained content and live reference resolution;
[0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md)
for unchanged learning state; [0005](../../../docs/adrs/0005-web-routes-accepted.md)
for identity-based/named navigation; [0006](../../../docs/adrs/0006-failure-handling-accepted.md)
for loud unexpected failures; [0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
for disposable test environments. No ADR change is required. Proposed ADR 0002
is not an accepted constraint or an authorization to add Git scope.

## Ordered slices

Target approximately 5 minutes per leaf including focused proof and cleanup.
Mandatory full-suite execution, E2E startup, and delivery waits may exceed that;
record waiting separately. Scrutinize active work above 5 minutes; above 10,
stop and finer-decompose with preserved evidence. Do not hide an overrun by
adding special-case handlers. Each slice must finish green; incomplete browser
journeys remain unfinished, not a commit boundary.

### 1. Share folder placement while preserving ordinary Move
Type: Structure
Status: planned
Change: Expose the existing non-merge folder placement through its current
relocation owner, separating placement from ordinary Move's reference capture
and rewrite. Keep ordinary moves, conflict handling, explicit merges, and
cross-notebook behavior unchanged. This enables Trash to reuse placement without
rewriting authored content in slice 2. Do not introduce a boolean mode matrix or
an abstract trashable-entity hierarchy.
Proof: Drive existing folder Move through the controller with a subtree beneath
`_trash`, then move it to an active parent. Establish retained folder/Readme,
note/tracker identities, recall history, stopped-tracking preferences and empty
descendants, with participation following ancestry. Reuse the established
`RelationControllerMoveNoteToFolderTests` fixture/assertion pattern. This proves
the shared placement needed by the next slice, not a new recovery mechanism.
Existing `NotebookFolderMoveControllerTest` and
`NotebookFolderMoveWikiLinkRewriteControllerTest` preserve ordinary Move and
reference rewriting. Run the full backend suite; no helper-level test surface.
Sizing: about 5 minutes active work; existing behavior owns the regression proof.

### 2. Trash a folder and recover its retained subtree through the web
Type: Behavior
Status: planned
Behavior: Given `Research/Biology/Topic` with saved Readme/frontmatter, a direct
note, a nested learned note and an empty nested folder, and no conflicting trash
destination → confirm Trash, revisit after reload, then Move the subtree to
notebook root → the same retained subtree is usable at `Topic`; active siblings
are unchanged. While in trash, descendants are excluded from active use.
Change: Extend existing trash-parent construction for the folder's ancestor
trail and use shared placement in an authorized transactional folder action in
`NotebookController`/the existing folder domain owner. Reuse current folder
Settings confirmation/loading/navigation/refresh and Move. API generation is
part of this slice. Preserve references by doing no content transformation.
Authorize and validate the source before creating trash parents. Root `_trash`
and already-trashed folders are not eligible for the active-folder Trash action.
Until slice 3 adds automatic collision naming, ordinary conflict refusal remains
a safe interim result; do not overwrite or merge.
Proof: One scenario in `e2e_test/features/folder_organization/folder_trash.feature`
owns Trash → reload/browse → Move recovery, observing the mirrored nested path,
saved Readme/content, and unchanged active siblings. Reuse existing folder and
Move steps. Controller tests drive the new Trash action with the retained-subtree
fixture from slice 1: prove the action retains those identities, leaves internal
and external authored references unchanged, and live reference/search/learning
eligibility follows trash ancestry. Reuse slice 1's full data-preservation proof
rather than repeat every assertion. Mounted-page tests own cancellation, action
visibility and former-parent/root navigation. Controller authorization tests
establish no writes for a foreign/mismatched source.
Atomicity: Use the real existing HTTP transaction boundary for parent creation
and placement; propagate unexpected errors. Review that no mutation runs outside
that transaction or in per-descendant transactions. Ordinary validation refusal
must leave the source and earlier trash unchanged. Do not add failure injection,
compensation machinery, or synthetic catch paths solely to test a loud failure.
Sizing: target 5 minutes active work; UI/API integration remains the main
uncertainty. Slice 1 owns shared placement and its detailed preservation proof.
Nested paths follow the general rule; do not build a root-only mode. Apply the
10-minute stop/refine limit rather than treating this estimate as an exception.

### 3. Keep a colliding folder Trash separate from earlier trash
Type: Behavior
Status: planned
Behavior: `_trash/Biology` and `_trash/Biology (3)` exist → Trash active `Biology`
→ the incoming complete subtree becomes `_trash/Biology (2)`; both earlier trash
subtrees are untouched. Revisit and Move `Biology (2)` to an existing active
destination; its visible suffix, contents, and identities are retained.
Change: Reuse the small numbered-name iteration in `NoteTitlePlacementRules`,
extracting only what both live callers need within this slice. Keep note/folder
occupancy rules with their existing owners; no generic naming service. Use
`FolderSiblingNameValidation` and the folder's actual length limit, then apply final name and parent once
through shared placement. Keep all descendants under the same folder identity.
Do not rename through a link-rewriting UI operation, merge, loop over notes to
trash them separately, or add an event identity. Remove the interim Trash
collision-refusal expectation from slice 2; ordinary Move conflicts are unchanged.
Proof: Add the colliding-folder round trip to the same E2E feature. Controller
proof owns first-free suffix selection (including holes, existing case rules,
and existing name length constraints) and preservation of both earlier subtrees.
Reuse existing Move rejection/explicit merge tests; only add missing coverage
that the same Move conflict leaves a trashed subtree recoverable. Do not duplicate
all learning assertions for a changed basename. Existing `NoteControllerTrashTests`
retain note suffix behavior, including its existing name-length handling.
Sizing: approximately 5 minutes active work. Name-selection extraction is small
local work owned by this behavior, not a separately deliverable subsystem.

## Proof ownership and verification

| Promise | Owner |
| --- | --- |
| Complete web Trash/revisit/Move recovery, nested original path, parent/root navigation | Slice 2 E2E and mounted-page observations |
| Readme/frontmatter, content, identity, learning history/preferences, empty descendants | Slice 1 shared-placement controller proof; slice 2 new-action identity checks and representative content/Readme in E2E |
| Location-driven search/learning/wiki eligibility; authored links and derived references remain coherent | Slice 2 public controller observations based on existing note-recovery tests |
| Permission, cancellation, active-source constraint, transactional mutation boundary | Slice 2 controller/mounted tests and transaction-boundary inspection |
| Earlier trash untouched; whole-folder first-free suffix; suffix survives Move | Slice 3 E2E/controller observations |
| Ordinary folder Move/reference rewriting/conflicts and note Trash/Undo preserved | Slice 1/3 existing regressions plus full backend checks during behavior slices |

Run repo tooling with `CURSOR_DEV=true nix develop -c`:

- `pnpm backend:test_only` — all backend unit tests, as required by backend rules.
- `pnpm frontend:test` — frontend suite for behavior changes (focused files while iterating).
- `pnpm generateTypeScript` — when controller contracts change; never edit generated files.
- `pnpm cy:run --spec e2e_test/features/folder_organization/folder_trash.feature`
  — the new complete user journeys, only once the feature exists.
- Existing `folder_organization.feature`, `folder_page_readme.feature`, and
  `note_creation_and_update/note_deletion.feature` are targeted preservation
  evidence when implicated; do not replace them with backend-only proof.

No manual testing is required by this plan. No new storage-engine assumption,
schema or migration needs an experiment. Ordinary transactions and ancestry
queries are existing mechanisms; changed callers still need their stated proof.

Execution delivery follows dough-execute-plan: Jidoka; fresh independent
dough-post-change-refactor agent; generated API if needed; coordinator runs
`./scripts/run.sh pnpm format:changed` once; update this plan; commit with the
check-only lint hook; push and observe CI asynchronously. Implementers/refactorers
do not run format:changed or standalone lint:changed. Preserve unrelated work.
Keep this plan and review evidence for retrospective; planning does not take the
story from the backlog, authorize execution, commit, push, or release.

## Remaining concerns and current evidence

- Slice 2 has the highest sizing uncertainty: a new action must integrate with
  the existing page, generated client, and real recovery journey. Slice 1 removes
  the identified placement preparation and owns detailed data-preservation proof.
  Do not create extra endpoints or UI modes to split the journey. If active work exceeds 10 minutes, stop with concrete
  evidence and refine the remaining work rather than broadening this plan.
- Slices 1–2 must preserve ordinary Move rewriting while Trash preserves authored
  spelling. Merely calling the existing complete Move method is insufficient.
  Inspect this distinction in the independent refactoring pass.
- Earlier inspection found note-recovery tests, but no dedicated folder-trash
  round trip. A browser rerun in this conversation failed during SUT startup
  with backend compilation errors and timeout, before Cypress scenarios ran.
  That is not passing evidence or a diagnosed product bug. Re-establish the
  baseline in an isolated execution environment; do not expand this story into
  tooling repairs without identifying the actual blocker.
- Cumulative design check: one trash-parent builder, one folder-placement owner,
  one numbered-name iteration, and existing location/reference eligibility. The
  slices extend the same rule; none creates a per-story or per-example engine.
  No architectural conflict was identified in this planning assessment.

Refinement: retained the shared-placement slice, narrowed the web slice's proof
to its new behavior, and folded the former name-selection Structure slice into
the collision behavior. Three slices remain. No story promise or North Star
direction changed; no implementation or product verification ran in refinement.
