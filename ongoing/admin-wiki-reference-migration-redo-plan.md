# Admin wiki reference migration redo plan

This plan extends the existing admin wiki reference migration without redoing completed work out of order.

## Current product behavior (wiki reference migration)

- **Active steps:** `relationship_wiki_backfill`, then `legacy_parent_frontmatter`. Migration completes when both are `COMPLETED`.
- Progress rows for **retired** batch steps are not part of this order; they do not gate completion.

## Target migration order (including future third active step)

1. `relationship_wiki_backfill` — fill missing legacy relationship title/details/cache data for relationship notes.
2. `legacy_parent_frontmatter` — parent wiki frontmatter on legacy child notes and cache refresh.
3. **`relationship_wiki_reference_refresh`** _(planned)_ — rewrite relationship note YAML/frontmatter/body links for qualified cross-notebook tokens and refresh `note_wiki_title_cache`; should be ordered **after** `legacy_parent_frontmatter` when implemented.

Use `50` as the batch size for admin wiki reference migration steps after the code change that introduces the third step (when that lands).

## Phase 1: Define relationship wiki reference refresh behavior

Type: Behavior

Goal: add a failing regression test for the new step from the admin migration surface.

Expected behavior:

- Given a relationship note whose source or target lives in a different notebook from the relationship note,
- when admin migration reaches the new step,
- then the relationship note details are rewritten so `source` and `target` YAML values use qualified wiki links when needed, such as `[[Other Notebook: Target]]`.
- The body line should use the same link tokens as the YAML.
- Existing user-authored suffix content below the generated relationship block is preserved.
- `note_wiki_title_cache` rows for the relationship note are refreshed to match the rewritten links.

Tests:

- Add or extend `AdminDataMigrationServiceTest` so it drives `runBatch` through the real migration service.
- Include at least one cross-notebook target case and assert both details and cache rows.
- Include a same-notebook case or assertion proving same-notebook links remain unqualified.

## Phase 2: Implement qualified relationship link formatting

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
- The Phase 1 migration regression test passes.

## Phase 3: Add the third migration step

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

- Migration test proves the third step runs only after the first two active steps complete.
- Migration test proves already-migrated relationship notes are still rewritten by the third step.
- Migration test proves cache rows are replaced/refreshed for rewritten relationship notes.
- Migration test proves the third step is resumable across multiple batches of size 50.

Deployment and ops:

- Deploy after this phase.
- After this deployment, trigger admin migration batches again.
- Because production should already have completed steps 1–2 before this deploy, the service should start `relationship_wiki_reference_refresh`.
- Continue triggering batches until the third step is `COMPLETED`.

## Phase 4: Verify production completion

Type: Behavior

Goal: confirm production data and progress rows match the intended final state.

Checks:

- `wiki_reference_migration_progress` has completed rows for all **active** migration steps (including the third step when present).
- Spot-check relationship notes with cross-notebook source/target links: YAML and body contain qualified wiki links where needed.
- Spot-check same-notebook relationship notes: YAML and body remain unqualified.
- Spot-check `note_wiki_title_cache` rows for rewritten relationship notes: link text and target note ids match the rewritten details.
- Admin migration status reports completion and does not show a failed gate on active steps.

Deployment and ops:

- No new deployment is expected in this phase unless verification finds a defect.
