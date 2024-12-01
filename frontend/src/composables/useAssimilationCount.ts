import { ref } from "vue"

const dueCount = ref<number | undefined>(undefined)

export function useAssimilationCount() {
  const setDueCount = (count: number | undefined) => {
    dueCount.value = count
  }

  return {
    dueCount,
    setDueCount,
  }
}
