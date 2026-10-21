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
  const fetching = ref(false)

  const fetchNextRecallPrompts = async () => {
    for (
      let index = props.currentIndex;
      index < props.currentIndex + props.eagerFetchCount;
      index++
    ) {
      const memoryTracker = props.memoryTrackers?.[index]
      if (memoryTracker === undefined) break

      const memoryTrackerId = memoryTracker.memoryTrackerId

      const previousLoad = recallPromptLoads.value[memoryTrackerId]
      if (
        previousLoad &&
        (previousLoad.status !== "failed" || !memoryTracker.spelling)
      )
        continue

      recallPromptLoads.value[memoryTrackerId] = { status: "loading" }
      const { data: recallPrompt, error } = await apiCallWithLoading(() =>
        MemoryTrackerController.getRecallPrompt({
          path: { memoryTracker: memoryTrackerId },
        })
      )
      if (!error) {
        recallPromptLoads.value[memoryTrackerId] = {
          status: "loaded",
          recallPrompt: recallPrompt!,
        }
      } else {
        recallPromptLoads.value[memoryTrackerId] = { status: "failed" }
      }
    }
  }

  let queuedFetch = false

  const fetchRecallPrompts = async () => {
    if (fetching.value) {
      queuedFetch = true
      return
    }
    fetching.value = true
    try {
      do {
        queuedFetch = false
        await fetchNextRecallPrompts()
      } while (queuedFetch)
    } finally {
      fetching.value = false
    }
  }

  return {
    recallPromptLoads,
    fetchRecallPrompts,
  }
}
