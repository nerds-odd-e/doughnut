import { ref } from "vue"
import { useRouter } from "vue-router"

const toRepeatCount = ref<number | undefined>(undefined)
const recallWindowEndAt = ref<string | undefined>(undefined)
const totalAssimilatedCount = ref<number | undefined>(undefined)
const isRecallPaused = ref(false)
const shouldResumeRecall = ref(false)
const treadmillMode = ref<boolean>(false)
const currentRecallIndex = ref<number | undefined>(undefined)

export function useRecallData() {
  const router = useRouter()

  const setToRepeatCount = (count: number | undefined) => {
    toRepeatCount.value = count
  }

  const setRecallWindowEndAt = (endAt: string | undefined) => {
    recallWindowEndAt.value = endAt
  }

  const setTotalAssimilatedCount = (count: number | undefined) => {
    totalAssimilatedCount.value = count
  }

  const setIsRecallPaused = (paused: boolean) => {
    isRecallPaused.value = paused
  }

  const resumeRecall = () => {
    shouldResumeRecall.value = true
    router.push({ name: "recall" })
  }

  const clearShouldResumeRecall = () => {
    shouldResumeRecall.value = false
  }

  const setCurrentRecallIndex = (index: number | undefined) => {
    currentRecallIndex.value = index
  }

  const decrementToRepeatCount = () => {
    if (toRepeatCount.value !== undefined && toRepeatCount.value > 0) {
      toRepeatCount.value -= 1
    }
  }

  const setTreadmillMode = (enabled: boolean) => {
    treadmillMode.value = enabled
  }

  return {
    toRepeatCount,
    recallWindowEndAt,
    totalAssimilatedCount,
    isRecallPaused,
    shouldResumeRecall,
    treadmillMode,
    currentRecallIndex,
    setToRepeatCount,
    setRecallWindowEndAt,
    setTotalAssimilatedCount,
    setIsRecallPaused,
    resumeRecall,
    clearShouldResumeRecall,
    decrementToRepeatCount,
    setTreadmillMode,
    setCurrentRecallIndex,
  }
}
