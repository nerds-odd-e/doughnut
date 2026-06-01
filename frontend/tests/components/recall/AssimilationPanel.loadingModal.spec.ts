import { flushPromises } from "@vue/test-utils"
import { wrapSdkError, wrapSdkResponse } from "@tests/helpers"
import { describe, expect, it, vi } from "vitest"
import { nextTick } from "vue"
import { mockedGoToNextAssimilation } from "./assimilationPanelMocks"
import {
  assimilateSpy,
  mountAssimilationPanel,
  setupAssimilationPanelTests,
} from "./assimilationPanelTestSupport"

vi.mock("@/composables/useRecallData")
vi.mock("@/composables/useAssimilationCount")
vi.mock("@/composables/useGoToNextAssimilation", () => ({
  useGoToNextAssimilation: () => ({
    goToNextAssimilation: mockedGoToNextAssimilation,
  }),
}))

setupAssimilationPanelTests()

describe("AssimilationPanel loading modal", () => {
  it("shows blocking modal while assimilate is in progress and hides it afterward", async () => {
    let resolveApi: () => void
    assimilateSpy.mockImplementation(async () => {
      await new Promise<void>((r) => {
        resolveApi = r
      })
      return wrapSdkResponse([])
    })
    const wrapper = mountAssimilationPanel()
    await flushPromises()

    await wrapper.find('[data-test="keep-for-recall"]').trigger("click")
    await nextTick()

    expect(document.querySelector(".loading-modal-mask")).toBeTruthy()
    expect(document.body.textContent).toContain("Assimilating...")
    resolveApi!()
    await flushPromises()
    expect(document.querySelector(".loading-modal-mask")).toBeNull()
  })

  it("hides modal when assimilate API returns an error", async () => {
    let resolveApi: () => void
    assimilateSpy.mockImplementation(async () => {
      await new Promise<void>((r) => {
        resolveApi = r
      })
      return {
        ...wrapSdkError({}),
        response: { status: 404 } as Response,
      }
    })
    const wrapper = mountAssimilationPanel()
    await flushPromises()

    await wrapper.find('[data-test="keep-for-recall"]').trigger("click")
    await nextTick()
    await flushPromises()

    expect(document.querySelector(".loading-modal-mask")).toBeTruthy()
    resolveApi!()
    await flushPromises()
    expect(document.querySelector(".loading-modal-mask")).toBeNull()
  })
})
