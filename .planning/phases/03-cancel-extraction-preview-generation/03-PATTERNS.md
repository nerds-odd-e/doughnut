# Phase 3: Cancel Extraction Preview Generation - Pattern Map

**Mapped:** 2026-07-21
**Files analyzed:** 3 (1 product + 1 new cancel spec + 1 extraction test support; layout-loading helpers reused as-is)
**Analogs found:** 3 / 3

## Scope Boundary

This Behavior phase opts **only** AI extraction-preview generation into the Phase 1 cancelable contract (`REFN-03`, `REFN-04`). Do **not** change create-note / remove / layout loading options, redesign `LoadingModal` / Cancel visuals, invent a cancelable `runWithBlockingApiLoading`, add a layout empty-state for preview cancel, or claim server-side AI stop.

Shared cancel mechanics (`clientSetup.ts`, `LoadingModal`, `GlobalApiLoadingModal`) and Phase 2 layout cancel are **read-only references** — copy the caller-adoption shape, not the empty-layout post-cancel domain behavior.

**Critical Phase 2 delta:** Layout cancel clears items and shows `refinement-layout-empty` + `retry-refinement-layout`. Preview cancel must be a **domain no-op**: keep `selectedItemIds`, stay on layout (Extract path) or keep prior preview fields (retry path), retry via existing **Extract** / **Ask AI to retry**.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `frontend/src/components/recall/NoteRefinement.vue` | component | request-response + cancel status narrowing | Same file: `loadRefinementLayout` (Phase 2 cancelable call) — replace `fetchExtractionPreview` + `runWithBlockingApiLoading` composite | exact role; domain post-cancel differs |
| `frontend/tests/components/recall/NoteRefinement.extractionPreview.cancel.spec.ts` | browser component test | pending gate + Cancel DOM + post-cancel UI | `NoteRefinement.layoutGeneration.cancel.spec.ts` + `extractNote.spec.ts` loading modal | role-match (compose; different landing asserts) |
| `frontend/tests/components/recall/noteRefinementExtractionTestSupport.ts` | test fixture / helpers | pending-preview mount + field/visibility asserts | Same file + `noteRefinementLayoutLoadingTestSupport.ts` (`createDeferredGate`, `clickLoadingModalCancel`) | exact + reuse |

**Read-only reuse (do not rewrite):** `noteRefinementLayoutLoadingTestSupport.ts` — `createDeferredGate`, `loadingModalMask`, `clickLoadingModalCancel`. `noteRefinementTestSupport.ts` — `layoutCheckbox`, `clickExtractRefinementLayout`, `selectRefinementLayoutItem`, `mountNoteRefinementReady`, `threePointLayoutTexts`.

## Pattern Assignments

### `frontend/src/components/recall/NoteRefinement.vue` (component, request-response + cancellation)

**Analog to replace (current composite — lines 334–388):** noncancelable outer blocker wrapping thin-bar inner call (no `signal`):

```typescript
const fetchExtractionPreview = async () => {
  const response = await apiCallWithLoading(() =>
    AiController.extractNotePreview({
      path: { note: props.note.id },
      body: layoutSelectionBody(),
    })
  )

  if (response.error || !response.data) {
    const openApiError = toOpenApiError(response.error)
    createError.value =
      openApiError.message ?? "Failed to generate extract preview"
    return false
  }

  extractionPreview.value = { ...response.data }
  lastAiExtractionResult.value = { ...response.data }
  createError.value = ""
  return true
}

const runExtractionPreview = async (showPreviewOnSuccess: boolean) => {
  createError.value = ""

  try {
    await runWithBlockingApiLoading(async () => {
      const success = await fetchExtractionPreview()
      if (success) {
        if (showPreviewOnSuccess) {
          showExtractionPreview.value = true
        }
      } else {
        showExtractionPreview.value = true // API error force-shows preview
      }
    }, "AI is generating preview...")
  } catch (err) {
    console.error("Failed to generate extract preview:", err)
    createError.value = `Error: ${err}`
    showExtractionPreview.value = true
  }
}
```

**Contract to adopt (Phase 1 overload — do not reimplement):** `frontend/src/managedApi/clientSetup.ts` lines 41–55:

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

**Core adoption pattern (copy call shape from `loadRefinementLayout` lines 261–280; change cancel branch to no-op):**

```typescript
// Analog: loadRefinementLayout cancelable call — same overload, different endpoint + cancel semantics
const outcome = await apiCallWithLoading(
  (signal) =>
    AiController.extractNotePreview({
      path: { note: props.note.id },
      body: layoutSelectionBody(),
      signal,
    }),
  {
    blockUi: true,
    message: "AI is generating preview...", // D-08: keep existing copy
    cancelable: true,
  }
)

if (outcome.status === "cancelled") {
  // D-01–D-06: NO clearSelection, NO resetExtractionPreview, NO createError,
  // NO showExtractionPreview toggle, NO contentUpdated
  return
}

const { data, error } = outcome.result
if (error || !data) {
  const openApiError = toOpenApiError(error)
  createError.value =
    openApiError.message ?? "Failed to generate extract preview"
  showExtractionPreview.value = true // existing error path — out of cancel scope
  return
}

extractionPreview.value = { ...data }
lastAiExtractionResult.value = { ...data }
createError.value = ""
if (showPreviewOnSuccess) {
  showExtractionPreview.value = true
}
```

**Contrast with Phase 2 layout cancel branch (do NOT copy domain settle):**

```typescript
// loadRefinementLayout — Phase 2 intentionally clears layout on cancel
if (outcome.status === "cancelled") {
  settleLayout([]) // clearSelection + resetExtractionPreview + empty items
  return
}
```

Preview cancel must **not** call `settleLayout`, `clearSelection`, `resetExtractionPreview`, or `loadRefinementLayout`.

**Recommended refactor (CONTEXT discretion):** Collapse `fetchExtractionPreview` into `runExtractionPreview` so there is one loading entry point. Keep thin wrappers:

```typescript
const openExtractionPreview = () => runExtractionPreview(true)
const retryExtractionPreview = async () => {
  // D-07: keep unsaved-edit confirm unchanged
  if (isExtractionPreviewEdited()) { /* existing confirm */ }
  await runExtractionPreview(false)
}
```

**Retry affordances already in template (do not add new empty panel):**

- Extract from layout (lines 45–54): `data-test-id="extract-refinement-layout"` — post–Extract-cancel retry (D-04).
- Ask AI to retry on preview (lines 136–142): `data-test-id="retry-extraction-preview"` — in-preview regenerate (D-06).

**Leave unchanged (Phase 4 / other paths):**

- Create note: `runWithBlockingApiLoading` + noncancelable `createExtractedNote` (lines 432–464), message `"AI is creating note..."`.
- Remove: `runWithBlockingApiLoading(..., "AI is removing content...")` (lines 307–331).
- Layout: existing cancelable `loadRefinementLayout` (lines 253–281) — do not regress.

---

### `frontend/tests/components/recall/NoteRefinement.extractionPreview.cancel.spec.ts` (browser component test)

**Primary analog:** `NoteRefinement.layoutGeneration.cancel.spec.ts` — toast mock, Cancel click, silent assert, late resolve, concurrent blocker pattern.

**Imports / silence mock (lines 1–33 of layout cancel spec):**

```typescript
import { AiController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it, vi } from "vitest"
import { nextTick } from "vue"
import { mockSdkServiceWithImplementation } from "@tests/helpers"
import {
  clickLoadingModalCancel,
  createDeferredGate,
  loadingModalMask,
} from "./noteRefinementLayoutLoadingTestSupport"
import {
  clickExtractRefinementLayout,
  layoutCheckbox,
  mountNoteRefinementReady,
  sampleExtractionPreview,
  selectRefinementLayoutItem,
  setupNoteRefinementTests,
  threePointLayoutTexts,
} from "./noteRefinementTestSupport"
import {
  clickRetryExtractionPreview,
  expectExtractionPreviewVisible,
  expectPreviewFields,
  labeledExtractionPreview,
  openExtractionPreview,
} from "./noteRefinementExtractionTestSupport"

const mockToast = {
  error: vi.fn(),
  warning: vi.fn(),
}

vi.mock("vue-toastification", () => ({
  useToast: () => mockToast,
}))

setupNoteRefinementTests()
```

**Pending-preview timing analog:** `extractNote.spec.ts` loading modal (lines 344–366) — mount layout **ready**, then install deferred `extractNotePreview` **before** Extract (unlike layout cancel, which gates before mount):

```typescript
const successGate = createDeferredGate()
mockSdkServiceWithImplementation(
  AiController,
  "extractNotePreview",
  async () => {
    await successGate.gate
    return sampleExtractionPreview()
  }
)
const successWrapper = await mountNoteRefinementReady(["Test layout point"])
await selectRefinementLayoutItem(successWrapper, "p1")
await clickExtractRefinementLayout(successWrapper)
await nextTick()
expect(loadingModalMask()).toBeTruthy()
expect(document.body.textContent).toContain("AI is generating preview...")
```

**Cancel click helper:** `noteRefinementLayoutLoadingTestSupport.ts` lines 53–55:

```typescript
export function clickLoadingModalCancel() {
  screen.getByText("Cancel").click()
}
```

**Targeted Extract-from-layout cancel shape (compose analogs; D-01–D-05 landing):**

```typescript
describe("NoteRefinement extraction preview cancel", () => {
  it("shows blocking Cancel while preview generates (REFN-03)", async () => {
    const { gate } = createDeferredGate()
    mockSdkServiceWithImplementation(
      AiController,
      "extractNotePreview",
      async () => {
        await gate
        return sampleExtractionPreview()
      }
    )
    const wrapper = await mountNoteRefinementReady([...threePointLayoutTexts])
    await selectRefinementLayoutItem(wrapper, "p2")
    await clickExtractRefinementLayout(wrapper)
    await nextTick()

    expect(loadingModalMask()).toBeTruthy()
    expect(document.body.textContent).toContain("AI is generating preview...")
    expect(document.body.textContent).toContain("Cancel")
  })

  it("cancels from Extract: stays on layout, keeps selection, silent, ignores late data (REFN-04)", async () => {
    const { gate, resolve } = createDeferredGate()
    mockSdkServiceWithImplementation(
      AiController,
      "extractNotePreview",
      async () => {
        await gate
        return sampleExtractionPreview({ newNoteTitle: "Should not appear" })
      }
    )
    const wrapper = await mountNoteRefinementReady([...threePointLayoutTexts])
    await selectRefinementLayoutItem(wrapper, "p2")
    await clickExtractRefinementLayout(wrapper)
    await nextTick()

    clickLoadingModalCancel()
    await flushPromises()

    expect(loadingModalMask()).toBeNull()
    expectExtractionPreviewVisible(wrapper, false)
    expect(wrapper.find('[data-test-id="refinement-layout"]').exists()).toBe(
      true
    )
    expect(layoutCheckbox(wrapper, "p2").checked).toBe(true)
    expect(
      wrapper.find('[data-test-id="extract-refinement-layout"]').element
    ).not.toHaveProperty("disabled", true) // or .disabled === false
    expect(mockToast.error).not.toHaveBeenCalled()
    expect(wrapper.emitted("contentUpdated")).toBeUndefined()
    // Do NOT expect refinement-layout-empty / retry-refinement-layout (Phase 2 only)

    resolve()
    await flushPromises()
    expectExtractionPreviewVisible(wrapper, false)
    expect(document.body.textContent).not.toContain("Should not appear")
  })
})
```

**In-preview regenerate cancel (D-06) — analog:** `extractNote.spec.ts` retry loading (lines 422–447) + field asserts from extraction support:

```typescript
it("cancels Ask AI to retry without wiping prior preview (REFN-04 / D-06)", async () => {
  const first = labeledExtractionPreview("First")
  const { gate, resolve } = createDeferredGate()
  let callCount = 0
  mockSdkServiceWithImplementation(
    AiController,
    "extractNotePreview",
    async () => {
      callCount++
      if (callCount === 1) return first
      await gate
      return labeledExtractionPreview("Should not appear")
    }
  )
  const wrapper = await mountNoteRefinementReady(["Test layout point"])
  await openExtractionPreview(wrapper, "p1")
  expectPreviewFields(wrapper, {
    newTitle: "First title",
    newContent: "First content",
    originalContent: "First original",
  })

  await clickRetryExtractionPreview(wrapper)
  await nextTick()
  expect(loadingModalMask()).toBeTruthy()
  expect(document.body.textContent).toContain("AI is generating preview...")

  clickLoadingModalCancel()
  await flushPromises()

  expect(loadingModalMask()).toBeNull()
  expectExtractionPreviewVisible(wrapper, true)
  expectPreviewFields(wrapper, {
    newTitle: "First title",
    newContent: "First content",
    originalContent: "First original",
  })
  expect(mockToast.error).not.toHaveBeenCalled()

  resolve()
  await flushPromises()
  expectPreviewFields(wrapper, {
    newTitle: "First title",
    newContent: "First content",
    originalContent: "First original",
  })
})
```

**Coverage map for this file (capability-named):**

| Req | Assert |
|-----|--------|
| REFN-03 | Pending: mask + `AI is generating preview...` + `Cancel` |
| REFN-04 Extract | After Cancel: preview hidden; layout visible; checkbox still checked; Extract enabled; no toast; no `contentUpdated`; late resolve ignored |
| REFN-04 Retry | After Cancel during Ask AI to retry: stay on preview; prior `expectPreviewFields`; late resolve ignored |
| Scope | Optional: create-note pending still has message without Cancel (leave `extractNote.spec.ts` create loading case; reinforce only if needed) |
| Regression | Do not re-assert Phase 2 empty panel; layout cancel suite remains green separately |

**Optional concurrent blocker:** Mirror layout cancel CANC-04 (`layoutGeneration.cancel.spec.ts` lines 91–131) only if planner wants product-level identity proof for preview Cancel; shared latch already covered in `clientSetup` tests — prefer not duplicating unless needed.

Do **not** re-implement shared race/latch coverage from `clientSetup.loading.spec.ts`.

---

### `frontend/tests/components/recall/noteRefinementExtractionTestSupport.ts` (test helpers)

**Analog:** existing helpers in the same file + pending pattern from `noteRefinementLayoutLoadingTestSupport.ts`.

**Reuse as-is from layout-loading support** (do not duplicate unless packaging prefers a single import):

```typescript
// noteRefinementLayoutLoadingTestSupport.ts lines 13–22, 53–55
export function createDeferredGate() { /* ... */ }
export const loadingModalMask = () =>
  document.querySelector(".loading-modal-mask")
export function clickLoadingModalCancel() {
  screen.getByText("Cancel").click()
}
```

**Existing extraction helpers to reuse (lines 41–69, 134–172):**

```typescript
export function expectPreviewFields(
  wrapper: ReturnType<typeof mountNoteRefinement>,
  expected: ExtractionPreviewFieldValues
) { /* field value asserts */ }

export function expectExtractionPreviewVisible(
  wrapper: ReturnType<typeof mountNoteRefinement>,
  visible = true
) {
  expect(wrapper.find('[data-test-id="extraction-preview"]').exists()).toBe(
    visible
  )
}

export async function openExtractionPreview(
  wrapper: ReturnType<typeof mountNoteRefinement>,
  itemId: string
) {
  await selectRefinementLayoutItem(wrapper, itemId)
  await clickExtractRefinementLayout(wrapper)
  await flushPromises()
}

export async function clickRetryExtractionPreview(
  wrapper: ReturnType<typeof mountNoteRefinement>
) {
  await wrapper
    .find('[data-test-id="retry-extraction-preview"]')
    .trigger("click")
}
```

**NEW helper recommendation (discretion — pending-preview mount):** layout is already ready; gate `extractNotePreview` before Extract (mirror extractNote loading tests, not `mountNoteRefinementPendingLayout`):

```typescript
/**
 * Mounts with layout flushed, selects item, starts Extract with extractNotePreview held pending.
 * Does not resolve the gate — caller asserts Cancel while preview request is open.
 */
export async function mountNoteRefinementPendingExtractionPreview(
  layoutItemTexts: string[] = [...threePointLayoutTexts],
  itemId = "p1",
  previewWhenResolved: NoteExtractionResult = sampleExtractionPreview({
    newNoteTitle: "Should not appear",
  })
) {
  const { gate, resolve } = createDeferredGate()
  const extractSpy = mockSdkServiceWithImplementation(
    AiController,
    "extractNotePreview",
    async () => {
      await gate
      return previewWhenResolved
    }
  )
  const wrapper = await mountNoteRefinementReady(layoutItemTexts)
  await selectRefinementLayoutItem(wrapper, itemId)
  await clickExtractRefinementLayout(wrapper)
  await nextTick()
  return { wrapper, resolve, gate, extractSpy }
}
```

**Selection assert helper (already in `noteRefinementTestSupport.ts` lines 164–170):**

```typescript
export function layoutCheckbox(
  wrapper: ReturnType<typeof mountNoteRefinementWithLayout>,
  itemId: string
): HTMLInputElement {
  return wrapper.find(`[data-test-id="refinement-layout-checkbox-${itemId}"]`)
    .element as HTMLInputElement
}
// expect(layoutCheckbox(wrapper, "p2").checked).toBe(true)
```

Do **not** add `clickRetryRefinementLayout` / empty-panel helpers for Phase 3 — those belong to layout cancel only.

## Shared Patterns

### Opt-in cancelable call (caller only)

**Source:** `clientSetup.ts` cancelable overload (lines 41–55) + Phase 2 `loadRefinementLayout` (lines 261–280).  
**Apply to:** `runExtractionPreview` only (both Extract and Ask-AI-to-retry).

- Callback receives one `AbortSignal`; pass into `AiController.extractNotePreview` Options.
- Options must be literal `{ blockUi: true, cancelable: true, message: "AI is generating preview..." }`.
- Branch `if (outcome.status === "cancelled") return` **before** any preview apply, `createError`, or `showExtractionPreview` mutation.
- **Forbidden:** wrapping cancelable call inside `runWithBlockingApiLoading`; cancelable mutations; inventing cancelable composite helper.

### Global blocker + Cancel (do not rebuild)

**Source:** Phase 1 `LoadingModal` + test harness `GlobalApiLoadingModal` (already composed by `NoteRefinementWithGlobalLoading` / `renderer` in noteRefinementTestSupport).  
**Apply to:** Product via options only; tests assert teleported mask + `clickLoadingModalCancel()` / body text `Cancel`.

### Post-cancel domain is path-local (D-12) — Phase 3 specifics

| Path | On `status === "cancelled"` |
|------|-----------------------------|
| Extract from layout (`showPreviewOnSuccess: true`) | No-op: stay on layout; selection intact; Extract still enabled (D-01–D-04) |
| Ask AI to retry (`showPreviewOnSuccess: false`) | No-op: stay on preview; prior fields unchanged (D-06) |
| API error (completed with `error`) | Existing force-show preview + `createError` — unchanged; never share with cancel |

### Silent cancel (Phase 1 D-11)

**Source:** Shared accepted-cancellation latch + layout cancel toast mock.  
**Apply to:** Cancel specs assert `mockToast.error` not called; no `createError` banner; no `contentUpdated`.

### Verification conventions

- Vitest browser mode; `mockSdkService` / `mockSdkServiceWithImplementation`; avoid role queries; use `data-test-id` / `getByText("Cancel")` / `layoutCheckbox`.
- No fixed waits; controlled deferred gates.
- Focused command:

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/NoteRefinement.extractionPreview.cancel.spec.ts
```

- Regression smoke:

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/NoteRefinement.extractNote.spec.ts tests/components/recall/NoteRefinement.layoutGeneration.cancel.spec.ts
```

- Phase gate: `CURSOR_DEV=true nix develop -c pnpm frontend:verify`.
- No Cypress E2E unless explicitly requested.

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| — | — | — | None. Cancelable overload, layout cancel suite, extraction pending-loading, and preview field helpers all exist. Only domain post-cancel rules differ from Phase 2. |

## Metadata

**Analog search scope:** `frontend/src/components/recall/NoteRefinement.vue`, `frontend/tests/components/recall/NoteRefinement.layoutGeneration.cancel.spec.ts`, `NoteRefinement.extractNote.spec.ts`, `noteRefinementExtractionTestSupport.ts`, `noteRefinementLayoutLoadingTestSupport.ts`, `noteRefinementTestSupport.ts`, `frontend/src/managedApi/clientSetup.ts`, Phase 2 `02-PATTERNS.md`

**Files scanned:** ~10 focused + Phase 2 pattern map  
**Pattern extraction date:** 2026-07-21
