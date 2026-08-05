import type { AnsweredQuestion } from "@generated/doughnut-backend-api"
import { MemoryTrackerController } from "@generated/doughnut-backend-api/sdk.gen"
import usePopups from "@/components/commons/Popups/usePopups"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import type { Ref } from "vue"

type RecallAnswerHandlingDeps = {
  previousAnsweredQuestions: Ref<(AnsweredQuestion | undefined)[]>
  previousAnsweredQuestionCursor: Ref<number | undefined>
  spellingRetryNonce: Ref<number>
  dueCount: Ref<number | undefined>
  setDueCount: (count: number | undefined) => void
  moveToNextMemoryTracker: () => void
  viewLastAnsweredQuestion: (cursor: number | undefined) => void
}

export function useRecallAnswerHandling({
  previousAnsweredQuestions,
  previousAnsweredQuestionCursor,
  spellingRetryNonce,
  dueCount,
  setDueCount,
  moveToNextMemoryTracker,
  viewLastAnsweredQuestion,
}: RecallAnswerHandlingDeps) {
  const { popups } = usePopups()

  const offerReAssimilation = async (answerResult: AnsweredQuestion) => {
    const memoryTrackerId = answerResult.memoryTrackerId
    if (memoryTrackerId === undefined) return
    const propertyKey = answerResult.recalledNote?.propertyKey
    const message = propertyKey
      ? `You have answered the "${propertyKey}" property incorrectly too many times. Would you like to re-assimilate it?`
      : "You have answered this note incorrectly too many times. Would you like to re-assimilate it?"
    const confirmed = await popups.confirm(message)
    if (confirmed) {
      await MemoryTrackerController.softDelete({
        path: { memoryTracker: memoryTrackerId },
      })
      setDueCount((dueCount.value ?? 0) + 1)
    }
  }

  const onAnswered = async (answerResult: AnsweredQuestion) => {
    if (answerResult.answer?.outcome === "OVERLAP") {
      previousAnsweredQuestions.value.push(answerResult)
      viewLastAnsweredQuestion(previousAnsweredQuestions.value.length - 1)
      return
    }

    moveToNextMemoryTracker()
    previousAnsweredQuestions.value.push(answerResult)
    if (!answerResult.answer?.correct) {
      viewLastAnsweredQuestion(previousAnsweredQuestions.value.length - 1)
      const memoryTrackerId = answerResult.memoryTrackerId
      if (memoryTrackerId !== undefined) {
        const { data } = await apiCallWithLoading(() =>
          MemoryTrackerController.getThresholdExceeded({
            path: { memoryTracker: memoryTrackerId },
          })
        )
        if (data?.thresholdExceeded) {
          await offerReAssimilation(answerResult)
        }
      }
    }
  }

  const onOverlapRetry = () => {
    previousAnsweredQuestionCursor.value = undefined
    spellingRetryNonce.value += 1
  }

  const onJustReviewed = () => {
    moveToNextMemoryTracker()
    previousAnsweredQuestions.value.push(undefined)
  }

  return { onAnswered, onOverlapRetry, onJustReviewed }
}
