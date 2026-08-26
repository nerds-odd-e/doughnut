import type { MemoryTracker } from "@generated/donut-backend-api"
import { MemoryTrackerController } from "@generated/donut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { useRecallData } from "@/composables/useRecallData"

export const REMOVE_FROM_RECALL_CONFIRM =
  "Confirm to hide this from recalls in the future?"

export function useRemoveFromRecall() {
  const { requestDueRecallsRefresh } = useRecallData()

  const removeMemoryTrackersFromRecall = async (
    trackers: MemoryTracker[]
  ): Promise<boolean> => {
    for (const tracker of trackers) {
      const { error } = await apiCallWithLoading(() =>
        MemoryTrackerController.removeFromRepeating({
          path: { memoryTracker: tracker.id },
        })
      )
      if (error) {
        return false
      }
    }
    requestDueRecallsRefresh()
    return true
  }

  return { removeMemoryTrackersFromRecall }
}
