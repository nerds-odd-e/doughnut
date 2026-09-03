import { flushPromises } from "@vue/test-utils"
import { describe, expect, it, vi } from "vitest"
import { loadingModalMask } from "./noteRefinementLayoutLoadingTestSupport"
import {
  clickExtractRefinementLayout,
  setupNoteRefinementTests,
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
  it("retries Extract with a fresh cancelable preview after Cancel", async () => {
    const { wrapper, extractSpy } =
      await mountNoteRefinementPendingExtractionPreview()

    const firstCancel = loadingModalMask()?.querySelector("button")
    expect(firstCancel?.textContent).toBe("Cancel")
    ;(firstCancel as HTMLButtonElement).click()
    await flushPromises()
    expect(loadingModalMask()).toBeNull()

    await clickExtractRefinementLayout(wrapper)

    expect(extractSpy).toHaveBeenCalledTimes(2)
    expect(loadingModalMask()?.querySelector("button")?.textContent).toBe(
      "Cancel"
    )
  })
})
