import { ref } from "vue"

const dueCount = ref<number | undefined>(undefined)
const assimilatedCountOfTheDay = ref<number | undefined>(undefined)
const totalUnassimilatedCount = ref<number | undefined>(undefined)

export function useAssimilationCount() {
  const setDueCount = (count: number | undefined) => {
    dueCount.value = count
  }

  const setAssimilatedCountOfTheDay = (count: number | undefined) => {
    assimilatedCountOfTheDay.value = count
  }

  const setTotalUnassimilatedCount = (count: number | undefined) => {
    totalUnassimilatedCount.value = count
  }

  const incrementAssimilatedCount = () => {
    if (assimilatedCountOfTheDay.value !== undefined) {
      assimilatedCountOfTheDay.value += 1
    }
    if (totalUnassimilatedCount.value !== undefined) {
      totalUnassimilatedCount.value -= 1
    }
  }

  return {
    dueCount,
    setDueCount,
    assimilatedCountOfTheDay,
    setAssimilatedCountOfTheDay,
    totalUnassimilatedCount,
    setTotalUnassimilatedCount,
    incrementAssimilatedCount,
  }
}
