import {
  onMounted,
  onActivated,
  onDeactivated,
  watch,
  type ComputedRef,
} from "vue"
import { useThinkingTimeTracker } from "./useThinkingTimeTracker"
import { useRecallData } from "./useRecallData"

export function useQuestionThinkingTime(
  isActiveQuestion: ComputedRef<boolean>
) {
  const { start, stop, pause, resume, isPaused, awayMs, awayCount } =
    useThinkingTimeTracker()
  const { isViewingAnsweredQuestion } = useRecallData()

  watch(
    isActiveQuestion,
    (isActive) => {
      if (isActive) {
        start()
      }
    },
    { immediate: true }
  )

  watch(isViewingAnsweredQuestion, (viewing) => {
    if (viewing) {
      pause()
    } else if (isActiveQuestion.value) {
      resume()
    }
  })

  onMounted(() => {
    if (isActiveQuestion.value) {
      start()
    }
  })

  onActivated(() => {
    if (isActiveQuestion.value) {
      resume()
    }
  })

  onDeactivated(() => {
    pause()
  })

  return {
    stop,
    isPaused,
    awayMs,
    awayCount,
  }
}
