# Doughnut Wiki Migration Plan - Phase 2 Sub-Phases

## Purpose

This document decomposes **Phase 2 - Introduce Slugs and Full Paths** from `ongoing/doughnut_wiki_migration_plan.md` into small, closed sub-phases.

Each sub-phase should be small enough to complete in about 5 minutes and commit independently. Do not start the next sub-phase until the current one is green, cleaned up, and commit-ready.

## Phase 2 Target

After Phase 2:

- notebooks have slugs
- folders have slugs and full paths
- notes have file slugs and full paths
- new data gets slugs as it is created
- existing production data can be migrated through a temporary admin-only flow
- notes can be resolved through slug/path routes
- moves validate folder-scoped slug uniqueness and recompute paths
- the temporary migration endpoint and dashboard control are removed after production data is migrated

## Key Decisions

- Use `com.github.slugify:slugify` as the single slugifier for notebooks, folders, and notes.
- Keep `fileSlug` as the local filename identity. Keep folder location in `fullPath`.
- Add schema fields as nullable first so the deployment is safe before production backfill.
- Make the data migration **idempotent and batched**. Production has about 30k notes, so no endpoint should try to migrate all rows in one long request or one long transaction.
- Expose a temporary admin-only migration capability:
  - status endpoint reports remaining work
  - batch endpoint processes a bounded chunk and returns progress
  - admin dashboard tab shows progress and has a button to start or continue the migration
  - frontend may repeatedly call the batch endpoint while the admin watches progress
- Add final uniqueness / not-null constraints only after production migration has completed.
- Remove the temporary dashboard tab, button, endpoint, and migration-only API client after production data has been migrated and constraints are in place.

## Status

All sub-phases are **planned**.

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
  - deterministic collision suffixing within a supplied sibling set

**Verification:**

- `CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests "*Slug*"`

### 2.2 Add Nullable Slug and Path Columns

**Type:** Structure

Add persistence fields without changing observable behavior.

**Commit includes:**

- migration adding nullable `notebook.slug`
- migration adding nullable `folder.slug` and `folder.full_path`
- migration adding nullable `note.file_slug` and `note.full_path`
- entity mappings for those fields

**Verification:**

- targeted backend repository/entity tests, or existing backend tests covering notebook, folder, and note persistence

### 2.3 Generate Slugs for Newly Created Data

**Type:** Behavior

New notebooks, folders, and notes created after this commit should already have slug/path data, even before old data is migrated.

**Commit includes:**

- controller-level or high-level service tests through existing creation paths
- notebook creation assigns `slug`
- folder creation assigns `slug` and `fullPath`
- note creation assigns `fileSlug` and `fullPath`
- no user-facing route changes yet

**Verification:**

- targeted backend tests for notebook, folder, and note creation paths

### 2.4 Temporary Migration Status Endpoint

**Type:** Behavior

Admins can inspect whether slug migration is complete before running it.

**Commit includes:**

- temporary admin-only status endpoint, for example under `/api/admin/wiki-slug-migration`
- response includes counts for missing notebook slugs, folder slugs, folder full paths, note file slugs, and note full paths
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

### 2.6 Batch-Migrate Notebook Slugs

**Type:** Behavior

The temporary migration endpoint can fill missing notebook slugs in a bounded, idempotent batch.

**Commit includes:**

- backend batch operation for notebooks
- endpoint returns processed count and remaining count
- tests prove running the batch twice is safe
- tests prove slug collisions are resolved deterministically

**Verification:**

- targeted backend controller/service tests

### 2.7 Batch-Migrate Folder Slugs

**Type:** Behavior

The temporary migration endpoint can fill missing folder slugs from folder names.

**Commit includes:**

- backend batch operation for folders
- folder slug derives from `folder.name`
- sibling folder collisions are resolved within `(notebook, parentFolder)`
- endpoint status reflects progress

**Verification:**

- targeted backend tests with nested folders and sibling collisions

### 2.8 Batch-Migrate Folder Full Paths

**Type:** Behavior

The temporary migration endpoint can fill missing folder full paths after folder slugs exist.

**Commit includes:**

- backend batch operation for folder full paths
- nested folder paths are built from ancestor folder slugs
- endpoint either skips or reports folders whose ancestors are not ready, without corrupting data
- tests cover root folders and nested folders

**Verification:**

- targeted backend tests for folder path migration

### 2.9 Batch-Migrate Note File Slugs

**Type:** Behavior

The temporary migration endpoint can fill missing note file slugs from note titles.

**Commit includes:**

- backend batch operation for notes
- note `fileSlug` derives from note title or the existing filename-equivalent field
- collisions are resolved within `(notebook, folder)`
- tests cover notes with the same title in different folders and duplicate titles in the same folder

**Verification:**

- targeted backend tests for note file slug migration

### 2.10 Batch-Migrate Note Full Paths

**Type:** Behavior

The temporary migration endpoint can fill missing note full paths after note file slugs and folder full paths exist.

**Commit includes:**

- backend batch operation for note full paths
- root notes use `fileSlug`
- foldered notes use `folder.fullPath + "/" + fileSlug`
- endpoint status reports remaining notes
- tests cover root notes, foldered notes, and nested folder notes

**Verification:**

- targeted backend tests for note path migration

### 2.11 Dashboard Button Runs Migration Batches

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

### 2.12 Migration Resume and Production Safety

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

After sub-phase 2.12 is deployed:

1. An admin opens the `Slug Migration` dashboard tab.
2. The admin starts the migration.
3. The UI runs bounded batches until all remaining counts are zero.
4. If the browser, request, or deployment interrupts the run, the admin reopens the tab and continues.
5. Capture the final status showing zero remaining notebook, folder, and note slug/path rows.

Do not proceed to schema constraints or endpoint removal until production status is complete.

### 2.13 Enforce Slug and Path Invariants

**Type:** Structure

Once production data is migrated, enforce the Phase 2 persistence invariants.

**Commit includes:**

- migration adding uniqueness constraints:
  - notebook slug uniqueness where applicable
  - `unique(notebook_id, parent_folder_id, folder_slug)`
  - `unique(notebook_id, folder_full_path)`
  - `unique(notebook_id, folder_id, file_slug)`
  - `unique(notebook_id, full_path)`
- not-null constraints for slug/path fields that must now always exist
- tests proving duplicate folder or note slugs in the same scope fail through observable creation/move paths

**Verification:**

- targeted backend tests and migrations locally

### 2.14 Resolve Notes by Unique File Slug

**Type:** Behavior

Users can open the temporary/convenience route for an unambiguous accessible note slug.

**Commit includes:**

- backend lookup by `fileSlug` among notes visible to the current user
- ambiguous slug returns a user-visible not-found or ambiguity error
- frontend `/notes/:fileSlug` route resolves through the backend and opens the note
- E2E scenario in a capability-named feature file

**Verification:**

- targeted backend controller tests
- targeted frontend route/component tests
- relevant Cypress spec only

### 2.15 Resolve Notes by Notebook and Full Path

**Type:** Behavior

Users can open a note by notebook slug plus folder-qualified note path.

**Commit includes:**

- backend lookup by notebook slug and note full path
- frontend route for notebook slug plus note full path
- E2E scenario covering a nested folder note
- existing ID-based internal loading may remain behind the route resolution

**Verification:**

- targeted backend controller tests
- relevant Cypress spec only

### 2.16 Move Recomputes Full Path and Rejects Slug Collisions

**Type:** Behavior

Moving a note keeps slug/path data consistent.

**Commit includes:**

- move path validates `unique(notebook_id, target_folder_id, file_slug)`
- successful move recomputes note `fullPath`
- moving a note with descendants or related folder-derived state preserves current Phase 1 behavior
- E2E or controller-level test covers a visible move conflict

**Verification:**

- targeted backend move tests
- relevant Cypress spec if move behavior is already covered there

### 2.17 Remove the Dashboard Migration Control

**Type:** Behavior

After production migration is complete, admins no longer see the temporary slug migration tool.

**Commit includes:**

- remove the `Slug Migration` admin tab and button
- remove frontend component tests and E2E steps that only exist for the temporary dashboard control
- keep user-facing slug routes and permanent slug behavior

**Verification:**

- targeted admin dashboard component tests
- relevant admin E2E spec if touched

### 2.18 Remove the Temporary Migration Endpoint

**Type:** Structure

Remove the backend API that was only needed for production backfill.

**Commit includes:**

- remove temporary migration controller endpoints
- remove migration-only DTOs/services that no permanent behavior uses
- regenerate the TypeScript API client
- remove frontend test helpers/mocks tied only to the temporary endpoint

**Verification:**

- targeted backend tests
- `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`
- targeted frontend tests if generated client usage changed

### 2.19 Remove Migration-Only Fallbacks

**Type:** Structure

Clean up code that only existed while slug/path fields could be missing.

**Commit includes:**

- remove null-handling branches for slug/path data that schema constraints now guarantee
- remove status/count repository methods used only by the temporary migration
- keep slug generation for new data and slug/path recomputation for moves

**Verification:**

- targeted backend tests for creation, lookup, and move behavior

## Completion Criteria

Phase 2 is complete when:

- all permanent slug/path behavior is covered by targeted backend, frontend, and E2E tests
- production data has all slug/path fields populated
- database constraints enforce the intended uniqueness rules
- slug-based note routes work
- note moves maintain `fullPath`
- no temporary migration dashboard tab, button, endpoint, DTO, generated client method, or migration-only fallback remains
