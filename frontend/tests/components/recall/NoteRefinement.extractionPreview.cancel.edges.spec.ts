import { AiController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { screen } from "@testing-library/vue"
import { describe, expect, it, vi } from "vitest"
import { nextTick } from "vue"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
} from "@tests/helpers"
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
  sampleExtractionPreview,
  setupNoteRefinementTests,
  threePointLayoutTexts,
} from "./noteRefinementTestSupport"
import {
  clickCreateNoteFromExtractionPreview,
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

describe("NoteRefinement extraction preview cancel edges", () => {
  it("retries Extract with a fresh cancelable preview after cancel", async () => {
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

  it("keeps layout selection after a second Cancel click", async () => {
    const { wrapper } = await mountNoteRefinementPendingExtractionPreview(
      [...threePointLayoutTexts],
      "p2"
    )

    // Hold the same Cancel element: after mask clears, re-query would throw.
    const cancelButton = screen.getByText("Cancel")
    cancelButton.click()
    await flushPromises()
    expect(loadingModalMask()).toBeNull()
    expect(layoutCheckbox(wrapper, "p2").checked).toBe(true)

    expect(() => cancelButton.click()).not.toThrow()
    expect(layoutCheckbox(wrapper, "p2").checked).toBe(true)
  })

  // Transactional create stays intentionally noncancelable.
  it("create-note pending shows creating message without Cancel", async () => {
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

    const mask = loadingModalMask()
    expect(mask).toBeTruthy()
    expect(document.body.textContent).toContain("AI is creating note...")
    expect(document.body.textContent).not.toContain("Cancel")
    resolve()
    await flushPromises()
    expect(loadingModalMask()).toBeNull()
  })
})
