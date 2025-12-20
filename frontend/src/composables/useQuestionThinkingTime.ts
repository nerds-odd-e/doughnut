import {
  onMounted,
  onActivated,
  onDeactivated,
  watch,
  type ComputedRef,
} from "vue"
import { useThinkingTimeTracker } from "./useThinkingTimeTracker"

export function useQuestionThinkingTime(
  isActiveQuestion: ComputedRef<boolean>
) {
  const { start, stop, pause, resume } = useThinkingTimeTracker()

  watch(
    isActiveQuestion,
    (isActive) => {
      if (isActive) {
        start()
      }
    },
    { immediate: true }
  )

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
  }
}
