# Removal landing loads the folder listing like other user actions

## Source

- Story: [SEED-047#story-2](../../seeds/SEED-047-deleted-node-navigation.md#story-2)
- **Identity:** SEED-047#story-2
- Correction of SEED-047#story-1 (`edfd7ba92a:.planning/seeds/SEED-047-deleted-node-navigation.md`),
  plan `edfd7ba92a:.planning/slice-plans/014-continue-to-neighboring-note-after-deletion/PLAN.md` (original at
  `a418efdb98`); reviewed commits `a41b1e0507` (trash lands on neighbor) and
  `ff9b3e7871` (permanent delete lands on neighbor), base `6cac6f9127`.

## Current findings (execution retrospective, at `ff9b3e7871`)

1. **No busy state during the new pre-removal request.**
   `StoredApiCollection.locationAfterRemoving` calls
   `requestNotebookFolderListing` directly. Every other user-triggered listing
   load wraps it in `apiCallWithLoading`
   (`utils/relationshipFolderResolve.ts` `loadFolderListing`,
   `composables/useFolderSelectorNeighbourListing.ts`); only the sidebar's
   background refresh does not. Before story 1 the first request after
   confirming a trash or permanent delete was the removal itself, which shows
   the loading modal at once. Now the app looks idle during the listing
   round-trip, and the confirm action can be repeated in that window.
2. **"Load a folder listing or fail loudly" is written three times.**
   `relationshipFolderResolve.loadFolderListing` already does exactly this
   (loading, throw on `error || !data`); `locationAfterRemoving` repeats it
   without loading. That mismatch is how finding 1 happened (a missed reuse).
3. **"Listing to sidebar rows" is written twice.**
   `neighborNoteAfterRemoval` (`components/notes/sidebarStructuralSort.ts`) and
   `SidebarInner.vue` `applyListing` both spell
   `buildUnsortedStructuralRows(listing.noteTopologies ?? [], listing.folders, listing.attachments)`.
   The neighbor rule's guarantee "never disagrees with the sidebar" depends on
   these staying the same.

## Goal and scope

One outcome: removal landing loads its listing through the one shared
user-action listing load (busy from confirmation until landing), and the
sidebar and the neighbor rule build rows from a listing through one function.

Preserved: every story 1 landing example (B→C, C→B, Z–A order, folder page
when only subfolders/files remain, notebook page at the root, trash-folder
"Cells" → "Tissue"); ADR 0006 (a failed listing fails loudly, no fallback);
the sidebar's background refresh stays without the loading modal.

Excluded: splitting `StoredApiCollection.ts` (~403 lines; pre-existing size);
`useFolderSelectorNeighbourListing`, which catches the failure to show its own
message, may keep its own call unless reusing the shared load is simpler.

## Outside-in proof

| Promise | Owning slice and observation |
| --- | --- |
| Busy from confirmation until landing | 1: a store or `NoteMoreOptionsForm` trash spec with `GlobalApiLoadingModal` holds the listing request pending and observes the loading modal (the pattern in `tests/components/recall/AssimilationPanel.loadingModal.spec.ts`) |
| Landing examples unchanged | 1: `tests/store/storedApi.noteRemovalLanding.spec.ts` unchanged and green |
| Relationship placement unchanged | 1: existing `relationshipFolderResolve` consumers' specs green |
| Sidebar rows unchanged | 1: existing sidebar specs green |

Focused checks: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes tests/store tests/toolbars tests/wiki-link-or-relationship tests/components/notes`,
the frontend skill's typecheck. E2E only if the step timing changes:
`CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/note_deletion.feature`.

## Ordered slices

### 1. Removal landing uses the shared listing load and row builder

Type: Behavior
Status: done
Proof: the loading-modal observation above fails before the change and passes
after; the landing, relationship and sidebar specs stay green.

Behavior: a note is open → the person confirms trash (or permanent delete) →
the loading modal shows while the peer listing loads and until the removal
finishes, and they land where story 1 lands them.

Move `loadFolderListing` from `relationshipFolderResolve.ts` beside
`requestNotebookFolderListing` in `utils/notebookFolderListingRequest.ts` and
use it from `relationshipFolderResolve` and `locationAfterRemoving` (removing
its inline error check). Give `buildUnsortedStructuralRows` a single
`FolderListing`-taking entry (or add one next to it) and use it from
`SidebarInner.applyListing` and `neighborNoteAfterRemoval`. Expected net
change: fewer lines.

Accepted proof: `tests/store/storedApi.trashNote.spec.ts` "shows the app as
busy while the peer listing loads before trashing" (one busy state while the
listing is pending, none after landing; failed before the change);
`CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes tests/store tests/toolbars tests/wiki-link-or-relationship tests/components/notes tests/composables`
and `tests/pages` green; `vue-tsc --noEmit` clean.

## Current decisions

- The shared load owns loading and the loud failure; callers add neither.
- `useFolderSelectorNeighbourListing` reuses the shared load too (simpler); it
  keeps its own catch and message.

## Learnings

- User-triggered listing loads show the non-blocking busy bar
  (`apiCallWithLoading` without `blockUi`), not `LoadingModal`. The proof
  observes that busy state, matching the other listing loads, instead of the
  modal the plan first named.
