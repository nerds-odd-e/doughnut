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

## Sub-Phase 6.3 - Sidebar Uses Folder-Scoped Listings For Structural Peers

**Type:** Behavior.

**Pre-condition:** Root and folder-scoped listing APIs exist.

**Trigger:** A user opens a notebook root, a folder, or a note whose peers are in the same folder.

**Post-condition:** The visible structural peers come from notebook root or folder membership rather than parent-note children.

**Work:** Extend the existing sidebar/tree feature or page-object tests to assert folder-scoped peers, then wire the frontend listing path to the root/folder APIs. Keep lazy loading behavior intact where possible.

**Verify:** Targeted frontend test and the relevant Cypress spec, likely `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_topology/note_tree_view.feature`.

**Commit boundary:** One folder-first navigation commit.

## Sub-Phase 6.4 - New Notes Appear In The Current Folder Scope

**Type:** Behavior.

**Pre-condition:** A user is browsing a folder-backed structural location.

**Trigger:** The user creates a note from that location.

**Post-condition:** The new note appears among notes in the current folder scope. Root creation creates a root note with no folder.

**Work:** Extend note creation tests for the folder-first surface and adjust creation targets to use folder scope where product semantics previously relied on structural parent note context.

**Verify:** Focused backend creation test plus targeted Cypress for note creation, likely `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_creation_and_update/note_creation.feature`.

**Commit boundary:** One creation-in-folder-scope commit.

## Sub-Phase 6.5 - Remove Short Details From Backend Topology

**Type:** Behavior / API cleanup.

**Pre-condition:** Card/listing consumers no longer require `NoteTopology.shortDetails` as a contract.

**Trigger:** Backend code builds a `NoteTopology`.

**Post-condition:** `NoteTopology` contains no `shortDetails` field, and backend tests no longer assert derived topology previews.

**Work:** Remove `shortDetails` from `NoteTopology`, remove `Note.getShortDetails()` if no longer used, update backend tests, and regenerate the TypeScript API client.

**Verify:** Focused backend tests plus `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`.

**Commit boundary:** One backend/API topology cleanup commit.

## Sub-Phase 6.6 - Remove Short Details From Frontend Cards And Fixtures

**Type:** Behavior / UI cleanup.

**Pre-condition:** Generated TypeScript no longer exposes `NoteTopology.shortDetails`.

**Trigger:** Users view note cards, title components, notebook index summaries, or tests using generated fixtures.

**Post-condition:** Card and fixture code compiles without `shortDetails`; note cards show title/slug-based topology only. Notebook index summary derives from loaded details when needed.

**Work:** Remove `shortDetails` references from frontend components, tests, and `doughnut-test-fixtures` builders. Keep any remaining summary behavior explicitly local to the surface that needs it.

**Verify:** Targeted frontend tests for affected components.

**Commit boundary:** One frontend/fixture cleanup commit.

## Sub-Phase 6.7 - Note Graph Siblings Come From Folder Scope

**Type:** Behavior.

**Pre-condition:** A note graph request includes a focus note that has folder siblings, or root siblings when it has no folder.

**Trigger:** The graph retrieves older/younger sibling relationships or sibling-derived relationship handlers.

**Post-condition:** Siblings are resolved from the note's folder; for a note without a folder, siblings are resolved from the notebook root. The graph no longer asks the parent note for children to determine siblings.

**Work:** Add focused graph retrieval tests that distinguish legacy parent siblings from folder/notebook-root siblings, then move sibling lookup behind a cohesive folder-scope query/service used by graph sibling handlers.

**Verify:** Focused graph retrieval tests, likely `GraphRAGServiceTest` or the existing graph relationship tests touched by sibling handlers.

**Commit boundary:** One graph-sibling-source commit.

## Sub-Phase 6.8 - Phase 6 Closeout And Plan Update

**Type:** Structure / cleanup.

**Pre-condition:** Folder-first listing, creation in folder scope, `shortDetails` removal, and graph folder-scope siblings are passing targeted tests.

**Trigger:** Final targeted verification for Phase 6.

**Post-condition:** Phase 6 docs reflect implementation status, generated clients are current, and no obsolete `shortDetails` or parent-child listing notes remain in active plan text.

**Work:**

- Update `ongoing/doughnut_wiki_migration_plan.md` with Phase 6 status and any discoveries.
- Update related graph docs if implementation changes the exact sibling ordering or query shape.
- Remove any temporary `@wip` tags introduced while driving Phase 6 behavior.
- Run the targeted backend/frontend/E2E checks touched in this phase.

**Commit boundary:** One cleanup/docs commit that leaves Phase 6 closed and ready for Phase 7.
