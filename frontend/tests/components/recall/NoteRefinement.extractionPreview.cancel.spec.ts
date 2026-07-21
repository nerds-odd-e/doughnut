import { AiController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { render, screen } from "@testing-library/vue"
import { describe, expect, it, vi } from "vitest"
import { nextTick } from "vue"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
} from "@tests/helpers"
import GlobalApiLoadingModal from "@tests/helpers/GlobalApiLoadingModal"
import helper from "@tests/helpers"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import NoteRefinement from "@/components/recall/NoteRefinement.vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import {
  clickLoadingModalCancel,
  createDeferredGate,
  loadingModalMask,
} from "./noteRefinementLayoutLoadingTestSupport"
import {
  clickExtractRefinementLayout,
  layoutCheckbox,
  mountNoteRefinementReady,
  note,
  refinementLayoutItems,
  sampleExtractionPreview,
  setupNoteRefinementTests,
  threePointLayoutTexts,
} from "./noteRefinementTestSupport"
import {
  clickCreateNoteFromExtractionPreview,
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
  it("shows blocking Cancel while preview generates (REFN-03)", async () => {
    await mountNoteRefinementPendingExtractionPreview()

    expect(loadingModalMask()).toBeTruthy()
    expect(document.body.textContent).toContain("AI is generating preview...")
    expect(document.body.textContent).toContain("Cancel")
  })

  it("cancels from Extract: stays on layout, keeps selection, silent, ignores late data (REFN-04)", async () => {
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

  it("retries Extract with a fresh cancelable preview after cancel (REFN-04 / D-04)", async () => {
    const { wrapper, extractSpy } =
      await mountNoteRefinementPendingExtractionPreview()
    const callsAfterExtract = extractSpy.mock.calls.length

    clickLoadingModalCancel()
    await flushPromises()

    await clickExtractRefinementLayout(wrapper)
    await nextTick()

    expect(extractSpy.mock.calls.length).toBeGreaterThan(callsAfterExtract)
    expect(loadingModalMask()).toBeTruthy()
    expect(document.body.textContent).toContain("AI is generating preview...")
    expect(document.body.textContent).toContain("Cancel")
  })

  it("ignores a second Cancel click after accepted cancel (idempotency)", async () => {
    const { wrapper } = await mountNoteRefinementPendingExtractionPreview(
      [...threePointLayoutTexts],
      "p2"
    )

    // Hold the same Cancel element: after mask clears, re-query would throw;
    // product idempotency is a second click on the original control.
    const cancelButton = screen.getByText("Cancel")
    cancelButton.click()
    await flushPromises()
    expect(loadingModalMask()).toBeNull()
    expect(mockToast.error).not.toHaveBeenCalled()
    expect(layoutCheckbox(wrapper, "p2").checked).toBe(true)

    expect(() => cancelButton.click()).not.toThrow()
    expect(mockToast.error).not.toHaveBeenCalled()
    expect(layoutCheckbox(wrapper, "p2").checked).toBe(true)
  })

  it("keeps an older concurrent blocker after preview Cancel", async () => {
    // One GlobalApiLoadingModal only — a second mount would reset setupGlobalClient.
    render(GlobalApiLoadingModal)

    const olderNeverSettles = () =>
      new Promise<{ data: string }>(() => {
        // Older blocker must outlive preview cancel.
      })
    const olderCall = apiCallWithLoading(() => olderNeverSettles(), {
      blockUi: true,
      message: "Older concurrent work...",
    })
    await nextTick()
    expect(document.body.textContent).toContain("Older concurrent work...")
    expect(olderCall).toBeTruthy()

    const { gate, resolve } = createDeferredGate()
    mockSdkService(AiController, "generateRefinementSuggestions", {
      items: refinementLayoutItems([...threePointLayoutTexts]),
    })
    mockSdkServiceWithImplementation(
      AiController,
      "extractNotePreview",
      async () => {
        await gate
        return labeledExtractionPreview("Should not appear")
      }
    )

    const wrapper = helper
      .component(NoteRefinement)
      .withRouter()
      .withCleanStorage()
      .withProps({ note })
      .mount()
    await flushPromises()
    await wrapper
      .find('[data-test-id="refinement-layout-checkbox-p1"]')
      .setValue(true)
    await flushPromises()
    await wrapper
      .find('[data-test-id="extract-refinement-layout"]')
      .trigger("click")
    await nextTick()

    expect(document.body.textContent).toContain("AI is generating preview...")
    clickLoadingModalCancel()
    await flushPromises()

    expect(loadingModalMask()).toBeTruthy()
    expect(document.body.textContent).toContain("Older concurrent work...")
    resolve()
  })

  it("create-note pending shows creating message without Cancel (D-10)", async () => {
    const { gate, resolve } = createDeferredGate()
    mockSdkService(
      AiController,
      "extractNotePreview",
      sampleExtractionPreview()
    )
    mockSdkServiceWithImplementation(
      AiController,
      "createExtractedNote",
      async () => {
        await gate
        return makeMe.aNoteRealm.please()
      }
    )
    const wrapper = await mountNoteRefinementReady(["Test layout point"])
    await openExtractionPreview(wrapper, "p1")
    await clickCreateNoteFromExtractionPreview(wrapper)
    await nextTick()

    expect(loadingModalMask()).toBeTruthy()
    expect(document.body.textContent).toContain("AI is creating note...")
    expect(document.body.textContent).not.toContain("Cancel")
    resolve()
    await flushPromises()
    expect(loadingModalMask()).toBeNull()
  })
})
