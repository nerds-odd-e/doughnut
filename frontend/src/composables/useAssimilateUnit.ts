import type { MemoryTracker } from "@generated/donut-backend-api"
import { AssimilationController } from "@generated/donut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { useAssimilationCount } from "@/composables/useAssimilationCount"
import { useGoToNextAssimilation } from "@/composables/useGoToNextAssimilation"
import { useRecallData } from "@/composables/useRecallData"

export type AssimilateUnitRequest = {
  noteId: number
  propertyKey?: string
  assimilateAsCommissioned?: boolean
  assimilateAsSpelling?: boolean
}

export type AssimilateEvent = Pick<
  AssimilateUnitRequest,
  "propertyKey" | "assimilateAsCommissioned" | "assimilateAsSpelling"
>

export type AssimilateUnitResult = {
  success: boolean
  navigated: boolean
  memoryTrackers?: MemoryTracker[]
}

export function useAssimilateUnit() {
  const { totalAssimilatedCount, requestDueRecallsRefresh } = useRecallData()
  const { incrementAssimilatedCount } = useAssimilationCount()
  const { goToNextAssimilation } = useGoToNextAssimilation()

  const assimilateUnit = async (
    request: AssimilateUnitRequest
  ): Promise<AssimilateUnitResult> => {
    const { data: memoryTrackers, error } = await apiCallWithLoading(
      () =>
        AssimilationController.assimilate({
          body: {
            noteId: request.noteId,
            ...(request.propertyKey
              ? { propertyKey: request.propertyKey }
              : {}),
            ...(request.assimilateAsCommissioned
              ? { assimilateAsCommissioned: true }
              : {}),
            ...(request.assimilateAsSpelling
              ? { assimilateAsSpelling: true }
              : {}),
          },
        }),
      { blockUi: true, message: "Assimilating..." }
    )

    if (error || !memoryTrackers) {
      return { success: false, navigated: false }
    }

    requestDueRecallsRefresh()

    const staysOnNote =
      request.assimilateAsCommissioned || request.assimilateAsSpelling
    if (staysOnNote) {
      return { success: true, navigated: false, memoryTrackers }
    }

    const newTrackerCount = memoryTrackers.length
    if (totalAssimilatedCount.value !== undefined) {
      totalAssimilatedCount.value += newTrackerCount
    }
    incrementAssimilatedCount(newTrackerCount)

    const navigated = await goToNextAssimilation()
    return { success: true, navigated, memoryTrackers }
  }

  return { assimilateUnit }
}
