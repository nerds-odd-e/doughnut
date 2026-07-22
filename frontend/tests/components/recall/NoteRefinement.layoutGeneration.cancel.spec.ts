import { flushPromises } from "@vue/test-utils"
import { describe, expect, it, vi } from "vitest"
import { nextTick } from "vue"
import {
  clickLoadingModalCancel,
  clickRetryRefinementLayout,
  loadingModalMask,
  mountNoteRefinementPendingLayout,
} from "./noteRefinementLayoutLoadingTestSupport"
import {
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
  it("cancels silently: clears mask, leaves dialog, no toast or contentUpdated", async () => {
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

  it("retries layout generation with a fresh request after cancel", async () => {
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

  it("cancels a pending retry without applying late layout items", async () => {
    const { wrapper, resolve } = await mountNoteRefinementPendingLayout(
      refinementLayoutItems(["Late retry should not appear"])
    )

    clickLoadingModalCancel()
    await flushPromises()

    await clickRetryRefinementLayout(wrapper)
    await nextTick()
    expect(loadingModalMask()).toBeTruthy()
    expect(document.body.textContent).toContain("AI is generating layout...")

    clickLoadingModalCancel()
    await flushPromises()

    expect(loadingModalMask()).toBeNull()
    expect(mockToast.error).not.toHaveBeenCalled()
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
    expect(document.body.textContent).not.toContain(
      "Late retry should not appear"
    )
  })
})
