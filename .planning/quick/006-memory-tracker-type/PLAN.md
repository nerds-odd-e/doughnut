# Memory tracker `type` field

**Status:** done (2026-08-07)

Replaced boolean `memory_tracker.spelling` with enum `type`:
`UNDERSTANDING` (default) | `SPELLING` | `COMMISSIONED`.

## Delivered

| Phase | Result |
|-------|--------|
| 1 | `V300000238` add `type` + backfill from `spelling`; dual-write |
| 2 | Domain/SQL use `type`; wire `spelling` derived |
| 3 | `V300000239` unique key on `type`; drop `spelling` column |

## Locked decisions (still true)

- Property trackers: `UNDERSTANDING` + `property_key`
- API `MemoryTrackerLite.spelling` / entity JSON `spelling` remain derived (`type == SPELLING`)
- Index name `user_note_spelling_active` kept
- Unlocks milestone Phase 1 via `type=COMMISSIONED` (replans boolean approach)

## Key files

- `MemoryTrackerType`, `MemoryTracker`
- Migrations `V300000238`, `V300000239`
- `MemoryTrackerAssimilation`, `SpellingRecallGrading`
- Builder `.spelling()` / `.commissioned()`
