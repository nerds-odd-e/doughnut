import { AssimilationController } from "@generated/donut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { useAssimilationCount } from "@/composables/useAssimilationCount"
import { useGoToNextAssimilation } from "@/composables/useGoToNextAssimilation"
import { useRecallData } from "@/composables/useRecallData"

export type AssimilateUnitRequest = {
  noteId: number
  propertyKey?: string
  propertyValue?: string
  assimilateAsCommissioned?: boolean
  assimilateAsSpelling?: boolean
}

export type AssimilateEvent = Pick<
  AssimilateUnitRequest,
  | "propertyKey"
  | "propertyValue"
  | "assimilateAsCommissioned"
  | "assimilateAsSpelling"
>

export type AssimilateUnitResult = {
  success: boolean
  navigated: boolean
  understandingAdded?: boolean
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
            ...(request.propertyValue
              ? { propertyValue: request.propertyValue }
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

    const addsOptionalMode =
      request.assimilateAsCommissioned || request.assimilateAsSpelling
    if (addsOptionalMode) {
      return { success: true, navigated: false }
    }

    const newTrackerCount = memoryTrackers.length
    if (totalAssimilatedCount.value !== undefined) {
      totalAssimilatedCount.value += newTrackerCount
    }
    incrementAssimilatedCount(newTrackerCount)

    if (!request.propertyKey) {
      return { success: true, navigated: false, understandingAdded: true }
    }

    const navigated = await goToNextAssimilation()
    return { success: true, navigated }
  }

  return { assimilateUnit }
}
