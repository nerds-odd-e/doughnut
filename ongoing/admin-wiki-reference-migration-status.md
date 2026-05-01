# Admin wiki reference data migration — status and open work

This note tracks **production progress** for `POST /api/admin/data-migration/run-batch` (`AdminDataMigrationService` / `wiki_reference_migration_progress`) and **known follow-ups** that imply redoing **relationship wiki backfill**.

## What the migration does (order)

1. **Relationship wiki backfill** — `relationship_wiki_backfill`: relationship notes get title, YAML `type: relationship`, `source` / `target` wiki tokens, relationship body line, slug assignment (`assignSlugForNewNote` during the batch), `note_wiki_title_cache` refresh (JDBC path in migration).
2. **Legacy parent frontmatter** — `legacy_parent_frontmatter`: non-relation children with a parent get missing `parent: "[[…]]"` in YAML (`LegacyParentWikiFrontmatterMerge`), then cache refresh.

Wiki reference migration **completes** when both steps above are `COMPLETED`. There is **no** admin migration step that regenerates slugs for all notes anymore (canonical note URLs use note id, not slug paths).

## Obsolete progress rows (`note_slug_path_regeneration`)

Older deployments may still have a `wiki_reference_migration_progress` row for `note_slug_path_regeneration`. The application **does not** read that step for completion, failure gating, or batch work. A `FAILED` (or any other) status on that row **does not** stop batches or status from reporting wiki reference migration complete once steps 1 and 2 are done.

**Optional housekeeping:** operators may delete the obsolete row or set it to `COMPLETED` and clear `last_error` so dashboards stay tidy—purely cosmetic.

## Historical production blockage (slug regeneration era)

Production previously stopped during batched slug regeneration with duplicate key **`note.uk_note_notebook_slug`**. Root cause was transactional buffering during JDBC sibling slug queries; that logic lived in `AdminDataMigrationBatchWorker` slug regeneration (removed). **`WikiSlugPathService.regenerateAllNoteSlugPaths`** may still exist for manual or other tooling outside this migration.

## Why step 1 (relationship backfill) should run again after code fixes

Requirements discovered after prod had already advanced:

1. **Cross-notebook targets — qualified wiki links in front matter**  
   Relationship YAML `source` / `target` may need **`[[Notebook name: Note title]]`** when the linked note lives in another notebook (same shape `WikiLinkResolver` supports).

Redoing step 1 in production implies: ship formatter + resolver fixes, then either **reuse eligibility** so relationship notes pick up corrections on the next batch pass, or **reset** `relationship_wiki_backfill` in `wiki_reference_migration_progress` (and possibly clear offending cache rows if needed) depending on migration eligibility rules chosen for the rerun.

## Ops reminders

- **`FAILED` gate:** batches are blocked only when one of the **active** steps (`relationship_wiki_backfill`, `legacy_parent_frontmatter`) is `FAILED`; see **`docs/gcp/prod_env.md`** for progress updates.
- **`doughnut`@`%`:** prod MySQL users are host-restricted; temporary **`--host=%`** user for Cloud SQL Proxy is documented there; remove after ops.
