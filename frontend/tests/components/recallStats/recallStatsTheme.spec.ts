import RecallActivityCalendar from "@/components/recallStats/RecallActivityCalendar.vue"
import WeekdayHourHeatmap from "@/components/recallStats/WeekdayHourHeatmap.vue"
import AmPmResponseTimeChart from "@/components/recallStats/AmPmResponseTimeChart.vue"
import ResponseTimeTrendChart from "@/components/recallStats/ResponseTimeTrendChart.vue"
import helper from "@tests/helpers"
import type {
  DayCount,
  AmPmResponseTime,
  DayAvgResponseTime,
} from "@generated/doughnut-backend-api"
import { afterEach, describe, expect, it } from "vitest"

const OLD_EMPTY = "rgb(235, 237, 240)"
const OLD_DARK_GREEN = "rgb(33, 110, 57)"

function setTheme(theme: "light" | "dark") {
  document.documentElement.setAttribute("data-theme", theme)
}

function fillOf(el: Element): string {
  return window.getComputedStyle(el).fill
}

afterEach(() => {
  document.documentElement.removeAttribute("data-theme")
})

function buildCalendar(): DayCount[] {
  const days: DayCount[] = []
  for (let i = 0; i < 365; i++) {
    days.push({
      date: `1989-01-${String((i % 28) + 1).padStart(2, "0")}`,
      count: i === 100 ? 10 : 0,
    })
  }
  return days
}

function emptyGrid(): number[][] {
  return Array.from({ length: 7 }, () => Array.from({ length: 24 }, () => 0))
}

function setCell(grid: number[][], wd: number, hr: number, value: number) {
  grid[wd]![hr] = value
}

describe("recall stats charts use theme tokens (dark-mode safe)", () => {
  it("calendar empty and filled cells are not hardcoded GitHub hex and adapt to dark theme", () => {
    const wrapper = helper
      .component(RecallActivityCalendar)
      .withProps({ calendar: buildCalendar() })
      .mount({ attachTo: document.body })

    const cells = wrapper.findAll('[data-testid="calendar-cell"]')
    const empty = cells.find((c) => c.attributes("data-count") === "0")!
    const filled = cells.find((c) => c.attributes("data-count") === "10")!

    setTheme("light")
    const lightEmpty = fillOf(empty.element)
    const lightFilled = fillOf(filled.element)
    expect(lightEmpty).not.toBe(OLD_EMPTY)
    expect(lightFilled).not.toBe(OLD_DARK_GREEN)

    setTheme("dark")
    const darkEmpty = fillOf(empty.element)
    expect(darkEmpty).not.toBe(OLD_EMPTY)
    expect(darkEmpty).not.toBe(lightEmpty)
  })

  it("count heatmap drops hardcoded hex for empty and filled cells", () => {
    const counts = emptyGrid()
    setCell(counts, 0, 0, 10)
    const wrapper = helper
      .component(WeekdayHourHeatmap)
      .withProps({ mode: "count", counts })
      .mount({ attachTo: document.body })

    const cells = wrapper.findAll('[data-testid="heatmap-cell"]')
    const empty = cells.find((c) => c.attributes("data-count") === "0")!
    const filled = cells.find((c) => c.attributes("data-count") === "10")!

    setTheme("light")
    expect(fillOf(empty.element)).not.toBe(OLD_EMPTY)
    expect(fillOf(filled.element)).not.toBe(OLD_DARK_GREEN)
  })

  it("retention heatmap ramp is distinct across levels (single-hue, not red-to-green)", () => {
    const answered = emptyGrid()
    const correct = emptyGrid()
    setCell(answered, 0, 0, 10)
    setCell(correct, 0, 0, 10)
    setCell(answered, 1, 0, 10)
    setCell(correct, 1, 0, 1)
    setCell(answered, 2, 0, 10)
    setCell(correct, 2, 0, 3)
    const wrapper = helper
      .component(WeekdayHourHeatmap)
      .withProps({ mode: "retention", counts: answered, correct })
      .mount({ attachTo: document.body })

    const cells = wrapper.findAll('[data-testid="heatmap-cell"]')
    const r4 = cells.find((c) => c.attributes("data-correct") === "10")!
    const r1 = cells.find((c) => c.attributes("data-correct") === "1")!
    const r2 = cells.find((c) => c.attributes("data-correct") === "3")!

    setTheme("light")
    const f1 = fillOf(r1.element)
    const f2 = fillOf(r2.element)
    const f4 = fillOf(r4.element)
    expect(new Set([f1, f2, f4]).size).toBe(3)
  })

  it("AM/PM label and bars adapt to dark theme (readable)", () => {
    const amPm: AmPmResponseTime = {
      morningMs: 5000,
      morningSamples: 5,
      afternoonMs: 6000,
      afternoonSamples: 5,
      eveningMs: undefined,
      eveningSamples: 2,
      nightMs: undefined,
      nightSamples: 1,
    }
    const wrapper = helper
      .component(AmPmResponseTimeChart)
      .withProps({ amPm })
      .mount({ attachTo: document.body })

    const label = wrapper.find(".rs-ampm-label").element
    const bar = wrapper.find('[data-testid="am-pm-bar"]').element

    setTheme("light")
    const lightLabel = fillOf(label)
    const lightBar = fillOf(bar)
    setTheme("dark")
    const darkLabel = fillOf(label)
    const darkBar = fillOf(bar)

    expect(darkLabel).not.toBe(lightLabel)
    expect(darkBar).not.toBe(lightBar)
  })

  it("trend line stroke uses the primary token, not a hardcoded color", () => {
    const trend: DayAvgResponseTime[] = [
      { date: "2026-07-23", avgMs: 4000, sampleSize: 4 },
      { date: "2026-07-24", avgMs: 5000, sampleSize: 4 },
    ]
    const wrapper = helper
      .component(ResponseTimeTrendChart)
      .withProps({ trend })
      .mount({ attachTo: document.body })

    const polyline = wrapper.find(
      '[data-testid="response-time-polyline"]'
    ).element
    expect(polyline.getAttribute("style")).toContain("var(--color-primary)")
  })
})
