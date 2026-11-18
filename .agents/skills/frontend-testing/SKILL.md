---
name: frontend-testing
description: Frontend Vitest browser-mode tests, Testing Library queries, mockSdkService, makeMe builders, deterministic Vue component testing. Use when writing or changing frontend Vitest tests.
paths:
  - "frontend/tests/**/*.ts"
  - "frontend/src/**/*.spec.ts"
---
# Frontend Testing Rules

Follow the [unit-testing skill](../unit-testing/SKILL.md) for shared guidance
and Donut proof conventions.

These commands cover behavioral testing only. Before accepting frontend proof,
also pass the typecheck required by the `frontend` skill's "Frontend proof" rule.

## Test Commands

From the repo root:

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test
```

All frontend unit-test commands use headless Chromium. Enable a visible browser
only at the user's request.

The legacy UI command also runs headlessly:

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test:ui
```

Run tests in headless watch mode (the Vitest UI remains available at its printed
URL without opening a browser):

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test:watch
```

Run a single test file from the repo root:

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/path/to/TestFile.spec.ts
```

Or only the frontend package:

```bash
CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/path/to/TestFile.spec.ts
```

Run a specific test case:

```bash
CURSOR_DEV=true nix develop -c pnpm -C frontend test -t "test name pattern"
```

The test file path is relative to `frontend/`. Do not use `pnpm ... test -- tests/...`; the `--` is forwarded to Vitest and file filtering is skipped.

For `dough-test-optimization`, `frontend:test` remains the ordinary local
feedback measurement because it runs Chromium browser mode. When per-test JSON
durations are needed for investigation, use
`CURSOR_DEV=true nix develop -c pnpm -C frontend exec vitest run --reporter=json`.
That plain Vitest run is profiling evidence only; verify retained changes with
the normal browser-mode command and do not compare its wall time directly with
`frontend:test`.

## Allowed Mocks

`vi.mock` is limited to these modules; everything else in-process runs for real:

- `@/managedApi/AiReplyEventSource`: the streaming backend API (same boundary as `mockSdkService`).
- `@/models/audio/audioRecorder`, `@/models/audio/recorderWorklet`, `@/models/wakeLocker`: microphone, audio worklet, and screen wake lock, which headless Chromium lacks.
- `file-saver`: starts a browser download.
- `pdfjs-dist` (gesture-zoom support file only): the PDF engine and worker, whose page geometry the spec must control.

Real in-process code, observed through shared helpers:

- Popups: the real `usePopups` stack, emptied before each test; answer and inspect with `@tests/helpers/popupStackTestSupport`.
- Router: `productionRouterAt(location)` and `countHistoryEntriesAdded(router)` from `@tests/helpers`; assert the router's current named location.
- Toasts: `showToastsOnPage()` from `@tests/helpers/toastTestSupport`; assert the toast on the page.

## Routing assertions

Navigation assertions use named locations (or helpers that return them).
Rendered-href assertions use `noteShowHref`. Path strings belong only in
`routes.spec.ts` (matching / redirects) and inbound URL classifiers. Test
routers that resolve named screen locations use production `routes` or
`dummyRouteRecordsFromMetadata` (the `routeMetadata` table with dummy
components; no page imports). A spec's router over production `routes`
comes from `productionRouterAt(location)` (from `@tests/helpers`) or plain
`withRouter()`; every test starts at the root location, reset in shared test
setup. Catch-all `/` or `/:pathMatch(.*)*` routers
and `useRoute` stubs with `path: "/"` are not a second screen dialect
(ADR 0005).

## Avoid Role Queries

- Do not use `getByRole`, `findByRole`, `queryByRole`, `getAllByRole`, and similar queries; they are slow due to expensive visibility checks.
- Testing Library recommends role queries for accessibility, but this project prioritizes test performance.
- Use faster alternatives: `getByText`, `getByLabelText`, `getByTitle`, or `querySelector` / `querySelectorAll`.

## Mock SDK Services

- Use `mockSdkService` from `@tests/helpers` for type-safe mocking: pass the generated **controller class** and the **method name** (same static methods as `@generated/donut-backend-api/sdk.gen`).
- It automatically wraps responses in the standard format `{ data, error, request, response }`.
- It returns a spy that can be reconfigured in tests.
- Use `wrapSdkResponse` when updating mock return values.
- Use `mockSdkServiceWithImplementation` only for custom async logic based on options.
- Arrange response payloads through [test-data guidance](#test-data-guidance).

```typescript
import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { mockSdkService } from "@tests/helpers"

beforeEach(() => {
  mockSdkService(NoteController, "getRecentNotes", [])
  mockSdkService(NoteController, "showNote", makeMe.aNoteRealm.please())
})
```

```typescript
import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"

const spy = mockSdkService(NoteController, "showNote", makeMe.aNoteRealm.please())
spy.mockResolvedValue(wrapSdkResponse(differentNote))
```

```typescript
import { TextContentController } from "@generated/donut-backend-api/sdk.gen"
import { mockSdkServiceWithImplementation } from "@tests/helpers"

mockSdkServiceWithImplementation(TextContentController, "updateNoteContent", async (options) => {
  return await someAsyncOperation(options)
})
```

## Component Props

- Use `helper.component(ComponentName).withStorageProps` if the component requires a `storageAccessor` prop.
- Use `withProps` instead of `withStorageProps` if the component does not require `storageAccessor`.

```typescript
const wrapper = helper
  .component(Component)
  .withStorageProps({ value: initialValue })
  .mount()

await wrapper.setProps({ value: newValue })
```

## Test-data guidance

Use [Dough MakeMe](../dough-makeme/SKILL.md) to select shared fixture guidance
for the test source.

The frontend's API-shaped fixtures live in `packages/donut-test-fixtures`.
Import `donut-test-fixtures/makeMe` only, rather than the bare package or a
`src/` deep path. For example:

```typescript
import makeMe from "donut-test-fixtures/makeMe"

const note = makeMe.aNoteRealm.please()
const answered = makeMe.anAnsweredQuestion
  .accidentalMatch("alias", [note.note.noteTopology])
  .please()
```

## Browser Mode Mounting

- Use Vitest browser mode with Chromium.
- Use `render()` from `@testing-library/vue` for most tests. Query the DOM.
  Use `data-testid` for test-specific selectors.
- Use `mount()` from `@vue/test-utils` when the contract under proof requires
  an observation such as an emitted event, a slot, or a provided value.

## Layout and Pointer Tests

- Prove that text is visible with geometry: `scrollWidth <= clientWidth`, and range or element rectangles inside their row. Checking `textContent` alone misses clipped text.
- Browser specs use production CSS. Set the container width, or call `page.viewport` for breakpoint (`sm:`) behavior; the default viewport is 414px wide.
- Name a spec `*.touch.spec.ts` when it needs a real coarse pointer. The `chromium-touch` browser instance in `vitest.config.ts` runs those files with touch emulation, so `(pointer: coarse)` matches. Do not substitute a `matchMedia` stub.
