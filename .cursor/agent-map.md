# Agent Map

Short navigation index — start here before generated API files or long docs. Skill contracts: `.agents/skills/`.

## Work Areas

- Backend HTTP/API behavior: start in `backend/src/main/java/com/odde/donut/controllers/`, then follow services in `backend/src/main/java/com/odde/donut/services/` and entities/repositories in `backend/src/main/java/com/odde/donut/entities/`. Stack: `backend.mdc` (Cursor auto-attaches `backend-code.mdc` / `backend-testing.mdc`).
- Backend tests: prefer controller-level unit tests under `backend/src/test/java/com/odde/donut/controllers/`; use `makeMe` fixtures and real database transactions ("small test" style: `unit-testing.mdc`). Stack: `backend.mdc`.
- Frontend pages and components: start in `frontend/src/pages/`, `frontend/src/components/`, `frontend/src/composables/`, and `frontend/src/store/`. Stack: `frontend.mdc` (Cursor auto-attaches `frontend-component.mdc` / `frontend-api.mdc` / `frontend-testing.mdc`).
- Frontend tests: use `frontend/tests/`; drive mounted components; mock only the backend API via `mockSdkService()` and build payloads with `donut-test-fixtures/makeMe` (`unit-testing.mdc`). Stack: `frontend.mdc`.
- E2E behavior: start with `e2e_test/features/`, then the matching step definitions in `e2e_test/step_definitions/`, then page objects in `e2e_test/start/`. After UI actions that leave the app busy (`data-app-busy`), wait with `waitUntilAppIsNotBusy()` (paired in `frontend.mdc` / `e2e-authoring.mdc`).
- CLI behavior: start in `cli/src/`; run focused CLI unit tests from `cli/` rather than broad workspace verification. Style: `unit-testing.mdc`; stack details: `cli.mdc`.
- MCP server behavior: start in `mcp-server/`; use `.cursor/rules/mcp-server.mdc` only for MCP-specific build/test details. Style: `unit-testing.mdc`.
- Database schema changes: add a new migration in `backend/src/main/resources/db/migration/`; never edit committed migrations.

## Generated API

Use `packages/generated/donut-backend-api/api-summary.md` as the default endpoint lookup. The larger generated API files are intentionally ignored by default indexing:

- `packages/generated/donut-backend-api/types.gen.ts`
- `packages/generated/donut-backend-api/sdk.gen.ts`
- `open_api_docs.yaml`

For frontend calls, import services from `@generated/donut-backend-api/sdk.gen`. For API-shaped fixtures, import `makeMe` from `donut-test-fixtures/makeMe`. Open `sdk.gen.ts` or `types.gen.ts` only when checking an exact generated signature. After backend controller signature or DTO changes, run:

```bash
CURSOR_DEV=true nix develop -c pnpm generateTypeScript
```

Never hand-edit `packages/generated/donut-backend-api/**` or `open_api_docs.yaml`; regenerate them. For whitespace hygiene, use `scripts/check_diff_whitespace.sh` instead of raw `git diff --check` so generated artifacts are not manually "fixed".

## Commands

Run repo tooling through Nix unless working in a documented Cloud VM path:

```bash
CURSOR_DEV=true nix develop -c <command>
```

**Exception:** `git` commands do not need the Nix prefix — run them directly (e.g. `git status`, `git diff`, `git commit`).

Useful focused checks:

- Backend: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
- Frontend single file: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/path/to/TestFile.spec.ts`
- E2E single feature: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/path/to.feature`
- Log inspection: `CURSOR_DEV=true nix develop -c pnpm logs:tail backend-e2e` (targets: `sut`, `backend-e2e`, `mountebank`)
- Diff whitespace: `scripts/check_diff_whitespace.sh` or `scripts/check_diff_whitespace.sh --cached`
- Format changed working-tree components: `./scripts/run.sh pnpm format:changed`
- Lint changed staged components: `./scripts/run.sh pnpm lint:changed`
- Lint all: `CURSOR_DEV=true nix develop -c pnpm lint:all`
- Format all: `CURSOR_DEV=true nix develop -c pnpm format:all`

Assume `pnpm sut` is already running. If unsure, check `CURSOR_DEV=true nix develop -c pnpm sut:healthcheck`. Do not ask developers to restart services after normal code changes; backend and frontend auto-reload.

## Architectural decisions (ADRs)

- Human propose / discuss / approve: `docs/adrs/README.md`
- Current recommendations: `docs/adrs/*-accepted.md` (read explicitly — under `docs/`)
- Agent use / cite / conflict / maintain: `.agents/skills/adr-awareness/SKILL.md`
- Rule pointer: `.cursor/rules/architecture-decisions.mdc`

## Ignored Reference Material

`docs/` and leftover `ongoing/` files are excluded from default indexing to reduce retrieval noise. Active planning lives in `.planning/` (GSD `phases/`, `quick/`, `STATE`, … — see `gsd-coexistence.mdc`). Test-optimization candidates: `.planning/test-optimization-blacklist.md`. Read `docs/` explicitly when the user asks for docs, a rule points to a document, or an ADR check is required (`docs/adrs/`).

## Planning modes (GSD vs local)

| Mode | Artifacts | Orchestrator |
|------|-----------|--------------|
| Story shaping | `.planning/seeds/SEED-NNN-slug.md` containing ordered candidate stories | **story-decomposition** |
| Formal milestone | `.planning/phases/NN-slug/*-PLAN.md`, STATE, ROADMAP | `/gsd-plan-phase` → `/gsd-execute-phase` → `/gsd-ship` (+ local wrap-up) |
| Ad-hoc | `.planning/quick/NNN-slug/PLAN.md` | **slice-planning** + **execute-plan** |
| Optional refinement | Existing phase/quick PLAN; no new artifact | **slice-plan-refinement** |
| Legacy | `ongoing/*.md` | **execute-plan** only; do not migrate |

Story-decomposition seeds are not executable: select one contained story, then
use slice-planning. Run slice-plan-refinement only when the resulting PLAN is
complex, sizing confidence is low, or execution overruns; straightforward plans
may execute directly. **Hard decomposition quality:** one evaluable outcome at the
current resolution; 3V stories; Behavior/Structure execution leaves —
`problem-decomposition.mdc`. Plan artifact and lifecycle rules: `planning.mdc`.
Do not write new flat `.planning/<name>.md` when `phases/` or `quick/` fits.
**Per-slice wrap-up:** Jidoka → post-change-refactor → API generation when
needed → fresh **format-changed** agent → update plan → commit → push
(**execute-plan**). The pre-commit hook lints staged components without
mutation. Skills emit completion markers (e.g. `## REFACTOR COMPLETE`) for
handoff.
