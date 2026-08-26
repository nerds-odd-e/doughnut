import { computed, ref } from "vue"
import { useRouter } from "vue-router"
import type {
  DueCommissionedMemoryTrackerLite,
  MemoryTrackerLite,
} from "@generated/donut-backend-api/types.gen"
import { primeSoftKeyboard } from "@/utils/focusTarget"

export type PotentialLearningSession = {
  notebookId: number
  notebookName: string
}

const toRepeat = ref<MemoryTrackerLite[] | undefined>(undefined)
const dueCommissioned = ref<DueCommissionedMemoryTrackerLite[] | undefined>(
  undefined
)
const currentRecallWindowEndAt = ref<string | undefined>(undefined)
const totalAssimilatedCount = ref<number | undefined>(undefined)
const isRecallPaused = ref(false)
const shouldResumeRecall = ref(false)
const treadmillMode = ref<boolean>(false)
const currentIndex = ref(0)
const diligentMode = ref<boolean>(false)

const dueRecallsRefreshNonce = ref(0)

const toRepeatCount = computed(() => {
  const length = toRepeat.value?.length ?? 0
  const index = currentIndex.value
  return Math.max(0, length - index)
})

const potentialLearningSessions = computed((): PotentialLearningSession[] => {
  const trackers = dueCommissioned.value ?? []
  const byNotebook = new Map<number, PotentialLearningSession>()
  for (const tracker of trackers) {
    if (!byNotebook.has(tracker.notebookId)) {
      byNotebook.set(tracker.notebookId, {
        notebookId: tracker.notebookId,
        notebookName: tracker.notebookName,
      })
    }
  }
  return [...byNotebook.values()]
})

export function useRecallData() {
  const router = useRouter()

  const setToRepeat = (trackers: MemoryTrackerLite[] | undefined) => {
    toRepeat.value = trackers
  }

  const setDueCommissioned = (
    trackers: DueCommissionedMemoryTrackerLite[] | undefined
  ) => {
    dueCommissioned.value = trackers
  }

  const setCurrentRecallWindowEndAt = (endAt: string | undefined) => {
    currentRecallWindowEndAt.value = endAt
  }

  const setTotalAssimilatedCount = (count: number | undefined) => {
    totalAssimilatedCount.value = count
  }

  const setIsRecallPaused = (paused: boolean) => {
    isRecallPaused.value = paused
  }

  const resumeRecall = () => {
    const current = toRepeat.value?.[currentIndex.value]
    if (current?.spelling && !treadmillMode.value) {
      primeSoftKeyboard()
    }
    shouldResumeRecall.value = true
    router.push({ name: "recall" })
  }

  const clearShouldResumeRecall = () => {
    shouldResumeRecall.value = false
  }

  const setTreadmillMode = (enabled: boolean) => {
    treadmillMode.value = enabled
  }

  const setCurrentIndex = (index: number) => {
    currentIndex.value = index
  }

  const setDiligentMode = (enabled: boolean) => {
    diligentMode.value = enabled
  }

  const requestDueRecallsRefresh = () => {
    dueRecallsRefreshNonce.value += 1
  }

  return {
    toRepeatCount,
    toRepeat,
    dueCommissioned,
    potentialLearningSessions,
    currentRecallWindowEndAt,
    totalAssimilatedCount,
    isRecallPaused,
    shouldResumeRecall,
    treadmillMode,
    currentIndex,
    diligentMode,
    setToRepeat,
    setDueCommissioned,
    setCurrentRecallWindowEndAt,
    setTotalAssimilatedCount,
    setIsRecallPaused,
    resumeRecall,
    clearShouldResumeRecall,
    setTreadmillMode,
    setCurrentIndex,
    setDiligentMode,
    dueRecallsRefreshNonce,
    requestDueRecallsRefresh,
  }
}
