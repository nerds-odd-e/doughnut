# Shared API test fixtures (frontend, MCP, CLI)

**Goal:** One implementation of `makeMe` and the `*Builder` types that mirror `@generated/doughnut-backend-api`, consumed by **frontend**, **mcp-server**, and **cli** unit tests — same types as production API clients.

**Current state**

- `frontend/tests/fixtures/` (~30 modules + `makeMe.ts`) imports types from `@generated/doughnut-backend-api`.
- Consumers are almost entirely `import makeMe from "@tests/fixtures/makeMe"` (plus Storybook); no direct builder imports from app code.
- `mcp-server` and `cli` already depend on `doughnut-api` → generated API; tests today barely use rich API-shaped objects (room to adopt builders as tests grow).

**Constraints**

- Builders must stay **pure TypeScript**: no Vue, no browser-only APIs, so Vitest browser mode and Node stay happy.
- `pnpm-workspace.yaml` lists packages explicitly — add the new package there.
- Prefer **observable / integration tests** at package boundaries when adopting in MCP/CLI; do not replace good CLI TTY tests with structure-mapped builder-only tests (see `.cursor/rules/planning.mdc`).

---

## Phase 1 — Workspace package + move code + frontend unchanged at import sites

**User-visible outcome:** Frontend tests and Storybook still pass; fixtures live in one workspace package.

1. Add `packages/doughnut-test-fixtures` (name TBD; align with existing `doughnut-api` naming).
2. Register it in `pnpm-workspace.yaml`.
3. `package.json`: dependency on `@generated/doughnut-backend-api` (`workspace:*`); `private: true`; minimal `exports` (e.g. `./makeMe` and/or `.` → `src/makeMe.ts`).
4. `tsconfig.json`: map `@generated/doughnut-backend-api` to `../generated/doughnut-backend-api` (same pattern as frontend).
5. Move all of `frontend/tests/fixtures/**` into the new package `src/` (or agreed layout); keep relative imports between builders working.
6. **Frontend wiring (no mass import edits):** Point `frontend/tsconfig.json` paths so `@tests/fixtures/*` resolves to the new package (e.g. `../packages/doughnut-test-fixtures/src/*`). Mirror the same resolution in Vite / Vitest / Storybook configs if anything does not follow `tsconfigPaths` alone.
7. Add `doughnut-test-fixtures` as a **devDependency** of `frontend` (`workspace:*`) for correct install graph and tooling.
8. Delete the old files under `frontend/tests/fixtures/` (avoid duplicate `makeMe`).
9. **Gate:** `pnpm frontend:test` (and Storybook build/list if part of CI) green; `pnpm format:all` / lint as usual.

**TDD note:** Regression is “everything still green”; optional tiny test inside the new package only if it locks an invariant (e.g. `makeMe.aNote.please()` shape) without duplicating frontend specs.

**Deploy gate:** Commit/push before Phase 2 if that is team practice.

---

## Phase 2 — MCP server tests consume shared fixtures

**User-visible outcome:** At least one MCP test (or new focused test) builds inputs/responses using `makeMe` instead of ad-hoc objects.

1. Add `doughnut-test-fixtures` as **devDependency** of `mcp-server`.
2. Configure Vitest/TS so imports resolve (`doughnut-test-fixtures` package exports + generated API).
3. Refactor **one** realistic scenario (e.g. tool handler test or API response fixture) to use `makeMe`; extend tests only where it improves clarity or catches API drift.
4. **Gate:** `pnpm mcp-server:test` green.

---

## Phase 3 — CLI tests consume shared fixtures

**User-visible outcome:** CLI tests that need API-shaped data use the same builders as frontend/MCP.

1. Add `doughnut-test-fixtures` as **devDependency** of `cli`.
2. Wire Vitest/TS resolution (consistent with MCP).
3. Replace inline mock objects **where** they duplicate builder semantics (e.g. recall/note payloads); keep CLI-specific harnesses as-is.
4. **Gate:** `pnpm cli:test` green.

---

## Phase 4 — Documentation and hygiene

**User-visible outcome:** Contributors know where fixtures live; no duplicate builders.

1. Update **cursor rules** that mention `@tests/fixtures/makeMe` only (frontend.mdc, Storybook section): state that implementation lives in `packages/doughnut-test-fixtures` and `@tests/fixtures` is a frontend **alias** (or document `import … from 'doughnut-test-fixtures/makeMe'` for non-frontend if you standardize on package imports later).
2. Optionally add a one-line pointer in `CLAUDE.md` table (if you want root discoverability).
3. Confirm CI jobs that run frontend + MCP + CLI tests all install the workspace package.
4. Remove this `ongoing/` file when the work is done and rules are updated.

---

## Optional later (only if needed)

- **Uniform imports:** Migrate frontend from `@tests/fixtures/makeMe` to `doughnut-test-fixtures/makeMe` and drop the path alias (large diff; do only if clarity wins).
- **Stricter package API:** Export only `makeMe` from the package public surface and keep builders internal if you want a smaller contract.
- **Biome scope:** If root `format:all` does not cover the new package, add it explicitly.

---

## Risks / decisions to make during Phase 1

- **Exact package name** and `exports` shape (must work with Vitest browser bundler + Node).
- **Whether** `doughnut-test-fixtures` gets its own `format` script or relies on root Biome — match repo patterns.
- **Generated API churn:** When OpenAPI regen changes types, builders may need updates; that is desirable (same pain as today, but in one place).
