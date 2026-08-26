import { ref } from "vue"
import type {
  MemoryTrackerLite,
  RecallPrompt,
} from "@generated/donut-backend-api"
import { MemoryTrackerController } from "@generated/donut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"

export function useRecallPromptFetching(props: {
  memoryTrackers: MemoryTrackerLite[]
  currentIndex: number
  eagerFetchCount: number
}) {
  const recallPromptCache = ref<Record<number, RecallPrompt | undefined>>({})
  const fetching = ref(false)
  const fetchingMemoryTrackerIds = ref<Set<number>>(new Set())

  const fetchNextRecallPrompts = async () => {
    for (
      let index = props.currentIndex;
      index < props.currentIndex + props.eagerFetchCount;
      index++
    ) {
      const memoryTracker = props.memoryTrackers?.[index]
      if (memoryTracker === undefined) break

      const memoryTrackerId = memoryTracker.memoryTrackerId

      const cachedValue = recallPromptCache.value[memoryTrackerId]
      if (cachedValue !== undefined) continue

      fetchingMemoryTrackerIds.value.add(memoryTrackerId)
      try {
        const { data: recallPrompt, error } = await apiCallWithLoading(() =>
          MemoryTrackerController.getRecallPrompt({
            path: { memoryTracker: memoryTrackerId },
          })
        )
        if (!error) {
          recallPromptCache.value[memoryTrackerId] = recallPrompt!
        } else {
          recallPromptCache.value[memoryTrackerId] = undefined
        }
      } finally {
        fetchingMemoryTrackerIds.value.delete(memoryTrackerId)
      }
    }
  }

  const fetchRecallPrompts = async () => {
    if (!fetching.value) {
      fetching.value = true
      try {
        await fetchNextRecallPrompts()
      } finally {
        fetching.value = false
      }
    }
  }

  return {
    recallPromptCache,
    fetchRecallPrompts,
    fetchingMemoryTrackerIds,
  }
}
