# Backend small-test renovation — context

**Created:** 2026-08-05  
**Scope:** All backend JUnit tests under `backend/src/test/java/` (~225 files, ~32k LOC).  
**Style source:** `.cursor/rules/unit-testing.mdc` + `.cursor/rules/backend-testing.mdc`.

## Why

The repo now standardizes on **"small test"** style: stable boundary, data over mocks, focused assertions, concise `makeMe`. Existing backend suites predate or only partially follow that. Renovate in place; do not change product behavior.

## Inventory (2026-08-05)

| Package | ~Files | Notes |
|---------|--------|--------|
| `services/` | 104 | Largest cohort; many may duplicate controller coverage |
| `controllers/` | 66 | Preferred HTTP boundary; several 500–860 LOC files |
| `algorithms/` | 17 | Mostly already domain-stable pure contracts |
| `entities/` (+ repos) | 15 | Entity/repo contracts |
| `configs/` | 10 | Framework/config contracts |
| `validators/` | 4 | Validation contracts |
| `utils/`, `factoryServices/`, `integration/`, `testability/` | ~9 | Small; renovate lightly |

Rough smell counts (heuristic, not failures): ~57 files touch Mockito/`@MockitoBean`; ~57 use verbose `creatorAndOwner` / notebook wiring patterns.

## Out of scope

- Frontend / CLI / MCP / E2E suites (separate plans if needed).
- Product feature work or intentional behavior changes.
- Full E2E suite runs (backend unit suite only per phase).
- **Do not update trunk `.planning/STATE.md`** for this workstream (singleton on main; parallel trunk-based work). Resume via this directory’s `PLAN.md` progress log only.
