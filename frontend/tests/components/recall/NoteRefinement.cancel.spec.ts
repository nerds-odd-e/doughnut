import { AiController } from "@generated/donut-backend-api/sdk.gen"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { nextTick } from "vue"
import { describe, expect, it, vi } from "vitest"
import { mockSdkServiceWithImplementation } from "@tests/helpers"
import {
  clickLoadingModalCancel,
  clickRetryRefinementLayout,
  createDeferredGate,
  loadingModalMask,
  mountNoteRefinementPendingLayout,
} from "./noteRefinementLayoutLoadingTestSupport"
import {
  clickExtractRefinementLayout,
  layoutCheckbox,
  mountNoteRefinementReady,
  refinementLayoutItems,
  setupNoteRefinementTests,
  threePointLayoutTexts,
} from "./noteRefinementTestSupport"
import {
  clickRetryExtractionPreview,
  expectExtractionPreviewVisible,
  expectPreviewFields,
  labeledExtractionPreview,
  mountNoteRefinementPendingExtractionPreview,
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

const exists = (wrapper: VueWrapper, testId: string) =>
  wrapper.find(`[data-test-id="${testId}"]`).exists()

function expectEmptyLayoutWithRetry(wrapper: VueWrapper) {
  expect(exists(wrapper, "refinement-layout-empty")).toBe(true)
  expect(exists(wrapper, "retry-refinement-layout")).toBe(true)
  expect(exists(wrapper, "refinement-layout")).toBe(false)
  expect(exists(wrapper, "extract-refinement-layout")).toBe(false)
}

describe("NoteRefinement layout generation cancel", () => {
  it("cancels silently: clears mask, leaves dialog, no toast or contentUpdated", async () => {
    const { wrapper, resolve } = await mountNoteRefinementPendingLayout()

    clickLoadingModalCancel()
    await flushPromises()

    expect(loadingModalMask()).toBeNull()
    expect(mockToast.error).not.toHaveBeenCalled()
    expect(wrapper.emitted("contentUpdated")).toBeUndefined()
    expectEmptyLayoutWithRetry(wrapper)

    resolve()
    await flushPromises()
    expect(exists(wrapper, "refinement-layout")).toBe(false)
  })

  it("retries with a fresh request after cancel and cancels that retry without applying late items", async () => {
    const { wrapper, resolve, generateSpy } =
      await mountNoteRefinementPendingLayout(
        refinementLayoutItems(["Late retry should not appear"])
      )
    const callsAfterMount = generateSpy.mock.calls.length

    clickLoadingModalCancel()
    await flushPromises()

    await clickRetryRefinementLayout(wrapper)
    await nextTick()
    expect(generateSpy.mock.calls.length).toBeGreaterThan(callsAfterMount)
    expect(loadingModalMask()).toBeTruthy()
    expect(document.body.textContent).toContain(
      "AI is generating refinement layout..."
    )

    clickLoadingModalCancel()
    await flushPromises()

    expect(loadingModalMask()).toBeNull()
    expect(mockToast.error).not.toHaveBeenCalled()
    expectEmptyLayoutWithRetry(wrapper)

    resolve()
    await flushPromises()
    expect(exists(wrapper, "refinement-layout")).toBe(false)
    expect(document.body.textContent).not.toContain(
      "Late retry should not appear"
    )
  })
})

describe("NoteRefinement extraction preview cancel", () => {
  it("cancels from Extract: stays on layout, keeps selection, silent, ignores late data", async () => {
    const { wrapper, resolve } =
      await mountNoteRefinementPendingExtractionPreview(
        [...threePointLayoutTexts],
        "p2",
        labeledExtractionPreview("Should not appear")
      )

    clickLoadingModalCancel()
    await flushPromises()

    expect(loadingModalMask()).toBeNull()
    expectExtractionPreviewVisible(wrapper, false)
    expect(exists(wrapper, "refinement-layout")).toBe(true)
    expect(exists(wrapper, "refinement-layout-empty")).toBe(false)
    expect(exists(wrapper, "retry-refinement-layout")).toBe(false)
    expect(layoutCheckbox(wrapper, "p2").checked).toBe(true)
    expect(
      (
        wrapper.find('[data-test-id="extract-refinement-layout"]')
          .element as HTMLButtonElement
      ).disabled
    ).toBe(false)
    expect(mockToast.error).not.toHaveBeenCalled()
    expect(wrapper.emitted("contentUpdated")).toBeUndefined()

    resolve()
    await flushPromises()
    expectExtractionPreviewVisible(wrapper, false)
    expect(document.body.textContent).not.toContain("Should not appear title")
  })

  it("retries Extract with a fresh cancelable preview after Cancel", async () => {
    const { wrapper, extractSpy } =
      await mountNoteRefinementPendingExtractionPreview()

    clickLoadingModalCancel()
    await flushPromises()
    expect(loadingModalMask()).toBeNull()

    await clickExtractRefinementLayout(wrapper)

    expect(extractSpy).toHaveBeenCalledTimes(2)
    expect(loadingModalMask()?.querySelector("button")?.textContent).toBe(
      "Cancel"
    )
  })

  it("cancels Ask AI to retry without wiping prior preview", async () => {
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

    await clickRetryExtractionPreview(wrapper)
    expect(loadingModalMask()).toBeTruthy()

    clickLoadingModalCancel()
    await flushPromises()

    expect(loadingModalMask()).toBeNull()
    expectExtractionPreviewVisible(wrapper, true)
    expectPreviewFields(wrapper, {
      newTitle: "First title",
      newContent: "First content",
      originalContent: "First original",
    })

    resolve()
    await flushPromises()
    expectPreviewFields(wrapper, { newTitle: "First title" })
  })
})
