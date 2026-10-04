import { describe, it, expect, beforeEach, afterEach } from "vitest"
import { page } from "vitest/browser"
import type { AssimilationNextDto } from "@generated/donut-backend-api"
import { AssimilationController } from "@generated/donut-backend-api/sdk.gen"
import {
  DAILY_GOAL_TOAST,
  NO_MORE_TOAST,
  useGoToNextAssimilation,
} from "@/composables/useGoToNextAssimilation"
import { useAssimilationCount } from "@/composables/useAssimilationCount"
import { useAssimilationView } from "@/composables/useAssimilationView"
import {
  notePropertyLocation,
  noteShowLocation,
} from "@/routes/noteShowLocation"
import LoadingModal from "@/components/commons/LoadingModal.vue"
import {
  currentBlockingApiState,
  type ApiStatus,
} from "@/managedApi/ApiStatusHandler"
import {
  setupGlobalClient,
  teardownGlobalClientForTesting,
} from "@/managedApi/clientSetup"
import { fireEvent } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import { computed, defineComponent, ref } from "vue"
import helper, {
  mockSdkService,
  mockSdkServiceWithImplementation,
  productionRouterAt,
} from "@tests/helpers"
import mockBrowserTimeZone from "@tests/helpers/mockBrowserTimeZone"
import { showToastsOnPage } from "@tests/helpers/toastTestSupport"

async function mountGoToNextAssimilation() {
  const router = await productionRouterAt({ name: "root" })
  let goToNextAssimilation!: () => Promise<boolean>
  helper
    .component(
      defineComponent({
        setup() {
          ;({ goToNextAssimilation } = useGoToNextAssimilation())
          return () => null
        },
      })
    )
    .withRouter(router)
    .mount()
  return { router, goToNextAssimilation }
}

describe("useGoToNextAssimilation", () => {
  mockBrowserTimeZone("Asia/Shanghai", beforeEach, afterEach)
  showToastsOnPage()

  beforeEach(() => {
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

    const { router, goToNextAssimilation } = await mountGoToNextAssimilation()
    const navigated = await goToNextAssimilation()
    expect(navigated).toBe(true)

    const { dueCount, assimilatedCountOfTheDay, totalUnassimilatedCount } =
      useAssimilationCount()
    expect(dueCount.value).toBe(2)
    expect(assimilatedCountOfTheDay.value).toBe(1)
    expect(totalUnassimilatedCount.value).toBe(5)

    const { showAssimilationPanel, targetNoteId } = useAssimilationView()
    expect(showAssimilationPanel.value).toBe(true)
    expect(targetNoteId.value).toBe(42)

    expect(router.currentRoute.value).toMatchObject(noteShowLocation(42))
  })

  it("pushes noteProperty and leaves settings off when nextUnit includes propertyKey", async () => {
    useAssimilationView().openForNote(42)
    mockSdkService(AssimilationController, "next", {
      nextUnit: { noteId: 42, propertyKey: "example of" },
      counts: {
        dueCount: 1,
        assimilatedCountOfTheDay: 1,
        totalUnassimilatedCount: 1,
      },
    })

    const { router, goToNextAssimilation } = await mountGoToNextAssimilation()
    await goToNextAssimilation()

    const { showAssimilationPanel } = useAssimilationView()
    expect(showAssimilationPanel.value).toBe(false)
    expect(router.currentRoute.value).toMatchObject(
      notePropertyLocation(42, "example of")
    )
  })

  it("shows daily goal toast when dueCount is zero but next unit exists", async () => {
    mockSdkService(AssimilationController, "next", {
      nextUnit: { noteId: 42 },
      counts: {
        dueCount: 0,
        assimilatedCountOfTheDay: 2,
        totalUnassimilatedCount: 3,
      },
    })

    const { goToNextAssimilation } = await mountGoToNextAssimilation()
    await goToNextAssimilation()

    await expect.element(page.getByText(DAILY_GOAL_TOAST)).toBeInTheDocument()
  })

  it("shows no-more toast and does not navigate when nextUnit is null", async () => {
    mockSdkService(AssimilationController, "next", {
      nextUnit: undefined,
      counts: {
        dueCount: 0,
        assimilatedCountOfTheDay: 3,
        totalUnassimilatedCount: 0,
      },
    })

    const { router, goToNextAssimilation } = await mountGoToNextAssimilation()
    const navigated = await goToNextAssimilation()
    expect(navigated).toBe(false)

    expect(router.currentRoute.value).toMatchObject({ name: "root" })
    await expect.element(page.getByText(NO_MORE_TOAST)).toBeInTheDocument()
  })

  it("shows the global loading modal while the next assimilation API is pending", async () => {
    let resolveNext: (value: AssimilationNextDto) => void = () => undefined

    mockSdkServiceWithImplementation(
      AssimilationController,
      "next",
      () =>
        new Promise((resolve) => {
          resolveNext = resolve
        })
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

    const router = await productionRouterAt({ name: "root" })
    const { getByText } = helper.component(Starter).withRouter(router).render()

    await fireEvent.click(getByText("Start assimilation"))

    expect(document.querySelector(".loading-modal-mask")).toBeTruthy()
    expect(getByText("Loading next note...")).toBeTruthy()

    resolveNext({
      nextUnit: { noteId: 42 },
      counts: {
        dueCount: 1,
        assimilatedCountOfTheDay: 0,
        totalUnassimilatedCount: 1,
      },
    })
    await flushPromises()

    expect(document.querySelector(".loading-modal-mask")).toBeNull()
  })
})
