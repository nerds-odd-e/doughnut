import RecallActivityCalendar from "@/components/recallStats/RecallActivityCalendar.vue"
import helper from "@tests/helpers"
import type { DayCount } from "@generated/doughnut-backend-api"
import { describe, it, expect } from "vitest"

const cellSize = 12

function weekdayOf(date?: string): number {
  if (!date) return 0
  const [y = 1970, m = 1, d = 1] = date.split("-").map(Number)
  return new Date(Date.UTC(y, m - 1, d)).getUTCDay()
}
function epochDayOf(date?: string): number {
  if (!date) return 0
  const [y = 1970, m = 1, d = 1] = date.split("-").map(Number)
  return Math.round(Date.UTC(y, m - 1, d) / 86_400_000)
}
function isoFromEpochDay(ed: number): string {
  const dt = new Date(ed * 86_400_000)
  const y = dt.getUTCFullYear()
  const m = String(dt.getUTCMonth() + 1).padStart(2, "0")
  const d = String(dt.getUTCDate()).padStart(2, "0")
  return `${y}-${m}-${d}`
}

// 365 consecutive days ending 2026-07-24, with a known max-count day.
function buildCalendar(maxDate: string, maxCount: number): DayCount[] {
  const end = epochDayOf("2026-07-24")
  const days: DayCount[] = []
  for (let i = 364; i >= 0; i--) {
    const date = isoFromEpochDay(end - i)
    const count = date === maxDate ? maxCount : 0
    days.push({ date, count })
  }
  return days
}

describe("RecallActivityCalendar", () => {
  it("aligns cells to weekdays, darkest for the max-count day on its weekday row, empty for zero days", () => {
    const maxDate = "2026-07-01"
    const calendar = buildCalendar(maxDate, 10)
    const wrapper = helper
      .component(RecallActivityCalendar)
      .withProps({ calendar })
      .mount()

    const cells = wrapper.findAll('[data-testid="calendar-cell"]')
    // Rectangular week x weekday grid (leading/trailing empty cells included)
    const firstSunday =
      epochDayOf(calendar[0]!.date) - weekdayOf(calendar[0]!.date)
    const lastSunday =
      epochDayOf(calendar[calendar.length - 1]!.date) -
      weekdayOf(calendar[calendar.length - 1]!.date)
    const cols = (lastSunday - firstSunday) / 7 + 1
    expect(cells).toHaveLength(cols * 7)

    const maxCell = cells.find((c) => c.attributes("data-count") === "10")
    expect(maxCell).toBeDefined()
    expect(maxCell!.classes()).toContain("rs-cal-l4")
    // The max day lands on the row matching its real weekday
    const maxRow = Number(maxCell!.attributes("y")) / cellSize
    expect(maxRow).toBe(weekdayOf(maxDate))

    const zeroCell = cells.find((c) => c.attributes("data-count") === "0")
    expect(zeroCell).toBeDefined()
    expect(zeroCell!.classes()).toContain("rs-cal-empty")
  })

  it("renders GitHub-style weekday and month captions", () => {
    const wrapper = helper
      .component(RecallActivityCalendar)
      .withProps({ calendar: buildCalendar("2026-07-01", 10) })
      .mount()

    const axisTexts = wrapper.findAll("text.rs-cal-axis").map((t) => t.text())
    expect(axisTexts).toContain("Mon")
    expect(axisTexts).toContain("Wed")
    expect(axisTexts).toContain("Fri")
    // The 365-day window spans ~13 months, so multiple month captions appear
    expect(axisTexts.filter((t) => t === "Jul").length).toBeGreaterThan(0)
  })
})
