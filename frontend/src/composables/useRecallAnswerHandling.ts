import type {
  AnsweredQuestion,
  ThresholdExceededResult,
} from "@generated/donut-backend-api"
import { MemoryTrackerController } from "@generated/donut-backend-api/sdk.gen"
import usePopups from "@/components/commons/Popups/usePopups"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import type { Ref } from "vue"

const OVERLAP_RETRY_FEEDBACK =
  "Your answer matches an overlapped note, but that's different from the expected answer"

type RecallAnswerHandlingDeps = {
  previousAnsweredQuestions: Ref<(AnsweredQuestion | undefined)[]>
  spellingRetryNonce: Ref<number>
  spellingOverlapFeedback: Ref<string | undefined>
  moveToNextMemoryTracker: () => void
  viewLastAnsweredQuestion: (cursor: number | undefined) => void
}

export function useRecallAnswerHandling({
  previousAnsweredQuestions,
  spellingRetryNonce,
  spellingOverlapFeedback,
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
      spellingOverlapFeedback.value = OVERLAP_RETRY_FEEDBACK
      spellingRetryNonce.value += 1
      return
    }

    spellingOverlapFeedback.value = undefined
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

  const onJustReviewed = () => {
    moveToNextMemoryTracker()
    previousAnsweredQuestions.value.push(undefined)
  }

  return { onAnswered, onJustReviewed }
}
