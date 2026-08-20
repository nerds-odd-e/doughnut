import { flushPromises } from "@vue/test-utils"
import { screen } from "@testing-library/vue"
import { describe, expect, it, vi } from "vitest"
import { nextTick } from "vue"
import { loadingModalMask } from "./noteRefinementLayoutLoadingTestSupport"
import {
  clickExtractRefinementLayout,
  layoutCheckbox,
  setupNoteRefinementTests,
  threePointLayoutTexts,
} from "./noteRefinementTestSupport"
import { mountNoteRefinementPendingExtractionPreview } from "./noteRefinementExtractionTestSupport"

const mockToast = {
  error: vi.fn(),
  warning: vi.fn(),
}

vi.mock("vue-toastification", () => ({
  useToast: () => mockToast,
}))

setupNoteRefinementTests()

describe("NoteRefinement extraction preview cancel edges", () => {
  it("keeps selection after Cancel, ignores a second Cancel, and retries with a fresh cancelable preview", async () => {
    const { wrapper, extractSpy } =
      await mountNoteRefinementPendingExtractionPreview(
        [...threePointLayoutTexts],
        "p2"
      )
    const callsAfterExtract = extractSpy.mock.calls.length

    // Hold the same Cancel element: after mask clears, re-query would throw.
    const cancelButton = screen.getByText("Cancel")
    cancelButton.click()
    await flushPromises()
    expect(loadingModalMask()).toBeNull()
    expect(layoutCheckbox(wrapper, "p2").checked).toBe(true)

    expect(() => cancelButton.click()).not.toThrow()
    expect(layoutCheckbox(wrapper, "p2").checked).toBe(true)

    await clickExtractRefinementLayout(wrapper)
    await nextTick()

    expect(extractSpy.mock.calls.length).toBeGreaterThan(callsAfterExtract)
    expect(loadingModalMask()).toBeTruthy()
    expect(document.body.textContent).toContain("AI is generating preview...")
    expect(document.body.textContent).toContain("Cancel")
  })
})
