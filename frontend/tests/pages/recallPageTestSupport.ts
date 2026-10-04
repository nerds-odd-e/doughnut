import {
  DailyProbeController,
  MemoryTrackerController,
  NoteController,
  RecallsController,
} from "@generated/donut-backend-api/sdk.gen"
import { useRecallData } from "@/composables/useRecallData"
import RecallPage from "@/pages/RecallPage.vue"
import type { MemoryTrackerLite } from "@generated/donut-backend-api"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService, productionRouterAt } from "@tests/helpers"
import { enableAutoUnmount, flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, vi } from "vitest"
import mockBrowserTimeZone from "@tests/helpers/mockBrowserTimeZone"
import { resetRecallData } from "@tests/helpers/recallDataTestSupport"

/** Puts the given trackers in the real recall queue. */
export function givenRecallQueue(...trackers: MemoryTrackerLite[]) {
  useRecallData().setToRepeat(trackers)
}

export function createMemoryTrackerLite(
  id: number,
  spelling = false
): MemoryTrackerLite {
  return {
    memoryTrackerId: id,
    spelling,
  }
}

/** A real router placed at the `recall` location. */
export const routerAtRecall = () => productionRouterAt({ name: "recall" })

function createRecallPageRenderer() {
  return helper
    .component(RecallPage)
    .withCleanStorage()
    .withProps({ eagerFetchCount: 1 })
}

function mockRecallPageDefaults() {
  mockSdkService(NoteController, "showNote", makeMe.aNoteRealm.please())
  mockSdkService(DailyProbeController, "getDailyProbeToday", {
    completed: false,
  })
  const recallingSpy = mockSdkService(
    RecallsController,
    "recalling",
    makeMe.aDueMemoryTrackersList.please()
  )
  const previouslyAnsweredSpy = mockSdkService(
    RecallsController,
    "previouslyAnswered",
    []
  )
  mockSdkService(
    MemoryTrackerController,
    "getRecallPrompt",
    makeMe.aRecallPrompt.withSpellingStem("Spell").please()
  )
  resetRecallData()
  return { recallingSpy, previouslyAnsweredSpy }
}

type RecallPageRenderer = ReturnType<typeof createRecallPageRenderer>

/** Shared mount + timezone + default SDK mocks for RecallPage specs. */
export function useRecallPageSpecContext(options?: { fakeTimers?: boolean }) {
  let renderer: RecallPageRenderer
  let recallingSpy: ReturnType<typeof mockSdkService>
  let previouslyAnsweredSpy: ReturnType<typeof mockSdkService>

  afterEach(() => {
    document.body.innerHTML = ""
    if (options?.fakeTimers) vi.useRealTimers()
  })
  // Recall state is shared, so a page left mounted would react to the next test.
  // Registered after the cleanup above so it unmounts before the DOM is cleared.
  enableAutoUnmount(afterEach)

  beforeEach(() => {
    vi.resetAllMocks()
    if (options?.fakeTimers) vi.useFakeTimers()
  })

  // Register after resetAllMocks so the timezone spy is not cleared.
  mockBrowserTimeZone("Asia/Shanghai", beforeEach, afterEach)

  beforeEach(() => {
    const defaults = mockRecallPageDefaults()
    recallingSpy = defaults.recallingSpy
    previouslyAnsweredSpy = defaults.previouslyAnsweredSpy
    renderer = createRecallPageRenderer()
  })

  const mountPage = async (mountOptions?: Record<string, unknown>) => {
    const wrapper = renderer
      .withRouter(await routerAtRecall())
      .mount(mountOptions)
    await flushPromises()
    return wrapper
  }

  return {
    get renderer() {
      return renderer
    },
    get recallingSpy() {
      return recallingSpy
    },
    get previouslyAnsweredSpy() {
      return previouslyAnsweredSpy
    },
    mountPage,
  }
}
