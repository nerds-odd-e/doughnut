import { ref } from "vue"

const toRepeatCount = ref<number | undefined>(undefined)

export function useRecallData() {
  const setToRepeatCount = (count: number | undefined) => {
    toRepeatCount.value = count
  }

  const decrementToRepeatCount = () => {
    if (toRepeatCount.value !== undefined && toRepeatCount.value > 0) {
      toRepeatCount.value -= 1
    }
  }

  return {
    toRepeatCount,
    setToRepeatCount,
    decrementToRepeatCount,
  }
}
