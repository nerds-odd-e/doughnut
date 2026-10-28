import type {
  AnsweredQuestion,
  DueCommissionedMemoryTrackerLite,
  DueMemoryTrackers,
  MemoryTrackerLite,
} from "@generated/donut-backend-api"
import { RecallsController } from "@generated/donut-backend-api/sdk.gen"
import timezoneParam from "@/managedApi/window/timezoneParam"
import {
  fetchDueRecalls,
  useRecallCatchUp,
} from "@/composables/useRecallCatchUp"
import {
  onActivated,
  onDeactivated,
  onMounted,
  ref,
  watch,
  type Ref,
} from "vue"

export function useRecallPageLoading(options: {
  currentIndex: Ref<number>
  previousAnsweredQuestions: Ref<(AnsweredQuestion | undefined)[]>
  dueRecallsRefreshNonce: Ref<number>
  setToRepeat: (trackers: MemoryTrackerLite[] | undefined) => void
  setDueCommissioned: (
    trackers: DueCommissionedMemoryTrackerLite[] | undefined
  ) => void
  setTotalAssimilatedCount: (count: number | undefined) => void
  setDiligentMode: (enabled: boolean) => void
  setCurrentRecallWindowEndAt: (endAt: string | undefined) => void
}) {
  const {
    currentIndex,
    previousAnsweredQuestions,
    dueRecallsRefreshNonce,
    setToRepeat,
    setDueCommissioned,
    setTotalAssimilatedCount,
    setDiligentMode,
    setCurrentRecallWindowEndAt,
  } = options

  const { catchUpDueRecalls } = useRecallCatchUp()
  const isGlobalBarVisible = ref(true)
  const isLoadingMore = ref(false)

  const applyDueList = (response: DueMemoryTrackers, dueInDays?: number) => {
    setDueCommissioned(response.dueCommissioned ?? [])
    currentIndex.value = 0
    setTotalAssimilatedCount(response.totalAssimilatedCount)
    const loadingFutureDays = (dueInDays ?? 0) > 0
    setDiligentMode(loadingFutureDays)
    setToRepeat(response.toRepeat)
  }

  const loadMore = async (dueInDays?: number) => {
    isLoadingMore.value = true
    try {
      const response = await fetchDueRecalls(dueInDays)
      if (response) {
        applyDueList(response, dueInDays)
        return response
      }
      return
    } finally {
      isLoadingMore.value = false
    }
  }

  const loadPreviouslyAnsweredRecallPrompts = async () => {
    const { data: response, error } =
      await RecallsController.previouslyAnswered({
        query: {
          timezone: timezoneParam(),
        },
      })
    if (!error && response) {
      previousAnsweredQuestions.value = [
        ...response,
        ...previousAnsweredQuestions.value,
      ]
    }
  }

  const loadCurrentDueRecalls = async () => {
    setToRepeat(undefined)
    const response = await loadMore(0)
    if (response) {
      setCurrentRecallWindowEndAt(response.currentRecallWindowEndAt)
    }
  }

  watch(dueRecallsRefreshNonce, async () => {
    await loadCurrentDueRecalls()
  })

  onMounted(() => {
    loadPreviouslyAnsweredRecallPrompts()
  })

  onActivated(() => {
    isGlobalBarVisible.value = true
    catchUpDueRecalls()
  })

  onDeactivated(() => {
    isGlobalBarVisible.value = false
  })

  return {
    isGlobalBarVisible,
    isLoadingMore,
    loadMore,
  }
}
