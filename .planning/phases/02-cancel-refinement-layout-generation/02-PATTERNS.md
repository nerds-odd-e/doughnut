# Phase 2: Cancel Refinement Layout Generation - Pattern Map

**Mapped:** 2026-07-21
**Files analyzed:** 4 (3 product/test + 1 optional rule)
**Analogs found:** 4 / 4

## Scope Boundary

This Behavior phase opts **only** initial AI refinement-layout generation into the Phase 1 cancelable contract, plus the empty/cancelled retry surface required by REFN-02. Do **not** change extract-preview / create-note / remove loading options, redesign `LoadingModal` / Overlay / Cancel visuals, regenerate `@generated/doughnut-backend-api`, or expand `runWithBlockingApiLoading`.

Shared cancel mechanics (`clientSetup.ts`, `ApiStatusHandler.ts`, `LoadingModal.vue`, `GlobalApiLoadingModal`) are **read-only references** — already shipped in Phase 1.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `frontend/src/components/recall/NoteRefinement.vue` | component | request-response + cancel status narrowing | Same file: `loadRefinementLayout` + extract-preview retry button | exact continuation |
| `frontend/tests/components/recall/NoteRefinement.layoutGeneration.cancel.spec.ts` | browser component test | pending gate + Cancel DOM + post-cancel UI | `NoteRefinement.removeLayout.loading.spec.ts` + `LoadingModal.spec.ts` Cancel click | role-match (compose two) |
| `frontend/tests/components/recall/noteRefinementTestSupport.ts` | test fixture / helpers | deferred mount + modal queries | Same file: `createDeferredGate`, `loadingModalMask`, `mountNoteRefinement*` | exact |
| `.cursor/rules/frontend-api.mdc` (optional) | config / docs | documentation | Same file: `blockUi` loading section | exact continuation |

## Pattern Assignments

### `frontend/src/components/recall/NoteRefinement.vue` (component, request-response + cancellation)

**Analog (call site to replace):** current thin-bar `loadRefinementLayout` (lines 235-253).

```typescript
const loadRefinementLayout = async () => {
  try {
    const result = await apiCallWithLoading(() =>
      AiController.generateRefinementSuggestions({
        path: { note: props.note.id },
      })
    )

    refinementLayoutItems.value =
      !result.error && result.data?.items ? result.data.items : []
    clearSelection()
    resetExtractionPreview()
  } catch (err) {
    console.error("Failed to generate note layout:", err)
    refinementLayoutItems.value = []
    clearSelection()
    resetExtractionPreview()
  }
}
```

**Contract to adopt (Phase 1 overload — do not reimplement):** `frontend/src/managedApi/clientSetup.ts` lines 41-55 and 48-50:

```typescript
export type CancelableApiLoadingOptions = ApiLoadingOptions & {
  blockUi: true
  cancelable: true
}

export type CancelableApiResult<T> =
  | { status: "completed"; result: T }
  | { status: "cancelled" }

export function apiCallWithLoading<T extends SdkResult>(
  apiCall: (signal: AbortSignal) => Promise<T>,
  options: CancelableApiLoadingOptions
): Promise<CancelableApiResult<T>>
```

**Core adoption pattern** (replace thin-bar call; narrow `status` before unwrapping SDK fields — D-03):

```typescript
const outcome = await apiCallWithLoading(
  (signal) =>
    AiController.generateRefinementSuggestions({
      path: { note: props.note.id },
      signal,
    }),
  {
    blockUi: true,
    message: "AI is generating layout...",
    cancelable: true,
  }
)

if (outcome.status === "cancelled") {
  refinementLayoutItems.value = []
  clearSelection()
  resetExtractionPreview()
  return
}

const { data, error } = outcome.result
refinementLayoutItems.value =
  !error && data?.items ? data.items : []
clearSelection()
resetExtractionPreview()
```

**Do not emit on this path:** `contentUpdated` is only used after remove/create mutations (e.g. lines 300-301). Cancel must leave emit unused.

**Leave unchanged (Phase 3/4 boundaries):**

- Extract preview: `runWithBlockingApiLoading(..., "AI is generating preview...")` (lines 341-354)
- Create note: `runWithBlockingApiLoading` + noncancelable `createExtractedNote` (lines 404+)
- Remove: `runWithBlockingApiLoading(..., "AI is removing content...")` (lines 279-303)

---

**Analog (empty / cancelled retry UI):** extract-preview retry control (lines 136-142) + populated layout shell (lines 1-10).

```vue
<button
  data-test-id="retry-extraction-preview"
  class="daisy-btn daisy-btn-ghost daisy-btn-sm"
  @click="retryExtractionPreview"
>
  Ask AI to retry
</button>
```

```vue
<div
  v-if="refinementLayoutItems.length > 0"
  class="mb-4 rounded-lg bg-accent p-4"
  data-test-id="refinement-layout"
>
  <div class="font-semibold mb-3 text-accent-content">
    Note layout:
  </div>
```

**UI-SPEC empty panel** (new branch when `items.length === 0` after an attempt; keep dialog open — parent `AssimilationSettings` owns Close):

```vue
<div
  v-else
  class="mb-4 rounded-lg bg-accent p-4"
  data-test-id="refinement-layout-empty"
>
  <div class="font-semibold mb-3 text-accent-content">
    Note layout:
  </div>
  <button
    data-test-id="retry-refinement-layout"
    class="daisy-btn daisy-btn-ghost daisy-btn-sm"
    @click="loadRefinementLayout"
  >
    Ask AI to retry
  </button>
</div>
```

Notes for planner:

- Today the root is a single `v-if="refinementLayoutItems.length > 0"` — empty state is invisible. Phase 2 must add an explicit empty branch (or equivalent) so Cancel is not a blank dialog.
- Reuse copy `Ask AI to retry` and ghost-sm styling; use **distinct** `data-test-id="retry-refinement-layout"` (not `retry-extraction-preview`).
- One shared empty surface for cancel, zero-item success, and cleared-after-error (UI-SPEC / RESEARCH A2).
- Prefer minimal Behavior change; file is already ~437 lines — extract only if post-change-refactor requires layout-load/retry cohesion.

---

### `frontend/tests/components/recall/NoteRefinement.layoutGeneration.cancel.spec.ts` (browser component test)

**Primary analog:** `NoteRefinement.removeLayout.loading.spec.ts` — deferred gate + `loadingModalMask` + message assertion (full file pattern):

```typescript
import { AiController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it } from "vitest"
import { mockSdkServiceWithImplementation, wrapSdkError } from "@tests/helpers"
import {
  createDeferredGate,
  loadingModalMask,
  mountNoteRefinementReady,
  setupNoteRefinementTests,
} from "./noteRefinementTestSupport"

setupNoteRefinementTests()

// Gate before action; assert mask + message; resolve; assert mask gone
const successGate = createDeferredGate()
mockSdkServiceWithImplementation(
  AiController,
  "removeRefinementSuggestion",
  async () => {
    await successGate.gate
    return { content: "Updated content" }
  }
)
expect(loadingModalMask()).toBeTruthy()
expect(document.body.textContent).toContain("AI is removing content...")
successGate.resolve()
await flushPromises()
expect(loadingModalMask()).toBeNull()
```

**Cancel click analog:** `LoadingModal.spec.ts` lines 181-198 — Testing Library `getByText("Cancel")` + native `.click()` through `GlobalApiLoadingModal`:

```typescript
const { getByText } = render(GlobalApiLoadingModal)
// ... cancelable apiCallWithLoading pending ...
await nextTick()
expect(getByText("Newest cancelable blocker")).toBeTruthy()
const newestCancelButton = getByText("Cancel")
newestCancelButton.click()
await expect(newestCall).resolves.toEqual({ status: "cancelled" })
```

**Pending-layout mount (critical difference from remove/extract loading specs):** `mountNoteRefinementReady` always `await flushPromises()` after mount (support lines 221-224), which completes layout generation before Cancel can appear. Install a deferred gate on `generateRefinementSuggestions` **before** mount and **do not** flush until after Cancel assertions.

**Targeted cancel-spec shape** (compose analogs + RESEARCH Pattern 3):

```typescript
setupNoteRefinementTests()

describe("NoteRefinement layout generation cancel", () => {
  it("shows blocking Cancel, then empty retry without applying late data", async () => {
    const { gate, resolve } = createDeferredGate()
    mockSdkServiceWithImplementation(
      AiController,
      "generateRefinementSuggestions",
      async () => {
        await gate
        return { items: refinementLayoutItems(["Should not appear"]) }
      }
    )

    const wrapper = mountNoteRefinementPendingLayout() // helper: mount, no flush
    await nextTick()

    expect(loadingModalMask()).toBeTruthy()
    expect(document.body.textContent).toContain("AI is generating layout...")
    getByText("Cancel").click() // document / Testing Library on teleported overlay
    await flushPromises()

    expect(loadingModalMask()).toBeNull()
    expect(wrapper.find('[data-test-id="retry-refinement-layout"]').exists()).toBe(true)
    expect(wrapper.find('[data-test-id="refinement-layout"]').exists()).toBe(false)
    expect(wrapper.find('[data-test-id="refinement-layout-empty"]').exists()).toBe(true)

    resolve() // late settlement must not populate layout
    await flushPromises()
    expect(wrapper.find('[data-test-id="refinement-layout"]').exists()).toBe(false)
  })
})
```

**Coverage map for this file (capability-named, not phase-numbered):**

| Req | Assert |
|-----|--------|
| REFN-01 | Pending: mask + `AI is generating layout...` + `Cancel` |
| CANC-01 | Cancel present only because layout opts in; leave existing extract/remove loading specs proving those paths unchanged |
| CANC-02 | Cancel clears layout mask promptly; late resolve does not apply items |
| CANC-03 | No toast (optional `vue-toastification` mock); no `contentUpdated` emit; dialog path stays open |
| CANC-04 | Older concurrent blocker survives layout Cancel (mirror `LoadingModal.spec.ts` nested cancelable/noncancelable cases — product-level) |
| REFN-02 | After cancel: `retry-refinement-layout` visible; click starts a fresh `generateRefinementSuggestions` |

**Toast silence analog (optional product-seam assertion):** `clientSetup.spec.ts` lines 11-18:

```typescript
const mockToast = {
  error: vi.fn(),
  warning: vi.fn(),
}
vi.mock("vue-toastification", () => ({
  useToast: () => mockToast,
}))
```

Do **not** re-implement shared race/latch coverage already in `clientSetup.loading.spec.ts`.

---

### `frontend/tests/components/recall/noteRefinementTestSupport.ts` (test helpers)

**Analog:** existing helpers in the same file.

**Global loading harness** (lines 26-38) — already correct for Cancel; keep composing `GlobalApiLoadingModal`:

```typescript
const NoteRefinementWithGlobalLoading = defineComponent({
  components: { GlobalApiLoadingModal, NoteRefinement },
  // ...
  template: `
    <NoteRefinement
      :note="note"
      @contentUpdated="$emit('contentUpdated', $event)"
    />
    <GlobalApiLoadingModal />
  `,
})
```

**Deferred gate + mask** (lines 210-219) — reuse as-is:

```typescript
export function createDeferredGate() {
  let resolveGate!: () => void
  const gate = new Promise<void>((resolve) => {
    resolveGate = resolve
  })
  return { gate, resolve: () => resolveGate() }
}

export const loadingModalMask = () =>
  document.querySelector(".loading-modal-mask")
```

**Ready mounts that flush** (lines 221-233) — keep for existing specs; add a **pending** variant that does not flush:

```typescript
export async function mountNoteRefinementReady(layoutItemTexts: string[]) {
  const wrapper = mountNoteRefinement(layoutItemTexts)
  await flushPromises()
  return wrapper
}

// NEW: install deferred generateRefinementSuggestions gate before mount;
// return { wrapper, resolve } without awaiting flushPromises
```

**Retry click analog** (lines 477-482) — mirror for layout retry:

```typescript
export async function clickRetryExtractionPreview(
  wrapper: ReturnType<typeof mountNoteRefinement>
) {
  await wrapper
    .find('[data-test-id="retry-extraction-preview"]')
    .trigger("click")
}

// NEW: clickRetryRefinementLayout → find('[data-test-id="retry-refinement-layout"]')
```

**Cancel click helper:** prefer Testing Library / `document` `getByText("Cancel")` (overlay teleports to `body`) — same as `LoadingModal.spec.ts`. Optionally wrap as `clickLoadingModalCancel()` if multiple specs need it.

---

### `.cursor/rules/frontend-api.mdc` (optional docs)

**Analog:** same file Loading indicators section (lines 31-45) — documents `{ blockUi: true, message? }` only today.

If included in this phase (RESEARCH open question recommendation): add a short subsection for literal `{ blockUi: true, cancelable: true }` that:

- Returns `CancelableApiResult` requiring `status` narrowing before `result`
- Projects Cancel on the global modal via selected-state action
- States accepted cancel is silent (no toast) and identity-bound

Do not invent a parallel loading API or document AbortError-name matching.

## Shared Patterns

### Opt-in cancelable call (caller only)

**Source:** `clientSetup.ts` cancelable overload (lines 41-55) + RESEARCH Pattern 1.  
**Apply to:** `loadRefinementLayout` and its retry click only.

- Callback receives one `AbortSignal`; pass it into generated `Options`.
- Options must be literal `{ blockUi: true, cancelable: true, message }`.
- Branch `if (outcome.status === "cancelled") return` before any layout apply / emit.

### Global blocker + Cancel (do not rebuild)

**Source:** Phase 1 `LoadingModal` + `GlobalApiLoadingModal` (already in test harness).  
**Apply to:** Product via options only; tests assert teleported mask + `getByText("Cancel")`.

### Empty / retry is domain-local (D-12)

**Source:** extract-preview `Ask AI to retry` button (NoteRefinement.vue lines 136-142).  
**Apply to:** layout empty panel with distinct test id; keep AssimilationSettings dialog open.

### Verification conventions

- Vitest browser mode; `mockSdkService` / `mockSdkServiceWithImplementation`; avoid role queries.
- No fixed waits/polling; controlled deferred gates.
- Focused command:

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/NoteRefinement.layoutGeneration.cancel.spec.ts
```

- Regression smoke (unchanged paths): existing `NoteRefinement.extractNote.spec.ts` loading cases + `NoteRefinement.removeLayout.loading.spec.ts`.
- Phase gate: `CURSOR_DEV=true nix develop -c pnpm frontend:verify`.
- No Cypress E2E required unless explicitly requested.

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| — | — | — | None. Empty-layout retry is a new branch inside an existing component; Cancel/pending-gate patterns exist. Optional `frontend-api.mdc` update continues the existing docs section. |

## Metadata

**Analog search scope:** `frontend/src/components/recall/NoteRefinement.vue`, `frontend/tests/components/recall/*`, `frontend/tests/components/commons/LoadingModal.spec.ts`, `frontend/src/managedApi/clientSetup.ts`, `frontend/tests/managedApi/clientSetup*.spec.ts`, `.cursor/rules/frontend-api.mdc`, Phase 1 `01-PATTERNS.md` style.

**Files scanned:** ~15 focused + Phase 1 pattern map  
**Pattern extraction date:** 2026-07-21
