# Phase 1: Shared Cancellation Contract - Pattern Map

**Mapped:** 2026-07-21
**Files analyzed:** 8 existing files
**Analogs found:** 8 / 8

## Scope Boundary

This Structure phase changes only the shared loading/cancellation seam and its focused tests. Existing callers keep raw SDK results and expose no Cancel control. Do not modify `NoteRefinement.vue`, other product callers, `LoadingThinBar.vue`, `runWithBlockingApiLoading`, generated API files, or backend code.

## File Classification

| Modified File | Role | Data Flow | Closest Analog | Match |
|---|---|---|---|---|
| `frontend/src/managedApi/ApiStatusHandler.ts` | store / state manager | event-driven lifecycle | Current unique-state lifecycle and newest-blocker selector in the same file | exact |
| `frontend/src/managedApi/clientSetup.ts` | service / request wrapper | request-response + cancellation race | Current `apiCallWithLoading` lifecycle and error path in the same file | exact |
| `frontend/src/DoughnutApp.vue` | app-shell component | reactive event-driven projection | Current selected-blocker/modal wiring in the same file | exact |
| `frontend/src/components/commons/LoadingModal.vue` | component | event-driven UI action | Current overlay/content stack; typed cancel event in `AcceptRejectButtons.vue` | exact continuation |
| `frontend/tests/helpers/GlobalApiLoadingModal.ts` | test fixture / provider | reactive event-driven projection | `DoughnutApp.vue` production wiring | exact |
| `frontend/tests/managedApi/clientSetup.loading.spec.ts` | browser integration test | request-response + concurrent state transitions | Current controlled-promise and exact-cleanup tests in the same file | exact |
| `frontend/tests/managedApi/clientSetup.spec.ts` | browser integration test | request-response error handling | Current wrapped-versus-silent toast tests in the same file | exact |
| `frontend/tests/components/commons/LoadingModal.spec.ts` | component test | event-driven DOM interaction | Current Testing Library and teleported-DOM tests in the same file | exact |

No production or test file needs to be created according to the approved phase research.

## Pattern Assignments

### `frontend/src/managedApi/ApiStatusHandler.ts`

**Role/data flow:** store; synchronous event-driven loading-state lifecycle.

**Analog:** the file's existing identity, cleanup, and selector patterns.

**State/options shape** (lines 1-14):

```typescript
export type ApiLoadingState = {
  id: number
  blockUi?: boolean
  message?: string
}

export type ApiLoadingOptions = {
  blockUi?: boolean
  message?: string
}
```

Add the optional state-bound cancel capability at this seam. Omission remains the noncancelable default; do not store a placeholder/no-op action.

**Unique identity and exact cleanup** (lines 25-39):

```typescript
const loadingState = {
  id: this.nextLoadingStateId++,
  blockUi: options.blockUi,
  message: options.message,
}
this.apiStatus.states.push(loadingState)

finishLoading(loadingState: ApiLoadingState) {
  this.apiStatus.states = this.apiStatus.states.filter(
    (state) => state.id !== loadingState.id
  )
}
```

Cancellation must remove only its original id and be idempotent. Never clear the whole state array or resolve a replacement state at click time.

**Newest blocker selection** (lines 42-50):

```typescript
for (let i = apiStatus.states.length - 1; i >= 0; i--) {
  const state = apiStatus.states[i]
  if (state?.blockUi) return state
}
```

Message, identity, cancelability, and action must all come from this one selected object. Do not add a separate cancellation selector or use `states.some(...)`.

### `frontend/src/managedApi/clientSetup.ts`

**Role/data flow:** service; SDK request-response plus deterministic cancellation race.

**Analog:** the existing `apiCallWithLoading` wrapper.

**Imports/local boundaries** (lines 1-10):

```typescript
import { client as globalClient } from "@generated/doughnut-backend-api/client.gen"
import {
  createClient,
  type Config,
} from "@generated/doughnut-backend-api/client"
import type { ApiLoadingOptions, ApiStatus } from "./ApiStatusHandler"
import ApiStatusHandler from "./ApiStatusHandler"
```

Keep cancellation ownership in `managedApi`; do not add a component composable or edit generated code.

**Default compatibility path** (lines 54-75):

```typescript
export async function apiCallWithLoading<T extends SdkResult>(
  apiCall: () => Promise<T>,
  options: ApiLoadingOptions = {}
): Promise<T> {
  const statusHandler = apiStatusHandler
  if (!statusHandler) return await apiCall()

  const loadingState = statusHandler.startLoading(options)
  try {
    const result = await apiCall()
    if (result.error) handleSdkError(result)
    return result
  } finally {
    statusHandler.finishLoading(loadingState)
  }
}
```

Preserve this as the public default overload: existing calls still return `Promise<T>`, start loading synchronously, use current error handling, and clean up the exact state. Do not expose `T | CancelableApiResult<T>` to existing callers.

**New opt-in contract:** no codebase analog exists for the result union, so use the locked contract from `01-CONTEXT.md` and Pattern 1 in `01-RESEARCH.md`:

```typescript
type CancelableApiResult<T> =
  | { status: "completed"; result: T }
  | { status: "cancelled" }
```

Use a literal opt-in overload whose callback receives one `AbortSignal`; recommend coupling it to `blockUi: true`. `result` must be unavailable until `status` is narrowed. The cancelled variant contains status only.

**Existing error path** (lines 149-162, 177-203):

```typescript
function handleSdkError(result: SdkResult) {
  if (!apiStatusHandler) return
  if (!result.error) return

  const status = result.response?.status
  if (status === 401) return
  if (status === 404) return
  // existing message extraction, toast, and 400 mapping
}
```

Ordinary failures continue through this path. Accepted cancellation must be gated by a private accepted-action latch before `handleSdkError`; do not detect cancellation from `AbortError` and do not add a toast, log, or telemetry event.

**Race pattern:** follow `01-RESEARCH.md` Pattern 2 because there is no existing local race implementation:

- Create one `AbortController` per opted-in call and expose only its signal.
- Attach fulfillment and rejection observers before racing so late settlement is consumed.
- On the exact state's first accepted cancel: latch first, remove that state, abort, and promptly resolve `{ status: "cancelled" }`.
- Gate both completed classification and SDK error handling on the latch so same-turn cancellation wins.
- Keep final cleanup idempotent when cancellation already removed the state.

**Generated transport evidence (read only):** `packages/generated/doughnut-backend-api/client/types.gen.ts` lines 13-14 and 56-66 show request options inherit `RequestInit`; `client/client.gen.ts` lines 42-46 and 83-101 spread those options into `new Request(...)`. Lines 199-221 catch fetch failure into the SDK `error` field under `throwOnError: false`, which is why error-name matching is not the shared contract.

Leave `runWithBlockingApiLoading` (lines 78-87) unchanged.

### `frontend/src/DoughnutApp.vue`

**Role/data flow:** app-shell component; reactive projection of shared state.

**Analog:** current computed selection and global modal binding.

```typescript
// lines 24-33
const apiStatus: Ref<ApiStatus> = ref({ states: [] })
setupGlobalClient(apiStatus.value)
const blockingApiState = computed(() =>
  currentBlockingApiState(apiStatus.value)
)
```

```vue
<!-- lines 68-73 -->
<LoadingThinBar v-if="user && apiStatus.states.length > 0" />
<LoadingModal
  v-if="user"
  :show="!!blockingApiState"
  :message="blockingApiState?.message"
/>
```

Pass only the selected state's optional action and identity through this same binding. Do not re-read `currentBlockingApiState` inside a click handler. Keep the modal mounted while an older blocker remains, allowing its message/control to replace the cancelled newest blocker without a close/reopen transition. `LoadingThinBar` needs no change because it already derives from the same state list.

### `frontend/src/components/commons/LoadingModal.vue`

**Role/data flow:** presentational component; optional user action.

**Primary analog:** its existing blocker stack (lines 1-20):

```vue
<Overlay v-if="show" class="loading-modal-mask" centered dark :z-index="10000">
  <div class="loading-modal-content">
    <div class="daisy-loading daisy-loading-spinner daisy-loading-lg"></div>
    <p class="loading-message">{{ message }}</p>
  </div>
</Overlay>
```

`Overlay.vue` lines 1-16 and 35-55 are authoritative for teleporting to `body`, full-viewport fixed positioning, centering, z-index, and 70% black background. Preserve it; this is not the general dialog component.

**Typed-action analog:** `AcceptRejectButtons.vue` lines 25-46 uses a native Cancel button and typed `cancel` event. Copy only that event/button convention, not its secondary color, small size, disabled state, or card layout.

The approved addition is a native `<button type="button">Cancel</button>` rendered only when the selected state supplies its action, beneath the message in the existing 16px-gap stack. Use neutral ghost styling equivalent to `daisy-btn daisy-btn-ghost text-white`, no icon, no placeholder gap, and a visible white focus outline. Bind/key the optional control to the selected state's identity while leaving the overlay mounted.

Do not add autofocus, Escape/backdrop cancellation, confirmation, `Cancelling...`, disabled-wait state, transition, toast, or explanatory copy.

**Existing visual values** (lines 23-40) remain authoritative: vertical centered flex, `gap: 1rem`, white spinner/message, 18px/500 message, zero margin. DaisyUI supplies the button's standard 40px height and 14px/600 label.

### `frontend/tests/helpers/GlobalApiLoadingModal.ts`

**Role/data flow:** high-level test fixture; production-equivalent reactive projection.

**Analog:** `DoughnutApp.vue`; its current mirror is lines 13-24:

```typescript
const apiStatus = ref<ApiStatus>({ states: [] })
setupGlobalClient(apiStatus.value)
const blockingApiState = computed(() =>
  currentBlockingApiState(apiStatus.value)
)
```

```vue
<LoadingModal
  :show="!!blockingApiState"
  :message="blockingApiState?.message"
/>
```

Mirror the exact new production props/binding, including selected-state identity if used by the modal. Do not create a controller, state copy, or cancellation registry in the helper.

### `frontend/tests/managedApi/clientSetup.loading.spec.ts`

**Role/data flow:** browser integration/contract test; request lifecycle, concurrency, and races.

**Analog:** the current file's real generated-client setup and controlled promises.

```typescript
// lines 1-25
import type { ApiStatus } from "@/managedApi/ApiStatusHandler"
import { apiCallWithLoading, setupGlobalClient } from "@/managedApi/clientSetup"
import { UserController } from "@generated/doughnut-backend-api/sdk.gen"
import createFetchMock from "vitest-fetch-mock"

const fetchMock = createFetchMock(vi)
fetchMock.enableMocks()

beforeEach(() => {
  fetchMock.resetMocks()
  apiStatus.states = []
  setupGlobalClient(apiStatus)
})
```

```typescript
// controlled-promise pattern, lines 103-125
let resolveCall: (value: typeof okApiResult) => void = () => undefined
const promise = apiCallWithLoading(
  () => new Promise((resolve) => { resolveCall = resolve }),
  { blockUi: true, message: "Loading next note..." }
)
expect(apiStatus.states).toEqual([
  expect.objectContaining({ blockUi: true, message: "Loading next note..." }),
])
resolveCall(okApiResult)
await promise
expect(apiStatus.states).toEqual([])
```

Lines 127-188 are the exact analog for nested/concurrent independence: assert ordered messages, settle one operation, and assert only that identity disappears. Lines 191-200 assert thrown rejection still cleans up.

Extend this high-level seam with deterministic cases:

- default call returns raw SDK result and has no cancel action;
- opted-in callback receives one signal; accepted cancel aborts, removes exactly its state, and resolves before the underlying request;
- resolve then cancel in the same turn returns cancelled;
- cancel then late resolve/reject is consumed with no unhandled rejection;
- cancelling newest reveals older immediately; repeated stale action does not remove it;
- older cancelable plus newest noncancelable exposes no selected cancel action;
- `expectTypeOf` proves narrowing is required, with `@ts-expect-error` for `result` on cancelled;
- at least one real generated-controller/fetch-mock case proves signal forwarding.

Use controlled promises, direct expected-vs-actual assertions, and no timers/polling or private latch access. Keep the touched file within the repository's 250-line refactor rule by consolidating overlapping current cases as needed.

### `frontend/tests/managedApi/clientSetup.spec.ts`

**Role/data flow:** browser integration test; SDK error/toast behavior.

**Analog:** current toast mock and wrapped-error tests.

```typescript
// lines 11-18
const mockToast = {
  error: vi.fn(),
  warning: vi.fn(),
}
vi.mock("vue-toastification", () => ({
  useToast: () => mockToast,
}))
```

Lines 38-63 establish the observable baseline: wrapped generated calls toast on ordinary error, while direct generated calls are silent. Add an opted-in accepted-cancellation case that asserts `mockToast.error` remains uncalled, retaining the ordinary error test unchanged. Do not add console/telemetry behavior or internal-error-name assertions.

### `frontend/tests/components/commons/LoadingModal.spec.ts`

**Role/data flow:** browser component test; conditional DOM and click action.

**Analog:** current Testing Library render and document queries (lines 14-22):

```typescript
const { getByText } = render(LoadingModal, {
  props: { show: true, message: "AI is creating note..." },
})
expect(document.querySelector(".loading-modal-mask")).toBeTruthy()
expect(document.querySelector(".daisy-loading-spinner")).toBeTruthy()
expect(getByText("AI is creating note...")).toBeTruthy()
```

The overlay teleports to `document.body`; continue using text/label queries and `document.querySelector`, not role queries or Vue internals. Cover:

- default/noncancelable rendering has spinner/message and no `Cancel` in the DOM;
- cancelable rendering shows exact text `Cancel` and native `.click()` invokes only the bound action/event;
- swapping selected-state props while still shown updates message and action together with one overlay;
- `show: false` remains no overlay/action.

## Shared Integration Patterns

### Identity owns the full lifecycle

`ApiStatusHandler.ts` lines 25-39 and `clientSetup.loading.spec.ts` lines 156-188 establish the rule: one id owns state creation, action, cleanup, and stale-action protection. Never retarget an event to whichever state is current later.

### Existing behavior is the default branch

`clientSetup.ts` lines 54-75 and `clientSetup.spec.ts` lines 38-76 define the compatibility contract: raw SDK return, synchronous loading, centralized ordinary-error toast, and exact final cleanup. Cancellation is literal opt-in only.

### Selection remains derived

`DoughnutApp.vue` lines 24-33 and `GlobalApiLoadingModal.ts` lines 13-18 both use `computed(currentBlockingApiState(...))`. Do not mirror selected message/action/id into refs or watchers.

### One global teleported blocker

`DoughnutApp.vue` lines 68-73, `LoadingModal.vue` lines 1-20, and `Overlay.vue` lines 1-16 keep one global surface. No product component gets a local modal or controller in Phase 1.

### Verification conventions

- Vitest runs in Playwright Chromium browser mode.
- Use real generated calls with `vitest-fetch-mock` for transport wiring and controlled promises for races.
- Use Testing Library text/label/DOM queries; avoid role queries.
- No fixed waits, conditional assertions, or unobserved losing promises.
- Focused tests:

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/managedApi/clientSetup.loading.spec.ts tests/managedApi/clientSetup.spec.ts tests/components/commons/LoadingModal.spec.ts
```

- Static type gate: `CURSOR_DEV=true nix develop -c pnpm frontend:build`.
- Phase gate: `CURSOR_DEV=true nix develop -c pnpm frontend:verify` and repository-prescribed formatting.
- No Phase 1 E2E run is required because no product call site changes.

## No Whole-File Analog Gaps

Every modified file has an exact or continuation analog. The only novel embedded pattern is the accepted-cancellation latch/race inside `clientSetup.ts`; use `01-RESEARCH.md` Pattern 2 and decisions D-04 through D-06. Do not substitute error-name matching, a global registry, or component-local cancellation.

## Metadata

**Analog search scope:** the eight target files plus `Overlay.vue`, `AcceptRejectButtons.vue`, and read-only generated fetch-client transport evidence.

**Pattern extraction date:** 2026-07-21
