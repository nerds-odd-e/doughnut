# Note property index + property-tracker backfill

Build the data foundation so that **every** note property is discoverable for memory
tracking without scanning YAML at read time, and seed the existing corpus so only
`example of` properties remain pending while all other existing properties are
explicitly marked as skipped.

This is the groundwork for the **next** behavior (surface untracked properties — e.g.
`example of` — in the assimilation queue), which is **out of scope here** but is what
justifies the structure below.

## Background

- **Phase A (done):** the unassimilated-note query now distinguishes note-level from
  property trackers. `NoteRepository.joinMemoryTracker` joins only note-level trackers
  via `MemoryTracker.JPA_WHERE_NOTE_LEVEL_TRACKER` (`propertyKey IS NULL OR = ''`), so a
  note that has only a property tracker is still assimilable at the note level.
- Property memory trackers already exist end to end (see
  `ongoing/property-memory-tracker.md`, complete): `memory_tracker.property_key`,
  per-property assimilate, property-focused recall.

## Requirement (as given, with confirmed decisions)

1. **Property discovery is for any (content) property**, not just `example of` — but
   **reserved/structural keys are excluded entirely** (see decisions below).
2. **No notebook setting.** Discovery and sync with `note_property_index` is **always on**.
3. **Bulk backfill:**
   - Create an index record for **all current (non-reserved) properties** of every
     (non-deleted) note.
   - Add a **skipped** memory tracker (`removed_from_tracking = true`) for every indexed
     property that does **not already have a memory tracker**, **except `example of`**
     (suffix-aware: `example of`, `example of 2`, …). If a tracker already exists, leave
     it unchanged. `assimilated_at` of the seeded skipped tracker = **now**.

### Confirmed decisions

- **Q1 — Circle-owned notebooks:** **skip** seeding (index rows still created; no seeded
  trackers, since there is no single owner user).
- **Q2 — Reserved/structural keys are EXCLUDED from the index entirely** (and therefore
  never get a skipped tracker): `image`, `image_mask`, `wikidata_id`, `url`,
  `title_pattern`, `question_generation_instruction` (and their legacy camelCase aliases).
  **`example of` is never excluded** — it is always indexed and left pending.
- **Q3 — Subscribers:** seed the **owner only**; per-subscriber handling is future work.

## Grounding (current code)

- **Properties = leading YAML frontmatter** in `note.content`. Backend parsing:
  `algorithms/NoteContentMarkdown.splitLeadingFrontmatter(...)` →
  `algorithms/Frontmatter.keys()` (case-insensitive lookups). No `NoteProperty` entity.
- **Analogous derived-data cache:** `note_wiki_title_cache` + entity `NoteWikiTitleCache`
  + `WikiTitleCacheService.refreshForNote(note, viewer)` (delete-and-reinsert per note).
  Wired into every content-save site:
  - `NoteConstructionService.createRootNoteWithWikidataService` and
    `createNoteFromExtractedSuggestion`
  - `TextContentController.updateNote` (content update)
  - `NoteService.reduceRelationNoteToSourceProperty` and the inbound wiki-link rewrite
  - `WikiTitleCacheService.rewriteInboundWikiLinks` (internal)
- **Tracker observability:** `GET /api/notes/{id}/note-info` →
  `UserService.getMemoryTrackersFor` → `findByUserAndNote` filters only
  `deleted_at IS NULL` (it returns `removed_from_tracking` rows too). So a seeded skipped
  tracker **is observable** through `NoteRecallInfo.memoryTrackers` — the backfill effect
  is testable at the controller boundary.
- **Owner user:** `note → notebook → ownership` (`Ownership.user_id`, nullable when the
  notebook is circle-owned).
- **`memory_tracker`** active uniqueness: `(user_id, note_id, spelling, property_key,
  <deleted_at IS NULL>)`. The unique index counts `removed_from_tracking` rows, so a
  seed insert must skip when **any** non-deleted row exists for
  `(user, note, spelling=0, property_key)`.
- **Naming helpers:** `algorithms/PropertyKeyNaming.propertyKeyBaseAndSuffix` (suffix-aware
  base). No Java `isExampleOf*` / `isReserved*` yet — the frontend equivalents are in
  `frontend/src/utils/noteContentPropertyKeys.ts` (`isExampleOfPropertyKey`,
  `isImagePropertyKey`, `isUrlPropertyKey`, `isWikidataIdPropertyKey`,
  `isReservedIndexOnlyPropertyKey`), plus `image_mask`. Mirror them on the backend
  (suffix- and case-aware, with camelCase aliases).
- **Migrations:** SQL only, latest `V300000204`. Arbitrary-key YAML extraction in SQL is
  impractical; Flyway scans `classpath:db/migration` for **Java** migrations in package
  `db.migration`, so the backfill is a Java migration reusing the static algorithm classes.

## Key design decisions

- **Table `note_property_index`** mirrors `note_wiki_title_cache`:
  `id`, `note_id` (FK → `note(id)` `ON DELETE CASCADE`), `property_key VARCHAR(255)
  NOT NULL`. Unique `(note_id, property_key)`; key on `property_key`. Collation
  `utf8mb4_0900_ai_ci` (case-insensitive, matching `memory_tracker.property_key` and
  frontmatter's case-insensitive key semantics).
- **Sync is delete-and-reinsert per note** from `Frontmatter.keys()`, called at the same
  sites as `wikiTitleCacheService.refreshForNote`. Index every key **except reserved
  structural keys**; `example of` is always indexed.
- **Reserved-key match and `example of` family match** each live in **one** backend place,
  suffix- and case-aware (with camelCase aliases), reused by both the sync and the backfill.
- **Backfill mechanism:** a Java Flyway migration delegating to a **static**
  `run(Connection, Timestamp now)` that uses the pure algorithm classes. The decision
  logic (which keys to index, which to seed-skip) is a **pure, fully unit-tested**
  function; the JDBC plumbing is thin and additionally covered by a connection-level
  integration test (set up notes, run against the test connection, assert).
- **Seed user = notebook owner** (`ownership.user_id`). **Circle-owned notebooks
  (no `user_id`) are skipped** in the backfill (no single owner to assign). *(Assumption —
  see open questions.)*
- **Seeded skipped tracker fields:** `spelling=0`, `property_key=<key>`,
  `removed_from_tracking=1`, `assimilated_at = last_recalled_at = next_recall_at = now`,
  default forgetting-curve index, `recall_count=0`.
- **Idempotency:** index insert is `INSERT IGNORE` on the unique key; seed insert is
  guarded by "no non-deleted tracker for `(user, note, spelling=0, property_key)`".

---

## Phases (commit-sized)

**Commit discipline:** each sub-phase is one commit that compiles and leaves all unit +
touched tests green. Push after each. Deploy gate at each **phase** boundary. Backend:
`CURSOR_DEV=true nix develop -c pnpm backend:verify` (migration phases) /
`backend:test_only` (no-migration phases).

---

### Phase B1 — `note_property_index` table + entity/repository  *(structure)* ✅

Enables B2 (the immediate next phase). No observable behavior on its own.

- Flyway `V300000205__create_note_property_index.sql`: create the table as above.
- Entity `NotePropertyIndex` (+ `@ManyToOne` lazy `note`, `propertyKey`) and
  `NotePropertyIndexRepository` (`findByNote_IdOrderByIdAsc`, `deleteByNoteIdInBulk`).
- Regenerate `docs/database-erd.md` (`database-erd` skill).
- **Check:** `backend:verify` green; existing tests unchanged (no observable difference).
  **Commit.** → no deploy gate (rolls up into B2).

### Phase B2 — Sync `note_property_index` on every content change  *(behavior — internal contract)*

**Outcome:** after any note content save, the note's index rows exactly match its current
**non-reserved** frontmatter keys; deleting the note cascades the rows away.

- **B2a — Reserved-key matcher** *(structure)* ✅: add `PropertyKeyNaming.isReservedStructuralKey(key)`
  mirroring the frontend reserved set (`image`, `image_mask`, `wikidata_id`, `url`,
  `title_pattern`, `question_generation_instruction` + camelCase aliases), suffix/case-aware.
  Unit tests: inputs→outputs incl. suffix and alias variants; `example of` and arbitrary
  content keys are **not** reserved. **Commit.**
- **B2b — Sync service + wiring** *(behavior)* ✅: `NotePropertyIndexService.refreshForNote(note)`
  pessimistic-locks the note, bulk-deletes its rows, and reinserts one row per
  `Frontmatter.keys()` entry **where `!isReservedStructuralKey(key)`** (empty when no
  frontmatter). Call it next to **every** `wikiTitleCacheService.refreshForNote(...)` site
  listed in Grounding. *(Optional structure tidy: if maintaining two parallel call lists
  feels like shotgun surgery, extract a `NoteDerivedDataService.refreshForNote` that fans
  out to both caches — only if it stays a no-behavior-change refactor.)*
  - **Tests (this commit):** service-level integration tests (precedent:
    `WikiTitleCacheServiceTest`) using `makeMe`: save with content + reserved keys → rows
    for content keys only (`example of` included, `image`/`url` excluded); edit a key →
    rows updated; remove a key → its row gone; no frontmatter → no rows; delete note → rows
    cascade. **Commit.** → **Phase B2 deploy gate.**

### Phase B3 — Backfill: index existing properties + seed skipped trackers  *(behavior + data)*

**Outcome:** after migration, every existing note has index rows for all its properties,
and every existing non-`example of` property without a tracker has a **skipped** tracker
for the notebook owner (visible in `note-info`), while `example of` properties remain
pending and pre-existing trackers are untouched.

- **B3a — Pure decision helpers + unit tests** *(structure)* ✅
  - Add `PropertyKeyNaming.isExampleOfFamily(key)` (suffix/case-aware).
  - Add a pure planner (e.g. `PropertyTrackingBackfillPlan.forNote(frontmatterKeys,
    existingNonDeletedPropertyKeys)`) returning `keysToIndex` (non-reserved, via
    `isReservedStructuralKey` from B2a) and `keysToSeedSkipped` (indexed, non-`example of`,
    not already tracked).
  - **Tests:** inputs→outputs — reserved keys excluded from both sets; `example of`/`example
    of 2` indexed but never skip-seeded; case-insensitive already-tracked skip; empty
    frontmatter. **Commit.**
- **B3b — Backfill runner + Java migration + integration test** *(behavior/data)*
  - Static `NotePropertyTrackingBackfill.run(Connection, Timestamp now)`: iterate
    non-deleted notes; parse keys via `NoteContentMarkdown`/`Frontmatter`; apply the B3a
    planner; `INSERT IGNORE` index rows for `keysToIndex`; resolve owner `user_id` via
    `notebook → ownership` (skip seeding when null); for each `keysToSeedSkipped` with no
    non-deleted tracker on `(user, note, spelling=0, property_key)`, insert a skipped
    tracker (fields per design).
  - Java migration `backend/src/main/java/db/migration/
    V300000206__seed_note_property_index_and_skipped_property_trackers.java` calls
    `run(context.getConnection(), now)`. Confirm Flyway discovers it (it runs during tests).
  - **Tests (this commit):** integration test that builds notes (`makeMe`) with content
    properties + reserved keys + some pre-existing trackers, runs `run(connection, now)`
    against the test connection, and asserts via repository/`note-info`: index rows for
    non-reserved keys only; skipped trackers for non-`example of` untracked keys
    (`removed_from_tracking=true`, `assimilated_at == now`); reserved keys neither indexed
    nor seeded; `example of` indexed but left with no tracker; pre-existing trackers
    unchanged; circle-owned notes get index rows but no seeded trackers.
  - **Commit.** → **Phase B3 deploy gate.**

---

## Stop-safe / value ordering

- **B1→B2** stand together: an index with no sync is stale immediately, so B1 is the
  structure for B2 and they share a deploy gate. After B2 the index is a correct, always-on
  derived cache.
- **B3** is the one-time data seeding that makes the future "surface untracked properties"
  behavior safe to ship (no nagging about thousands of `image`/`url` properties) and leaves
  `example of` as the pending learnable unit. Its skipped-tracker effect is observable today
  via `note-info`, so it is not pure waste even before the consuming behavior lands.
- **Next (out of scope):** consume the index to surface untracked properties (starting with
  `example of`) in the assimilation queue / counts.
