import { AiController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { render, screen } from "@testing-library/vue"
import { describe, expect, it, vi } from "vitest"
import { nextTick } from "vue"
import { mockSdkServiceWithImplementation } from "@tests/helpers"
import GlobalApiLoadingModal from "@tests/helpers/GlobalApiLoadingModal"
import helper from "@tests/helpers"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import NoteRefinement from "@/components/recall/NoteRefinement.vue"
import {
  clickLoadingModalCancel,
  clickRetryRefinementLayout,
  createDeferredGate,
  loadingModalMask,
  mountNoteRefinementPendingLayout,
} from "./noteRefinementLayoutLoadingTestSupport"
import {
  note,
  refinementLayoutItems,
  setupNoteRefinementTests,
} from "./noteRefinementTestSupport"

const mockToast = {
  error: vi.fn(),
  warning: vi.fn(),
}

vi.mock("vue-toastification", () => ({
  useToast: () => mockToast,
}))

setupNoteRefinementTests()

describe("NoteRefinement layout generation cancel", () => {
  it("shows blocking Cancel while layout generates (REFN-01, CANC-01)", async () => {
    await mountNoteRefinementPendingLayout()

    expect(loadingModalMask()).toBeTruthy()
    expect(document.body.textContent).toContain("AI is generating layout...")
    expect(document.body.textContent).toContain("Cancel")
  })

  it("cancels silently: clears mask, leaves dialog, no toast or contentUpdated (CANC-02, CANC-03)", async () => {
    const { wrapper, resolve } = await mountNoteRefinementPendingLayout(
      refinementLayoutItems(["Should not appear"])
    )

    clickLoadingModalCancel()
    await flushPromises()

    expect(loadingModalMask()).toBeNull()
    expect(mockToast.error).not.toHaveBeenCalled()
    expect(wrapper.emitted("contentUpdated")).toBeUndefined()
    expect(wrapper.exists()).toBe(true)
    expect(
      wrapper.find('[data-test-id="refinement-layout-empty"]').exists()
    ).toBe(true)
    expect(
      wrapper.find('[data-test-id="retry-refinement-layout"]').exists()
    ).toBe(true)
    expect(wrapper.find('[data-test-id="refinement-layout"]').exists()).toBe(
      false
    )
    expect(
      wrapper.find('[data-test-id="extract-refinement-layout"]').exists()
    ).toBe(false)

    resolve()
    await flushPromises()
    expect(wrapper.find('[data-test-id="refinement-layout"]').exists()).toBe(
      false
    )
  })

  it("ignores a second Cancel click after accepted cancel (CANC-02 idempotency)", async () => {
    await mountNoteRefinementPendingLayout()

    // Hold the same Cancel element: after mask clears, re-query would throw;
    // product idempotency is a second click on the original control (LoadingModal pattern).
    const cancelButton = screen.getByText("Cancel")
    cancelButton.click()
    await flushPromises()
    expect(loadingModalMask()).toBeNull()
    expect(mockToast.error).not.toHaveBeenCalled()

    expect(() => cancelButton.click()).not.toThrow()
    expect(mockToast.error).not.toHaveBeenCalled()
  })

  it("keeps an older concurrent blocker after layout Cancel (CANC-04)", async () => {
    // One GlobalApiLoadingModal only — a second mount would reset setupGlobalClient.
    render(GlobalApiLoadingModal)

    const olderNeverSettles = () =>
      new Promise<{ data: string }>(() => {
        // Older blocker must outlive layout cancel.
      })
    const olderCall = apiCallWithLoading(() => olderNeverSettles(), {
      blockUi: true,
      message: "Older concurrent work...",
    })
    await nextTick()
    expect(document.body.textContent).toContain("Older concurrent work...")
    expect(olderCall).toBeTruthy()

    const { gate, resolve } = createDeferredGate()
    mockSdkServiceWithImplementation(
      AiController,
      "generateRefinementSuggestions",
      async () => {
        await gate
        return { items: refinementLayoutItems(["Should not appear"]) }
      }
    )

    helper
      .component(NoteRefinement)
      .withRouter()
      .withCleanStorage()
      .withProps({ note })
      .mount()
    await nextTick()

    expect(document.body.textContent).toContain("AI is generating layout...")
    clickLoadingModalCancel()
    await flushPromises()

    expect(loadingModalMask()).toBeTruthy()
    expect(document.body.textContent).toContain("Older concurrent work...")
    resolve()
  })

  it("retries layout generation with a fresh request after cancel (REFN-02)", async () => {
    const { wrapper, generateSpy } = await mountNoteRefinementPendingLayout()
    const callsAfterMount = generateSpy.mock.calls.length

    clickLoadingModalCancel()
    await flushPromises()

    await clickRetryRefinementLayout(wrapper)
    await nextTick()

    expect(generateSpy.mock.calls.length).toBeGreaterThan(callsAfterMount)
    expect(loadingModalMask()).toBeTruthy()
    expect(document.body.textContent).toContain("AI is generating layout...")
  })
})
