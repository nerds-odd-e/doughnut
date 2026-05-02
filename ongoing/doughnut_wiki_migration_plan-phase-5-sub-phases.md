# Doughnut Wiki Migration Plan - Phase 5 Sub-Phases

## Parent Phase

Phase 5 of `ongoing/doughnut_wiki_migration_plan.md`: Convert Relationship Notes into Normal Notes.

## Goal

Convert relationship notes from special notes identified by `target_note_id` / `relation_type` / `parent_id` into ordinary notes whose title, details, folder, slug, frontmatter, and cached wiki-title references carry the relationship meaning.

By the end of Phase 5:

- every relationship note has a non-empty title derived from its relationship and truncated to `Note.MAX_TITLE_LENGTH` (currently 150 characters)
- relationship note details include relationship frontmatter and readable Markdown content
- relation type, subject, and target are only a templated way to create a new note
- notes created from a relationship template are ordinary notes in title display, sidebar display, note show, references, and graph behavior
- wiki-title references are persisted in a cache derived from note details/frontmatter
- `NoteRealm.wikiTitles`, unified note-show **references** (`NoteRealm.references` after sub-phase 5.21.3), and graph retrieval use the cached wiki-title references instead of legacy `target_note_id` / child relationship notes
- legacy non-relationship notes have a migration-only `parent: "[[...]]"` frontmatter property that preserves the old semantic parent as a wiki reference
- relationship `relation` and `target` data come from frontmatter/cache instead of `relation_type` / `target_note_id`
- no note title may be null or empty after the Phase 5 data migration and schema/API tightening
- there is no relation-note-specific product model; every note is just a normal note

## Design Decisions

- **Relationship title:** derive the title from the source note title, relation label, and target note title. Keep it human-readable first; the slug is derived separately from the truncated title by the existing slug service.
- **Title length:** truncate the derived relationship title before persistence so it never exceeds `Note.MAX_TITLE_LENGTH`. Prefer truncating at the title boundary over adding IDs or hashes.
- **Reference source of truth during Phase 5:** note details/frontmatter are the source of truth for wiki-style references. The persisted wiki-title cache is derived data, refreshed whenever note details are updated and backfilled during migration.
- **Legacy relationship columns:** keep `relation_type` / `target_note_id` only until relationship UI, `NoteRealm`, references, and graph retrieval read from frontmatter/cache. Remove those columns before Phase 5 closes.
- **Legacy parent semantics:** for existing non-relationship notes only, migrate the old semantic parent into a `parent: "[[Parent Title]]"` frontmatter property and update the wiki-title cache. New notes do not receive this frontmatter by default.
- **Title invariant:** Phase 5 is the last phase that may tolerate legacy null or empty note titles. After Phase 5, production code and schema should treat note title as required.
- **Relationship creation template:** relation type, subject, and target are treated as creation-time template inputs that seed title/details/frontmatter. After creation, the result is an ordinary note; frontend and backend code should not branch on a "relation note" display model.

## Sizing Rule

Each sub-phase below is sized as a **small cohesive slice** of work (roughly a five-minute *planning* granularity: one concern, one verify step). If a slice cannot be finished and tested inside that scope, split it further before continuing.

**Git commits:** There is **no** requirement to run `git commit` once per sub-phase or immediately after each one. Developers may batch several sub-phases, combine with unrelated fixes, or split one sub-phase across multiple commits as long as the tree stays green. The **`Commit boundary:`** lines under each sub-phase name the **natural cohesion / review unit** (what belongs together in one change when you *do* commit), not a literal commit cadence.

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

## Sub-Phase 5.20 - Relationship Template UI Becomes Normal Note UI

**Type:** Behavior.

**Pre-condition:** Relationship notes store `relation` in frontmatter and existing notes are migrated.

**Trigger:** A user views, edits, or deletes a note created from a relationship template.

**Post-condition:** Relationship-specific UI is removed from title, icon, and sidebar paths. **Display** of relation type (icons, inverted labels) reads the `relation` value from **note details** frontmatter (see 5.20.4), not `NoteTopology`. Topology DTO fields such as `relationType` are removed after that read path migrates (5.20.5), then inbound note-show rows lose `RelationshipOfNote` (5.20.6), relation-row API identity moves out of `RichMarkdownEditor` (5.20.7), and `targetNoteTopology` is removed from topology (5.20.8). **Editing** the relation continues through the note property surface and existing creation flows until the wiki model fully absorbs those flows.

### Sub-Sub-Phase 5.20.1 - Edit Relation Through Property Value

**Type:** Behavior.

**Pre-condition:** A note has a frontmatter property named `relation`.

**Trigger:** A user clicks the value field for the `relation` property in rich mode.

**Post-condition:** The value is edited as a relation type selector/editor, not as direct rich-text content and not through the title icon.

**Work:** Treat `relation` as a special property in the rich property editor. Remove the relation-type edit affordance from the title/icon path and update the existing relationship edit E2E coverage to drive the property value field instead.

**Verify:** `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/relationships/relationship_edit_and_remove.feature`.

**Commit boundary:** One relation-property-edit commit.

### Sub-Sub-Phase 5.20.2 - Relation Notes Use Normal Note Title In Main Frontend

**Type:** Behavior.

**Pre-condition:** Notes created from a relationship template have ordinary titles.

**Trigger:** A user opens one of those notes in the frontend.

**Post-condition:** The frontend displays the note title normally and removes the special relationship title presentation.

**Work:** Remove the special title rendering for relation notes in the main note page/card surfaces and update the existing E2E test expectations for the new normal-title behavior.

**Verify:** Target the existing relationship E2E spec that asserts note title display.

**Commit boundary:** One normal-title-display commit.

### Sub-Sub-Phase 5.20.3 - Sidebar Uses Normal Note Title

**Type:** Behavior.

**Pre-condition:** The frontend main surfaces display ordinary note titles for notes created from relationship templates.

**Trigger:** A user views the sidebar/tree containing one of those notes.

**Post-condition:** The sidebar uses the normal note title and no longer builds a special relationship label.

**Work:** Remove relation-specific title composition from sidebar display and update the existing E2E test that covers relationship visibility/navigation.

**Verify:** Target the existing relationship/sidebar E2E spec that observes the sidebar title.

**Commit boundary:** One sidebar-normal-title commit.

### Sub-Sub-Phase 5.20.4 - Frontend Reads Relation Type From Note Details Frontmatter

**Type:** Behavior.

**Pre-condition:** Relationship notes store `relation` in frontmatter. API responses that drive relation-type **display** include `Note.details` (or an equivalent parsed-properties shape) for those notes.

**Trigger:** A user views a surface that shows the relation type icon or inverted label for a relationship note (for example inbound rows on note show before **5.21.3** unifies them into **`NoteReferences`**, or the relationship row before `RelationshipOfNote` is removed in 5.20.6).

**Post-condition:** Display uses the `relation` value parsed from the note’s markdown frontmatter (same normalization as the property editor / `relationTypeFromKebab`), not `NoteTopology.relationType`.

**Work:** Add or reuse a small helper to read the relation label from `details`. Update `NoteShow` (label span beside the inbound link), `RelationNob` / `RelationshipOfNote` until 5.20.6 removes the latter, and any other **read-only** UI that still reads `noteTopology.relationType`. Keep a single mapping from stored values (kebab / slug) to display labels shared with `RichFrontmatterProperties`.

**Verify:** Targeted relationship display E2E (`--spec` for relationship / note-show coverage touched) plus any focused frontend tests for the parser if the helper is non-trivial.

**Commit boundary:** One frontmatter-relation-display commit.

**Replaceability (can this drop all `relationType` on `NoteTopology` for the frontend?):**

| Area | Covered by frontmatter parse? | Notes |
|------|--------------------------------|-------|
| **Display** (`NoteShow` inbound refs, `RelationshipOfNote` until 5.20.6 / `RelationNob`) | **Yes**, once `details` is reliably present on those `Note` payloads | Inbound row: label from frontmatter; linked title becomes a normal note link in 5.20.6 (no `RelationshipOfNote`). |
| **Cards / search hits / any surface with only `NoteTopology`** | **Not automatically** | No body to parse unless the wire shape adds `details`, a short relation snippet, or the UI stops showing relation type there. Today **`Card` does not use `RelationNob`**; search results use topology without relation icon—confirm when implementing. |
| **Editing** (`RichFrontmatterProperties`, relation property row) | **Already** | Driven by frontmatter rows and API update; not a `noteTopology.relationType` read. |
| **Creation** (`AddRelationshipFinalize`, DTO `RelationshipCreation`) | **N/A** | User-chosen type on create; not derived from topology display. |
| **Types** (`NoteTopology["relationType"]` on selects) | **Mechanical follow-up** | After display migration, retype selectors from a shared label union or string; then **5.20.5** can remove the field from `NoteTopology`. |

**Conclusion:** Frontmatter parsing can replace **all current** frontend **reads** of `noteTopology.relationType` for surfaces that already carry `Note.details`. It does **not** replace hypothetical future list-only payloads; if those must show relation type, extend the API or accept no icon until the note is opened. Ordering stays: **this phase (5.20.4) → then 5.20.5 removes `relationType` from topology → 5.20.6 removes `RelationshipOfNote` → 5.20.7 stops threading note id through `RichMarkdownEditor` → 5.20.8 removes `targetNoteTopology` → 5.20.9 backend frontmatter reads.**

### Sub-Sub-Phase 5.20.5 - Remove Relation Type From NoteTopology

**Type:** Structure cleanup.

**Pre-condition:** Frontend relation **display** no longer reads `NoteTopology.relationType` (reads go through note-details frontmatter per 5.20.4). Editing continues through the property surface / API, not topology.

**Trigger:** API types are regenerated or compiled.

**Post-condition:** `NoteTopology` no longer exposes `relationType`, and frontend/backend references to that field are gone.

**Work:** Remove the field from the backend topology DTO, regenerate the TypeScript API client if OpenAPI changes, and delete obsolete frontend reads.

**Verify:** Focused backend compile/tests plus affected frontend typecheck/test target.

**Commit boundary:** One topology-relation-type-removal commit.

### Sub-Sub-Phase 5.20.6 - Note Show Inbound References: Normal Linked Title Only (Remove RelationshipOfNote)

**Type:** Behavior / structure cleanup.

**Pre-condition:** Inbound reference notes have ordinary persisted titles (Phase 5 relationship title work). The “referenced by” relation label reads from frontmatter (5.20.4).

**Trigger:** A user opens a note that has inbound references.

**Post-condition:** Each “referenced by” row shows the inverted relation label and a **normal** navigable note title (same title treatment as other note links), pointing at the referring relationship note. `RelationshipOfNote.vue` is removed.

**Work:** In `NoteShow.vue`, replace `RelationshipOfNote` with a `router-link` (or shared small link component) to the referring note’s show route using `noteTopology.id` / `note.id`, plus `NoteTitleComponent` or the same title source used elsewhere—**not** a subject/target split from `targetNoteTopology` / `parentOrSubjectNoteTopology`. Delete `frontend/src/components/links/RelationshipOfNote.vue`, remove stale `components.d.ts` entries, and delete any helpers/styles/tests used only by that component.

**Verify:** Targeted relationship / note-show E2E (`--spec` for referenced-by behavior); frontend tests that referenced the removed component.

**Commit boundary:** One inbound-ref presentation commit.

### Sub-Sub-Phase 5.20.7 - Relation Property API Note Id Only at Note-Editing Layer (Not RichMarkdownEditor)

**Type:** Structure (layering / cohesion).

**Pre-condition:** Rich mode composes `NoteEditableDetails` → `RichMarkdownEditor` → `RichFrontmatterProperties`.

**Trigger:** A developer extends the generic rich markdown editor.

**Post-condition:** `RichMarkdownEditor` does **not** declare or forward `relationPropertyApiNoteId` (it is the persisted id of the note whose `details` are being edited—nothing markdown-generic about it). `RichFrontmatterProperties` still receives that id for relation-type API updates, supplied from `NoteEditableDetails` or `NoteTextContent` via `provide`/`inject`, a thin note-local wrapper, or template composition that keeps the prop on the frontmatter block only.

**Work:** Remove the prop from `RichMarkdownEditor`; wire the id into `RichFrontmatterProperties` from the note-editing parent only. Replace any `targetNoteTopology`-based predicate in `NoteTextContent` (or siblings) that only existed to decide “pass note id for relation row” with a frontmatter-based check or the straightforward **editing note id** when the `relation` property is present—so the markdown stack does not depend on topology for identity. Update `RichMarkdownEditor.spec.ts`, `NoteEditableDetails.spec.ts`, and any builders that set the prop on the editor.

**Verify:** Focused frontend tests for relation property edit in rich mode (paths that hit the relation row API).

**Commit boundary:** One editor-layering commit.

### Sub-Sub-Phase 5.20.8 - Remove Target Note Topology From NoteTopology

**Type:** Structure cleanup.

**Pre-condition:** Frontend relation display/navigation no longer reads `NoteTopology.targetNoteTopology` (including removal of `RelationshipOfNote` in 5.20.6 and any remaining reads in cards/search).

**Trigger:** API types are regenerated or compiled.

**Post-condition:** `NoteTopology` no longer exposes `targetNoteTopology`, and frontend/backend references to that field are gone.

**Work:** Remove the field from the backend topology DTO, regenerate the TypeScript API client if OpenAPI changes, and delete obsolete frontend reads.

**Verify:** Focused backend compile/tests plus affected frontend typecheck/test target.

**Commit boundary:** One topology-target-removal commit.

### Sub-Sub-Phase 5.20.9 - Backend Reads Relation Type From Frontmatter

**Type:** Behavior.

**Pre-condition:** Relation type is stored in the `relation` frontmatter property.

**Trigger:** A backend path still needs the relation type while relationship-template behavior is being removed.

**Post-condition:** The backend derives relation type from frontmatter instead of `Note.relationType` / topology fields.

**Work:** Update remaining backend read/edit/delete paths that need a relation label to parse the `relation` frontmatter property. Keep this scoped to runtime compatibility until the relation-specific paths are removed.

**Verify:** `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/relationships/relationship_edit_and_remove.feature`.

**Commit boundary:** One backend-frontmatter-relation commit.

## Sub-Phase 5.21 - References Use Cached Wiki Titles

**Type:** Behavior (split into three cohesive slices; commit cadence is up to the developer).

**Pre-condition:** Relationship and parent semantics are represented in note details/frontmatter and rows exist in `note_wiki_title_cache` when links resolve.

**Overall post-condition (after 5.21.3):** Note show uses a single `NoteRealm.references` list built from the cache (plus the same authorization rules as today). The separate “referenced by” sidebar and the old split between inbound vs relationship-child lists are gone from the UI. Legacy `NoteRealm.inboundReferences` and `NoteRealm.relationshipsDeprecating` remain on the wire **only until** graph retrieval is migrated in **5.23**, which then removes those properties from `NoteRealm` and generated clients (see 5.23).

**E2E (all slices):** After each slice, run the smallest relevant `--spec` set (relationship + note-show features touched, for example `e2e_test/features/relationships/*.feature` and any note-show / reference scenarios that assert the reference surface). Keep scenarios green; add or adjust assertions when the observable surface moves (unified list, layout).

### Sub-Sub-Phase 5.21.1 - Inbound References From Wiki Link Cache

**Type:** Behavior.

**Pre-condition:** Wiki-title cache rows exist for referring notes (`note_id` = referrer, `target_note_id` = resolved target).

**Trigger:** `NoteRealm` is built for a note **N** that other notes link to.

**Post-condition:** The list that represents “notes that link to **N** as target” is populated by querying the cache for rows whose `target_note_id` equals **N**’s id (not by `Note.findAllByTargetNote` / legacy `target_note_id` column semantics). Visibility and same-notebook / cross-notebook authorization match current inbound behavior.

**Work:** Add or reuse a repository/service query over `note_wiki_title_cache` joined to source notes; wire `NoteRealmService` (or equivalent) to populate the inbound slice from that query. Keep the existing DTO field name (`inboundReferences`) until **5.23** removes it—this slice proves cache parity with focused tests.

**Verify:** Focused controller/service tests for same-notebook and cross-notebook visible inbound rows; targeted E2E if any user-visible inbound list behavior changes.

**Commit boundary:** One inbound-from-cache commit.

### Sub-Sub-Phase 5.21.2 - Relationship / Parent–Subject Links From Wiki Link Cache

**Type:** Behavior.

**Pre-condition:** Inbound population can use the cache (5.21.1). Relationship and legacy parent links appear as resolved cache rows where **N** is the **subject** side: e.g. relationship note details whose `source` wiki link resolves to **N**, and non-relationship notes whose `parent` frontmatter link resolves to **N** (exact filter rules follow the same semantics the product used when those edges came from `parent_id` / relationship structure).

**Trigger:** `NoteRealm` is built for a note **N** that is the parent or subject of other notes in the wiki sense.

**Post-condition:** The list that represents “notes that stand in the old relation-child / subject relationship to **N**” is derived from the wiki-title cache (and the same authorization), not from traversing child relationship notes or structural parent edges for that purpose.

**Work:** Implement the cache-backed query for the “**N** as parent/subject” slice; feed the slice that today maps to relationship rows / `relationshipsDeprecating` (name may still be `relationshipsDeprecating` on the DTO until 5.23). Add focused tests that distinguish target-linked vs subject/parent-linked rows.

**Verify:** Focused backend tests; targeted relationship / note-show E2E if the observable list order or membership changes.

**Commit boundary:** One relations-from-cache commit.

### Sub-Sub-Phase 5.21.3 - Unified `NoteRealm.references` and `NoteReferences` UI

**Type:** Behavior.

**Pre-condition:** Inbound and relation-style slices are both cache-backed (5.21.1 and 5.21.2).

**Trigger:** A user opens note show for a note with any mix of inbound and relation-style referring notes.

**Post-condition:** `NoteRealm` exposes a single ordered list property **`references`** (array of `Note` or the same shape consumers need—align with OpenAPI). It merges the two slices with a documented ordering (for example dedupe by referring note id, stable sort). The frontend **removes** the separate “Referenced by” / `inboundReferences` sidebar in `NoteShow.vue`. All items render in one place: rename `frontend/src/components/notes/ChildrenNotes.vue` to **`NoteReferences.vue`**, update imports (including `components.d.ts` if used), and render the **combined** `noteRealm.references` list (expand/collapse / cards behavior stays coherent with one list). Until **5.23**, you may still populate `inboundReferences` / `relationshipsDeprecating` in parallel for graph or other readers; the note-show path must use only `references` and `NoteReferences`.

**Work:** Backend merge + OpenAPI field `references`; frontend rename and single-column layout; remove duplicate inbound-only UI. Update `NoteRealmBuilder` / fixtures and any Vitest builders that assert realm shape.

**Verify:** Focused backend tests for merge ordering and dedupe; frontend tests; targeted Cypress `--spec` for relationship + note-show reference assertions.

**Commit boundary:** One unified-references + UI commit.

## Sub-Phase 5.22 - Remove Relationship Link Type Field

**Type:** Persistence cleanup.

**Pre-condition:** Runtime relationship behavior reads relation from frontmatter.

**Trigger:** Database migrations and generated API are applied.

**Post-condition:** The `relation_type` / link-type field is removed from the note model, schema, OpenAPI, generated client, and frontend/backend references.

**Work:** Drop the column, remove entity/DTO/API references, regenerate TypeScript if OpenAPI changes, and delete obsolete converter code.

**Verify:** Focused backend tests plus affected frontend compile/test target; run relationship E2E if wire behavior changed.

**Commit boundary:** One link-type-removal commit.

## Sub-Phase 5.23 - Note Graph Uses Cached Wiki References

**Type:** Behavior.

**Pre-condition:** Cached wiki-title references drive **note show** (5.21). `NoteRealm` may still expose `inboundReferences` and `relationshipsDeprecating` for older graph code paths until this sub-phase completes.

**Trigger:** A user or AI flow requests a note graph.

**Post-condition:** The graph gets related notes through cache-backed outgoing/incoming reference handlers and no longer traverses children, child relationship notes, or target-child relationship handlers as incoming references for existing notes. The reference quota is increased to account for the broader wiki-reference source.

**API cleanup (same sub-phase or an immediately following commit):** Remove **`NoteRealm.inboundReferences`** and **`NoteRealm.relationshipsDeprecating`** from the backend DTO, OpenAPI, and generated TypeScript. Callers use **`NoteRealm.references`** and/or query the wiki-title cache directly—no parallel inbound vs “relations deprecating” lists on the wire. Regenerate the API client if OpenAPI changes; update **`NoteRealmBuilder`**, MCP, and any tests that assert the old shape.

**Work:** Replace graph relationship handlers that depend on `children`, relationship children, or `targetNote` with cache-backed handlers; adjust the quota in the graph retrieval policy; delete obsolete realm fields and fix compile errors.

**Verify:** Focused graph retrieval tests showing cached references are included and legacy child-only relationships are not required; typecheck and targeted E2E where graph or realm responses are asserted.

**Commit boundary:** One cache-backed-graph slice (optionally two cohesive slices—e.g. graph behavior vs DTO removal—if that improves review; do not leave failing E2E between them). Same git-commit flexibility as [Sizing Rule](#sizing-rule).

## Sub-Phase 5.24 - Remove Relationship Target Field

**Type:** Persistence cleanup.

**Pre-condition:** Runtime reference behavior and relationship displays no longer read `Note.targetNote`.

**Trigger:** Database migrations and generated API are applied.

**Post-condition:** The `target_note_id` field is removed from the note model, schema, OpenAPI, generated client, and frontend/backend references.

**Work:** Drop the column, remove `targetNote` mappings and repository methods used only by the legacy column, and route remaining target behavior through frontmatter/cache.

**Verify:** Focused backend tests and targeted relationship E2E specs.

**Commit boundary:** One target-field-removal commit.

## Sub-Phase 5.25 - Title Rename Updates Cached Wiki References

**Type:** Behavior.

**Pre-condition:** Referring notes are discoverable from the wiki-title cache (and, after 5.21.3, surfaced on `NoteRealm.references` for note show).

**Trigger:** A note's title changes.

**Post-condition:** Notes that reference the old title are reverse-updated to reference the new title, and their cache rows are refreshed.

**Work:** Add a title-update test that creates at least one referencing note, changes the target title, and verifies both the source details/frontmatter and cache use the new wiki title.

**Verify:** Focused controller/service tests for note title update.

**Commit boundary:** One reverse-reference-title-update commit.

## Sub-Phase 5.26 - Note Show Stops Surfacing Relationships As Child Notes

**Type:** Behavior.

**Pre-condition:** References and graph retrieval use cached wiki-title references; **`NoteRealm.relationshipsDeprecating`** and **`NoteRealm.inboundReferences`** are removed from the API in **5.23** (note show already uses **`NoteRealm.references`** and **`NoteReferences`** from **5.21.3**).

**Trigger:** A user opens a note show page for a note that used to have relationship child notes.

**Post-condition:** Child relationship notes are not presented as a separate “relationship children” concept on note show; users find those edges only through the unified reference surface (`references` / wiki links). Any residual UI, copy, or E2E steps that still assumed a distinct relationship-child list or sidebar inbound panel are removed or rewritten.

**Work:** Align remaining note-show copy and E2E with **`NoteReferences`** + **`references`** only. If 5.23 already dropped `relationshipsDeprecating` from the wire, this sub-phase is mostly behavioral confirmation and test cleanup; otherwise complete that removal here in lockstep with the plan above.

**Verify:** Target the existing E2E feature that observes note-show relationships/references, plus focused frontend tests for `NoteReferences`.

**Commit boundary:** One note-show-reference-surface commit.

## Sub-Phase 5.27 - Phase 5 Closeout and Plan Update

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
