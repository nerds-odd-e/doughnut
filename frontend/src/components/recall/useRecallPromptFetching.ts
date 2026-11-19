import { computed, ref } from "vue"
import type {
  MemoryTrackerLite,
  RecallPrompt,
} from "@generated/donut-backend-api"
import { MemoryTrackerController } from "@generated/donut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"

type RecallPromptLoad =
  | { status: "loading"; requestId: number }
  | { status: "prepared" }
  | { status: "failed" }
  | { status: "loaded"; recallPrompt: RecallPrompt }

export function useRecallPromptFetching(props: {
  memoryTrackers: MemoryTrackerLite[]
  currentIndex: number
  eagerFetchCount: number
}) {
  const recallPromptLoads = ref<Record<number, RecallPromptLoad>>({})
  const currentMemoryTracker = computed<MemoryTrackerLite | undefined>(
    () => props.memoryTrackers[props.currentIndex]
  )

  let nextRequestId = 0

  const loadRecallPrompt = async (
    tracker: MemoryTrackerLite,
    foreground: boolean
  ) => {
    const { memoryTrackerId } = tracker
    const requestId = ++nextRequestId
    recallPromptLoads.value[memoryTrackerId] = { status: "loading", requestId }
    const request = () =>
      MemoryTrackerController.getRecallPrompt({
        path: { memoryTracker: memoryTrackerId },
      })
    const { data: recallPrompt, error } = await (foreground
      ? apiCallWithLoading(request)
      : request())
    const current = recallPromptLoads.value[memoryTrackerId]
    if (current?.status !== "loading" || current.requestId !== requestId) return
    if (error) {
      recallPromptLoads.value[memoryTrackerId] = { status: "failed" }
    } else if (
      tracker.spelling ||
      (foreground &&
        currentMemoryTracker.value?.memoryTrackerId === memoryTrackerId)
    ) {
      recallPromptLoads.value[memoryTrackerId] = {
        status: "loaded",
        recallPrompt: recallPrompt!,
      }
    } else {
      recallPromptLoads.value[memoryTrackerId] = { status: "prepared" }
    }
  }

  const fetchRecallPrompts = async () => {
    const trackers = props.memoryTrackers.slice(
      props.currentIndex,
      props.currentIndex + props.eagerFetchCount
    )
    for (const [offset, tracker] of trackers.entries()) {
      const foreground = offset === 0
      const previous = recallPromptLoads.value[tracker.memoryTrackerId]
      const needsSelection =
        foreground &&
        !tracker.spelling &&
        (previous?.status === "loading" || previous?.status === "prepared")
      if (!previous || needsSelection)
        await loadRecallPrompt(tracker, foreground)
    }
  }

  const retryRecallPrompt = () => {
    const tracker = currentMemoryTracker.value
    if (
      !tracker ||
      recallPromptLoads.value[tracker.memoryTrackerId]?.status !== "failed"
    )
      return
    return loadRecallPrompt(tracker, true)
  }

  return {
    currentMemoryTracker,
    recallPromptLoads,
    fetchRecallPrompts,
    retryRecallPrompt,
    startRecallAttempt: (tracker: MemoryTrackerLite) =>
      loadRecallPrompt(tracker, true),
  }
}
