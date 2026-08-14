import { AssimilationSequenceSkipController } from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { useGoToNextAssimilation } from "@/composables/useGoToNextAssimilation"

export const SEQUENCE_SKIP_CONFIRM =
  "Leave this note out of the assimilation sequence?"

export function useAssimilationSequenceSkip() {
  const { goToNextAssimilation } = useGoToNextAssimilation()

  const skipFromAssimilationSequence = async (
    noteId: number
  ): Promise<{ success: boolean; navigated: boolean }> => {
    const { error } = await apiCallWithLoading(
      () =>
        AssimilationSequenceSkipController.create({
          body: { noteId },
        }),
      { blockUi: true, message: "Skipping..." }
    )

    if (error) {
      return { success: false, navigated: false }
    }

    const navigated = await goToNextAssimilation()
    return { success: true, navigated }
  }

  return { skipFromAssimilationSequence }
}
