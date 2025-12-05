import { ref } from "vue"
import { useRouter } from "vue-router"

const toRepeatCount = ref<number | undefined>(undefined)
const recallWindowEndAt = ref<string | undefined>(undefined)
const totalAssimilatedCount = ref<number | undefined>(undefined)
const isRecallPaused = ref(false)
const shouldResumeRecall = ref(false)

const TREADMILL_MODE_KEY = "treadmillMode"
const getTreadmillModeFromStorage = (): boolean => {
  if (typeof window === "undefined") return false
  const stored = localStorage.getItem(TREADMILL_MODE_KEY)
  return stored === "true"
}
const treadmillMode = ref<boolean>(getTreadmillModeFromStorage())

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

  const decrementToRepeatCount = () => {
    if (toRepeatCount.value !== undefined && toRepeatCount.value > 0) {
      toRepeatCount.value -= 1
    }
  }

  const setTreadmillMode = (enabled: boolean) => {
    treadmillMode.value = enabled
    if (typeof window !== "undefined") {
      localStorage.setItem(TREADMILL_MODE_KEY, enabled.toString())
    }
  }

  return {
    toRepeatCount,
    recallWindowEndAt,
    totalAssimilatedCount,
    isRecallPaused,
    shouldResumeRecall,
    treadmillMode,
    setToRepeatCount,
    setRecallWindowEndAt,
    setTotalAssimilatedCount,
    setIsRecallPaused,
    resumeRecall,
    clearShouldResumeRecall,
    decrementToRepeatCount,
    setTreadmillMode,
  }
}
