import {
  AiController,
  AssimilationController,
  NoteController,
} from "@generated/doughnut-backend-api/sdk.gen"
import AssimilationPanel from "@/components/recall/AssimilationPanel.vue"
import { flushPromises } from "@vue/test-utils"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import RenderingHelper from "@tests/helpers/RenderingHelper"
import { useRecallData } from "@/composables/useRecallData"
import { useAssimilationCount } from "@/composables/useAssimilationCount"
import type { MemoryTrackerLite } from "@generated/doughnut-backend-api"
import { computed, ref } from "vue"
import usePopups from "@/components/commons/Popups/usePopups"
import { afterEach, beforeEach, vi } from "vitest"
import { mockedGoToNextAssimilation } from "./assimilationPanelMocks"
import { refinementLayoutItems } from "./noteRefinementTestSupport"

export const noteRealm = makeMe.aNoteRealm.please()
export const memoryTracker = makeMe.aMemoryTracker.ofNote(noteRealm).please()
export const { note } = memoryTracker

export let renderer: RenderingHelper<typeof AssimilationPanel>
export let assimilateSpy: ReturnType<typeof mockSdkService>

export const mockedIncrementAssimilatedCount = vi.fn()
export const mockedRequestDueRecallsRefresh = vi.fn()
export const mockedTotalAssimilatedCount = ref(0)
const toRepeat = ref<MemoryTrackerLite[] | undefined>(undefined)

export function setupAssimilationPanelTests() {
  afterEach(() => {
    document.body.innerHTML = ""
    vi.clearAllMocks()
    const popups = usePopups()
    while (popups.popups.peek().length) {
      popups.popups.done(false)
    }
  })

  beforeEach(() => {
    mockedGoToNextAssimilation.mockClear()
    mockedGoToNextAssimilation.mockResolvedValue(true)
    assimilateSpy = mockSdkService(AssimilationController, "assimilate", [])
    mockSdkService(NoteController, "getNoteInfo", {})
    mockSdkService(AiController, "generateRefinementSuggestions", {
      items: refinementLayoutItems([]),
    })

    vi.mocked(useRecallData).mockReturnValue({
      totalAssimilatedCount: mockedTotalAssimilatedCount,
      toRepeatCount: computed(() => toRepeat.value?.length ?? 0),
      toRepeat: ref(undefined),
      currentRecallWindowEndAt: ref(undefined),
      isRecallPaused: ref(false),
      shouldResumeRecall: ref(false),
      treadmillMode: ref(false),
      currentIndex: ref(0),
      diligentMode: ref(false),
      setToRepeat: vi.fn(),
      setCurrentRecallWindowEndAt: vi.fn(),
      setTotalAssimilatedCount: vi.fn(),
      setIsRecallPaused: vi.fn(),
      resumeRecall: vi.fn(),
      clearShouldResumeRecall: vi.fn(),
      setTreadmillMode: vi.fn(),
      setCurrentIndex: vi.fn(),
      setDiligentMode: vi.fn(),
      dueRecallsRefreshNonce: ref(0),
      requestDueRecallsRefresh: mockedRequestDueRecallsRefresh,
    })

    vi.mocked(useAssimilationCount).mockReturnValue({
      incrementAssimilatedCount: mockedIncrementAssimilatedCount,
      dueCount: ref(0),
      setDueCount: vi.fn(),
      assimilatedCountOfTheDay: ref(0),
      setAssimilatedCountOfTheDay: vi.fn(),
      totalUnassimilatedCount: ref(0),
      setTotalUnassimilatedCount: vi.fn(),
      applyAssimilationCountDto: vi.fn(),
    })

    renderer = helper.component(AssimilationPanel)
  })
}

export const assimilateButtonSelector = '[data-test="assimilate"]' as const

export function mountAssimilationPanel(overrides?: { note?: typeof note }) {
  return renderer
    .withCleanStorage()
    .withProps({
      note: overrides?.note ?? note,
    })
    .withRouter()
    .mount()
}

export async function clickAssimilate(
  wrapper: Awaited<ReturnType<typeof mountAssimilationPanel>>
) {
  await wrapper.find(assimilateButtonSelector).trigger("click")
  await flushPromises()
}
