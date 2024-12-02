import { ref, computed } from "vue"
import type { AnsweredQuestion } from "@/generated/backend"
import _ from "lodash"
import getEnvironment from "@/managedApi/window/getEnvironment"

const toRepeatCount = ref<number | undefined>(undefined)
const toRepeat = ref<number[] | undefined>(undefined)
const currentIndex = ref(0)
const previousResults = ref<(AnsweredQuestion | undefined)[]>([])

export function useRecallData() {
  const setToRepeatCount = (count: number | undefined) => {
    toRepeatCount.value = count
  }

  const decrementToRepeatCount = () => {
    if (toRepeatCount.value !== undefined && toRepeatCount.value > 0) {
      toRepeatCount.value -= 1
    }
  }

  const setToRepeat = async (numbers: number[] | undefined, shuffle = true) => {
    toRepeat.value = numbers
    currentIndex.value = 0
    if (toRepeat.value?.length === 0) {
      return
    }
    if (shuffle && getEnvironment() !== "testing") {
      toRepeat.value = _.shuffle(toRepeat.value)
    }
  }

  const addPreviousResult = (result: AnsweredQuestion | undefined) => {
    currentIndex.value += 1
    previousResults.value.push(result)
  }

  const moveToEnd = (index: number) => {
    const currentToRepeat = toRepeat.value
    if (!currentToRepeat) return

    const item = currentToRepeat[index]
    if (item === undefined) return
    toRepeat.value = [
      ...currentToRepeat.slice(0, index),
      ...currentToRepeat.slice(index + 1),
      item,
    ]
  }

  const remainingCount = computed(
    () => (toRepeat.value?.length ?? 0) - currentIndex.value
  )

  const clearPreviousResults = () => {
    previousResults.value = []
  }

  return {
    toRepeatCount,
    setToRepeatCount,
    decrementToRepeatCount,
    toRepeat,
    currentIndex,
    previousResults,
    setToRepeat,
    addPreviousResult,
    moveToEnd,
    remainingCount,
    clearPreviousResults,
  }
}
