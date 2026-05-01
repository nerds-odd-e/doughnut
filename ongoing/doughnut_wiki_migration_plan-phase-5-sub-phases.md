# Doughnut Wiki Migration Plan - Phase 5 Sub-Phases

## Parent Phase

Phase 5 of `ongoing/doughnut_wiki_migration_plan.md`: Convert Relationship Notes into Normal Notes.

## Goal

Convert relationship notes from special notes identified by `target_note_id` / `relation_type` / `parent_id` into ordinary notes whose title, details, folder, slug, frontmatter, and cached wiki-title references carry the relationship meaning.

By the end of Phase 5:

- every relationship note has a non-empty title derived from its relationship and truncated to `Note.MAX_TITLE_LENGTH` (currently 150 characters)
- relationship note details include relationship frontmatter and readable Markdown content
- relationship notes remain visible in existing relationship UI flows while becoming portable Markdown notes
- wiki-title references are persisted in a cache derived from note details/frontmatter
- `NoteRealm.wikiTitles`, reference lists, and graph retrieval use the cached wiki-title references instead of legacy `target_note_id` / child relationship notes
- legacy non-relationship notes have a migration-only `parent: "[[...]]"` frontmatter property that preserves the old semantic parent as a wiki reference
- relationship `relation` and `target` data come from frontmatter/cache instead of `relation_type` / `target_note_id`
- no note title may be null or empty after the Phase 5 data migration and schema/API tightening

## Design Decisions

- **Relationship title:** derive the title from the source note title, relation label, and target note title. Keep it human-readable first; the slug is derived separately from the truncated title by the existing slug service.
- **Title length:** truncate the derived relationship title before persistence so it never exceeds `Note.MAX_TITLE_LENGTH`. Prefer truncating at the title boundary over adding IDs or hashes.
- **Reference source of truth during Phase 5:** note details/frontmatter are the source of truth for wiki-style references. The persisted wiki-title cache is derived data, refreshed whenever note details are updated and backfilled during migration.
- **Legacy relationship columns:** keep `relation_type` / `target_note_id` only until relationship UI, `NoteRealm`, references, and graph retrieval read from frontmatter/cache. Remove those columns before Phase 5 closes.
- **Legacy parent semantics:** for existing non-relationship notes only, migrate the old semantic parent into a `parent: "[[Parent Title]]"` frontmatter property and update the wiki-title cache. New notes do not receive this frontmatter by default.
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

## Current Production Migration Note

Sub-phases 5.1 through 5.13 are already implemented. The admin data migration code introduced by 5.8 and 5.9 has not yet been run against production data.

Do not run the old relationship title/details migration as one long blocking admin request. The production run should happen after wiki-title cache persistence exists, so the title backfill, details/frontmatter backfill, slug regeneration, and cache backfill can be performed together in resumable batches.

## Sub-Phase 5.13 - Wiki Title Cache Persistence

**Type:** Structure for the next behavior.

**Pre-condition:** Relationship and ordinary note details may contain wiki links or frontmatter wiki-link values.

**Trigger:** A note's details are parsed for wiki-title references.

**Post-condition:** The parsed wiki-title references can be persisted as cache rows owned by the note, with ordering preserved for stable API output.

**Physical cache row** (`note_wiki_title_cache`): `id`, `note_id` (source note whose details were parsed), `target_note_id` (resolved target note), `link_text` (full wiki token inside `[[]]`, including `|` aliases and qualified `notebook:title`). Only **resolved** links produce rows (same omission rules as runtime wiki resolution today). Stable ordering uses surrogate `id` ascending (insert order matches resolver order); no separate ordinal column.

**Work:** Add the cache table/entity/repository and a cohesive parser/refresher service (`WikiTitleCacheService`) that reuses `WikiLinkResolver` resolution and replaces all rows for a note in one transaction.

**Verify:** Focused backend tests for parser/refresher behavior and persistence replacement semantics.

**Commit boundary:** One cache persistence commit.

## Sub-Phase 5.14 - Details Updates Refresh Wiki Title Cache

**Type:** Behavior.

**Pre-condition:** Wiki-title cache persistence exists.

**Trigger:** A user updates a note's details.

**Post-condition:** The note's cached wiki-title references match the saved details.

**Work:** Wire the details update path through the cache refresher and add a controller-level test that updates details, reloads the note realm, and observes the changed wiki titles.

**Verify:** Focused controller/service tests for details update.

**Commit boundary:** One cache-refresh-on-update commit.

## Sub-Phase 5.15 - NoteRealm Uses Persisted Wiki Titles

**Type:** Behavior.

**Pre-condition:** Details updates refresh the persisted wiki-title cache.

**Trigger:** Any supported endpoint builds a `NoteRealm`.

**Post-condition:** `NoteRealm.wikiTitles` comes from the persisted cache instead of reparsing note details during DTO construction.

**Work:** Move `NoteRealmService` wiki-title population to the cache query path while keeping authorization and resolution behavior equivalent.

**Verify:** Focused `NoteRealmService` / controller tests that prove cached values are used and stale details-only values are not surfaced without a refresh.

**Commit boundary:** One NoteRealm cache-read commit.

## Sub-Phase 5.16 - Resumable Admin Migration Progress

**Type:** Structure for the next behavior.

**Pre-condition:** The wiki-title cache exists, and the relationship title/details migration logic from 5.8 and 5.9 exists but has not been run in production.

**Trigger:** The admin migration endpoint is asked to start or resume the wiki-reference migration.

**Post-condition:** Migration progress is persisted in the database so a backend timeout, crash, deploy, or failed batch does not lose completed work.

**Work:** Add a temporary persisted progress table for this migration, for example a capability-named table such as `wiki_reference_migration_progress`, with one row per migration step. Track step name, status, total count, processed count, last processed note id, last error, timestamps, and a completion marker. Each batch must run in a short transaction and update progress before returning.

**Verify:** Focused backend tests that start a migration, process a batch, simulate a new service instance or repeated call, and resume from persisted progress without reprocessing completed rows.

**Commit boundary:** One migration-progress-persistence commit.

## Sub-Phase 5.17 - Admin Migration Shows Batched Progress

**Type:** Behavior.

**Pre-condition:** Migration progress is persisted.

**Trigger:** The admin data migration runs.

**Post-condition:** The admin frontend can show migration progress after each batch and does not wait on one long request that risks a timeout.

**Work:** Change the admin migration flow so each request processes at most one bounded batch and returns progress. The frontend can call again, poll, or continue until the response says the migration is complete. Surface current step, processed count, total count, and failure message when a batch fails.

**Verify:** Focused backend API tests for progress responses plus the smallest frontend test needed for the admin progress display.

**Commit boundary:** One admin-progress behavior commit.

## Sub-Phase 5.18 - Batched Relationship Migration and Cache Backfill

**Type:** Behavior.

**Pre-condition:** The admin migration can resume from persisted progress and report batch progress.

**Trigger:** The admin data migration runs.

**Post-condition:** Existing relationship notes are migrated in batches: titles from 5.8 are backfilled, details/frontmatter from 5.9 are backfilled, slugs are regenerated where needed, and `source` / `target` wiki-title cache rows are created in the same resumable production run.

**Work:** Fold the 5.8 title migration and 5.9 details migration into the new batched runner instead of requiring the old one-shot admin button. Process relationship notes by stable id ranges. For each note in a batch, derive the title if needed, write relationship frontmatter/body if needed, regenerate the slug after title changes, refresh the wiki-title cache, then mark progress. Keep each operation idempotent so rerunning a batch after a failure is safe.

**Verify:** Focused backend migration tests through the public admin migration entry point for first batch, resume, idempotent rerun, and final completion.

**Commit boundary:** One batched relationship production-migration commit.

## Sub-Phase 5.19 - Batched Legacy Parent Frontmatter and Cache Backfill

**Type:** Behavior.

**Pre-condition:** Relationship notes have been migrated and cache rows can be refreshed in batches.

**Trigger:** The admin data migration advances to existing non-relationship notes.

**Post-condition:** Existing non-relationship notes with a parent have `parent: "[[Parent Title]]"` in frontmatter, and their wiki-title cache includes that parent title. New notes still do not receive this frontmatter by default.

**Work:** Add the legacy-parent step to the same resumable migration job. Serialize or merge frontmatter without overwriting existing properties, refresh the note's cache in the same batch, and persist progress separately from the relationship migration step so it can resume independently.

**Verify:** Focused backend migration tests for ordinary child notes, existing frontmatter preservation, resume, and a note-creation regression showing new notes do not receive default `parent` frontmatter.

**Commit boundary:** One batched legacy-parent-frontmatter commit.

## Sub-Phase 5.20 - Relationship UI Reads Relation From Frontmatter

**Type:** Behavior.

**Pre-condition:** Relationship notes store `relation` in frontmatter and existing notes are migrated.

**Trigger:** A user views, edits, or deletes a relationship.

**Post-condition:** Relationship behavior no longer needs `Note.relationType` / `linkType`; it reads and writes the `relation` frontmatter property.

**Work:** Update relationship read/edit paths to use the frontmatter representation, keeping existing relation UI behavior stable.

**Verify:** `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/relationships/relationship_edit_and_remove.feature`.

**Commit boundary:** One relation-frontmatter-runtime commit.

## Sub-Phase 5.21 - References Use Cached Wiki Titles

**Type:** Behavior.

**Pre-condition:** Relationship and parent references are present in the wiki-title cache.

**Trigger:** A note realm is built for a note that is referenced by other notes.

**Post-condition:** Incoming references are resolved by cached wiki-title entries that point at the note's current title, not by `target_note_id` or relationship child notes.

**Work:** Replace `NoteRealm.inboundReferences` population and related reference lookup paths with cache-backed queries, preserving existing authorization behavior.

**Verify:** Focused controller/service tests for same-notebook and cross-notebook visible references.

**Commit boundary:** One cache-backed-references commit.

## Sub-Phase 5.22 - Remove Relationship Link Type Field

**Type:** Persistence cleanup.

**Pre-condition:** Runtime relationship behavior reads relation from frontmatter.

**Trigger:** Database migrations and generated API are applied.

**Post-condition:** The `relation_type` / link-type field is removed from the note model, schema, OpenAPI, generated client, and frontend/backend references.

**Work:** Drop the column, remove entity/DTO/API references, regenerate TypeScript if OpenAPI changes, and delete obsolete converter code.

**Verify:** Focused backend tests plus affected frontend compile/test target; run relationship E2E if wire behavior changed.

**Commit boundary:** One link-type-removal commit.

## Sub-Phase 5.23 - Remove Relationship Target Field

**Type:** Persistence cleanup.

**Pre-condition:** Runtime reference behavior and relationship displays no longer read `Note.targetNote`.

**Trigger:** Database migrations and generated API are applied.

**Post-condition:** The `target_note_id` field is removed from the note model, schema, OpenAPI, generated client, and frontend/backend references.

**Work:** Drop the column, remove `targetNote` mappings and repository methods used only by the legacy column, and route remaining target behavior through frontmatter/cache.

**Verify:** Focused backend tests and targeted relationship E2E specs.

**Commit boundary:** One target-field-removal commit.

## Sub-Phase 5.24 - Title Rename Updates Cached Wiki References

**Type:** Behavior.

**Pre-condition:** Incoming references are discoverable from the wiki-title cache.

**Trigger:** A note's title changes.

**Post-condition:** Notes that reference the old title are reverse-updated to reference the new title, and their cache rows are refreshed.

**Work:** Add a title-update test that creates at least one referencing note, changes the target title, and verifies both the source details/frontmatter and cache use the new wiki title.

**Verify:** Focused controller/service tests for note title update.

**Commit boundary:** One reverse-reference-title-update commit.

## Sub-Phase 5.25 - Note Graph Uses Cached Wiki References

**Type:** Behavior.

**Pre-condition:** Cached wiki-title references replace legacy inbound relationship lookup.

**Trigger:** A user or AI flow requests a note graph.

**Post-condition:** The graph gets related notes through cache-backed wiki references and no longer traverses children, child relationship notes, or target-child relationship handlers as incoming references for existing notes. The reference quota is increased to account for the broader wiki-reference source.

**Work:** Replace graph relationship handlers that depend on `children`, relationship children, or `targetNote` with cache-backed outgoing/incoming reference handlers; adjust the quota in the graph retrieval policy.

**Verify:** Focused graph retrieval tests showing cached references are included and legacy child-only relationships are not required.

**Commit boundary:** One cache-backed-graph commit.

## Sub-Phase 5.26 - Phase 5 Closeout and Plan Update

**Type:** Structure / cleanup.

**Pre-condition:** Relationship creation, migration, edit, delete, cache-backed references, graph retrieval, and title invariants are passing. The resumable admin migration has reached complete status in production or the deployment checklist explicitly records why it has not.

**Trigger:** Final targeted verification for Phase 5.

**Post-condition:** Phase 5 docs reflect implementation status and no temporary `@wip` scenarios, dead code, or obsolete notes remain.

**Work:**

- Remove any `@wip` tags introduced while driving Phase 5 behavior.
- Update `ongoing/doughnut_wiki_migration_plan.md` with Phase 5 status and any discoveries.
- Remove obsolete relationship-note-specific code that depended on `relation_type`, `target_note_id`, child relationship notes, or child-derived incoming references.
- Drop the temporary migration progress table only after the production migration has completed and rollback/resume is no longer needed; otherwise leave it with an explicit follow-up cleanup note.
- Run the relationship specs touched in this phase with targeted `--spec` commands.

**Commit boundary:** One cleanup/docs commit that leaves Phase 5 closed and ready for Phase 6 (folder-first listing and removal of note **`shortDetails`**).
