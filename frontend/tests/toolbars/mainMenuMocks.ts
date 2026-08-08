import { computed, ref } from "vue"
import type { MemoryTrackerLite } from "@generated/doughnut-backend-api"
import { vi } from "vitest"

export const memoryTrackerLitesStub = (n: number): MemoryTrackerLite[] =>
  Array.from({ length: n }, (_, i) => ({
    memoryTrackerId: i + 1,
    spelling: false,
  }))

export const defaultMenuData = {
  assimilationCount: {
    dueCount: 0,
    assimilatedCountOfTheDay: 0,
    totalUnassimilatedCount: 0,
  },
  recallStatus: {
    toRepeat: [] as MemoryTrackerLite[],
    currentRecallWindowEndAt: "",
    totalAssimilatedCount: 0,
  },
  unreadConversations: [],
}

export const createMenuData = (
  overrides?: Partial<typeof defaultMenuData>
) => ({
  ...defaultMenuData,
  ...overrides,
})

export const createUseRecallDataMock = (overrides?: {
  toRepeat?: MemoryTrackerLite[]
  isRecallPaused?: boolean
  currentIndex?: number
  resumeRecall?: () => void
  diligentMode?: boolean
}) => {
  const toRepeat = ref<MemoryTrackerLite[] | undefined>(
    overrides?.toRepeat ?? []
  )
  const currentIndex = ref(overrides?.currentIndex ?? 0)
  return {
    toRepeatCount: computed(() => {
      const length = toRepeat.value?.length ?? 0
      const index = currentIndex.value
      return Math.max(0, length - index)
    }),
    toRepeat,
    dueCommissioned: ref(undefined),
    awaitingReportSessions: ref([]),
    potentialLearningSessions: computed(() => []),
    currentRecallWindowEndAt: ref(undefined),
    totalAssimilatedCount: ref(0),
    isRecallPaused: ref(overrides?.isRecallPaused ?? false),
    shouldResumeRecall: ref(false),
    treadmillMode: ref(false),
    currentIndex,
    diligentMode: ref(overrides?.diligentMode ?? false),
    setToRepeat: vi.fn(),
    setDueCommissioned: vi.fn(),
    setAwaitingReportSessions: vi.fn(),
    setCurrentRecallWindowEndAt: vi.fn(),
    setTotalAssimilatedCount: vi.fn(),
    setIsRecallPaused: vi.fn(),
    resumeRecall: (overrides?.resumeRecall ?? vi.fn()) as () => void,
    clearShouldResumeRecall: vi.fn(),
    setTreadmillMode: vi.fn(),
    setCurrentIndex: vi.fn(),
    setDiligentMode: vi.fn(),
    dueRecallsRefreshNonce: ref(0),
    requestDueRecallsRefresh: vi.fn(),
  }
}

export function aiReplyEventSourceMockExports() {
  return {
    default: class {
      onMessage = vi.fn(() => this)
      onError = vi.fn(() => this)
      start = vi.fn()
    },
  }
}
