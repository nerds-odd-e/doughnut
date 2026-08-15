import {
  MemoryTrackerController,
  NoteController,
  RecallsController,
} from "@generated/doughnut-backend-api/sdk.gen"
import { useRecallData } from "@/composables/useRecallData"
import type { PotentialLearningSession } from "@/composables/useRecallData"
import RecallPage from "@/pages/RecallPage.vue"
import type {
  DueCommissionedMemoryTrackerLite,
  MemoryTrackerLite,
} from "@generated/doughnut-backend-api"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { computed, ref } from "vue"
import { afterEach, beforeEach, vi } from "vitest"
import mockBrowserTimeZone from "@tests/helpers/mockBrowserTimeZone"

export function createUseRecallDataMock(overrides?: {
  toRepeat?: MemoryTrackerLite[]
  dueCommissioned?: DueCommissionedMemoryTrackerLite[]
  potentialLearningSessions?: PotentialLearningSession[]
  currentRecallWindowEndAt?: string
  totalAssimilatedCount?: number
  isRecallPaused?: boolean
  shouldResumeRecall?: boolean
  treadmillMode?: boolean
  currentIndex?: number
  diligentMode?: boolean
}) {
  const toRepeatRef = ref<MemoryTrackerLite[] | undefined>(overrides?.toRepeat)
  const dueCommissionedRef = ref<
    DueCommissionedMemoryTrackerLite[] | undefined
  >(overrides?.dueCommissioned)
  const treadmillModeRef = ref(overrides?.treadmillMode ?? false)
  const currentIndexRef = ref(overrides?.currentIndex ?? 0)
  const diligentModeRef = ref(overrides?.diligentMode ?? false)
  const dueRecallsRefreshNonce = ref(0)
  const potentialLearningSessions = computed(
    () => overrides?.potentialLearningSessions ?? []
  )
  return {
    toRepeatCount: computed(() => toRepeatRef.value?.length ?? 0),
    toRepeat: toRepeatRef,
    dueCommissioned: dueCommissionedRef,
    potentialLearningSessions,
    currentRecallWindowEndAt: ref(overrides?.currentRecallWindowEndAt),
    totalAssimilatedCount: ref(overrides?.totalAssimilatedCount ?? 0),
    isRecallPaused: ref(overrides?.isRecallPaused ?? false),
    shouldResumeRecall: ref(overrides?.shouldResumeRecall ?? false),
    treadmillMode: treadmillModeRef,
    currentIndex: currentIndexRef,
    diligentMode: diligentModeRef,
    setToRepeat: vi.fn((trackers: MemoryTrackerLite[] | undefined) => {
      toRepeatRef.value = trackers
    }),
    setDueCommissioned: vi.fn(
      (trackers: DueCommissionedMemoryTrackerLite[] | undefined) => {
        dueCommissionedRef.value = trackers
      }
    ),
    setCurrentRecallWindowEndAt: vi.fn(),
    setTotalAssimilatedCount: vi.fn(),
    setIsRecallPaused: vi.fn(),
    resumeRecall: vi.fn(),
    clearShouldResumeRecall: vi.fn(),
    setTreadmillMode: vi.fn((enabled: boolean) => {
      treadmillModeRef.value = enabled
    }),
    setCurrentIndex: vi.fn((index: number) => {
      currentIndexRef.value = index
    }),
    setDiligentMode: vi.fn((enabled: boolean) => {
      diligentModeRef.value = enabled
    }),
    dueRecallsRefreshNonce,
    requestDueRecallsRefresh: vi.fn(() => {
      dueRecallsRefreshNonce.value += 1
    }),
  }
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

export function createRecallPageRenderer() {
  return helper
    .component(RecallPage)
    .withCleanStorage()
    .withProps({ eagerFetchCount: 1 })
}

export function mockRecallPageDefaults() {
  mockSdkService(NoteController, "showNote", makeMe.aNoteRealm.please())
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
  vi.mocked(useRecallData).mockReturnValue(createUseRecallDataMock())
  return { recallingSpy, previouslyAnsweredSpy }
}

export type RecallPageRenderer = ReturnType<typeof createRecallPageRenderer>

/** Shared mount + timezone + default SDK mocks for RecallPage specs. */
export function useRecallPageSpecContext(options?: { fakeTimers?: boolean }) {
  let renderer: RecallPageRenderer
  let recallingSpy: ReturnType<typeof mockSdkService>
  let previouslyAnsweredSpy: ReturnType<typeof mockSdkService>

  afterEach(() => {
    document.body.innerHTML = ""
    if (options?.fakeTimers) vi.useRealTimers()
  })

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

  const mountPage = async () => {
    const wrapper = renderer.currentRoute({ name: "recall" }).mount()
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

export async function toggleTreadmillMode(
  wrapper: {
    find: (s: string) => { trigger: (e: string) => Promise<unknown> }
    vm: { $nextTick: () => Promise<void> }
  },
  enabled: boolean
) {
  let toggle = document.body.querySelector(
    'input[type="checkbox"]'
  ) as HTMLInputElement

  if (!toggle) {
    await wrapper.find(".progress-bar").trigger("click")
    await wrapper.vm.$nextTick()
    await flushPromises()
    await vi.waitUntil(
      () => {
        toggle = document.body.querySelector(
          'input[type="checkbox"]'
        ) as HTMLInputElement
        return !!toggle
      },
      { timeout: 1000 }
    )
  }

  toggle.checked = enabled
  toggle.dispatchEvent(new Event("change", { bubbles: true }))
  await wrapper.vm.$nextTick()
  await flushPromises()
}
