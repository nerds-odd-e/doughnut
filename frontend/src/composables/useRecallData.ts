import { computed, ref } from "vue"
import { useRouter } from "vue-router"
import type { MemoryTrackerLite } from "@/generated/backend/types.gen"

const toRepeat = ref<MemoryTrackerLite[] | undefined>(undefined)
const recallWindowEndAt = ref<string | undefined>(undefined)
const totalAssimilatedCount = ref<number | undefined>(undefined)
const isRecallPaused = ref(false)
const shouldResumeRecall = ref(false)
const treadmillMode = ref<boolean>(false)
const currentIndex = ref(0)

const toRepeatCount = computed(() => toRepeat.value?.length ?? 0)

export function useRecallData() {
  const router = useRouter()

  const setToRepeat = (trackers: MemoryTrackerLite[] | undefined) => {
    toRepeat.value = trackers
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

  const setTreadmillMode = (enabled: boolean) => {
    treadmillMode.value = enabled
  }

  const setCurrentIndex = (index: number) => {
    currentIndex.value = index
  }

  return {
    toRepeatCount,
    toRepeat,
    recallWindowEndAt,
    totalAssimilatedCount,
    isRecallPaused,
    shouldResumeRecall,
    treadmillMode,
    currentIndex,
    setToRepeat,
    setRecallWindowEndAt,
    setTotalAssimilatedCount,
    setIsRecallPaused,
    resumeRecall,
    clearShouldResumeRecall,
    setTreadmillMode,
    setCurrentIndex,
  }
}
