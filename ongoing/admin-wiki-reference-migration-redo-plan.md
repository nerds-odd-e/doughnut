# Admin wiki reference migration redo plan

This plan extends the existing admin wiki reference migration without redoing the currently interrupted work out of order.

## Current production state

- Production is stopped in `note_slug_path_regeneration` with a `FAILED` row in `wiki_reference_migration_progress`.
- The intended immediate production action is to deploy the slug-regeneration flush fix, clear the failed gate, and continue `note_slug_path_regeneration` from its persisted cursor.
- The new relationship YAML correction should run after the original three steps complete, as a new fourth step, not by resetting the original `relationship_wiki_backfill` step.

## Target migration order

1. `relationship_wiki_backfill` keeps its original meaning: fill missing legacy relationship title/details/cache/slug data.
2. `legacy_parent_frontmatter` keeps its original meaning: add parent wiki frontmatter to legacy child notes and refresh cache.
3. `note_slug_path_regeneration` keeps its original meaning: regenerate note slugs, including the current interrupted production cursor.
4. `relationship_wiki_reference_refresh` rewrites relationship note YAML frontmatter/body links for all relationship notes and refreshes `note_wiki_title_cache`.

Use `50` as the batch size for admin wiki reference migration steps after the code change that introduces the fourth step.

## Phase 1: Resume interrupted slug regeneration

Type: Behavior

Goal: finish the already-started production `note_slug_path_regeneration` safely, without changing the scope of migrated relationship details.

Code work:

- Keep the per-note `merge` + `flush` behavior in slug regeneration so slug disambiguation sees earlier notes processed in the same HTTP batch.
- Keep the existing three-step order for this deployment.

Tests:

- Backend test proves slug regeneration can process multiple notes in one HTTP batch without assigning duplicate slugs.
- Backend migration tests for existing three-step progress still pass.

Deployment and ops:

- Deploy after this phase.
- After this deployment, clear only the `FAILED` gate for `note_slug_path_regeneration` and trigger admin migration batches again.
- Continue triggering batches until `note_slug_path_regeneration` completes.
- Do not reset `relationship_wiki_backfill` for this phase.

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

Goal: make admin migration run `relationship_wiki_reference_refresh` after `note_slug_path_regeneration`.

Code work:

- Add `relationship_wiki_reference_refresh` to the ordered migration step list after `note_slug_path_regeneration`.
- Add batch worker logic that selects all non-deleted relationship notes in stable id order.
- Process 50 notes per HTTP request.
- For each relationship note, rewrite the generated relationship YAML/frontmatter/body block using current source/target notebook context while preserving user suffix content.
- Refresh `note_wiki_title_cache` for each processed relationship note using the existing JDBC replacement path.
- Record progress in `wiki_reference_migration_progress` with the same cursor semantics as the existing steps.
- Update status/ready text so the admin UI clearly reports the fourth step.

Tests:

- Migration test proves the fourth step runs only after the first three steps complete.
- Migration test proves already-migrated relationship notes are still rewritten by the fourth step.
- Migration test proves cache rows are replaced/refreshed for rewritten relationship notes.
- Migration test proves the fourth step is resumable across multiple batches of size 50.

Deployment and ops:

- Deploy after this phase.
- After this deployment, trigger admin migration batches again.
- Because production should already have completed `note_slug_path_regeneration` after Phase 1, the service should start the new `relationship_wiki_reference_refresh` step.
- Continue triggering batches until the fourth step is `COMPLETED`.

## Phase 5: Verify production completion

Type: Behavior

Goal: confirm production data and progress rows match the intended final state.

Checks:

- `wiki_reference_migration_progress` has completed rows for all four steps.
- Spot-check relationship notes with cross-notebook source/target links: YAML and body contain qualified wiki links where needed.
- Spot-check same-notebook relationship notes: YAML and body remain unqualified.
- Spot-check `note_wiki_title_cache` rows for rewritten relationship notes: link text and target note ids match the rewritten details.
- Admin migration status reports completion and does not show a failed gate.

Deployment and ops:

- No new deployment is expected in this phase unless verification finds a defect.
