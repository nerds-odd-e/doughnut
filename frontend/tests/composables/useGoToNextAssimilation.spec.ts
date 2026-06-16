import { describe, it, expect, beforeEach, vi } from "vitest"
import type { AssimilationNextDto } from "@generated/doughnut-backend-api"
import { AssimilationController } from "@generated/doughnut-backend-api/sdk.gen"
import {
  DAILY_GOAL_TOAST,
  NO_MORE_TOAST,
  useGoToNextAssimilation,
} from "@/composables/useGoToNextAssimilation"
import { useAssimilationCount } from "@/composables/useAssimilationCount"
import { useAssimilationView } from "@/composables/useAssimilationView"
import LoadingModal from "@/components/commons/LoadingModal.vue"
import {
  currentBlockingApiState,
  type ApiStatus,
} from "@/managedApi/ApiStatusHandler"
import {
  setupGlobalClient,
  teardownGlobalClientForTesting,
} from "@/managedApi/clientSetup"
import { fireEvent, render } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import { computed, defineComponent, ref } from "vue"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"

const routerPush = vi.fn()
const showSuccessToast = vi.fn()

vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRouter: () => ({ push: routerPush }),
  }
})

vi.mock("@/managedApi/window/timezoneParam", () => ({
  default: () => "Asia/Shanghai",
}))

vi.mock("@/composables/useToast", () => ({
  useToast: () => ({
    showSuccessToast,
    showErrorToast: vi.fn(),
  }),
}))

describe("useGoToNextAssimilation", () => {
  beforeEach(() => {
    routerPush.mockReset()
    showSuccessToast.mockReset()
    useAssimilationView().dismiss()
    const {
      setDueCount,
      setAssimilatedCountOfTheDay,
      setTotalUnassimilatedCount,
    } = useAssimilationCount()
    setDueCount(undefined)
    setAssimilatedCountOfTheDay(undefined)
    setTotalUnassimilatedCount(undefined)
    teardownGlobalClientForTesting()
  })

  it("updates counts, enables settings, and navigates when nextUnit is returned", async () => {
    mockSdkService(AssimilationController, "next", {
      nextUnit: { noteId: 42 },
      counts: {
        dueCount: 2,
        assimilatedCountOfTheDay: 1,
        totalUnassimilatedCount: 5,
      },
    })

    const { goToNextAssimilation } = useGoToNextAssimilation()
    const navigated = await goToNextAssimilation()
    expect(navigated).toBe(true)

    const { dueCount, assimilatedCountOfTheDay, totalUnassimilatedCount } =
      useAssimilationCount()
    expect(dueCount.value).toBe(2)
    expect(assimilatedCountOfTheDay.value).toBe(1)
    expect(totalUnassimilatedCount.value).toBe(5)

    const { showAssimilationSettings, targetNoteId, pendingPropertyKey } =
      useAssimilationView()
    expect(showAssimilationSettings.value).toBe(true)
    expect(targetNoteId.value).toBe(42)
    expect(pendingPropertyKey.value).toBeNull()

    expect(routerPush).toHaveBeenCalledWith({
      name: "noteShow",
      params: { noteId: "42" },
    })
    expect(showSuccessToast).not.toHaveBeenCalled()
  })

  it("stores pending property key when nextUnit includes propertyKey", async () => {
    mockSdkService(AssimilationController, "next", {
      nextUnit: { noteId: 42, propertyKey: "example of" },
      counts: {
        dueCount: 1,
        assimilatedCountOfTheDay: 1,
        totalUnassimilatedCount: 1,
      },
    })

    const { goToNextAssimilation } = useGoToNextAssimilation()
    await goToNextAssimilation()

    const { targetNoteId, pendingPropertyKey } = useAssimilationView()
    expect(targetNoteId.value).toBe(42)
    expect(pendingPropertyKey.value).toBe("example of")
  })

  it("shows daily goal toast and navigates when dueCount is zero but next unit exists", async () => {
    mockSdkService(AssimilationController, "next", {
      nextUnit: { noteId: 42 },
      counts: {
        dueCount: 0,
        assimilatedCountOfTheDay: 2,
        totalUnassimilatedCount: 3,
      },
    })

    const { goToNextAssimilation } = useGoToNextAssimilation()
    const navigated = await goToNextAssimilation()
    expect(navigated).toBe(true)

    expect(showSuccessToast).toHaveBeenCalledWith(DAILY_GOAL_TOAST)
    expect(routerPush).toHaveBeenCalledWith({
      name: "noteShow",
      params: { noteId: "42" },
    })
  })

  it("updates counts but does not navigate when nextUnit is null", async () => {
    mockSdkService(AssimilationController, "next", {
      nextUnit: undefined,
      counts: {
        dueCount: 0,
        assimilatedCountOfTheDay: 3,
        totalUnassimilatedCount: 0,
      },
    })

    const { goToNextAssimilation } = useGoToNextAssimilation()
    const navigated = await goToNextAssimilation()
    expect(navigated).toBe(false)

    const { dueCount, assimilatedCountOfTheDay } = useAssimilationCount()
    expect(dueCount.value).toBe(0)
    expect(assimilatedCountOfTheDay.value).toBe(3)
    expect(routerPush).not.toHaveBeenCalled()
    expect(showSuccessToast).toHaveBeenCalledWith(NO_MORE_TOAST)

    const { showAssimilationSettings } = useAssimilationView()
    expect(showAssimilationSettings.value).toBe(false)
  })

  it("shows the global loading modal while the next assimilation API is pending", async () => {
    let resolveNext: (
      value: ReturnType<typeof wrapSdkResponse<AssimilationNextDto>>
    ) => void = () => undefined

    vi.spyOn(AssimilationController, "next").mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveNext = resolve
        }) as ReturnType<typeof AssimilationController.next>
    )

    const Starter = defineComponent({
      components: { LoadingModal },
      setup() {
        const apiStatus = ref<ApiStatus>({ states: [] })
        setupGlobalClient(apiStatus.value)
        const blockingApiState = computed(() =>
          currentBlockingApiState(apiStatus.value)
        )
        const { goToNextAssimilation } = useGoToNextAssimilation()
        return { blockingApiState, goToNextAssimilation }
      },
      template: `
        <button @click="goToNextAssimilation">Start assimilation</button>
        <LoadingModal
          :show="!!blockingApiState"
          :message="blockingApiState?.message"
        />
      `,
    })

    const { getByText } = render(Starter)

    await fireEvent.click(getByText("Start assimilation"))

    expect(document.querySelector(".loading-modal-mask")).toBeTruthy()
    expect(getByText("Loading next note...")).toBeTruthy()

    resolveNext(
      wrapSdkResponse({
        nextUnit: { noteId: 42 },
        counts: {
          dueCount: 1,
          assimilatedCountOfTheDay: 0,
          totalUnassimilatedCount: 1,
        },
      })
    )
    await flushPromises()

    expect(document.querySelector(".loading-modal-mask")).toBeNull()
  })
})
