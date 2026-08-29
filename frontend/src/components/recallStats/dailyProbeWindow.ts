import type { DailyProbeDay } from "@generated/donut-backend-api"

export function localDayIso(timeZone: string, instant = new Date()): string {
  const parts = new Intl.DateTimeFormat("en-US", {
    timeZone,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(instant)
  const lookup = Object.fromEntries(
    parts.map((part) => [part.type, part.value])
  )
  return `${lookup.year}-${lookup.month}-${lookup.day}`
}

export function isoDateDaysBefore(iso: string, days: number): string {
  const [year, month, day] = iso.split("-").map(Number)
  return new Date(Date.UTC(year, month - 1, day - days))
    .toISOString()
    .slice(0, 10)
}

export function dailyProbePointsInWindow(
  points: DailyProbeDay[],
  selectedWindow: number | "all",
  todayIso: string
): DailyProbeDay[] {
  if (selectedWindow === "all") return points
  const cutoff = isoDateDaysBefore(todayIso, selectedWindow - 1)
  return points.filter((point) => (point.date ?? "") >= cutoff)
}
