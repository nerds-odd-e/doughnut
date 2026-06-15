import type { MemoryTracker } from "@generated/doughnut-backend-api"
import { AssimilationController } from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { useAssimilationCount } from "@/composables/useAssimilationCount"
import { useGoToNextAssimilation } from "@/composables/useGoToNextAssimilation"
import { useRecallData } from "@/composables/useRecallData"

export type AssimilateUnitRequest = {
  noteId: number
  skipMemoryTracking: boolean
  propertyKey?: string
}

export type AssimilateEvent = Pick<
  AssimilateUnitRequest,
  "skipMemoryTracking" | "propertyKey"
>

export type AssimilateUnitResult = {
  success: boolean
  navigated: boolean
  memoryTrackers?: MemoryTracker[]
}

export function skipRecallConfirmMessage(propertyKey?: string) {
  return propertyKey
    ? "Confirm to hide this property from recalls in the future?"
    : "Confirm to hide this note from recalls in the future?"
}

export function useAssimilateUnit() {
  const { totalAssimilatedCount, requestDueRecallsRefresh } = useRecallData()
  const { incrementAssimilatedCount } = useAssimilationCount()
  const { goToNextAssimilation } = useGoToNextAssimilation()

  const assimilateUnit = async (
    request: AssimilateUnitRequest
  ): Promise<AssimilateUnitResult> => {
    const { data: memoryTrackers, error } = await apiCallWithLoading(() =>
      AssimilationController.assimilate({
        body: {
          noteId: request.noteId,
          ...(request.propertyKey ? { propertyKey: request.propertyKey } : {}),
          ...(request.skipMemoryTracking ? { skipMemoryTracking: true } : {}),
        },
      })
    )

    if (error || !memoryTrackers) {
      return { success: false, navigated: false }
    }

    const newTrackerCount = memoryTrackers.filter(
      (tracker) => !tracker.removedFromTracking
    ).length
    if (totalAssimilatedCount.value !== undefined) {
      totalAssimilatedCount.value += newTrackerCount
    }
    incrementAssimilatedCount(newTrackerCount)
    requestDueRecallsRefresh()

    const navigated = await goToNextAssimilation()
    return { success: true, navigated, memoryTrackers }
  }

  return { assimilateUnit }
}
