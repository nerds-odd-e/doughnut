import RecallActivityCalendar from "@/components/recallStats/RecallActivityCalendar.vue"
import helper from "@tests/helpers"
import type { DayCount } from "@generated/doughnut-backend-api"
import { describe, it, expect } from "vitest"

function buildCalendar(maxIndex: number, maxCount: number): DayCount[] {
  const days: DayCount[] = []
  for (let i = 0; i < 365; i++) {
    let count = 0
    if (i === maxIndex) count = maxCount
    else if (i % 7 === 0) count = 2
    days.push({
      date: `1989-01-${String((i % 28) + 1).padStart(2, "0")}`,
      count,
    })
  }
  return days
}

describe("RecallActivityCalendar", () => {
  it("renders 365 cells, darkest for the max-count day and empty for zero days", () => {
    const calendar = buildCalendar(100, 10)
    const wrapper = helper
      .component(RecallActivityCalendar)
      .withProps({ calendar })
      .mount()

    const cells = wrapper.findAll('[data-testid="calendar-cell"]')
    expect(cells).toHaveLength(365)

    const maxCell = cells.find((c) => c.attributes("data-count") === "10")
    expect(maxCell).toBeDefined()
    expect(maxCell!.classes()).toContain("rs-cal-l4")

    const zeroCell = cells.find((c) => c.attributes("data-count") === "0")
    expect(zeroCell).toBeDefined()
    expect(zeroCell!.classes()).toContain("rs-cal-empty")
  })
})
