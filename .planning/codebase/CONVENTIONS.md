# Coding Conventions

**Analysis Date:** 2026-07-15

## Naming Patterns

**Files:**
- **Backend Java:** PascalCase class files matching the type — e.g. `BooksController.java`, `NoteBuilder.java`, `ClozeDescriptionTest.java`. Package path mirrors `com.odde.doughnut.*` under `backend/src/main/java/` and `backend/src/test/java/`.
- **Frontend Vue:** PascalCase component files — e.g. `NoteShow.vue`, `Modal.vue`, `LoadingModal.vue` in `frontend/src/components/`. TypeScript modules use camelCase or descriptive names — e.g. `clientSetup.ts`, `useStorageAccessor.ts`.
- **Frontend tests:** Match the component/capability with a `.spec.ts` suffix — e.g. `LoadingModal.spec.ts`, `RecallPage.spec.ts` under `frontend/tests/`.
- **CLI / MCP:** Production under `cli/src/`, `mcp-server/src/`; tests use `.test.ts` / `.test.tsx` under `cli/tests/`, `mcp-server/tests/`.
- **E2E:** Capability-named Gherkin — e.g. `note_creation.feature`, `spaced_repetition.feature` in `e2e_test/features/`. Never name by phase number. Step defs in `e2e_test/step_definitions/`; page objects in `e2e_test/start/pageObjects/`.
- **Scripts:** Descriptive `.sh` names in `scripts/`; Bach unit tests as `scripts/test/<name>.test`.
- **Permanent artifacts:** Name by product capability (notes, assimilation, recall), not delivery history or phase numbers. See `.cursor/rules/planning.mdc`.

**Functions / methods:**
- **Java:** camelCase methods; test methods describe behavior — e.g. `shouldBeAbleToSaveNoteWhenValid`, `rejectsNotebookWithoutReadAccess`, `returnsPdfWhenSourceFileRefPointsAtBlob` in controller tests.
- **TypeScript:** camelCase functions; Vue event handlers often `handle*` or domain verbs; composables prefixed `use*` — e.g. `useStorageAccessor`, `usePopups`.
- **E2E page objects:** Methods name user actions and return the next page object (fluent) — e.g. `startAConversationAboutNote()`.

**Variables:**
- camelCase in Java and TypeScript.
- Prefer `const` (Biome `useConst: error`).
- Frontend API calls: destructure with meaningful names — `const { data: updatedUser, error } = await ...` (see `.cursor/rules/frontend-api.mdc`).

**Types:**
- **Java:** PascalCase classes/interfaces; DTOs often `*DTO` under `controllers/dto/`; domain entities under `entities/`.
- **TypeScript:** PascalCase for types/interfaces imported from `@generated/doughnut-backend-api`; prefer generated API types over hand-rolled wire shapes.
- Vue props via `defineProps<{ ... }>()`; emits via `defineEmits<{ ... }>()`.

**Test selectors:**
- Prefer `data-testid` on new UI (e.g. `data-testid="book-reading-page"`). Some older recall UI still uses `data-test` (e.g. `data-test="assimilate"`). E2E and unit tests query these attributes; do not add production OCR/DOM text layers only for tests.

## Code Style

**Formatting:**
- **TypeScript / Vue / JS (frontend, CLI, MCP, E2E, fixtures):** Biome `@biomejs/biome@2.5.3`. Indent 2 spaces, line width 80. Configs: root `biome.json` (workspace), `frontend/biome.json` (`"root": false`, Vue rules).
- **Java:** Spotless + Google Java Format via `backend/build.gradle` (`spotless { java { googleJavaFormat() } }`). `pnpm backend:format` / `pnpm backend:lint` (Spotless check).
- During development prefer `CURSOR_DEV=true nix develop -c pnpm format:all` (fixes most issues). Use `pnpm lint:all` for CI-style validation. Set `CI=true` when running `lint:all` non-interactively.

**Linting:**
- Biome rules: preset `none` with an explicit correctness/complexity/style set in `biome.json` / `frontend/biome.json`.
- OpenAPI: Redocly via `pnpm openapi:lint` on generated `open_api_docs.yaml` — fix controllers, then `pnpm generateTypeScript`; never hand-edit generated API files.
- Biome suppressions use slash form: `// biome-ignore lint/<GROUP>/<RULE>: reason`.
- Do **not** enable Biome project-domain / MFA rules (e.g. `noImportCycles`, `noFloatingPromises`) in root `biome.json` — Vue SFC MFA resolution breaks (see `.cursor/rules/linting_formating.mdc`).
- `suspicious/noUnusedExpressions` is off for `e2e_test/**` (Chai property assertions).

**Java import style:**
- Always use top-of-file imports; do not use inline FQCNs except for name collisions (`.cursor/rules/backend-code.mdc`).

## Import Organization

**Order (observed practice):**
1. External / framework packages (`vue`, `vitest`, Spring, JUnit).
2. Generated API (`@generated/doughnut-backend-api/...`).
3. Internal aliases (`@/…`, `@tests/…`) or same-package relatives.
4. Type-only imports where Biome warns (`useImportType`).

**Path aliases:**
- Frontend: `@` → `frontend/src`, `@tests` → `frontend/tests` (see `frontend/vitest.config.ts`).
- Generated SDK: `@generated/doughnut-backend-api/sdk.gen` and types from `@generated/doughnut-backend-api`.
- Shared fixtures: `doughnut-test-fixtures/makeMe` only — do not import the bare package name or deep `src/` paths.
- CLI / MCP resolve `@generated/doughnut-backend-api` via Vitest aliases to `packages/generated/doughnut-backend-api`.

**Organize imports:** Biome `organizeImports` assist is **off** in both biome configs — do not rely on auto-sort; keep imports tidy manually.

## Error Handling

**Patterns:**
- **Backend HTTP:** `@ControllerAdvice` in `CustomRestExceptionHandler.java` and `ControllerSetup.java` map exceptions to `ApiError` / status codes (validation, integrity, OpenAI auth, multipart, etc.). Domain access failures throw `UnexpectedNoAccessRightException` (asserted in controller tests with `assertThrows`).
- **Frontend API:** Client uses `responseStyle: "fields"` and `throwOnError: false`. Always check `error` before using `data`. User-initiated calls go through `apiCallWithLoading` in `frontend/src/managedApi/clientSetup.ts` (loading bar + error toasts). Field validation: `toOpenApiError(error)` from `frontend/src/managedApi/openApiError.ts`. Silent background fetches call the SDK directly without `apiCallWithLoading`.
- **Whole-UI blocking:** `apiCallWithLoading(..., { blockUi: true, message? })` or `runWithBlockingApiLoading` — do not add component-local `LoadingModal` for global blocking; global modal is mounted from `DoughnutApp.vue`.
- **Frontend API ↔ E2E loading:** `apiCallWithLoading` / blocking helpers own `.loading-bar` and `.loading-modal-mask`; Cypress waits with `pageIsNotLoading()` from `e2e_test/start/pageBase.ts` after actions that trigger those calls (`.cursor/rules/frontend-api.mdc`, `.cursor/rules/e2e-authoring.mdc`).
- **CLI:** Map slash-command failures with `userVisibleSlashCommandError`; red transcript lines via `pastAssistantErrorBlock.tsx`.
- **E2E:** Assertions must include expected vs actual and domain meaning; put assertions in page objects when that keeps steps thin (`.cursor/rules/e2e-authoring.mdc`).

## Logging

**Framework:**
- Backend: Spring / Logback (inspect via `pnpm logs:tail backend-e2e` / `sut` / `mountebank` per `.cursor/agent-map.md`). Local MySQL/Redis: `mysql/mysql.log`, `redis/redis.log`.
- Frontend / CLI: prefer domain error surfaces (toasts, user-visible text) over `console` for user-facing failures. Biome may restrict unused / console patterns; use `biome-ignore` only with a reason when needed.

**Patterns:**
- Do not log secrets. Prefer actionable messages at boundaries (API errors, CLI stdout).

## Comments

**When to Comment:**
- Prefer self-explanatory names and small functions over comments.
- Do not add comments about past implementations or phase history (`.cursor/rules/general.mdc`).
- Use comments for non-obvious constraints (e.g. Ink/`useEffect` exit ordering in CLI rules, Biome MFA Vue limitation).

**JSDoc/TSDoc:**
- Used sparingly on shared test helpers (e.g. `mockSdkService` in `frontend/tests/helpers/index.ts`). Not required on every export.

## Function Design

**Size:**
- Prefer small, focused units. Post-change refactor skill targets splitting files larger than ~250 lines into cohesive modules (`.cursor/skills/post-change-refactor/SKILL.md`).
- One observable behavior per test method / `it` block.

**Parameters:**
- Prefer typed options objects for API/SDK calls (`{ body, path, query }` from generated client).
- Vue: `defineProps` + `withDefaults` for optional props; avoid mirroring props into writable `ref`s — use `computed` for derived state (`.cursor/rules/frontend-component.mdc`).

**Return Values:**
- Controllers: prefer returning entities (or existing API body types) with `@JsonIgnore` / `@JsonView`; introduce DTOs only when the wire shape differs (`.cursor/rules/backend-code.mdc`).
- Frontend SDK: always treat return as `{ data, error, request, response }`.
- E2E page objects: return next page object or `this` for fluent chains.
- CLI modules: export only what other modules need — do not export “for tests” (`.cursor/rules/cli.mdc`).

## Module Design

**Exports:**
- Keep public surfaces small (especially CLI). Test through high-level entry points (`run`, `runInteractive`, controllers, mounted components) rather than widening exports for unit tests.

**Barrel Files:**
- Frontend tests re-export helpers from `frontend/tests/helpers/index.ts` (`helper`, `mockSdkService`, `wrapSdkResponse`, …).
- MCP tools registry in `mcp-server/src/tools/index.ts`.
- Prefer capability-local helpers over giant barrels.

**Vue components:**
- Use `<script setup lang="ts">` structure shown in `.cursor/rules/frontend-component.mdc`.
- Styling: DaisyUI with `daisy-` prefix + unprefixed Tailwind; Lucide icons via `@lucide/vue`; scoped SCSS when needed. Avoid Bootstrap.
- Modals: use `Modal` from `frontend/src/components/commons/Modal.vue` (`close_request`, optional `showCloseButton`, `sidebar` / `alignTop`).

**High cohesion:**
- Minimize duplication; one representation per concept; place related code together (`.cursor/rules/general.mdc`).
- Do not add speculative structure or defensive layers unless observed behavior requires them.

## Tooling Commands (prescriptive)

```bash
CURSOR_DEV=true nix develop -c pnpm format:all   # prefer while developing
CURSOR_DEV=true nix develop -c pnpm lint:all     # CI validation (set CI=true in non-TTY)
CURSOR_DEV=true nix develop -c pnpm generateTypeScript  # after backend API signature changes
```

Git commands run **without** the Nix prefix.

---

*Convention analysis: 2026-07-15*
