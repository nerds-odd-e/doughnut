import { computed, ref } from "vue"
import { useRouter } from "vue-router"
import type { MemoryTrackerLite } from "@/generated/backend/types.gen"

const toRepeat = ref<MemoryTrackerLite[] | undefined>(undefined)
const currentRecallWindowEndAt = ref<string | undefined>(undefined)
const totalAssimilatedCount = ref<number | undefined>(undefined)
const isRecallPaused = ref(false)
const shouldResumeRecall = ref(false)
const treadmillMode = ref<boolean>(false)
const currentIndex = ref(0)
const diligentMode = ref<boolean>(false)

const toRepeatCount = computed(() => {
  const length = toRepeat.value?.length ?? 0
  const index = currentIndex.value
  return Math.max(0, length - index)
})

export function useRecallData() {
  const router = useRouter()

  const setToRepeat = (trackers: MemoryTrackerLite[] | undefined) => {
    toRepeat.value = trackers
  }

  const setCurrentRecallWindowEndAt = (endAt: string | undefined) => {
    currentRecallWindowEndAt.value = endAt
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

  const setTreadmillMode = (enabled: boolean) => {
    treadmillMode.value = enabled
  }

  const setCurrentIndex = (index: number) => {
    currentIndex.value = index
  }

  const setDiligentMode = (enabled: boolean) => {
    diligentMode.value = enabled
  }

  return {
    toRepeatCount,
    toRepeat,
    currentRecallWindowEndAt,
    totalAssimilatedCount,
    isRecallPaused,
    shouldResumeRecall,
    treadmillMode,
    currentIndex,
    diligentMode,
    setToRepeat,
    setCurrentRecallWindowEndAt,
    setTotalAssimilatedCount,
    setIsRecallPaused,
    resumeRecall,
    clearShouldResumeRecall,
    setTreadmillMode,
    setCurrentIndex,
    setDiligentMode,
  }
}
