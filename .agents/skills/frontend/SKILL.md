---
name: frontend
description: Vue frontend — components, Vitest, generated API client, apiCallWithLoading, DaisyUI. Read this first for any frontend work. Use when working on the Vue frontend.
paths:
  - "frontend/**"
---
# Frontend

Vue 3 + TypeScript in `frontend/src/`. Tests in `frontend/tests/`. Import the backend client from `@generated/donut-backend-api/sdk.gen`.

Matching `frontend-component`, `frontend-testing`, `frontend-api`, and `frontend-storybook` skills attach via `paths` when those files are in context — do not fetch them up front. Lint: `linting_formating` skill.

## Commands

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test
```

Spec paths are relative to `frontend/`. Do not pass `--` before the path (Vitest skips file filtering). Prefer the full frontend unit suite; a single file only while iterating on that file.

## Frontend proof

`frontend:test` runs Vitest only; `frontend/vitest.config.ts` disables
typechecking there for speed. Passing behavioral tests alone is incomplete
frontend proof: before accepting a frontend change, also require this
typecheck to pass against the same working-tree content, including generated
API types:

```bash
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
```

Reuse existing typecheck evidence from a `lint` or `build` run on
identical content instead of repeating it. This requirement applies at proof
acceptance, not to every focused Vitest run while iterating on one file.

## Components

- `<script setup lang="ts">`. PascalCase `.vue` files; tests `ComponentName.spec.ts`.
- DaisyUI uses the `daisy-` prefix (`daisy-btn`, `daisy-card`). Unprefixed Tailwind utilities. No Bootstrap. Scoped SCSS. Lucide via `@lucide/vue` (`currentColor`, `:size` or Tailwind `size-*`).
- Derived state: `computed`, not a `ref` that mirrors props/store. `ref` only for owned mutable state (input, toggles).
- Modals: `Modal` from `@/components/commons/Modal.vue`. In tests, query `document.querySelector("dialog")`.
- Routing: name and params (helpers such as `noteShowLocation`); href only via `noteShowHref` (compiled from the named location). ADR 0005.

## API

User actions: `apiCallWithLoading(() => Controller.method(...))` from `@/managedApi/clientSetup`. Wrapped `{ data, error, request, response }`. Check `!error` before using `data`. Silent refresh: call the SDK directly (no loading bar or error toast).

`{ blockUi: true }` for a whole-UI blocker. `{ blockUi: true, cancelable: true }` only for allowlisted read-only sites gated by `frontend/tests/managedApi/cancelableAllowlist.spec.ts`. Do not opt mutations into cancelable. Do not add component-local `LoadingModal` refs.

Loading UI that means unfinished work marks `data-app-busy` (`LoadingThinBar`, `ContentLoader`, `LoadingModal`). E2E: `waitUntilAppIsNotBusy()`. Validation 400: `toOpenApiError(error)`.

## Tests

Drive the mounted component. Mock only the backend HTTP API with `mockSdkService(Controller, "method", payload)` from `@tests/helpers`. Payloads: `donut-test-fixtures/makeMe`. No `getByRole` (slow). Prefer `render()` from Testing Library. Vitest browser mode; do not use jsdom.
