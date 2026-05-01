# Admin wiki reference data migration — status and open work

This note tracks **production progress** for `POST /api/admin/data-migration/run-batch` (`AdminDataMigrationService` / `wiki_reference_migration_progress`) and **known follow-ups** that imply redoing **relationship wiki backfill**.

## What the migration does (order)

1. **Relationship wiki backfill** — `relationship_wiki_backfill`: relationship notes get title, YAML `type: relationship`, `source` / `target` wiki tokens, relationship body line, slug assignment, `note_wiki_title_cache` refresh (JDBC path in migration).
2. **Legacy parent frontmatter** — `legacy_parent_frontmatter`: non-relation children with a parent get missing `parent: "[[…]]"` in YAML (`LegacyParentWikiFrontmatterMerge`), then cache refresh.
3. **Note slug path regeneration** — `note_slug_path_regeneration`: recomputes `note.slug` under `uk_note_notebook_slug` (`notebook_id`, `slug`).

## Snapshot where things stopped

The last blocking error reported on production migration:

- **Step:** `note_slug_path_regeneration`
- **Counts shown in UI:** about **2220 / 27377**
- **Error:** duplicate key **`note.uk_note_notebook_slug`** on `UPDATE note … slug=…` (example slug prefix resembled notebook `15` with many **`untitled/`** segments).

### Cause (understood)

`WikiSlugPathService.assignSlugForNewNote` uses **JDBC** to list sibling slugs in-folder. If the slug step **buffers many `merge`s and only flushes once per HTTP batch**, later notes do not see sibling slug updates written earlier **in the same transaction**, so basename disambiguation can assign the **same** full slug twice → unique violation.

### Fix in codebase (needs deploy before retrying slug batches)

Slug regeneration loop now **`merge` then `flush` per note** so sibling slug queries see prior rows in that transaction (`AdminDataMigrationBatchWorker.runSlugRegenerationBatch`; same idea in `WikiSlugPathService.regenerateAllNoteSlugPaths`).

After deploy **and** resetting `FAILED` rows in `wiki_reference_migration_progress`, slug batches should be able to continue from the persisted cursor without rewriting earlier steps unless you intentionally reset progress.

Other fixes already folded into mainline for migration robustness include: transactional batch isolation (`AdminDataMigrationBatchWorker`), wiki title cache via JDBC + NFKC duplicate handling + upsert where needed for `uq_note_wiki_title_cache_note_link`, and widening `note.details` to **`MEDIUMTEXT`** for legacy YAML growth (`V300000162`).

## Why step 1 (relationship backfill) should run again after code fixes

Requirements discovered after prod had already advanced:

1. **Cross-notebook targets — qualified wiki links in front matter**  
   Today `RelationshipNoteMarkdownFormatter.format` builds **unqualified** tokens `[[title]]` for `source` and `target` from display titles only. If the **target** (or source) note lives in **another notebook**, resolution and UX expect the qualified form **`[[Notebook name: Note title]]`** (same shape `WikiLinkResolver` already supports for qualified tokens).  
   **Action:** extend relationship backfill so YAML `source` / `target` use qualified links when the linked note’s notebook differs from the relationship note’s notebook (exact notebook display string / escaping rules to match resolver).

Redoing step 1 in production implies: ship the formatter + resolver fixes, then either **reuse eligibility** so relationship notes pick up corrections on the next batch pass, or **reset** `relationship_wiki_backfill` in `wiki_reference_migration_progress` (and possibly clear offending cache rows if needed) depending on migration eligibility rules chosen for the rerun.

## Ops reminders

- **`FAILED` gate:** batches are blocked until the failed step row is set back to a runnable status (e.g. `RUNNING`, `last_error` cleared); see **`docs/gcp/prod_env.md`** (laptop connection + example `UPDATE`).
- **`doughnut`@`%`:** prod MySQL users are host-restricted; temporary **`--host=%`** user for Cloud SQL Proxy is documented there; remove after ops.
