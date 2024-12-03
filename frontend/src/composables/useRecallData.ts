import { ref } from "vue"

const toRepeatCount = ref<number | undefined>(undefined)
const recallWindowEndAt = ref<string | undefined>(undefined)

export function useRecallData() {
  const setToRepeatCount = (count: number | undefined) => {
    toRepeatCount.value = count
  }

  const setRecallWindowEndAt = (endAt: string | undefined) => {
    recallWindowEndAt.value = endAt
  }

  const decrementToRepeatCount = () => {
    if (toRepeatCount.value !== undefined && toRepeatCount.value > 0) {
      toRepeatCount.value -= 1
    }
  }

  return {
    toRepeatCount,
    recallWindowEndAt,
    setToRepeatCount,
    setRecallWindowEndAt,
    decrementToRepeatCount,
  }
}
