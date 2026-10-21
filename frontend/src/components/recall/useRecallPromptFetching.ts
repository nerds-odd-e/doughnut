import { ref } from "vue"
import type {
  MemoryTrackerLite,
  RecallPrompt,
} from "@generated/donut-backend-api"
import { MemoryTrackerController } from "@generated/donut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"

type RecallPromptLoad =
  | { status: "loading" }
  | { status: "failed" }
  | { status: "loaded"; recallPrompt: RecallPrompt }

export function useRecallPromptFetching(props: {
  memoryTrackers: MemoryTrackerLite[]
  currentIndex: number
  eagerFetchCount: number
}) {
  const recallPromptLoads = ref<Record<number, RecallPromptLoad>>({})

  const loadRecallPrompt = async (
    memoryTrackerId: number,
    foreground: boolean
  ) => {
    if (recallPromptLoads.value[memoryTrackerId]?.status === "loading") return
    recallPromptLoads.value[memoryTrackerId] = { status: "loading" }
    const request = () =>
      MemoryTrackerController.getRecallPrompt({
        path: { memoryTracker: memoryTrackerId },
      })
    const { data: recallPrompt, error } = await (foreground
      ? apiCallWithLoading(request)
      : request())
    recallPromptLoads.value[memoryTrackerId] = error
      ? { status: "failed" }
      : { status: "loaded", recallPrompt: recallPrompt! }
  }

  const fetchNextRecallPrompts = async () => {
    const trackers = props.memoryTrackers.slice(
      props.currentIndex,
      props.currentIndex + props.eagerFetchCount
    )
    for (const [offset, memoryTracker] of trackers.entries()) {
      const memoryTrackerId = memoryTracker.memoryTrackerId

      const previousLoad = recallPromptLoads.value[memoryTrackerId]
      if (
        previousLoad &&
        (previousLoad.status !== "failed" || !memoryTracker.spelling)
      )
        continue

      await loadRecallPrompt(memoryTrackerId, offset === 0)
    }
  }

  const retryRecallPrompt = (memoryTrackerId: number) => {
    if (recallPromptLoads.value[memoryTrackerId]?.status !== "failed") return
    return loadRecallPrompt(memoryTrackerId, true)
  }

  return {
    recallPromptLoads,
    fetchRecallPrompts: fetchNextRecallPrompts,
    retryRecallPrompt,
  }
}
