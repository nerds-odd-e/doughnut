# Doughnut Wiki Migration Plan - Phase 1 Sub-Phases

## Purpose

This document decomposes `Phase 1 - Introduce Folder` from `ongoing/doughnut_wiki_migration_plan.md` into small, closed sub-phases.

Each sub-phase should be small enough to implement in about five minutes of focused coding and land as one focused commit. Required verification may take longer, but the code change itself should stay tiny. Each commit must leave the application compiling, tests passing, and the existing user-visible behavior unchanged unless the sub-phase explicitly says otherwise.

Phase 1 is a structure phase for the immediate next behavior: folder-scoped file slugs and full paths in Phase 2. The goal here is not to expose the final wiki model, but to make `Folder` a first-class persistence and domain concept while legacy note parenthood continues to work.

## Phase 1 Scope

Introduce:

```text
Folder
  id
  notebookId
  parentFolderId optional
  name
  createdAt
  updatedAt
```

Connect notes to folders:

```text
Note
  folderId optional
```

Preserve the existing observable behavior:

```text
parent note still drives current navigation, ordering, creation, and movement
folder is introduced as parallel containment data
```

## Non-Goals

- Do not introduce folder slugs or full paths.
- Do not change frontend routes.
- Do not remove or relax `Note.parent`.
- Do not convert relationship notes.
- Do not expose Obsidian import/export behavior.
- Do not rename permanent classes, tests, migrations, or feature files with phase wording.

## Design Decisions

- `Folder` belongs to one `Notebook`.
- `Folder.parentFolder` represents folder nesting, not semantic note parenthood.
- `Note.folder` is nullable during Phase 1 so every commit can be deployed safely while the backfill is introduced.
- Existing `Note.parent` remains the source of truth for current behavior until a later phase replaces navigation.
- For **existing data**, each backfilled folder’s `**name`** is the same as the **title** of the parent note that defined that child container (one string: the parent note’s title).
- `**folder.slug`** is not introduced in Phase 1. When folder slugs are added (`ongoing/doughnut_wiki_architecture_north_star.md`), they are **derived from `folder.name`** via the standard slugifier; collision handling and routes remain Phase 2+.

## Discovered Current State

- There is no existing backend `Folder` entity.
- `Note` currently has `notebook`, `parent`, `children`, and `siblingOrder`.
- `Notebook` still owns a `headNote`.
- `NoteConstructionService` creates notes under a parent note.
- `NoteMotionService` moves notes by changing parent and sibling order.
- Migrations currently use the `V300000xxx` series; new migrations must use a version greater than the current latest migration.

## Sub-Phases

### 1.1 Add Folder Persistence

Status: planned

Type: structure

Goal: add the durable table that can hold folders without changing any note behavior.

Implementation:

- Add a migration that creates `folder` with `id`, `notebook_id`, `parent_folder_id`, `name`, `created_at`, and `updated_at`.
- Add foreign keys to `notebook(id)` and `folder(id)`.
- Keep the table independent from `note` for this commit.

Tests:

- Run backend tests with migrations enabled.

Verification:

```bash
CURSOR_DEV=true nix develop -c pnpm backend:verify
```

Commit boundary:

- Database can migrate forward.
- No Java model is required yet.
- No observable behavior changes.

### 1.2 Add Folder Domain Model

Status: planned

Type: structure

Goal: make folders available to backend code through the domain model.

Implementation:

- Add `Folder` entity under the backend entity package.
- Add `FolderRepository`.
- Map `Folder.notebook` and optional `Folder.parentFolder`.
- Add the smallest test-data builder support needed to create a folder in tests.

Tests:

- Add a backend persistence test that creates a notebook folder and reloads it through the repository.
- Keep the test capability-named, such as folder persistence or notebook folder containment, not phase-named.

Verification:

```bash
CURSOR_DEV=true nix develop -c pnpm backend:verify
```

Commit boundary:

- `Folder` can be persisted and read.
- No note is assigned to a folder yet.
- Existing note tree behavior is unchanged.

### 1.3 Attach Notes to Folders

Status: planned

Type: structure

Goal: allow a note to reference a folder while keeping that reference optional.

Implementation:

- Add a migration that adds nullable `folder_id` to `note`.
- Add the foreign key from `note(folder_id)` to `folder(id)`.
- Add `Note.folder` mapping with getter and package/domain-level setter consistent with existing entity style.
- Extend test builders only as needed to create a note in a folder.

Tests:

- Add a backend persistence test that saves a note assigned to a folder and reloads the note with the folder reference.
- Assert that existing note creation without a folder still works.

Verification:

```bash
CURSOR_DEV=true nix develop -c pnpm backend:verify
```

Commit boundary:

- Notes may have folders.
- Existing notes may still have no folder.
- No production path depends on folder yet.

### 1.4 Backfill Parent-Derived Folder Records

Status: planned

Type: structure

Goal: create folder rows for existing parent notes that currently contain child notes.

Implementation:

- Add a migration that creates one folder per parent note that currently contains child notes.
- Set each new folder’s `name` to that parent note’s **title** (same value as `note.title` for that parent).
- Do not update `note.folder_id` yet.
- Do not add `slug`; when slugs exist, they will come from `name`.

Tests:

- Add or extend a backend persistence test that verifies a folder can represent an existing parent note's child container.
- Keep the assertion about externally stable behavior: parent and child notes still exist unchanged.

Verification:

```bash
CURSOR_DEV=true nix develop -c pnpm backend:verify
```

Commit boundary:

- Existing parent notes with children have matching folder rows.
- The current parent tree remains intact.
- Notes are not assigned to folders yet.
- No UI or route behavior changes.

### 1.5 Link Derived Folders into a Folder Tree

Status: planned

Type: structure

Goal: make parent-derived folders reflect the current nesting shape.

Implementation:

- Add a migration or small domain routine that sets each derived folder's `parent_folder_id` from the parent note's own derived folder when available.
- Leave top-level derived folders without a parent folder.
- Do not update `note.folder_id` yet.

Tests:

- Add or extend a backend persistence test for nested parent notes.
- Assert child-container folders can be nested under their parent-container folders.
- Assert note parent relationships remain unchanged.

Verification:

```bash
CURSOR_DEV=true nix develop -c pnpm backend:verify
```

Commit boundary:

- Folder nesting mirrors existing parent-note nesting.
- Notes are still not assigned to folders.
- No observable behavior changes.

### 1.6 Assign Existing Child Notes to Derived Folders

Status: planned

Type: structure

Goal: populate `note.folder_id` from existing parent containment.

Implementation:

- Add a migration that sets each child note's `folder_id` to the folder derived from its current parent note.
- Leave notes without a parent-derived folder untouched.
- Preserve `note.parent_id` and `note.sibling_order`.

Tests:

- Add a backend migration or repository-level test that prepares a parent note with children and verifies the child notes receive the derived folder.
- Assert parent and sibling order are still intact.

Verification:

```bash
CURSOR_DEV=true nix develop -c pnpm backend:verify
```

Commit boundary:

- Existing child notes have folder assignments.
- The current parent tree remains intact.
- No UI or route behavior changes.

### 1.7 Maintain Folder Assignment on Note Creation

Status: planned

Type: structure

Goal: keep new notes aligned with folder data after the backfill.

Implementation:

- When a note is created under a parent note, assign it to the folder that represents that parent note's children.
- If that folder does not exist, create or find it as part of the same note-creation transaction; the folder `**name**` must match the **parent note’s title** (same rule as backfill).
- Keep the note parent assignment unchanged.

Tests:

- Add or extend a controller/service-level backend test for creating a child note.
- Assert the existing observable result still works.
- Assert the persisted note has the expected folder.

Verification:

```bash
CURSOR_DEV=true nix develop -c pnpm backend:verify
```

Commit boundary:

- New child notes receive folder data.
- Existing child-note creation behavior is unchanged from the user's perspective.

### 1.8 Maintain Folder Assignment on Note Movement

Status: planned

Type: structure

Goal: keep folder data aligned when a note moves within or across notebooks.

Implementation:

- Update note movement so the moved note receives the folder corresponding to its new parent note.
- When a moved subtree changes notebook, update descendant folder assignments only where they are derived from the moved containment path.
- Preserve current sibling ordering behavior.

Tests:

- Extend movement tests around moving a note under a different parent.
- Assert the note's folder follows the new parent-derived folder.
- Keep existing assertions for parent, notebook, descendants, and sibling order.

Verification:

```bash
CURSOR_DEV=true nix develop -c pnpm backend:verify
```

Commit boundary:

- Folder data remains current after moves.
- Parent-based behavior and ordering remain unchanged.



### 1.10 Update Phase 1 Plan State

Status: planned

Type: structure

Goal: keep the migration documents accurate after Phase 1 implementation is complete.

Implementation:

- Update `ongoing/doughnut_wiki_migration_plan.md` with Phase 1 discoveries.
- Update `ongoing/doughnut_wiki_architecture_north_star.md` only if implementation revealed a better domain rule.
- Remove obsolete notes from this sub-phase plan.

Tests:

- No code tests required if this commit only updates planning documents.

Verification:

```bash
CURSOR_DEV=true nix develop -c git diff --check
```

Commit boundary:

- Planning documents reflect the current state.
- No production code changes.

## Phase 1 Completion Criteria

Phase 1 is complete when:

- `Folder` exists as a first-class backend entity.
- Notes can be assigned to folders.
- Existing note trees have folder data derived from parent containment.
- New note creation and note movement keep folder data aligned.
- Existing parent-based behavior still passes its current tests.
- Folder identity is available through an existing backend surface for future UI work.
- No slug, full path, import/export, or parent-removal work has been started.

