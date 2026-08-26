import type { NoteRecallInfo } from "@generated/donut-backend-api"
import { AssimilationSequenceSkipController } from "@generated/donut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { useGoToNextAssimilation } from "@/composables/useGoToNextAssimilation"

export const SEQUENCE_SKIP_CONFIRM =
  "Leave this note out of the assimilation sequence?"

export function isSkippedFromAssimilationSequence(
  noteRecallInfo: NoteRecallInfo | null | undefined,
  propertyKey?: string
): boolean {
  return (
    noteRecallInfo?.skippedPropertyKeys?.includes(propertyKey ?? "") === true
  )
}

const skipRequestBody = (noteId: number, propertyKey?: string) => ({
  noteId,
  ...(propertyKey ? { propertyKey } : {}),
})

export function useAssimilationSequenceSkip() {
  const { goToNextAssimilation } = useGoToNextAssimilation()

  const skipFromAssimilationSequence = async (
    noteId: number,
    propertyKey?: string
  ): Promise<{ success: boolean; navigated: boolean }> => {
    const { error } = await apiCallWithLoading(
      () =>
        AssimilationSequenceSkipController.create({
          body: skipRequestBody(noteId, propertyKey),
        }),
      { blockUi: true, message: "Skipping..." }
    )

    if (error) {
      return { success: false, navigated: false }
    }

    const navigated = await goToNextAssimilation()
    return { success: true, navigated }
  }

  const returnToAssimilationSequence = async (
    noteId: number,
    propertyKey?: string
  ): Promise<boolean> => {
    const { error } = await apiCallWithLoading(
      () =>
        AssimilationSequenceSkipController.deleteAssimilationSequenceSkip({
          body: skipRequestBody(noteId, propertyKey),
        }),
      { blockUi: true, message: "Returning to sequence..." }
    )
    return !error
  }

  return { skipFromAssimilationSequence, returnToAssimilationSequence }
}
