import type {
  AnsweredQuestion,
  ThresholdExceededResult,
} from "@generated/doughnut-backend-api"
import { MemoryTrackerController } from "@generated/doughnut-backend-api/sdk.gen"
import usePopups from "@/components/commons/Popups/usePopups"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import type { Ref } from "vue"

type RecallAnswerHandlingDeps = {
  previousAnsweredQuestions: Ref<(AnsweredQuestion | undefined)[]>
  previousAnsweredQuestionCursor: Ref<number | undefined>
  spellingRetryNonce: Ref<number>
  moveToNextMemoryTracker: () => void
  viewLastAnsweredQuestion: (cursor: number | undefined) => void
}

export function useRecallAnswerHandling({
  previousAnsweredQuestions,
  previousAnsweredQuestionCursor,
  spellingRetryNonce,
  moveToNextMemoryTracker,
  viewLastAnsweredQuestion,
}: RecallAnswerHandlingDeps) {
  const { popups } = usePopups()

  const showFrequentFailureWarning = async (
    answerResult: AnsweredQuestion,
    thresholdResult: ThresholdExceededResult
  ) => {
    const propertyKey = answerResult.recalledNote?.propertyKey
    const { wrongCount, periodDays } = thresholdResult
    const message = propertyKey
      ? `You've answered the "${propertyKey}" property incorrectly ${wrongCount} times within the last ${periodDays} days.`
      : `You've answered incorrectly ${wrongCount} times within the last ${periodDays} days.`
    await popups.alert(message)
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
          await showFrequentFailureWarning(answerResult, data)
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
