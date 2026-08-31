# Note level as frontmatter `note_level`

**Status:** in progress (slices 1–2 done)

## Goal

`note_level` in note YAML is the source of truth for assimilation queue order. A small indexed cache table stays in sync for “next to assimilate.” The Assimilation settings radios go away. Legacy `note.level` is migrated into both YAML and the cache, then dropped.

## Live system (today)

- `note.level` is a `tinyint NOT NULL DEFAULT 0` column, mapped as embedded `NoteRecallSetting` on `Note`.
- Assimilation order (SQL and in-memory `AssimilationUnit.ORDER`) is `level`, then `createdAt`, then `id` (then property grain).
- Users set it with radios **0–6** labeled “Level” inside Assimilation settings (`NoteRecallSettingForm` → `POST /api/notes/{note}/recall-setting`).
- E2E: `e2e_test/features/assimilation/edit_when_assimilating.feature` (“Update recall level while assimilating”) opens those radios.
- Other frontmatter caches already refresh on content save/create via `ResolvedWikiLinkRefresh` (`note_property_index`, `note_alias_index`).
- Structural / passthrough keys (including `note_level`) are excluded from `note_property_index` and automatic property trackers. `note_level_index` already refreshes on content save; queue still reads `note.level` until slice 4.

## Requirements

- Property name: `note_level` (case-insensitive lookup, like other Donut keys). Canonical authored spelling is `note_level`.
- Valid stored values: integers **1–6** only. Absent key ⇒ level **0**. `note_level: 0` and any other shape/value **cannot be saved**.
- Frontmatter is the true copy. `note_level_index` is a derived cache used to find the next assimilation unit.
- Migration backfills cache **and** frontmatter from legacy `note.level` for 1–6.
- Remove the level control from Assimilation settings (memory-tracker table and assimilate/skip actions stay).

## Design decisions

Locked 2026-08-31.

| Topic | Decision | Why |
| --- | --- | --- |
| Cache table | `note_level_index (note_id PK, level TINYINT NOT NULL, CHECK (level BETWEEN 1 AND 6), FK note ON DELETE CASCADE)` | Same 1–6 constraint as the property. **No row ⇒ 0.** Matches `note_alias_index` / `note_property_index`. |
| Sync | Parse `note_level` on every note content create/save (same `refreshForNote` hook as alias/property indexes). Valid 1–6 upserts the row; absent/removed deletes it. | Frontmatter remains source of truth; keep index refresh cohesive. |
| Queue (narrow) | Level is **only** for next-to-assimilate. SQL `LEFT JOIN` + `COALESCE` stays on the unassimilated queries. `AssimilationUnit` carries the cached level for `ORDER` (owned vs subscribed heads). **Do not** add `Note.noteLevel()`, expose level on `NoteRecallInfo`/`Note` JSON, or JOIN FETCH the cache for ordinary note load. | Getting next to assimilate is the only consumer; other note uses should not see it. |
| Invalid save | Reject the **whole** note content write (aliases/overlaps pattern in `AuthoredNoteContent`). Message on field `note_level`. Rich editor blocks commit the same way. | ADR 0006: make invalid state unrepresentable. Do not strip or coerce. |
| Zero | Never store `note_level: 0`. Absence = 0. Legacy column `0` is **dropped** (no YAML key, no cache row). An authored `note_level: 0` cannot be saved. | Confirmed. |
| Accepted YAML | Unquoted integer `1`–`6` and digit strings `"1"`–`"6"`. Lists, maps, floats, booleans, blank, `0`, `7`, suffixed `note_level 2` are invalid. | SnakeYAML numbers stringify (`2` → `"2"`). Donut-authored YAML writes unquoted integers. |
| Indexing | Reserved structural key: **not** in `note_property_index`, **no** property tracker, **no** assimilate/skip on that row. Still a normal editable property. | Confirmed — otherwise migrated notes get a fake assimilable property. |
| Setting UX after radios | Note-only preset `note_level` + typing a scalar. **No 1–6 picker.** | Confirmed. |
| Interim radios | Until radios go, `updateNoteRecallSetting` dual-writes frontmatter + cache. | Existing E2E stays green between slices. |
| Backfill | Column 1–6 → cache row + verbatim `note_level: N` if YAML has no valid key. Column 0 / out-of-range → drop (no key, no row). Valid YAML already present wins; cache from YAML. Skip soft-deleted. | ADR 0004: do not re-dump the fence. |
| `makeMe.aNote().level(n)` | 1–6 writes frontmatter + cache refresh; 0 omits the key. | Queue tests keep driving next-to-assimilate only. |
| Readme | Note-only. Not a preset; not cached. | Cache is per note row. |
| Drop column | Last slice, after nothing reads `NoteRecallSetting` / `note.level`. | Stop-safe. |
| Glossary | Optional human amendment to [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md) (**Note level**). | Agents do not approve ADRs. |

### ADRs

- [0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md) — `note_level` is author-owned YAML; backfill inserts without re-dumping the fence; portable tree round-trips the key.
- [0006](../../../docs/adrs/0006-failure-handling-accepted.md) — CHECK + save rejection; do not silently coerce.
- [0001](../../../docs/adrs/0001-ubiquitous-language.md) — this is a **Property**, distinct from note-level *memory trackers*. No conflict.
- Proposed [0002](../../../docs/adrs/0002-git-native-notebooks-backed-by-mysql.md) (not binding) — moving level off a private column into markdown matches Git-native content.

No Accepted ADR is contradicted.

## Open questions

None. Resolved: no picker; zeros dropped (not stored).

## Out of scope

- Changing assimilation order keys besides level (createdAt / id / property grain).
- Subscriber-specific levels (today the column is on the shared note; YAML is the same).
- Readme / folder levels.
- New ADR (unless you want to draft 0001 glossary text yourself).

## Slices

### 1. Exclude `note_level` from property assimilation — Structure — done

`note_level` is a reserved structural key: not in `note_property_index`, no property tracker, no Assimilation buttons, no `note_level 2` suffix. Frontend hide/singleton uses `isNoteLevelPropertyKey` (generic reserved helper was unused and dropped).

### 2. Cache table and refresh-from-frontmatter — Structure — done

`V300000310__create_note_level_index.sql`; `NoteLevelIndexService.refreshForNote` from `ResolvedWikiLinkRefresh`. Parse via `FrontmatterNoteLevel`. 1:1 upsert/delete (not bulk-replace — Hibernate would keep the old entity). Invalid YAML leaves no cache row. ERD regenerated.

### 3. Backfill cache and frontmatter from legacy `note.level` — Behavior — planned

**Pre:** notes exist with `note.level` in 1–6 (production) and no / invalid YAML key.  
**Trigger:** migrate.  
**Post:** those notes’ markdown contains `note_level: N`; cache has `(note_id, N)`. Level 0 notes unchanged (no key).

- JDBC backfill class (same shape as `NotePropertyTrackingBackfill`) + Flyway Java or SQL+Java after 310.
- Verbatim YAML insert; frontmatter wins when already valid.
- Tests: backfill against seeded JDBC rows (not empty migrateTestDB alone).

**Done when:** backfill tests prove YAML + cache for 1–6 and omit 0 / out-of-range.

### 4. Assimilation order follows the cache (missing = 0) — Behavior — planned

**Pre:** two unassimilated notes; one has `note_level: 2` (or migrated level 2), the other has no key.  
**Trigger:** ask for next to assimilate.  
**Post:** the undefined (0) note is offered first; then lower levels before higher. Equal levels still use `createdAt`, then id.

- Unassimilated JPQL only: `LEFT JOIN` cache, `ORDER BY COALESCE(level, 0), createdAt, id`.
- Put the cached level on `AssimilationUnit` (constructor from those queries). Comparator uses that field, not `Note`.
- `makeMe.level` writes frontmatter + refresh.
- Dual-write `updateNoteRecallSetting` → YAML + cache (keeps radios/E2E working).
- Extend `AssimilationServiceQueueOrderingTest` so the unique claim is frontmatter/cache (canonical order already covered).
- E2E `edit_when_assimilating` still uses radios (interim dual-write). Do not remove `@wip` needs — scenario should stay green.

**Done when:** queue tests pass without reading `note.level`; production migrated rows still order correctly via cache.

### 5. Invalid `note_level` cannot be saved — Behavior — planned

**Pre:** user (or API) sets `note_level` to 0, 7, `"hard"`, a list, etc.  
**Trigger:** save note content.  
**Post:** save fails; markdown and cache unchanged. Absent key still saves and means 0.

- `FrontmatterNoteLevel` (read + `authoredValidationError`) wired in `AuthoredNoteContent`.
- Rich `validatePropertyRowsForRichEdit` / insert commit.
- Controller-level tests on `TextContentController` content patch; frontend row-validation tests.
- Loud DB CHECK remains a backstop for cache writes, not the user-facing message.

**Done when:** invalid YAML cannot persist; valid 1–6 still can.

### 6. Level control leaves Assimilation settings — Behavior — planned

**Pre:** user is on Assimilation settings.  
**Trigger:** look for Level radios / set level while assimilating.  
**Post:** no Level radios; memory trackers and assimilate/skip remain. Level is set by adding/editing `note_level` on the note (preset in the property key dropdown).

- Remove `NoteRecallSettingForm` from `NoteInfoComponent` / settings.
- Add `note_level` to `NOTE_ONLY_PRESET_PROPERTY_KEYS`.
- Retarget E2E `edit_when_assimilating` (and `setLevel` page object) to the property editor; drop `form.getField('Level')`.
- Frontend tests for settings (no radios) and preset list. No specialized picker.

**Done when:** that E2E passes without Assimilation settings radios; preset offers `note_level` on notes, not readme.

### 7. Drop `note.level` and recall-setting API — Structure — planned

Enables nothing user-facing; removes the leftover column after 4–6.

- Drop column; remove `NoteRecallSetting`, `updateNoteRecallSetting`, `recallSetting` on `NoteRecallInfo`.
- `pnpm generateTypeScript`; fixture builders.
- ERD again.

**Done when:** no production code reads `note.level`; existing queue/E2E still pass.

## Testing notes

- Queue and save: backend unit tests at AssimilationService / TextContentController (small-test style).
- Main E2E: `e2e_test/features/assimilation/edit_when_assimilating.feature` (slice 6). Keep it green via dual-write until then.
- Backfill: JDBC tests on the backfill class, not only empty-schema migrate.
- Do not name product files after this plan number.

## Resume

Next slice: **3**. Learnings: cache refresh is 1:1 upsert/delete keyed by `note_id` (not alias-style bulk replace). `FrontmatterNoteLevel` already parses valid 1–6; slice 5 only needs to add save rejection. Backfill should reuse that parse helper and write cache via the same service.
