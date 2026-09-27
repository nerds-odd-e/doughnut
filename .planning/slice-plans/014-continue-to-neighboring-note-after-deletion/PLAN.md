# Continue to a neighboring note after deletion

## Source

- Story: [SEED-047#story-1](../../seeds/SEED-047-deleted-node-navigation.md#story-1)
- **Identity:** SEED-047#story-1

## Goal and scope

After trashing a note, or permanently deleting a note already in trash, the
person lands on the neighboring note among the deleted note's folder peers (or
notebook-root peers), in this browser's current sidebar sort order: the note
that followed it, else the note now last. When no other note remains, today's
destination stays: the folder page, or the notebook page at the root.

Excluded: subfolders and files as destinations; folder, file, and move
navigation; undo navigation; relationship-note reduction; any backend change.

## Existing solution and direction

PFE: the sidebar already owns the browser-selected order.
`usePeerSort` (`frontend/src/composables/usePeerSort.ts`) holds the spec in
localStorage, and `sortSidebarStructuralRows` / `buildUnsortedStructuralRows`
(`frontend/src/components/notes/sidebarStructuralSort.ts`) produce the
displayed peer order from a `FolderListing` fetched by
`requestNotebookFolderListing`. Removal navigation is owned by
`StoredApiCollection.trashNote` and `permanentlyDeleteNote`
(`frontend/src/store/StoredApiCollection.ts`), which both
`router.replace(containingLocationOf(cachedRealm))`
(`frontend/src/routes/containingLocation.ts`). No next/previous note logic
exists anywhere.

Design (one rule, one place):

- Beside the sort, add the domain rule "the note to show after this note leaves
  these peers": sort the listing's rows with the given spec, keep note rows,
  return the note after the removed one, else the one before it, else none.
  It reuses `sortSidebarStructuralRows`, so the destination can never disagree
  with what the sidebar shows.
- In `routes/containingLocation.ts`, add the location decision:
  `noteShowLocation(neighbor)` when the rule returns a note, otherwise
  `containingLocationOf(realm)`.
- In `StoredApiCollection`, both removals request the containing peers'
  listing (`notebook id`, the realm's leaf folder id or null) **before** the
  removal request, then replace the route with that location. The pre-removal
  listing still contains the deleted note, so its position is found directly
  in the order the person saw. The sort spec comes from `usePeerSort()`, which
  is a module-level ref and needs no component context.

The sidebar's listing cache is not used: it holds only expanded folders and is
cleared on every structural refresh. Accepted ADRs: ADR 0001 (note, folder, and
file are the terms; there is no "node"), ADR 0005 (named note route through
`noteShowLocation`), and ADR 0006 (a failed listing request fails loudly; add no
fallback handling). No North Star topic applies.

## Outside-in proof

| Story example | Owning slice and observation |
| --- | --- |
| A, B, C: deleting B opens C | 1: store `trashNote` with a mocked peer listing replaces the route with B's successor `noteShow` |
| A, B, C: deleting C opens B | 1: same entry point, last note → the note now last |
| Only A beside a subfolder or file opens the folder page | 1: same entry point; the listing has a subfolder and a file but no other note → `folderPage` (and `notebookPage` at the root, keeping the existing root case) |
| Title (Z–A) order: deleting B opens A | 1: same entry point with `donut.noteSidebar.peerSort` stored as title/desc |
| E2E trash journey | 1: `note_deletion.feature` "Trashing a note in a folder opens its neighboring note": trash "TDD" (peers "CI System", "TDD" by title) → `the note title should be "CI System"`; the root scenario keeps `I should be on the notebook root page` |
| In trash, permanently deleting opens the neighbor | 2: store `permanentlyDeleteNote` with a mocked listing → successor `noteShow`; `note_deletion.feature` "Permanently delete a note that is in trash": delete "Cells" → `the note title should be "Tissue"` |

Focused checks: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/store/storedApi.trashNote.spec.ts`
(slice 2 adds its permanent-delete store spec), the frontend skill's typecheck,
then `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/note_deletion.feature`.

## Ordered slices

### 1. Trashing a note opens its neighboring note

Type: Behavior
Status: done
Proof: store `trashNote` examples above (B→C, C→B, alone beside a subfolder
and a file → folder page, title Z–A → A, root without other notes → notebook
page); the updated E2E trash scenario.

Behavior: a note with peers in its folder, sidebar order chosen in this
browser → the person trashes it → the neighboring note's page opens; with no
other note the folder or notebook page opens as before.

Adds the neighbor rule beside the sidebar sort, the location decision beside
`containingLocationOf`, and the pre-removal listing request in `trashNote`.
Update the existing store spec's expectations for folder and root cases
(their listings have no other note) rather than adding a parallel spec, and
rename the E2E folder scenario to its new outcome.

### 2. Permanently deleting a trashed note opens its neighboring note

Type: Behavior
Status: done
Proof: store `permanentlyDeleteNote` with peers A, B, C in the trash folder →
B's successor opens; the updated E2E permanent-delete scenario.

Behavior: a note in trash with peers in its trash folder → the person
permanently deletes it → the neighboring trashed note opens; with no other
note the folder page opens as before.

Reuses slice 1's rule and location unchanged; only `permanentlyDeleteNote`
requests the listing before removal and replaces the route with that location.

## Execution

- Story Branch Mode; worktree `.claude/worktrees/story-continue-to-neighboring-note`,
  branch `story/continue-to-neighboring-note`; claim `28974e985c` on `origin/main`.
- Slice 1 accepted proof: `storedApi.trashNote.spec.ts` "where the person lands"
  (12/12); `pnpm frontend:test tests/notes tests/store tests/toolbars` 353/353
  (all trash consumers mock an empty listing); `vue-tsc --noEmit` clean;
  `note_deletion.feature` 12 passing.
- Slice 2 accepted proof: landing examples for both removals live in
  `tests/store/storedApi.noteRemovalLanding.spec.ts` (notebook-root case for
  trashing only; permanent delete happens inside trash); `pnpm frontend:test
  tests/notes tests/store tests/toolbars` 357/357; `vue-tsc --noEmit` clean;
  `note_deletion.feature` 12/12 with "Cells" → "Tissue". The step
  `I should be on a notebook folder page` lost its last use and was removed.
- Learning: `StoredApiCollection.ts` is ~405 lines, over the 250-line guide
  (pre-existing ~380); splitting the store class is out of this story's scope.

## Current decisions

- Neighbors are notes only; folder and file rows are skipped because the story
  scopes neighbors to notes.
- The destination comes from the listing fetched before the removal, sorted by
  the current `usePeerSort` spec, so it matches the order the person saw.

## Execution complete

Product advice: No backlog change. Story 1 delivers the owner's request as
refined; its only learning is bounded correction SEED-047#story-2 (plan 015:
removal landing loads the folder listing through the shared loading path, and
the sidebar and neighbor rule share one listing-to-rows function). Deferred
folder, file, move, undo, and relationship-reduction navigation stay deferred.
