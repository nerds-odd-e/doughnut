import { flushPromises } from "@vue/test-utils"
import type { AssimilationNextDto } from "@generated/donut-backend-api"
import { AssimilationController } from "@generated/donut-backend-api/sdk.gen"
import AssimilationPanel from "@/components/recall/AssimilationPanel.vue"
import usePopups from "@/components/commons/Popups/usePopups"
import { teardownGlobalClientForTesting } from "@/managedApi/clientSetup"
import helper, { wrapSdkError, wrapSdkResponse } from "@tests/helpers"
import GlobalApiLoadingModal from "@tests/helpers/GlobalApiLoadingModal"
import { afterEach, describe, expect, it, vi } from "vitest"
import { defineComponent } from "vue"
import {
  assimilateSpy,
  assimilateButtonEl,
  note,
  setupAssimilationPanelTests,
  skipButtonEl,
  skipSequenceSpy,
} from "./assimilationPanelTestSupport"

setupAssimilationPanelTests()

describe("AssimilationPanel loading modal", () => {
  afterEach(() => {
    teardownGlobalClientForTesting()
  })

  const mountPanelWithGlobalLoadingModal = () => {
    const Host = defineComponent({
      components: { AssimilationPanel, GlobalApiLoadingModal },
      setup: () => ({ note }),
      template: `
        <AssimilationPanel :note="note" />
        <GlobalApiLoadingModal />
      `,
    })

    return helper
      .component(Host)
      .withCleanStorage()
      .withRouter()
      .mount({ attachTo: document.body })
  }

  const nextAssimilationResponse: AssimilationNextDto = {
    nextUnit: { noteId: 42 },
    counts: {
      dueCount: 1,
      assimilatedCountOfTheDay: 0,
      totalUnassimilatedCount: 1,
    },
  }

  const delayNextAssimilation = () => {
    let resolveNext: (
      value: ReturnType<typeof wrapSdkResponse<AssimilationNextDto>>
    ) => void = () => undefined
    vi.spyOn(AssimilationController, "next").mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveNext = resolve
        }) as ReturnType<typeof AssimilationController.next>
    )
    return {
      resolve: () => resolveNext(wrapSdkResponse(nextAssimilationResponse)),
    }
  }

  const delaySuccessfulAssimilation = () => {
    let resolveAssimilate: () => void = () => undefined
    assimilateSpy.mockImplementation(async () => {
      await new Promise<void>((resolve) => {
        resolveAssimilate = resolve
      })
      return wrapSdkResponse([])
    })
    return {
      resolve: () => resolveAssimilate(),
    }
  }

  const delaySuccessfulSkip = () => {
    let resolveSkip: () => void = () => undefined
    skipSequenceSpy.mockImplementation(async () => {
      await new Promise<void>((resolve) => {
        resolveSkip = resolve
      })
      return wrapSdkResponse({ id: 1 })
    })
    return {
      resolve: () => resolveSkip(),
    }
  }

  const loadingModal = () => document.querySelector(".loading-modal-mask")

  it("shows the global modal while assimilating and hides it on success or error", async () => {
    const assimilation = delaySuccessfulAssimilation()
    const succeeding = mountPanelWithGlobalLoadingModal()
    await flushPromises()

    assimilateButtonEl(succeeding)!.click()
    await succeeding.vm.$nextTick()

    expect(loadingModal()).toBeTruthy()
    expect(document.body.textContent).toContain("Assimilating...")
    assimilation.resolve()
    await flushPromises()
    expect(loadingModal()).toBeNull()
    succeeding.unmount()

    let resolveApi: () => void = () => undefined
    assimilateSpy.mockImplementation(async () => {
      await new Promise<void>((r) => {
        resolveApi = r
      })
      return {
        ...wrapSdkError({}),
        response: { status: 404 } as Response,
      }
    })
    const wrapper = mountPanelWithGlobalLoadingModal()
    await flushPromises()

    assimilateButtonEl(wrapper)!.click()
    await wrapper.vm.$nextTick()

    expect(loadingModal()).toBeTruthy()
    resolveApi()
    await flushPromises()
    expect(loadingModal()).toBeNull()
    wrapper.unmount()
  })

  it("keeps the global modal open from skip through loading the next unit", async () => {
    const skipping = delaySuccessfulSkip()
    const nextAssimilation = delayNextAssimilation()
    const wrapper = mountPanelWithGlobalLoadingModal()
    await flushPromises()

    skipButtonEl(wrapper)!.click()
    usePopups().popups.done(true)
    await flushPromises()

    expect(loadingModal()).toBeTruthy()
    expect(document.body.textContent).toContain("Skipping...")
    skipping.resolve()
    await flushPromises()
    expect(loadingModal()).toBeTruthy()
    expect(document.body.textContent).toContain("Loading next note...")
    nextAssimilation.resolve()
    await flushPromises()
    expect(loadingModal()).toBeNull()
    wrapper.unmount()
  })
})
