# Find and recover trashed notes through web navigation

Status: planned
Source: [SEED-009, story 34](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-34), refined 2026-09-13.
Authority: Slice planning and scope/refinement review only. Product execution
is not authorized by this request.

## Goal and scope

A notebook owner starts at their notebook, finds an older trashed note without
its URL or immediate Undo, and recovers it with ordinary Move. Use the existing
notebook tree, folder pages, note view, and Move interaction. Trash folders and
notes display a warning; recovery preserves identity, content, learning history,
and independent tracking preferences, removes the warning, and resumes normal
location-based eligibility.

The delivery examples use an existing active folder or root in the same notebook.
This is a proof boundary, not a new restriction on ordinary Move. Preserve
authorization, conflicts, editing/direct access, Trash/Undo, legacy recovery,
reference handling, and active eligibility rules. No migration (story 31),
Restore/missing-parent creation (32), folder Trash (33), Git/local compatibility
(28), bulk recovery, trash dashboard/search/filtering/counts, or permanent-delete
UI. Do not reconstruct references deliberately removed during trashing.

## Existing solutions and decisions

PFE inspection at `b0359f1f20` supports reuse; no new lifecycle owner is needed:

- `NotebookController.listNotebookFolderListing` and `NoteService` use content
  listings, not active-note selection. `NoteRepository.findNotesInFolderOrderByIdAsc`
  excludes legacy `deletedAt` notes but includes location-based trash. Root and
  child folder listings already return `_trash` and its descendants.
- `SidebarInner` / `SidebarFolderItem` render those listings and ordinary named
  links. Keep this navigation; exposing trash does not mean admitting it into
  search or learning. Existing empty-folder behavior is sufficient.
- `FolderRealm` already supplies root-to-parent ancestors and the current folder.
  `FolderPage` lacks a trash warning. `NoteShow` already derives its warning from
  root ancestry. Share that small location rule between these views, including
  the current folder when determining a folder page's root; no extra API field,
  stored flag, folder-index fetch, or parallel state is needed.
- `RelationController` / `NoteMotionService` already own ordinary Move and
  conflict checks before mutation. `SearchForm` and `StoredApiCollection` own
  its UI and refresh note realms/sidebar listings after success. Reuse these
  owners and correct only gaps demonstrated by the selected recovery examples.
- `RelationControllerMoveNoteToFolderTests.movingLearnedNoteIntoAndOutOfTrashChangesParticipationButPreservesItsData`
  already observes Move-based search/recall/assimilation/wiki participation,
  direct access, retained recall history, and stopped tracking. It uses ordinary
  Move into trash and back to root; it does not prove the web navigation journey
  or all retained data after web Trash followed by Move. `NoteControllerTrashTests`
  covers web Trash/Undo data and reference choices, not delayed Move recovery.

Follow the seed's **Portable trash → North Star and completion boundary**:
location owns membership, ordinary moves own recovery. This story leaves the
legacy retirement obligation with story 31. No new direction topic is warranted.

Accepted decisions: [ADR 0004 — Trash](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md#trash)
requires case-insensitive notebook-root membership and retained identity/history;
[ADR 0005](../../../docs/adrs/0005-web-routes-accepted.md) keeps named, identity-based
navigation; [ADR 0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md#recall-history-and-current-state)
governs retained learning state. Follow [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md)
for existing error handling and [ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
for isolated test data. ADR index and record statuses agree; ADR 0002 is Proposed.
No ADR exception or new storage assumption is needed.

## Ordered slices

Each slice is planned and owns one behavior/proof loop, including any necessary
small implementation change and its proof. Do not change working production
code merely to manufacture implementation work. Target about 5 minutes including
focused verification and local cleanup; scrutinize >5 and stop/finer-decompose
>10 minutes unless irreducible test startup/suite runtime is the cause. Record
that external wait separately. On an implementation overrun, preserve completed
evidence, safely park/revert only incomplete attempt-owned work, record the
failed sizing assumption here, and reassess before subdivision. No blanket
exception permits a broad navigation or Move rewrite.

### 1. Recognize trash while browsing ordinary folders
Type: Behavior
Status: done
Sizing: about 5 minutes, medium confidence; existing realm data is sufficient.

Behavior: Owner opens notebook-root `_trash`, or a folder beneath it, through
ordinary navigation → the folder page clearly says it is in trash. Existing
note content and warnings remain usable through the same tree.

Proof: Mounted `FolderPage` examples cover root and descendant ancestry,
case-insensitive root matching, and active `Projects/_trash` not being trash.
Retain the mounted `NoteShow.spec.ts` warning proof when sharing the location
predicate. Existing sidebar/listing tests preserve discovery and absent/empty
folder navigation; the actual nested browser journey is owned by slice 2.
No trash folder is created by viewing a notebook.

### 2. Recover an older note into an existing active folder
Type: Behavior
Status: done
Sizing: about 5 minutes, medium confidence; reuse existing folder-navigation,
Trash, remount, and Move page objects. Test startup is an external-wait exception.

Behavior: `Biology/Cells` was trashed; the owner returns to the notebook after a
full app remount with no session Undo → navigates `_trash/Biology`, opens `Cells`,
and Moves it to existing active `Biology` → the same note appears there without
the warning, with retained content/history/preferences and normal eligibility.

Proof: Add this journey to `e2e_test/features/note_creation_and_update/note_deletion.feature`.
Use real web Trash in setup, remount at the notebook, then use tree UI for the
discovery trigger (never jump to the target URL or locate it through search).
Reuse the existing notebook-page reload step for the remount. Parent-scoped
folder activation already exists in `noteSidebar`; opening the nested folder
page may need a small corresponding parent-scoped link helper. Keep that helper
with this behavior, not a separate navigation framework or preparation slice.
Assert folder/note warnings while browsing and the destination placement,
same note identity/content, and cleared warning after Move. The page objects
must select the child under the chosen parent, since `Biology` occurs twice.

At `RelationController`'s stable boundary, cover the missing canonical web
Trash → ordinary Move retention observation: existing note/tracker IDs, recall
history, and stopped-tracking preference after recovery. Reuse existing
participation and reference-choice proof rather than recreating it at every
layer; add only a missing removed-property non-recovery observation if current
assertions do not cover it. No new recovery endpoint or Undo dependency.

### 3. Recover to notebook root when the original parent is absent
Type: Behavior
Status: planned
Sizing: about 5 minutes, medium confidence; existing root destination is reused.

Behavior: `_trash/Biology/Cells` exists without active `Biology` → owner chooses
notebook root in ordinary Move → `Cells` is available at root without recreating
`Biology` or using Restore.

Proof: Extend the same browser feature with this destination variant, reusing
the navigation/Move steps from slice 2. Observe root placement and absent active
parent; do not repeat identity/history assertions. Use Given data for the
missing-parent precondition rather than adding a parent-deletion workflow.

### 4. Keep recovery safe when the selected destination is occupied
Type: Behavior
Status: planned
Sizing: about 5 minutes, medium confidence; validation already precedes mutation.

Behavior: Independent active `Biology/Cells` occupies the requested destination
→ owner attempts Move from trash → ordinary conflict is reported, both notes
remain intact, and a subsequent Move to a free destination succeeds.

Proof: A focused controller example drives ordinary Move out of trash and
checks occupied-target failure without overwrite or relocation. A mounted
`SearchDialog` action example shows the existing conflict feedback retains a
usable destination interaction for a successful retry. Extend existing Move
proof where sufficient; Undo-conflict tests alone do not establish this promise.
Preserve ordinary authorization regressions; do not add a trash-specific
conflict policy, automatic suffix, or silent replacement.

## Proof ownership and safe stopping points

| Story promise or boundary | Owner |
| --- | --- |
| Find older nested trash from notebook without URL/search/Undo | 2: remounted real browser journey |
| Trash-root/descendant folder warnings, note content/warning | 1: mounted variants; 2: actual navigation |
| Existing-folder recovery, refreshed placement and cleared warning | 2: real Move through UI |
| Identity, content, history, independent preferences retained | 2: browser identity/content and controller retention delta |
| Missing original parent, root recovery without reconstruction | 3: browser destination variant |
| Occupied destination preserves both notes and permits retry | 4: controller safety and mounted interaction |
| No trash folder / existing empty folder remain navigable | 1: existing listing/sidebar proof; add only an uncovered boundary |
| Eligibility follows location; browsing does not activate notes | 2: reuse Move participation controller proof and read-only listing behavior |
| Direct access/editing, authorization, legacy recovery, Trash/Undo, references | 2/4: retain existing controller, note-view, Move and Trash regression proof |

Slice 1 supplies a useful warning without changing recovery. Slice 2 is the
first full feedback journey; ask the owner to evaluate tree and Move
discoverability when available. Slices 3–4 complete the agreed boundaries;
feedback after slice 2 does not mark the story complete or cancel them.

## Verification and delivery on later authorized execution

- Frontend: `CURSOR_DEV=true nix develop -c pnpm frontend:test`
  (mounted spec selection during iteration; full suite for delivery).
- Backend changes or new controller proof:
  `CURSOR_DEV=true nix develop -c pnpm backend:test_only` (all backend unit tests).
- Browser journey:
  `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/note_deletion.feature`
  (wrapper owns the disposable stack). No manual testing is required by this
  plan; the owner feedback checkpoint is separate from automated proof.
- API shape changes are not expected; if necessary, regenerate with
  `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`, never hand-edit it.
- Apply the required execution delivery: Jidoka, fresh
  `dough-post-change-refactor` agent, generation if needed, coordinator's one
  `./scripts/run.sh pnpm format:changed`, plan update, commit/check-only hook,
  push, and asynchronous CI. Retain the plan for retrospective and wrap-up.

## Scope and slice review

Four Behavior slices; no preparatory framework or new architecture. Construction
separated the missing-parent and occupied-destination outcomes from the main
journey so each owns its delta without repeating the full retention proof.
All refined examples and continuity constraints have an owner; excluded sibling
promises remain excluded. No prior slices or completed evidence were replaced.

Second review checked the existing notebook reload step, parent-scoped sidebar
activation, Move destination actions, and controller retention fixtures. Keep
all four slices: each has one behavior and its own observable delta; no slice
replacement or story resplit is indicated by this inspection. No sizing
exception beyond irreducible test startup/suite runtime is allocated.

Remaining sizing concern: slice 2 still composes those existing actions into a
journey that has not been run, including a possible parent-scoped folder-page
link helper. Reusing the existing fixture and controls is the sizing assumption,
not a claim of tested integration. If substantial orchestration is needed,
reassess that slice before exceeding the hard limit; do not hide discovery behind
direct routing. No other slice-specific concern was identified. Product tests
have not been run during planning; this assessment does not authorize execution.
