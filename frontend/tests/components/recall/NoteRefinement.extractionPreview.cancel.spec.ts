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
  layoutCheckbox,
  mountNoteRefinementReady,
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
    expect(wrapper.find('[data-test-id="refinement-layout"]').exists()).toBe(
      true
    )
    expect(
      wrapper.find('[data-test-id="refinement-layout-empty"]').exists()
    ).toBe(false)
    expect(
      wrapper.find('[data-test-id="retry-refinement-layout"]').exists()
    ).toBe(false)
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
})
