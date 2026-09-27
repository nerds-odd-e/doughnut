# Notebook pages and the sidebar say when content cannot be loaded

## Source

- Story: [SEED-050#story-3](../../seeds/SEED-050-local-ai-notebook-technical-debt.md#story-3)
- **Identity:** SEED-050#story-3
- Found by the closing review of the local AI notebook effort; re-evaluated
  against `d9abdcc2eb` on 2026-09-27 and cut to the frontend part (the
  foreign-key change was already done; the Git-binding part was dropped).
- Failure handling: fail loudly, catch only for a clearer message (ADR 0006).

## Goal and scope

A notebook owner who opens a notebook, folder or file page that cannot be
loaded, or whose sidebar listing fails, sees a clear message instead of an
endless spinner, and a folder never shows as empty when it is not.

Included: one failure state in the shared route loader shown as one inline
error in place of the page; a failed sidebar listing keeps its rows and shows
an inline message.

Excluded: backend changes; a not-found page; retry buttons; note pages;
toast changes; E2E tests.

## Starting facts (checked 2026-09-27 at `d9abdcc2eb`)

- `frontend/src/composables/useNotebookSidebarRouteRealms.ts`:
  `loadRealmOnRoute` (`:19-40`) stores whatever the fetcher returns; the three
  fetchers turn any error into `undefined` (`:59`, `:76`, `:89`), and the
  folder fetcher also checks `page?.notebookRealm?.notebook`.
- `NotebookPage.vue`, `FolderPage.vue`, `AttachmentPage.vue` spin while their
  realm is `undefined` (line 2 of each); the layout also hosts note routes,
  and `routeViewProps` is used only by it; `frontend/src/layouts/NotebookSidebarLayout.vue:65-67`
  renders the route component with `routeViewProps`.
- `frontend/src/components/notes/SidebarInner.vue:108-121` (`refreshListing`)
  sets `rawRows = []` and reports 0 peers on failure, also wiping rows applied
  from the cache (`:123-133`). The listing request deliberately avoids the
  global busy indicator (locked by `SidebarFolderListingReload.spec.ts:35`).
- A non-404 error already shows a 3-second toast through
  `apiCallWithLoading` (folder and file pages); a 404 shows nothing.
- Inline error convention: `<p class="text-error text-sm">`
  (`FolderSettings.vue:31`, `FolderSearchForm.vue:18`).
- Harnesses: `frontend/tests/notes/sidebar/SidebarRouteNavigation.spec.ts`
  mounts `NotebookSidebarLayout` with the real router;
  `frontend/tests/notes/sidebar/SidebarFolderListingReload.spec.ts` covers
  listing refresh; `wrapSdkError` in `@tests/helpers`.

## Outside-in proof

| Seed example | Slice |
| --- | --- |
| 1. deleted file's URL → "Could not load this page.", no spinner | 1 |
| 2. gone folder's URL → same message (same shared loader) | 1 |
| 3. refresh fails → earlier rows stay, plus "Could not load this folder's contents." | 2 |
| 4. first root listing fails → the message, not an empty tree | 2 |

## Slices

### 1. A notebook, folder or file page that cannot be loaded says so

Type: Behavior
Status: done
Proof: a new case in `SidebarRouteNavigation.spec.ts` mounts the layout at
the `attachmentPage` route with `NotebookAttachmentController.getAttachmentPage`
returning `wrapSdkError("not found")` (any error) and asserts "Could not
load this page." is shown and no `[data-app-busy]` spinner remains; it then
navigates to a note route and the message is gone. Fails first, then passes.
One failing page suffices because the three pages share the loader —
`CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/sidebar/SidebarRouteNavigation.spec.ts`,
plus the frontend typecheck from the `frontend` skill.

Behavior: a page's realm request fails (any error) → the layout shows
"Could not load this page." in place of the page.

Change: `loadRealmOnRoute` takes the SDK result, checks `error` once, and
keeps a failed flag cleared on each reload and when the route is left (its
`path === undefined` branch); the layout shows the inline `text-error`
message instead of the route component only while the current route's
loader has failed. The three per-fetcher `error ? undefined` checks, the
folder fetcher's defensive notebook check and `FolderPage.vue`'s duplicate
`notebookRealm?.notebook == null` guard go.

### 2. A failed sidebar listing keeps what it showed and says it could not load

Type: Behavior
Status: done
Proof: a new case in `SidebarFolderListingReload.spec.ts`: the first listing
succeeds, then `refreshSidebarStructuralListings()` fires with the listing
mock returning an error → the earlier rows stay, "Could not load this
folder's contents." is shown, and the busy-state list stays empty; a second
case where the first load fails shows the message. Fail first, then pass —
`CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/sidebar/SidebarFolderListingReload.spec.ts`,
plus the frontend typecheck.

Behavior: a sidebar listing request fails → its rows stay as they were and an
inline message appears for that listing; a later success clears it.

Change: `refreshListing`'s catch sets a failed flag instead of wiping rows and
the peer count; success clears it; one inline `text-error` line under that
listing (the root is a single `<ul v-if>`, so the message sits in a fragment
beside it). The root listing and each folder mount their own `SidebarInner`,
so each listing shows its own message. Still no `apiCallWithLoading`.

## Current decisions

- One generic message per surface; no separate 404 wording.
- Existing toasts stay; no new requests.

## Learnings

None yet.
