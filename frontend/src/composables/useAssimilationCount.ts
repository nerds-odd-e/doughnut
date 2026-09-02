import type { AssimilationCountDto } from "@generated/donut-backend-api"
import { ref } from "vue"

const ABBREVIATION_UNITS: [number, string][] = [
  [1_000_000_000, "b"],
  [1_000_000, "m"],
  [1_000, "k"],
]

export function abbreviateCount(count: number): string {
  const unit = ABBREVIATION_UNITS.find(([threshold]) => count >= threshold)
  if (!unit) {
    return String(count)
  }
  const [divisor, suffix] = unit
  const rounded = Math.round((count / divisor) * 10) / 10
  return `${rounded}${suffix}`
}

export function formatAssimilationBadge(due: number, total: number): string {
  return `${abbreviateCount(due)}/${abbreviateCount(total)}`
}

export function assimilationBadgeTitle(due: number, total: number): string {
  return `${due} due today, ${total} total unassimilated`
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
