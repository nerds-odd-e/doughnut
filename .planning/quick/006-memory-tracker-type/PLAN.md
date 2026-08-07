# Memory tracker `type` field

Replace boolean `memory_tracker.spelling` with enum column `type`:
`UNDERSTANDING` (default) | `SPELLING` | `COMMISSIONED`.

Migrate production rows, switch domain code to `type`, rebuild uniqueness,
drop `spelling`. Keep API wire `MemoryTrackerLite.spelling` as a **derived**
boolean (`type == SPELLING`) so recall UI needs no change in this plan.

**Location:** `.planning/quick/006-memory-tracker-type/` (ad-hoc; unlocks
milestone Phase 1 commissioned trackers without a separate boolean column).

**Supersedes:** GSD Phase 1 plan’s boolean `commissioned` + unique-key-on-boolean
approach — use `type=COMMISSIONED` instead when that milestone resumes.

---

## Design decisions (locked)

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Storage | `VARCHAR(32)` + JPA `@Enumerated(STRING)` | Matches `AnswerOutcome` / batch status enums |
| Values | `UNDERSTANDING`, `SPELLING`, `COMMISSIONED` | Caller-specified; UNDERSTANDING = today’s `spelling=false` note-level and property trackers |
| Property trackers | Stay `UNDERSTANDING` + non-empty `property_key` | No fourth enum; uniqueness still uses `property_key` |
| Backfill | `spelling=1` → `SPELLING`; else → `UNDERSTANDING` | Faithful production migrate; no COMMISSIONED rows yet |
| Unique key | Rebuild `user_note_spelling_active` to use `type` instead of `spelling`; **keep index name** | Same coexistence model; spelling=0/1 → type UNDERSTANDING/SPELLING; COMMISSIONED can coexist later |
| API `spelling` | Keep derived on lite + entity JSON | Stop-safe for frontend/E2E |
| Drop `spelling` column | Yes, final Structure phase | Single representation |

---

## Phase 1: Add `type`, backfill, dual-write with `spelling`

Type: Structure  
Status: done

**Learnings:** OpenAPI regenerated because `type` surfaces on `MemoryTracker`. Dual-write collapsed onto `setType` (`setSpelling` delegates). `setSpelling(false)` maps to `UNDERSTANDING` only — fine until callers stop writing via spelling (Phase 2).

**Structure change:** Flyway `V300000238__add_memory_tracker_type.sql` (or next free
version > `300000237`): `ADD COLUMN type VARCHAR(32) NOT NULL DEFAULT 'UNDERSTANDING'`;
`UPDATE memory_tracker SET type = 'SPELLING' WHERE spelling = 1`. Entity
`MemoryTrackerType` enum + `type` field default `UNDERSTANDING`. Dual-write:
`setType` / `setSpelling` / builder `.spelling()` keep both columns consistent.
Unique key still on `spelling` this phase.

**Unlocks next:** Code can read `type` without breaking uniqueness or existing
`spelling` SQL.

**Verify:** Controller or entity-boundary unit: ordinary fixture →
`UNDERSTANDING`; `.spelling()` fixture → `SPELLING`. `pnpm backend:verify`.

---

## Phase 2: Domain code reads/writes `type` (keep derived `spelling`)

Type: Structure  
Status: planned

**Structure change:** Switch production callers of `getSpelling`/`setSpelling` on
`MemoryTracker` to `getType`/`setType` (or `isSpelling()` / `isCommissioned()`
helpers). Touch at least:

- `MemoryTrackerService` (create assimilate / property paths)
- `MemoryTrackerRepository` native SQL (`ORDER BY` / batch `spelling IS FALSE` → type)
- `NotePropertyIndexRepository`, `NotePropertyTrackingBackfill`
- `NoteService`, `RecallQuestionService`, `MemoryTrackerController`
- `RecallService` lite mapping via `isSpelling()`
- Tests/builders: `.spelling()` sets `type=SPELLING`; prefer asserting type where
  the scenario is about tracker kind

Entity: prefer `type` as source of truth; `getSpelling()` / `setSpelling` either
derived sync or thin wrappers so accidental call sites still work until Phase 3.

**Unlocks next:** Safe to rebuild unique key and drop column.

**Verify:** `pnpm backend:verify` green; no behavior change for assimilate/recall.

---

## Phase 3: Unique key on `type` + drop `spelling` column

Type: Structure  
Status: planned

**Structure change:** One-way Flyway: `DROP INDEX user_note_spelling_active`;
`ADD UNIQUE KEY user_note_spelling_active (user_id, note_id, type, property_key,
(if((deleted_at is null),1,NULL)))`; `DROP COLUMN spelling`. Remove persisted
`spelling` field from entity; keep `@JsonProperty` / lite `spelling` **derived**
from `type == SPELLING`. Regenerate `docs/database-erd.md` (`database-erd` skill).
Regenerate OpenAPI client if Springdoc surfaces `type` on `MemoryTracker`.

**Observable Structure proof (unit):** Same note can persist UNDERSTANDING +
SPELLING (existing) and UNDERSTANDING + COMMISSIONED (makeMe) without unique
violation — unlocks milestone commissioned work.

**Verify:** `pnpm backend:verify`; ERD contains `type`; no `spelling` column on
`memory_tracker`.

---

## Out of scope

- Creating COMMISSIONED trackers via assimilate UI / Learning Session (milestone)
- Excluding COMMISSIONED from due-recall / batch (milestone Phase 1 after this)
- Renaming frontend fields from `spelling` to `type`
- Renaming DB index `user_note_spelling_active`

---

## Testing notes

- Prefer high-level entry points (`RecallsController`, `AssimilationController`)
  with `makeMe`; extend builders, don’t mock repositories.
- At most one intentionally failing test while driving a phase.
- Capability naming only — no phase numbers in product/test names.

## Commands

```bash
CURSOR_DEV=true nix develop -c pnpm backend:verify
CURSOR_DEV=true nix develop -c pnpm export:database-erd   # after Phase 3 schema
CURSOR_DEV=true nix develop -c pnpm generateTypeScript      # if OpenAPI changed
```
