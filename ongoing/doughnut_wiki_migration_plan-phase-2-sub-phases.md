# Doughnut Wiki Migration Plan - Phase 2 Sub-Phases

## Purpose

This document decomposes **Phase 2 - Introduce Slug Paths** from `ongoing/doughnut_wiki_migration_plan.md` into small, closed sub-phases.

Each sub-phase should be small enough to complete in about 5 minutes and commit independently. Do not start the next sub-phase until the current one is green, cleaned up, and commit-ready.

## Phase 2 Target

After Phase 2:

- folders have **`folder.slug`** as the persisted notebook-local full path
- notes have **`note.slug`** as the persisted notebook-local full path (one column; basename is derivable as the suffix after the last `/`)
- new data gets slugs as it is created
- existing production data can be migrated through a temporary admin-only flow
- notes can be resolved through slug/path routes
- moves validate uniqueness via recomputed **`note.slug`**
- after production slug backfill, a **generalized** admin data-migration dashboard shell and admin-only HTTP surface remain (auth-checked stub; no slug-specific DTOs or migration logic)

## Key Decisions

- Use `com.github.slugify:slugify` as the single slugifier for folder and note basenames (title-derived segments).
- Persist **`folder.slug`** as the folder’s notebook-local full path: **`basename`** at notebook root, or **`parentFolder.slug + "/" + basename`** when nested.
- Persist **`note.slug` only** for note addressing: **`folder.slug + "/" + basename`** when foldered, or **`basename`** at notebook root. Do not persist a separate **`file_slug`** / **`full_path`** pair on **`Note`**.
- Notebook endpoints continue to identify notebooks by internal ID; do not add **`notebook.slug`** in Phase 2.
- Add schema fields as nullable first so the deployment is safe before production backfill.
- Make the data migration **idempotent and batched**. Production has about 30k notes, so no endpoint should try to migrate all rows in one long request or one long transaction.
- Expose a temporary admin-only migration capability:
  - status endpoint reports remaining work
  - batch endpoint processes a bounded chunk and returns progress
  - admin dashboard tab shows progress and has a button to start or continue the migration
  - frontend may repeatedly call the batch endpoint while the admin watches progress
- Add final uniqueness / not-null constraints only after production migration has completed.
- After slug backfill and constraints: **strip** slug-specific migration DTOs, batch/status implementation, and dead code; **retain** a generalized admin dashboard area (tab name and copy not tied to slugs) with a button and progress affordances, plus an admin-only migration endpoint that performs **only** authorization (empty or minimal handler body) for future DB migrations.

## Status

All sub-phases are **planned**. **Progress:** Sub-phase **2.11** backend deliverables (basename lookup API and access control) are implemented; **2.11.1** and **2.11.2** are the remaining frontend router/page consolidation and E2E alignment for that capability.

## Sub-Phases

### 2.1 Standard Slug Helper Contract

**Type:** Structure

Create the shared slug-generation helper used by all later Phase 2 behavior.

**Commit includes:**

- add the Java slugify dependency if it is not already present
- introduce a small backend slug helper/service with black-box tests for:
  - normal English titles
  - Japanese / non-ASCII titles
  - empty or punctuation-only inputs
  - deterministic collision suffixing within a supplied sibling set (basenames in the same folder)

**Verification:**

- `CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests "*Slug*"`

### 2.2 Add Nullable Slug Columns

**Type:** Structure

Add persistence fields without changing observable behavior.

**Commit includes:**

- migration adding nullable `folder.slug` (notebook-local full path; basename is derivable from the suffix after the last `/`)
- migration adding nullable `note.slug` (notebook-local full path; see main plan)
- entity mappings for those fields

**Verification:**

- targeted backend repository/entity tests, or existing backend tests covering folder and note persistence

### 2.3 Generate Slugs for Newly Created Data

**Type:** Behavior

New folders and notes created after this commit should already have slug/path data, even before old data is migrated.

**Commit includes:**

- controller-level or high-level service tests through existing creation paths
- folder creation assigns **`folder.slug`** from the parent folder slug and title-derived basename (or basename only at root)
- note creation assigns **`note.slug`** from **`folder.slug`** and title-derived basename (or basename only at root)
- no user-facing route changes yet

**Verification:**

- targeted backend tests for folder and note creation paths

### 2.4 Temporary Migration Status Endpoint

**Type:** Behavior

Admins can inspect whether slug migration is complete before running it.

**Commit includes:**

- temporary admin-only status endpoint, for example under `/api/admin/wiki-slug-migration`
- response includes counts for missing **`folder.slug`** and **`note.slug`**
- access-control test proving non-admin users cannot call it
- generated TypeScript client update

**Verification:**

- targeted backend controller tests
- `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`

### 2.5 Admin Dashboard Tab Shows Migration Status

**Type:** Behavior

Admins can open a dashboard tab and see current slug migration status.

**Commit includes:**

- a new admin dashboard tab named by the capability, such as `Slug Migration`
- a small admin component that loads and displays status counts
- component test using `mockSdkService()`
- optional `@wip` E2E scenario in an admin capability feature if the UI is not complete in this commit

**Verification:**

- targeted frontend component test
- relevant admin E2E spec if a scenario is added

### 2.6 Batch-Migrate Folder Slugs

**Type:** Behavior

The temporary migration endpoint can fill missing **`folder.slug`** values: one string per folder, **`basename`** at root or **`parentFolder.slug + "/" + basename`** when nested.

**Commit includes:**

- backend batch operation for folders
- basename derives from `folder.name`; collision handling is scoped to sibling folders under the same parent
- nested folder slugs are built from ancestor folder slugs
- endpoint either skips or reports folders whose ancestors are not ready, without corrupting data
- endpoint status reflects progress

**Verification:**

- targeted backend tests with root folders, nested folders, and sibling collisions

### 2.7 Batch-Migrate Note Slug (Full Path)

**Type:** Behavior

The temporary migration endpoint can fill missing **`note.slug`** values: one string per note, **`basename`** at root or **`folder.slug + "/" + basename`** when foldered. This replaces separate “file slug then full path” passes.

**Commit includes:**

- backend batch operation for notes
- basename derives from note title or the existing filename-equivalent field; collision handling within the same folder
- tests cover notes with the same title in different folders and duplicate titles in the same folder
- tests cover root notes, foldered notes, and nested folder notes

**Verification:**

- targeted backend tests for note slug migration

### 2.8 Dashboard Button Runs Migration Batches

**Type:** Behavior

Admins can trigger the migration from the dashboard without holding one long production request.

**Commit includes:**

- button on the `Slug Migration` admin tab
- button calls the batch endpoint and refreshes progress
- frontend keeps calling batches until complete or until the admin stops/navigates away
- UI shows running, processed, remaining, completed, and error states
- component test for successful progress and error display

**Verification:**

- targeted frontend component test
- relevant admin E2E spec with a small test dataset

### 2.9 Migration Resume and Production Safety

**Type:** Behavior

An admin can safely resume migration after a timeout, page reload, or partial production run.

**Commit includes:**

- E2E or controller-level test proving repeated button presses continue from current database state
- status remains correct after partial batches
- no batch depends on client-side memory for correctness
- dashboard copy says the migration can take time and can be resumed

**Verification:**

- targeted backend tests plus the admin migration E2E spec

## Production Migration Checkpoint

This is an operational checkpoint, not a code commit.

After sub-phase 2.9 is deployed:

1. An admin opens the admin data migration dashboard tab (during backfill it may still be labeled for slug work until sub-phase 2.14 renames it).
2. The admin starts the migration.
3. The UI runs bounded batches until all remaining counts are zero.
4. If the browser, request, or deployment interrupts the run, the admin reopens the tab and continues.
5. Capture the final status showing zero remaining **`folder.slug`** and **`note.slug`** rows.

Do not proceed to schema constraints or endpoint removal until production status is complete.

### 2.10 Enforce Slug Invariants

**Type:** Structure

Once production data is migrated, enforce the Phase 2 persistence invariants.

**Commit includes:**

- migration adding uniqueness constraints:
  - `unique(notebook_id, slug)` on folders (slug is the full notebook-local path)
  - `unique(notebook_id, slug)` on notes (slug is the full notebook-local path)
- not-null constraints for slug/path fields that must now always exist
- tests proving duplicate basenames in the same folder or duplicate **`note.slug`** fail through observable creation/move paths

**Verification:**

- targeted backend tests and migrations locally

### 2.11 Resolve Notes by Ambiguous-Friendly Basename

**Type:** Behavior

Users can open the temporary/convenience route for an unambiguous accessible note when matching the **basename** (local segment of **`note.slug`**).

**Commit includes (backend — done):**

- backend lookup by basename among notes visible to the current user
- ambiguous basename returns a user-visible not-found or ambiguity error

Frontend router/page consolidation and E2E updates for this capability are **2.11.1** and **2.11.2**.

**Verification:**

- targeted backend controller tests

### 2.11.1 Single note show route and resolved note id on NoteShowPage

**Type:** Behavior

Opening a note by internal id and opening a note by ambiguous basename use the **same** note-show router configuration and the **same** `frontend/src/pages/NoteShowPage.vue`. The page waits until the note id is resolved from **`noteRealm`** (and related loading/error state) before passing a stable **`resolvedNoteId`** into **`NoteShow`**. **`resolvedNoteId`** is a **computed** value derived from realm state (not ad-hoc imperative timing).

**Commit includes:**

- one shared note-show route/path pattern for both id-based and basename-based entry (no duplicate note-show page components for the two cases)
- `NoteShowPage` passes **`resolvedNoteId`** to **`NoteShow`** only once resolution is known; avoid flashing or wiring **`NoteShow`** with an unresolved id
- high-level frontend tests (mounted page or router-level) that cover resolution and hand-off to **`NoteShow`**

**Verification:**

- targeted frontend tests only (no new E2E in this sub-phase)

### 2.11.2 E2E sluggify and basename-first “jump to note”

**Type:** Behavior

E2E builds basename/slug expectations with the same slug rules the product uses, and navigation tests prefer the ambiguous note-show path instead of notebook list caches.

**Commit includes:**

- introduce **sluggify** (or an equivalent single helper aligned with backend slug rules) in the E2E layer for constructing basename/path expectations in steps and URLs
- replace “jump to note” flows that looked up an internal id from a cached list with navigation to the ambiguous note-show page (basename route) where that matches the scenario intent

**Verification:**

- relevant Cypress spec(s) for the touched flows

### 2.12 Resolve Notes by Notebook ID and Note Slug Path

**Type:** Behavior

Users can open a note by notebook ID plus **`note.slug`** path.

**Commit includes:**

- backend lookup by notebook ID and **`note.slug`**
- frontend route for notebook ID plus note path (implementation may encode path segments)
- E2E scenario covering a nested folder note
- existing ID-based internal loading may remain behind the route resolution

**Verification:**

- targeted backend controller tests
- relevant Cypress spec only

### 2.13 Move Recomputes Note Slug and Rejects Collisions

**Type:** Behavior

Moving a note keeps **`note.slug`** consistent with the target folder.

**Commit includes:**

- move path validates `unique(notebook_id, newSlug)` after recomputing **`note.slug`**
- successful move updates **`note.folderId`** and **`note.slug`**
- moving a note with descendants or related folder-derived state preserves current Phase 1 behavior
- E2E or controller-level test covers a visible move conflict

**Verification:**

- targeted backend move tests
- relevant Cypress spec if move behavior is already covered there

### 2.14 Generalize the Admin Data Migration Dashboard

**Type:** Structure

Keep the admin dashboard **data migration skeleton**: a tab (or equivalent surface) where an admin can click a button to trigger a DB migration and see progress. Remove **slug migration–specific** UI: labels, counts, polling logic, and types that only served folder/note slug backfill. The shell should read as a reusable **database / data migration** control, not a one-off slug tool.

**Commit includes:**

- rename the tab and user-visible copy to a generalized capability (for example **Data migration** or **Database migration** — pick one name and use it consistently)
- keep layout affordances: trigger control, progress or status area (may show idle, generic, or empty states once the backend stub has no work)
- remove slug-only display fields, SDK calls, and props tied to slug status/batch DTOs
- update component tests to cover the shell (loads for admin, button/progress wiring) without slug migration fixtures
- trim or rewrite E2E steps that only asserted slug backfill; keep minimal admin coverage if it still adds value

**Verification:**

- targeted admin dashboard component tests
- relevant admin E2E spec if touched

### 2.15 Admin Data Migration Endpoint Stub (Auth Only)

**Type:** Structure

Remove slug migration **implementation** from the backend: batch processing, status counts, and **slug-specific DTOs** or services that exist only for that backfill. **Keep** an admin-only migration HTTP surface (path may be renamed to match the generalized dashboard purpose) whose handler is **empty except the admin authorization check** (for example returns 200 with no body or a minimal placeholder — no business logic).

**Commit includes:**

- delete slug-migration-only controllers/handlers, DTOs, and unused services or repository helpers
- add or narrow to a single admin-gated route used by the dashboard button; implementation = auth check only
- regenerate the TypeScript API client
- adjust frontend to call the stub as needed for the button; remove dead mocks/helpers for removed slug endpoints

**Verification:**

- targeted backend tests (prove non-admin cannot call the route; admin receives success with no slug-specific contract)
- `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`
- targeted frontend tests if generated client usage changed

### 2.16 Remove Migration-Only Fallbacks

**Type:** Structure

Clean up code that only existed while slug/path fields could be missing.

**Commit includes:**

- remove null-handling branches for slug data that schema constraints now guarantee
- remove any **remaining** status/count or migration-only repository helpers not already deleted in 2.15
- keep slug generation for new data and **`note.slug`** recomputation for moves

**Verification:**

- targeted backend tests for creation, lookup, and move behavior

## Completion Criteria

Phase 2 is complete when:

- all permanent slug behavior is covered by targeted backend, frontend, and E2E tests
- production data has **`folder.slug`** and **`note.slug`** fields populated as required
- database constraints enforce the intended uniqueness rules
- slug-based note routes work
- note moves maintain correct **`note.slug`**
- no slug-specific migration DTOs, batch/status endpoints, or migration-only fallback remains; a **generalized** admin data-migration UI shell and an **auth-only** admin migration endpoint stub may remain for future use
