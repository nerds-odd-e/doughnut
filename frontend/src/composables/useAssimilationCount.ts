import type { AssimilationCountDto } from "@generated/doughnut-backend-api"
import { ref } from "vue"

export type AssimilationProgressCounts = {
  assimilated: number
  planned: number
  total: number
}

export function assimilationProgressFromCounts(
  assimilatedCountOfTheDay: number | undefined,
  dueCount: number | undefined,
  totalUnassimilatedCount: number | undefined
): AssimilationProgressCounts | null {
  if (
    assimilatedCountOfTheDay === undefined ||
    totalUnassimilatedCount === undefined
  ) {
    return null
  }
  const assimilated = assimilatedCountOfTheDay
  return {
    assimilated,
    planned: assimilated + (dueCount ?? 0),
    total: assimilated + totalUnassimilatedCount,
  }
}

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
