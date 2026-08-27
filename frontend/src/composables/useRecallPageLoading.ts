import type {
  AnsweredQuestion,
  DueCommissionedMemoryTrackerLite,
  DueMemoryTrackers,
  MemoryTrackerLite,
} from "@generated/donut-backend-api"
import { RecallsController } from "@generated/donut-backend-api/sdk.gen"
import getEnvironment from "@/managedApi/window/getEnvironment"
import timezoneParam from "@/managedApi/window/timezoneParam"
import { shuffle } from "es-toolkit"
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
  currentRecallWindowEndAt: Ref<string | undefined>
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
    currentRecallWindowEndAt,
    dueRecallsRefreshNonce,
    setToRepeat,
    setDueCommissioned,
    setTotalAssimilatedCount,
    setDiligentMode,
    setCurrentRecallWindowEndAt,
  } = options

  const isProgressBarVisible = ref(true)
  const isLoadingMore = ref(false)

  const applySessionStrips = (response: DueMemoryTrackers) => {
    setDueCommissioned(response.dueCommissioned ?? [])
  }

  const loadSessionStrips = async () => {
    const { data: response, error } = await RecallsController.recalling({
      query: {
        timezone: timezoneParam(),
        dueindays: 0,
      },
    })
    if (!error && response) {
      applySessionStrips(response)
      return response
    }
    return
  }

  const loadMore = async (dueInDays?: number) => {
    isLoadingMore.value = true
    try {
      const { data: response, error } = await RecallsController.recalling({
        query: {
          timezone: timezoneParam(),
          dueindays: dueInDays,
        },
      })
      if (!error && response) {
        applySessionStrips(response)
        let trackers = response.toRepeat
        currentIndex.value = 0
        setTotalAssimilatedCount(response.totalAssimilatedCount)
        setDiligentMode((dueInDays ?? 0) > 0)
        if (trackers?.length === 0) {
          setToRepeat(trackers)
          return response
        }
        if (getEnvironment() !== "testing" && trackers) {
          trackers = shuffle(trackers)
        }
        setToRepeat(trackers)
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

  onActivated(async () => {
    isProgressBarVisible.value = true
    const response = await loadSessionStrips()
    if (
      response &&
      response.currentRecallWindowEndAt !== currentRecallWindowEndAt.value
    ) {
      loadCurrentDueRecalls()
    }
  })

  onDeactivated(() => {
    isProgressBarVisible.value = false
  })

  return {
    isProgressBarVisible,
    isLoadingMore,
    loadMore,
  }
}
