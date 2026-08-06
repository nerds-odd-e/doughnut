# Frontend / CLI / MCP small-test renovation — context

**Created:** 2026-08-06  
**Scope:** Unit tests only under:

| Package | Path | ~Files |
|---------|------|--------|
| MCP | `mcp-server/tests/` | 6 |
| CLI | `cli/tests/` | 42 |
| Frontend | `frontend/tests/` | 255 |

**Style source:** `.cursor/rules/unit-testing.mdc` plus package rules (`frontend-testing.mdc`, `cli.mdc`, `mcp-server.mdc`).

## Why

Backend unit tests were renovated to the "small test" style. Apply the same bar to the remaining TypeScript unit suites (MCP → CLI → Frontend), stop-safe, capability-named artifacts.

## Out of scope

- Backend Java unit tests (already renovated).
- Cypress E2E / Cucumber features.
- Product feature work.
- **Do not update trunk `.planning/STATE.md`** for this workstream (singleton on main; parallel trunk-based work). Resume via this directory’s `PLAN.md` only.
