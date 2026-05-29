import type { AssimilationCountDto } from "@generated/doughnut-backend-api"
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

  const incrementAssimilatedCount = (count: number = 1) => {
    if (assimilatedCountOfTheDay.value !== undefined) {
      assimilatedCountOfTheDay.value += count
    }
    if (totalUnassimilatedCount.value !== undefined) {
      totalUnassimilatedCount.value -= count
    }
  }

  const applyAssimilationCountDto = (
    counts: AssimilationCountDto | undefined
  ) => {
    if (!counts) {
      return
    }
    setDueCount(counts.dueCount)
    setAssimilatedCountOfTheDay(counts.assimilatedCountOfTheDay)
    setTotalUnassimilatedCount(counts.totalUnassimilatedCount)
  }

  return {
    dueCount,
    setDueCount,
    assimilatedCountOfTheDay,
    setAssimilatedCountOfTheDay,
    totalUnassimilatedCount,
    setTotalUnassimilatedCount,
    incrementAssimilatedCount,
    applyAssimilationCountDto,
  }
}
