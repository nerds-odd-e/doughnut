# Order wiki-link properties after their target note

**Status: planned.**

A frontmatter property whose value is a wiki link (e.g. `example of: "[[Word]]"`) must be
assimilated *after* its target note. Implemented as a **gate**: while the resolved target
note is still a pending assimilation candidate for the same viewer, the property is
suppressed from the queue; once the target is assimilated (or the target is null / deleted
/ `skipMemoryTracking`), the property surfaces normally.

## Key design decisions

- **Gate, not comparator rewrite.** The queue is a lazy min-over-stream-heads
  ([AssimilationService.getNextAssimilationUnit](../backend/src/main/java/com/odde/doughnut/services/AssimilationService.java))
  that requires each SQL stream to stay pre-sorted by `AssimilationUnit.ORDER`. A WHERE
  filter preserves that invariant; changing the comparator would not. So `AssimilationUnit.ORDER`
  is left untouched.
- **Store `target_note_id` on the property index** so the gate is a single extra join, no
  N+1 (developer requirement: keep the get-next-to-assimilate path fast). `note_property_index`
  has no value/target today, and `note_wiki_title_cache` has no `property_key`, so neither
  alone can map a property to its target in SQL.
- **any_target.** Resolve against any note (cross-notebook, regardless of readability) by
  reusing [WikiLinkResolver](../backend/src/main/java/com/odde/doughnut/services/WikiLinkResolver.java),
  which already handles unqualified (focus-notebook) and qualified (`Notebook:Title`) links.
- **No starvation.** Do not gate when the target is null, deleted, or has
  `skipMemoryTracking` — those targets will never be assimilated, so there is nothing to
  wait for. This exception ships *with* the gate (a gate without it would be a bug), so the
  gate behavior is atomic and cannot be split further by outcome.
- **Counts follow the gate.** The two property stream queries are shared by the queue and
  the counts (`countAssimilable` in
  [UnassimilatedPropertyService](../backend/src/main/java/com/odde/doughnut/services/UnassimilatedPropertyService.java)),
  so a gated property is also excluded from `totalUnassimilatedCount` until its target is
  assimilated. Deliberate single-source decision.

## Discoveries that shape the work

- Existing tests use `[[Word]]` without creating a `Word` note, so target resolves to null
  and they stay green (no gating) — the change is backward compatible.
- `WikiTitleCacheService.refreshForNote(note, viewer)` already calls
  `notePropertyIndexService.refreshForNote(note)` right after rebuilding the wiki title
  cache; keeping the `refreshForNote(Note)` signature unchanged avoids churn across ~30 call
  sites.
- Latest migration is `V300000217`; next free version is `V300000218`. Java migrations share
  the version namespace (precedent: `V300000206` Java seed).
- `MemoryTracker.JPA_WHERE_NOTE_LEVEL_TRACKER` = `(rp.propertyKey IS NULL OR rp.propertyKey = '')`
  is the reusable note-level tracker predicate.

## Stop-safe ordering

- **After Phase 1:** target persisted, nothing reads it yet (structure; existing suite
  green). Paired with Phase 2 — minimal standalone value, so keep it thin.
- **After Phase 2:** full feature for every note saved/edited after deploy. Pre-existing,
  untouched notes keep today's behavior (null target → not gated): a correct, non-buggy
  interim.
- **After Phase 3:** existing data retrofitted; coverage complete.

---

## Phase 1 (Structure) — Persist the wiki-link target on the property index

Enables the gate; no externally observable change.

1. Migration `V300000218__add_target_note_id_to_note_property_index.sql`: add nullable
   `target_note_id int unsigned NULL`, `CONSTRAINT fk_note_property_index_target_note FOREIGN
   KEY (target_note_id) REFERENCES note(id) ON DELETE SET NULL`, and `KEY
   idx_note_property_index_target_note (target_note_id)`. Mirror
   [V300000205__create_note_property_index.sql](../backend/src/main/resources/db/migration/V300000205__create_note_property_index.sql).
2. Add `@ManyToOne(fetch = LAZY, optional = true) targetNote` to
   [NotePropertyIndex](../backend/src/main/java/com/odde/doughnut/entities/NotePropertyIndex.java).
3. Populate in
   [NotePropertyIndexService.refreshForNote](../backend/src/main/java/com/odde/doughnut/services/NotePropertyIndexService.java):
   inject `WikiLinkResolver`; per non-reserved key, read the value via
   `Frontmatter.getString(key)`, take the first inner from
   `WikiLinkMarkdown.innerTitlesInOccurrenceOrder(value)`, resolve to a target note, set it
   on the row (null when no link / unresolved).
4. Regenerate `docs/database-erd.md` (database-erd skill).
5. **Tests / verify:** full backend suite green; add a focused unit test in
   [NotePropertyIndexServiceTest](../backend/src/test/java/com/odde/doughnut/services/NotePropertyIndexServiceTest.java)
   — `key: [[Existing]]` stores the target id; non-link / unresolvable stores null.
6. Commit, push, deploy gate.

## Phase 2 (Behavior) — Gate property units behind a pending target note

Delivers the core value for all notes saved/edited after deploy.

1. **Red:** add scenarios to
   [AssimilationServicePropertyUnitsTest](../backend/src/test/java/com/odde/doughnut/services/AssimilationServicePropertyUnitsTest.java):
   - owned target note pending → property not offered as next unit *and* excluded from
     `totalUnassimilatedCount`; after the target is assimilated → property offered.
   - target in a different notebook (any_target) → still gated.
   - target with `skipMemoryTracking` (and a deleted target) → property offered (no
     starvation).
   Confirm they fail for the right reason.
2. **Green:** extend both property streams in
   [NotePropertyIndexRepository](../backend/src/main/java/com/odde/doughnut/entities/repositories/NotePropertyIndexRepository.java)
   via the shared `unassimilatedJoinMemoryTracker` / `unassimilatedWhereClause` constants:
   `LEFT JOIN i.targetNote t`, `LEFT JOIN t.memoryTrackers tmt ON tmt.user.id = :userId AND
   tmt.deletedAt IS NULL AND (tmt.propertyKey IS NULL OR tmt.propertyKey = '')`, and gate
   with `AND (t IS NULL OR t.deletedAt IS NOT NULL OR COALESCE(t.recallSetting.skipMemoryTracking,
   FALSE) = TRUE OR tmt IS NOT NULL)`. Leave `unassimilatedOrderBy` unchanged. (One edit
   covers both owned and subscription streams.)
3. **Verify:** new tests pass; full backend suite green.
4. Commit, push, deploy gate.

## Phase 3 (Data) — Backfill existing property rows

Retrofits pre-existing data so the gate applies without requiring a re-save.

1. Java Flyway migration
   `V300000219__backfill_note_property_index_target_note.java` (mirror
   [V300000206 Java seed](../backend/src/main/java/db/migration/V300000206__seed_note_property_index_and_skipped_property_trackers.java)):
   for each `note_property_index` row, parse the owning note's content, extract the property
   value's first wiki-link inner, and resolve the target by matching `note_wiki_title_cache
   (note_id, link_text)` → `target_note_id`; `UPDATE` the row. Idempotent; leaves non-link
   properties null.
2. **Verify:** migration test confirms link rows get a target and non-link rows stay null.
3. Commit, push, deploy gate.

## Out of scope

Frontend property *display* order
([noteContentPropertyRows.ts](../frontend/src/utils/noteContentPropertyRows.ts)) is
alphabetical and unrelated to assimilation order.
