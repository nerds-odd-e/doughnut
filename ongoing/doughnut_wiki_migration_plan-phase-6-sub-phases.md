# Doughnut Wiki Migration Plan - Phase 6 Sub-Phases

## Parent Phase

Phase 6 of `ongoing/doughnut_wiki_migration_plan.md`: Folder-First Child Listing; Remove Note Short Details.

## Goal

Make structural note listing folder-first while `Note.parent` still exists for compatibility, remove `NoteTopology.shortDetails` from topology/card surfaces, and make note graph sibling retrieval use folder membership instead of parent-note children.

By the end of Phase 6:

- main listing/navigation APIs treat notes as members of a folder, or notebook root when `folderId` is absent
- note cards and topology DTOs expose title and slug, not a derived `shortDetails` preview
- note graph sibling relationships come from the note's folder; when a note has no folder, siblings come from the notebook root
- existing parent fields may still exist until Phase 7, but they are no longer the primary source for folder-first listing or note graph siblings

## Design Decisions

- **Folder sibling source:** a note's structural siblings are notes in the same `folderId`. If `folderId` is null, structural siblings are root notes in the same notebook with null `folderId`.
- **Graph sibling order:** preserve the existing sibling-order behavior as much as possible while changing the source collection. The sibling order concept remains a migration-era ordering mechanism and is not removed in Phase 6.
- **Short details removal:** remove `shortDetails` from `NoteTopology`, OpenAPI/generated TypeScript, fixtures, and card surfaces. Notebook index previews may derive directly from the loaded note details when they need a summary line.
- **Naming:** keep permanent artifacts capability-named. Do not create tests, files, classes, or routes named after Phase 6.

## Sizing Rule

Each sub-phase below is planned as a five-minute commit. If implementation discovers a sub-phase cannot be finished, tested, and committed inside that timebox, stop and split it before continuing.

## Status

All sub-phases are **planned** and not started. Each sub-phase below is intended to be independently testable and committable; do not batch adjacent sub-phases into one commit unless the plan is first updated to explain why the split no longer makes sense.

## Sub-Phase 6.1 - Root Listing Uses Notebook Root Folder Scope

**Type:** Behavior.

**Pre-condition:** A notebook contains notes whose legacy `parent` and persisted `folderId` do not imply the same root membership.

**Trigger:** An API client loads the notebook root note listing.

**Post-condition:** The root listing returns notes in the notebook root folder scope (`folderId` is null), not notes chosen by legacy `parent is null`.

**Work:** Add or adjust a controller/service test around `GET /api/notebooks/{notebook}/root-notes`, then change the repository/service query to use notebook root folder scope.

**Verify:** Focused backend controller/service test for notebook root listing.

**Commit boundary:** One root-listing behavior commit.

## Sub-Phase 6.2 - Folder Listing API Returns Notes In A Folder

**Type:** Behavior.

**Pre-condition:** A notebook has a folder with notes assigned through `note.folderId`.

**Trigger:** An API client asks what notes live in that folder.

**Post-condition:** The response returns the non-deleted notes in that folder, using the same authorization and topology shape as other note listings.

**Work:** Add the smallest folder-scoped note listing endpoint or service path that matches existing controller patterns. Prefer reusing `NoteRealmService` / `NoteTopology` construction instead of introducing a second card DTO.

**Verify:** Focused backend controller/service test for folder-scoped listing.

**Commit boundary:** One folder-listing behavior commit.

## Sub-Phase 6.3 - Testability Note Injection Supports Folder Placement

**Type:** Behavior.

**Pre-condition:** E2E scenarios inject notebook notes using a table with `Title` and `Parent Title`, while folder data may need to differ from the legacy parent-note tree.

**Trigger:** A scenario uses `And I have a notebook "..." with a note "..." and notes:` and provides a `Folder` column.

**Post-condition:** The testability endpoint treats `Parent Title` only as the legacy parent-id relationship and does not create or infer a folder from it. When `Folder` is provided, the injected note is placed in that folder, creating nested folder paths such as `LeSS in Action/TDD` as needed. Existing scenarios without `Folder` keep working as before.

**Example setup shape:**

```gherkin
And I have a notebook "LeSS training" with a note "LeSS in Action" and notes:
  | Title | Parent Title   | Folder              |
  | TDD   | LeSS in Action | LeSS in Action      |
  | ATDD  | LeSS in Action | LeSS in Action      |
  | CI    | LeSS in Action | LeSS in Action      |
  | TPP   | TDD            | LeSS in Action/TDD  |
  | Const | TPP            | LeSS in Action/TPP  |
  | Pull  | ATDD           | LeSS in Action/ATDD |
```

**Work:** Extend the testability inject-note DTO/step mapping to accept optional `Folder`, resolve or create each folder path within the notebook, and assign the injected note's `folderId` from that path. Do not let `Parent Title` create folders implicitly.

**Verify:** Run the existing E2E feature(s) that inject notes through this step, starting with `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_topology/note_tree_view.feature`, and keep the existing no-`Folder` scenarios passing.

**Commit boundary:** One testability-folder-placement commit.

## Sub-Phase 6.4 - Sidebar Uses Folder-Scoped Listings For Structural Peers

**Type:** Behavior.

**Pre-condition:** Root and folder-scoped listing APIs exist, and E2E setup can place notes in explicit folder paths independently from legacy `Parent Title`.

**Trigger:** A user opens a notebook root, a folder, or a note whose peers are in the same folder.

**Post-condition:** The visible structural peers come from notebook root or folder membership rather than parent-note children. The frontend no longer asks a note for its children for sidebar listing; only notebook root and folders have children in the sidebar.

**Work:** Extend the existing sidebar/tree feature or page-object tests to assert folder-scoped peers using the new `Folder` setup column, then wire the frontend listing path to the root/folder APIs. Keep lazy loading behavior intact for root and folder nodes, and remove note-child sidebar loading.

**Verify:** Targeted frontend test and the relevant Cypress spec, likely `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_topology/note_tree_view.feature`.

**Commit boundary:** One folder-first navigation commit.

**Temporary presentation note:** During this sub-phase, a folder returned by structural listing may be merged into the note row with the same title/slug at the same level. This keeps migrated former parent-note trees visually stable while the sidebar data source changes from note children to folder listings.

## Sub-Phase 6.4.1 - Sidebar Renders Folders And Notes As Separate Items

**Type:** Behavior.

**Pre-condition:** Sidebar structural branches are loaded from notebook root and folder-scoped listing APIs, and the temporary 6.4 presentation merges matching folder/note rows.

**Trigger:** A user opens a notebook root or folder that contains both folders and notes.

**Post-condition:** Folders and notes are rendered as separate sidebar items. Folder items can only open and collapse to reveal the notes and child folders inside them. Note items are the only items that navigate to notes. Path navigation clicks folder items for every path segment except the last segment, then clicks the final note item.

**Work:** Replace the temporary matching-folder merge with explicit folder rows in the sidebar row model and components. Update the E2E navigation helper so `navigateToNoteFromPath` expands/clicks folder items for intermediate path segments and clicks the final note item.

**Verify:** Targeted frontend sidebar test plus `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_topology/note_tree_view.feature`.

**Commit boundary:** One separate-folder-sidebar commit.

## Sub-Phase 6.5 - New Notes Appear In The Current Folder Scope

**Type:** Behavior.

**Pre-condition:** A user is browsing a folder-backed structural location.

**Trigger:** The user creates a note from that location.

**Post-condition:** The new note appears among notes in the current folder scope. Root creation creates a root note with no folder.

**Work:** Extend note creation tests for the folder-first surface and adjust creation targets to use folder scope where product semantics previously relied on structural parent note context.

**Verify:** Focused backend creation test plus targeted Cypress for note creation, likely `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_creation_and_update/note_creation.feature`.

**Commit boundary:** One creation-in-folder-scope commit.

## Sub-Phase 6.6 - Remove Short Details From Backend Topology

**Type:** Behavior / API cleanup.

**Pre-condition:** Card/listing consumers no longer require `NoteTopology.shortDetails` as a contract.

**Trigger:** Backend code builds a `NoteTopology`.

**Post-condition:** `NoteTopology` contains no `shortDetails` field, and backend tests no longer assert derived topology previews.

**Work:** Remove `shortDetails` from `NoteTopology`, remove `Note.getShortDetails()` if no longer used, update backend tests, and regenerate the TypeScript API client.

**Verify:** Focused backend tests plus `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`.

**Commit boundary:** One backend/API topology cleanup commit.

## Sub-Phase 6.7 - Remove Short Details From Frontend Cards And Fixtures

**Type:** Behavior / UI cleanup.

**Pre-condition:** Generated TypeScript no longer exposes `NoteTopology.shortDetails`.

**Trigger:** Users view note cards, title components, notebook index summaries, or tests using generated fixtures.

**Post-condition:** Card and fixture code compiles without `shortDetails`; note cards show title/slug-based topology only. Notebook index summary derives from loaded details when needed.

**Work:** Remove `shortDetails` references from frontend components, tests, and `doughnut-test-fixtures` builders. Keep any remaining summary behavior explicitly local to the surface that needs it.

**Verify:** Targeted frontend tests for affected components.

**Commit boundary:** One frontend/fixture cleanup commit.

## Sub-Phase 6.8 - Note Graph Siblings Come From Folder Scope

**Type:** Behavior.

**Pre-condition:** A note graph request includes a focus note that has folder siblings, or root siblings when it has no folder.

**Trigger:** The graph retrieves older/younger sibling relationships or sibling-derived relationship handlers.

**Post-condition:** Siblings are resolved from the note's folder; for a note without a folder, siblings are resolved from the notebook root. The graph no longer asks the parent note for children to determine siblings.

**Work:** Add focused graph retrieval tests that distinguish legacy parent siblings from folder siblings and separately prove that a no-folder note gets siblings from the notebook root. Then move sibling lookup behind a cohesive folder-scope query/service used by graph sibling handlers.

**Verify:** Focused graph retrieval tests, likely `GraphRAGServiceTest` or the existing graph relationship tests touched by sibling handlers.

**Commit boundary:** One graph-sibling-source commit.

## Sub-Phase 6.9 - Phase 6 Closeout And Plan Update

**Type:** Structure / cleanup.

**Pre-condition:** Testability folder placement, folder-first listing, creation in folder scope, `shortDetails` removal, and graph folder-scope siblings are passing targeted tests.

**Trigger:** Final targeted verification for Phase 6.

**Post-condition:** Phase 6 docs reflect implementation status, generated clients are current, and no obsolete `shortDetails` or parent-child listing notes remain in active plan text.

**Work:**

- Update `ongoing/doughnut_wiki_migration_plan.md` with Phase 6 status and any discoveries.
- Update related graph docs if implementation changes the exact sibling ordering or query shape.
- Remove any temporary `@wip` tags introduced while driving Phase 6 behavior.
- Run the targeted backend/frontend/E2E checks touched in this phase.

**Commit boundary:** One cleanup/docs commit that leaves Phase 6 closed and ready for Phase 7.
