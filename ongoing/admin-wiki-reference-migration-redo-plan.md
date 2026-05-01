# Admin wiki reference migration redo plan

This plan extends the existing admin wiki reference migration without redoing completed work out of order.

## Current product behavior (wiki reference migration)

- **Active steps:** `relationship_wiki_backfill`, then `legacy_parent_frontmatter`. Migration completes when both are `COMPLETED`.
- **`note_slug_path_regeneration`:** removed from the ordered migration. Existing `wiki_reference_migration_progress` rows for this step are **ignored** for gating and batch execution. Operators may delete the row or mark it `COMPLETED` for housekeeping only.
- Slugs are still assigned during relationship backfill for each note (`assignSlugForNewNote`); there is no separate full-corpus slug regeneration batch in admin migration.

## Prior production state (historical)

Production had been stopped with a `FAILED` row on `note_slug_path_regeneration`. After deploy that removes slug regeneration from the migration service, **that failure no longer blocks** wiki reference completion. No need to clear that row for migration logic to finish steps 1–2; optional cleanup as above.

## Target migration order (including future fourth step)

1. `relationship_wiki_backfill` — fill missing legacy relationship title/details/cache/slug data for relationship notes.
2. `legacy_parent_frontmatter` — parent wiki frontmatter on legacy child notes and cache refresh.
3. _(obsolete)_ `note_slug_path_regeneration` — not run by the application anymore.
4. **`relationship_wiki_reference_refresh`** _(planned)_ — rewrite relationship note YAML/frontmatter/body links for qualified cross-notebook tokens and refresh `note_wiki_title_cache`; should be ordered **after** `legacy_parent_frontmatter` when implemented.

Use `50` as the batch size for admin wiki reference migration steps after the code change that introduces the fourth step (when that lands).

## Phase 1: Production unblocked from obsolete slug gate

Type: Behavior

Goal: wiki reference migration completes after relationship backfill and legacy parent frontmatter without requiring slug regeneration batches.

Deployment and ops:

- Deploy the version that omits `note_slug_path_regeneration` from `AdminDataMigrationService`.
- Run migration batches until steps 1 and 2 report complete.
- Optionally tidy obsolete `note_slug_path_regeneration` progress rows.

## Phase 2: Define relationship wiki reference refresh behavior

Type: Behavior

Goal: add a failing regression test for the new fourth step from the admin migration surface.

Expected behavior:

- Given a relationship note whose source or target lives in a different notebook from the relationship note,
- when admin migration reaches the new fourth step,
- then the relationship note details are rewritten so `source` and `target` YAML values use qualified wiki links when needed, such as `[[Other Notebook: Target]]`.
- The body line should use the same link tokens as the YAML.
- Existing user-authored suffix content below the generated relationship block is preserved.
- `note_wiki_title_cache` rows for the relationship note are refreshed to match the rewritten links.

Tests:

- Add or extend `AdminDataMigrationServiceTest` so it drives `runBatch` through the real migration service.
- Include at least one cross-notebook target case and assert both details and cache rows.
- Include a same-notebook case or assertion proving same-notebook links remain unqualified.

Deployment and ops:

- Do not deploy or trigger production migration after this phase by itself unless the test is passing with implementation from the next phase.

## Phase 3: Implement qualified relationship link formatting

Type: Behavior

Goal: make relationship details generation capable of using notebook-qualified wiki tokens.

Code work:

- Extend relationship markdown formatting to accept enough note context to decide whether source and target links need notebook qualification.
- Use unqualified `[[Title]]` when the linked note is in the same notebook as the relationship note.
- Use qualified `[[Notebook name: Title]]` when the linked note is in a different notebook.
- Preserve existing YAML double-quote escaping.
- Update relationship creation and relationship title refresh paths so newly created or edited relationship notes use the same rules as the migration.

Tests:

- Formatter-level tests cover same-notebook, cross-notebook, blank title, quotes, and backslashes.
- Controller or service tests cover relationship creation/update through the public application path.
- The Phase 2 migration regression test passes.

Deployment and ops:

- Do not trigger production migration yet if the fourth migration step is not in the ordered step list.

## Phase 4: Add the fourth migration step

Type: Behavior

Goal: make admin migration run `relationship_wiki_reference_refresh` after `legacy_parent_frontmatter`.

Code work:

- Add `relationship_wiki_reference_refresh` to the ordered migration step list after `legacy_parent_frontmatter`.
- Add batch worker logic that selects all non-deleted relationship notes in stable id order.
- Process 50 notes per HTTP request.
- For each relationship note, rewrite the generated relationship YAML/frontmatter/body block using current source/target notebook context while preserving user suffix content.
- Refresh `note_wiki_title_cache` for each processed relationship note using the existing JDBC replacement path.
- Record progress in `wiki_reference_migration_progress` with the same cursor semantics as the existing steps.
- Update status/ready text so the admin UI clearly reports the third active step.

Tests:

- Migration test proves the fourth step runs only after the first two active steps complete.
- Migration test proves already-migrated relationship notes are still rewritten by the fourth step.
- Migration test proves cache rows are replaced/refreshed for rewritten relationship notes.
- Migration test proves the fourth step is resumable across multiple batches of size 50.

Deployment and ops:

- Deploy after this phase.
- After this deployment, trigger admin migration batches again.
- Because production should already have completed steps 1–2 before this deploy, the service should start `relationship_wiki_reference_refresh`.
- Continue triggering batches until the fourth step is `COMPLETED`.

## Phase 5: Verify production completion

Type: Behavior

Goal: confirm production data and progress rows match the intended final state.

Checks:

- `wiki_reference_migration_progress` has completed rows for all **active** migration steps (including the fourth step when present).
- Spot-check relationship notes with cross-notebook source/target links: YAML and body contain qualified wiki links where needed.
- Spot-check same-notebook relationship notes: YAML and body remain unqualified.
- Spot-check `note_wiki_title_cache` rows for rewritten relationship notes: link text and target note ids match the rewritten details.
- Admin migration status reports completion and does not show a failed gate on active steps.

Deployment and ops:

- No new deployment is expected in this phase unless verification finds a defect.
