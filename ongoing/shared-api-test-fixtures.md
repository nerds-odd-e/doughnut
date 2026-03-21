# Shared API test fixtures (frontend, MCP, CLI)

**Goal:** One implementation of `makeMe` and the `*Builder` types that mirror `@generated/doughnut-backend-api`, consumed by **frontend**, **mcp-server**, and **cli** unit tests — same types as production API clients.

**Current state**

- Implementation: `packages/doughnut-test-fixtures` (`exports`: **`./makeMe` only** → `src/makeMe.ts`).
- Frontend tests and Storybook: `import makeMe from "@tests/fixtures/makeMe"` (alias to the package via `frontend/tsconfig.json`, Vite, Vitest).
- MCP and CLI tests: `import makeMe from 'doughnut-test-fixtures/makeMe'`.

**Constraints**

- Builders stay **pure TypeScript**: no Vue, no browser-only APIs, so Vitest browser mode and Node stay happy.
- `pnpm-workspace.yaml` lists packages explicitly — the fixtures package is registered there.
- Prefer **observable / integration tests** at package boundaries when adopting in MCP/CLI; do not replace good CLI TTY tests with structure-mapped builder-only tests (see `.cursor/rules/planning.mdc`).

---

## Phase 1 — Workspace package + move code + frontend unchanged at import sites

**Status:** Done — fixtures in `packages/doughnut-test-fixtures`; `@tests/fixtures/*` and Vite/Vitest aliases point there.

**User-visible outcome:** Frontend tests and Storybook still pass; fixtures live in one workspace package.

1. Add `packages/doughnut-test-fixtures`.
2. Register it in `pnpm-workspace.yaml`.
3. `package.json`: dependency on `@generated/doughnut-backend-api` (`workspace:*`); `private: true`; `exports` for `makeMe`.
4. `tsconfig.json`: map `@generated/doughnut-backend-api` to `../generated/doughnut-backend-api`.
5. Move former `frontend/tests/fixtures/**` into the package; keep relative imports between builders working.
6. Frontend: `tsconfig` paths + Vite/Vitest/Storybook so `@tests/fixtures/*` resolves to the package.
7. `frontend` devDepends on `doughnut-test-fixtures` (`workspace:*`).
8. Remove duplicate fixtures under `frontend/tests/fixtures/`.
9. **Gate:** `pnpm frontend:test` (and Storybook if in CI) green; format/lint as usual.

---

## Phase 2 — MCP server tests consume shared fixtures

**Status:** Done — `mcp-server` devDepends on `doughnut-test-fixtures`; tests use `makeMe` where it helps.

**User-visible outcome:** At least one MCP test builds inputs/responses using `makeMe` instead of ad-hoc objects.

1. Add `doughnut-test-fixtures` as **devDependency** of `mcp-server`.
2. Configure Vitest/TS so imports resolve (`doughnut-test-fixtures` exports + generated API).
3. Refactor at least one realistic scenario to use `makeMe`.
4. **Gate:** `pnpm mcp-server:test` green.

---

## Phase 3 — CLI tests consume shared fixtures

**Status:** Done — `cli` devDepends on `doughnut-test-fixtures`; recall/process tests use `makeMe` where it helps.

**User-visible outcome:** CLI tests that need API-shaped data use the same builders as frontend/MCP.

1. Add `doughnut-test-fixtures` as **devDependency** of `cli`.
2. Wire Vitest/TS resolution (consistent with MCP).
3. Replace inline mock objects where they duplicate builder semantics.
4. **Gate:** `pnpm cli:test` green.

---

## Phase 4 — Documentation and hygiene

**Status:** Done — `.cursor/rules/frontend.mdc` and `CLAUDE.md` describe `packages/doughnut-test-fixtures`, the `@tests/fixtures` alias, and MCP/CLI imports; CI `Other-unit-tests` uses recursive `pnpm install`, so the workspace package is always present for `frontend:test` / `mcp-server:test` / `cli:test`.

**User-visible outcome:** Contributors know where fixtures live; no duplicate builders.

1. Update cursor rules (frontend / Storybook): implementation vs alias vs package import paths.
2. Root discoverability in `CLAUDE.md` (directory tree + frontend conventions).
3. Confirm CI installs the full workspace before those test scripts.
4. **Keep this plan** until phases 5–7 are done (or explicitly deprioritized); then remove or archive.

---

## Phase 5 — Explicit Biome coverage for `doughnut-test-fixtures`

**Status:** Done — `packages/doughnut-test-fixtures` has `format` / `lint` (Biome); root `test-fixtures:format` / `test-fixtures:lint`; `format:all` and `lint:all` run them.

**User-visible outcome:** Formatting and lint for the fixtures package are impossible to miss in scripts and docs; no reliance on “it happens to be inside root `**`.”

**Context:** Root `biome.json` currently includes `**` with specific excludes; `packages/doughnut-test-fixtures` is not excluded, so `pnpm cy:format` / `pnpm cy:lint` (repo-root Biome) already touch it. This phase makes that **explicit** in the developer workflow.

1. Add root scripts (or package-local scripts) that run Biome **only** on `packages/doughnut-test-fixtures` (e.g. `biome check --write packages/doughnut-test-fixtures` and `biome check packages/doughnut-test-fixtures`), and wire the format script into `pnpm format:all` and the lint script into `pnpm lint:all` (or document a single dedicated `test-fixtures:format` / `test-fixtures:lint` if you prefer not to lengthen `format:all` — then at minimum document it in `CLAUDE.md` and run it in CI for the same job that runs frontend tests).
2. If package-local scripts are chosen, add `@biomejs/biome` as a **devDependency** of `doughnut-test-fixtures` (mirror `mcp-server` / small packages) so `pnpm -C packages/doughnut-test-fixtures format` works without guessing hoisting.
3. **Gate:** `pnpm format:all` and `pnpm lint:all` green; touching a file under `packages/doughnut-test-fixtures` with a deliberate style violation is caught by the new explicit step.

---

## Phase 6 — Stricter public surface on `doughnut-test-fixtures`

**Status:** Done — `exports` lists **`./makeMe` only**; `CLAUDE.md` and `frontend.mdc` document the public surface.

**User-visible outcome:** The supported entrypoint for consumers is **`makeMe` only**; individual `*Builder` modules are internal implementation details unless you deliberately export them later.

**Context:** The package previously exported both `"."` and `"./makeMe"`; only `./makeMe` remains. No consumer used the bare package name (verified before the change). Internal builders live under `src/` and stay outside `exports`.

1. Remove the redundant **`"."`** export if nothing needs it; keep **`"./makeMe"`** (and document that as the only supported public API).
2. Grep the repo (and any external docs) for `from 'doughnut-test-fixtures'` / `from "doughnut-test-fixtures"` without a subpath; fix any usage to `doughnut-test-fixtures/makeMe`.
3. Optionally add a short comment at the top of `src/makeMe.ts` or in package README (only if the repo convention allows — prefer updating `CLAUDE.md` / `frontend.mdc` only to avoid duplicate long docs).
4. **Gate:** `pnpm frontend:test`, `pnpm mcp-server:test`, `pnpm cli:test` green after export tightening.

---

## Phase 7 — Uniform imports (frontend uses the package name)

**Status:** Not started.

**User-visible outcome:** Every consumer imports the same specifier: `doughnut-test-fixtures/makeMe` (no special-case `@tests/fixtures` alias for `makeMe`).

**Context:** Large mechanical diff across `frontend/tests/**`, `frontend/src/**/*.stories.ts`, and `frontend/.storybook/preview.ts` (~70+ files today). MCP and CLI already use the package import.

1. Replace `import makeMe from "@tests/fixtures/makeMe"` (and `MakeMe` variant) with `import makeMe from "doughnut-test-fixtures/makeMe"` everywhere under `frontend/`.
2. Remove **`@tests/fixtures/*`** from `frontend/tsconfig.json` paths and the matching **`@tests/fixtures`** alias blocks from **`frontend/vite.config.ts`** and **`frontend/vitest.config.ts`** (and Storybook config if it duplicates alias config outside tsconfig).
3. Ensure `frontend/package.json` keeps **`doughnut-test-fixtures`: `workspace:*`** as a devDependency so TypeScript and bundlers resolve the package name.
4. Update `.cursor/rules/frontend.mdc` and `CLAUDE.md` to describe **only** `doughnut-test-fixtures/makeMe` for new code (keep one line that `@tests/*` still means `frontend/tests/` for helpers).
5. **Gate:** `pnpm frontend:test`, Storybook build/smoke if CI runs it, `pnpm format:all` / `pnpm lint:all` green.

**TDD note:** No new behavior — regression is “all green after import churn.”

**Deploy gate:** Commit/push after this phase if team practice requires it before unrelated work.

---

## Risks / reminders

- **OpenAPI churn:** When generated API types change, update builders in **one** place (`packages/doughnut-test-fixtures`).
- **Phase 7 size:** Prefer a single focused PR or scripted find-replace plus CI to avoid merge pain.
- **Phase 6 + 7 order:** Doing **Phase 6** before **Phase 7** keeps export cleanup small; **Phase 5** is independent and can ship anytime.
