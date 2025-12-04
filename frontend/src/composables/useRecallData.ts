import { ref } from "vue"

const toRepeatCount = ref<number | undefined>(undefined)
const recallWindowEndAt = ref<string | undefined>(undefined)
const totalAssimilatedCount = ref<number | undefined>(undefined)

export function useRecallData() {
  const setToRepeatCount = (count: number | undefined) => {
    toRepeatCount.value = count
  }

  const setRecallWindowEndAt = (endAt: string | undefined) => {
    recallWindowEndAt.value = endAt
  }

  const setTotalAssimilatedCount = (count: number | undefined) => {
    totalAssimilatedCount.value = count
  }

  const decrementToRepeatCount = () => {
    if (toRepeatCount.value !== undefined && toRepeatCount.value > 0) {
      toRepeatCount.value -= 1
    }
  }

  return {
    toRepeatCount,
    recallWindowEndAt,
    totalAssimilatedCount,
    setToRepeatCount,
    setRecallWindowEndAt,
    setTotalAssimilatedCount,
    decrementToRepeatCount,
  }
}
