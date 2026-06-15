import { flushPromises } from "@vue/test-utils"
import type { AssimilationNextDto } from "@generated/doughnut-backend-api"
import { AssimilationController } from "@generated/doughnut-backend-api/sdk.gen"
import LoadingModal from "@/components/commons/LoadingModal.vue"
import AssimilationPanel from "@/components/recall/AssimilationPanel.vue"
import usePopups from "@/components/commons/Popups/usePopups"
import {
  currentBlockingApiState,
  type ApiStatus,
} from "@/managedApi/ApiStatusHandler"
import {
  setupGlobalClient,
  teardownGlobalClientForTesting,
} from "@/managedApi/clientSetup"
import helper, { wrapSdkError, wrapSdkResponse } from "@tests/helpers"
import { afterEach, describe, expect, it, vi } from "vitest"
import { computed, defineComponent, ref } from "vue"
import {
  assimilateSpy,
  note,
  setupAssimilationPanelTests,
} from "./assimilationPanelTestSupport"

vi.mock("@/composables/useRecallData")
vi.mock("@/composables/useAssimilationCount")

setupAssimilationPanelTests()

describe("AssimilationPanel loading modal", () => {
  afterEach(() => {
    teardownGlobalClientForTesting()
  })

  const mountPanelWithGlobalLoadingModal = () => {
    const Host = defineComponent({
      components: { AssimilationPanel, LoadingModal },
      setup() {
        const apiStatus = ref<ApiStatus>({ states: [] })
        setupGlobalClient(apiStatus.value)
        const blockingApiState = computed(() =>
          currentBlockingApiState(apiStatus.value)
        )
        return { blockingApiState, note }
      },
      template: `
        <AssimilationPanel :note="note" />
        <LoadingModal
          :show="!!blockingApiState"
          :message="blockingApiState?.message"
        />
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

  const loadingModal = () => document.querySelector(".loading-modal-mask")

  const expectGlobalModalThroughNextUnit = async (
    startAssimilation: (
      wrapper: ReturnType<typeof mountPanelWithGlobalLoadingModal>
    ) => Promise<void>
  ) => {
    const assimilation = delaySuccessfulAssimilation()
    const nextAssimilation = delayNextAssimilation()
    const wrapper = mountPanelWithGlobalLoadingModal()
    await flushPromises()

    await startAssimilation(wrapper)

    expect(loadingModal()).toBeTruthy()
    expect(document.body.textContent).toContain("Assimilating...")

    assimilation.resolve()
    await flushPromises()

    expect(loadingModal()).toBeTruthy()
    expect(document.body.textContent).toContain("Loading next note...")

    nextAssimilation.resolve()
    await flushPromises()
    expect(loadingModal()).toBeNull()
  }

  it("keeps the global modal open from keep-for-recall through loading the next unit", async () => {
    await expectGlobalModalThroughNextUnit(async (wrapper) => {
      await wrapper.find('[data-test="keep-for-recall"]').trigger("click")
    })
  })

  it("keeps the global modal open from skip-recall through loading the next unit", async () => {
    await expectGlobalModalThroughNextUnit(async (wrapper) => {
      await wrapper.find('input[name="skip"]').trigger("click")
      usePopups().popups.done(true)
      await flushPromises()
    })
  })

  it("hides global modal when assimilate API returns an error", async () => {
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

    await wrapper.find('[data-test="keep-for-recall"]').trigger("click")

    expect(loadingModal()).toBeTruthy()
    resolveApi()
    await flushPromises()
    expect(loadingModal()).toBeNull()
  })
})
