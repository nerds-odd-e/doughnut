# Doughnut Wiki Migration Plan - Phase 5 Sub-Phases

## Parent Phase

Phase 5 of `ongoing/doughnut_wiki_migration_plan.md`: Convert Relationship Notes into Normal Notes.

## Goal

Convert relationship notes from special notes identified by `target_note_id` / `relation_type` / `parent_id` into ordinary notes whose title, details, folder, slug, and frontmatter carry the relationship meaning.

By the end of Phase 5:

- every relationship note has a non-empty title derived from its relationship and truncated to `Note.MAX_TITLE_LENGTH` (currently 150 characters)
- relationship note details include relationship frontmatter and readable Markdown content
- relationship notes remain visible in existing relationship UI flows while becoming portable Markdown notes
- no note title may be null or empty after the Phase 5 data migration and schema/API tightening

## Design Decisions

- **Relationship title:** derive the title from the source note title, relation label, and target note title. Keep it human-readable first; the slug is derived separately from the truncated title by the existing slug service.
- **Title length:** truncate the derived relationship title before persistence so it never exceeds `Note.MAX_TITLE_LENGTH`. Prefer truncating at the title boundary over adding IDs or hashes.
- **Source of truth during Phase 5:** write relationship frontmatter into note details while keeping legacy columns long enough for existing screens and until note-parent removal (**Phase 7**). Removing those columns belongs to **Phase 7** or later cleanup.
- **Title invariant:** Phase 5 is the last phase that may tolerate legacy null or empty note titles. After Phase 5, production code and schema should treat note title as required.

## Sizing Rule

Each sub-phase below is planned as a five-minute commit. If implementation discovers a sub-phase cannot be finished, tested, and committed inside that timebox, stop and split it before continuing.

## Sub-Phase 5.1 - Relationship Note Reachability Test

**Type:** Behavior regression.

**Pre-condition:** Existing relationship creation works.

**Trigger:** A user creates a relationship and opens the relationship card.

**Post-condition:** The relationship note page is reachable through the current UI.

**Work:** Add the smallest E2E/page-object assertion needed in `e2e_test/features/relationships/add_relationship.feature`.

**Verify:** `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/relationships/add_relationship.feature`.

**Commit boundary:** One passing E2E commit.

## Sub-Phase 5.2 - Relationship Title Formatter

**Type:** Structure for the next behavior.

**Pre-condition:** Relationship note creation has source, relation type, and target available.

**Trigger:** Source title, relation label, and target title are passed to a formatter.

**Post-condition:** The formatter returns a non-empty title truncated to `Note.MAX_TITLE_LENGTH`.

**Work:** Add a small formatter near relationship-note creation with focused unit tests for normal and over-limit titles.

**Verify:** Focused backend test for the formatter.

**Commit boundary:** One formatter + test commit.

## Sub-Phase 5.3 - New Relationship Notes Use Derived Titles

**Type:** Behavior.

**Pre-condition:** The relationship title formatter exists.

**Trigger:** A user creates a relationship.

**Post-condition:** The created relationship note has the derived non-empty title.

**Work:** Wire `NoteService.createRelationship` to set the title and update the relationship creation test from Sub-Phase 5.1 to assert it.

**Verify:** Focused backend test plus `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/relationships/add_relationship.feature`.

**Commit boundary:** One creation behavior commit.

## Sub-Phase 5.4 - Relationship Markdown Formatter

**Type:** Structure for the next behavior.

**Pre-condition:** Relationship source, relation type, target, and optional old details are available.

**Trigger:** Those values are passed to a formatter.

**Post-condition:** The formatter returns frontmatter plus readable Markdown body.

**Work:** Add focused tests and a formatter for `type`, `relation`, `source`, `target`, and preserved details.

**Verify:** Focused backend formatter test.

**Commit boundary:** One Markdown formatter commit.

## Sub-Phase 5.5 - New Relationship Notes Store Markdown Details

**Type:** Behavior.

**Pre-condition:** The relationship Markdown formatter exists.

**Trigger:** A user creates and opens a relationship note in a normal note editing surface.

**Post-condition:** Details contain relationship frontmatter and readable body.

**Work:** Wire relationship creation to set details and extend the E2E assertion through the existing note editing surface.

**Verify:** `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/relationships/add_relationship.feature`.

**Commit boundary:** One portable-content behavior commit.

## Sub-Phase 5.6 - Relation Type Edit Keeps Title Consistent

**Type:** Behavior.

**Pre-condition:** New relationship notes derive titles from relation type.

**Trigger:** A user changes a relationship type.

**Post-condition:** The relationship note title updates to match the new relation.

**Work:** Extend `relationship_edit_and_remove.feature` and update the relation edit path.

**Verify:** `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/relationships/relationship_edit_and_remove.feature`.

**Commit boundary:** One edit-title consistency commit.

## Sub-Phase 5.7 - Relation Type Edit Keeps Details Consistent

**Type:** Behavior.

**Pre-condition:** Relationship details contain frontmatter and body.

**Trigger:** A user changes a relationship type.

**Post-condition:** Frontmatter/body relation text updates without losing user details.

**Work:** Extend the same feature only for the content assertion and update the edit path.

**Verify:** `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/relationships/relationship_edit_and_remove.feature`.

**Commit boundary:** One edit-content consistency commit.

## Sub-Phase 5.8 - Migrate Old Relationship Titles

**Type:** Behavior.

**Pre-condition:** Existing rows may contain relationship notes with null titles.

**Trigger:** The admin data migration runs.

**Post-condition:** Old relationship notes have derived non-empty titles before slug regeneration.

**Work:** Add a focused migration test through `AdminDataMigrationService.run()`, then backfill relationship titles inside `AdminDataMigrationService` before `regenerateAllSlugPaths()`.

**Verify:** Focused backend migration test.

**Commit boundary:** One migration-title commit.

## Sub-Phase 5.9 - Migrate Old Relationship Details

**Type:** Behavior.

**Pre-condition:** Old relationship notes may contain legacy optional details.

**Trigger:** The admin data migration runs.

**Post-condition:** Old relationship notes have relationship frontmatter/body details.

**Work:** Add the smallest migration assertion for details through the public migration entry point, then reuse the Markdown formatter in `AdminDataMigrationService`.

**Verify:** Focused backend migration test.

**Commit boundary:** One migration-details commit.

## Sub-Phase 5.10 - Runtime Rejects Blank Note Titles

**Type:** Behavior.

**Pre-condition:** Relationship creation and migration no longer need title-less notes.

**Trigger:** A caller creates or updates a note with null or blank title.

**Post-condition:** Supported runtime entry points reject the request.

**Work:** Add/adjust controller-level tests and validation annotations for note creation/title update.

**Verify:** Focused controller tests; run note creation/update E2E only if user-facing flows are touched.

**Commit boundary:** One runtime validation commit.

## Sub-Phase 5.11 - Persisted Titles Cannot Be Null or Empty

**Type:** Behavior / persistence cleanup.

**Pre-condition:** Runtime paths reject blank titles and migration backfills old relationship notes.

**Trigger:** Database migrations are applied.

**Post-condition:** `note.title` is non-null and non-empty in persisted data.

**Work:** Add a migration that backfills remaining title-less rows, makes `note.title` non-null, and enforces non-empty titles with the project’s existing database style.

**Verify:** Focused backend persistence/migration tests.

**Commit boundary:** One database invariant commit.

## Sub-Phase 5.12 - Relationship Delete Compatibility

**Type:** Behavior regression.

**Pre-condition:** Relationship notes are normal titled Markdown notes.

**Trigger:** A user deletes a relationship.

**Post-condition:** Existing deletion semantics still hold.

**Work:** Run or minimally extend `relationship_edit_and_remove.feature`; adjust delete path only if the new representation broke it.

**Verify:** `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/relationships/relationship_edit_and_remove.feature`.

**Commit boundary:** One delete-compatibility commit.

## Sub-Phase 5.13 - Phase 5 Closeout and Plan Update

**Type:** Structure / cleanup.

**Pre-condition:** Relationship creation, migration, edit, delete, and title invariants are passing.

**Trigger:** Final targeted verification for Phase 5.

**Post-condition:** Phase 5 docs reflect implementation status and no temporary `@wip` scenarios, dead code, or obsolete notes remain.

**Work:**

- Remove any `@wip` tags introduced while driving Phase 5 behavior.
- Update `ongoing/doughnut_wiki_migration_plan.md` with Phase 5 status and any discoveries.
- Remove obsolete relationship-note-specific code only when no current behavior uses it; otherwise leave explicit cleanup for Phase 7 or later.
- Run the relationship specs touched in this phase with targeted `--spec` commands.

**Commit boundary:** One cleanup/docs commit that leaves Phase 5 closed and ready for Phase 6 (folder-first listing and removal of note **`shortDetails`**).
