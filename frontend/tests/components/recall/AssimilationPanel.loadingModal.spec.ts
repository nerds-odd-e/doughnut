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

vi.mock("@/composables/useRecallData")

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

  const expectGlobalModalThroughNextUnit = async (
    startAction: (
      wrapper: ReturnType<typeof mountPanelWithGlobalLoadingModal>
    ) => Promise<void>,
    delayMutation: () => { resolve: () => void },
    mutationMessage: string
  ) => {
    const mutation = delayMutation()
    const nextAssimilation = delayNextAssimilation()
    const wrapper = mountPanelWithGlobalLoadingModal()
    await flushPromises()

    await startAction(wrapper)

    expect(loadingModal()).toBeTruthy()
    expect(document.body.textContent).toContain(mutationMessage)

    mutation.resolve()
    await flushPromises()

    expect(loadingModal()).toBeTruthy()
    expect(document.body.textContent).toContain("Loading next note...")

    nextAssimilation.resolve()
    await flushPromises()
    expect(loadingModal()).toBeNull()
    wrapper.unmount()
  }

  it("keeps the global modal open from assimilate through next unit and hides on assimilate error", async () => {
    await expectGlobalModalThroughNextUnit(
      async (wrapper) => {
        assimilateButtonEl(wrapper)!.click()
        await wrapper.vm.$nextTick()
      },
      delaySuccessfulAssimilation,
      "Assimilating..."
    )

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
    await expectGlobalModalThroughNextUnit(
      async (wrapper) => {
        skipButtonEl(wrapper)!.click()
        usePopups().popups.done(true)
        await flushPromises()
      },
      delaySuccessfulSkip,
      "Skipping..."
    )
  })
})
