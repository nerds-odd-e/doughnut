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

function hueOf(fill: string): number {
  const oklch = fill.match(/oklch\([^)]*\s([\d.]+)\)/)
  if (oklch) return Number(oklch[1])
  const oklab = fill.match(/oklab\(\s*[\d.]+\s+(-?[\d.]+)\s+(-?[\d.]+)\)/)
  if (oklab) {
    const [, a, b] = oklab
    const degrees = (Math.atan2(Number(b), Number(a)) * 180) / Math.PI
    return degrees < 0 ? degrees + 360 : degrees
  }
  throw new Error(`Unrecognized fill format: ${fill}`)
}

afterEach(() => {
  document.documentElement.removeAttribute("data-theme")
})

function buildCalendar(): DayCount[] {
  // Two weeks is enough to assert empty vs filled theme tokens.
  const end = Math.round(Date.UTC(2026, 6, 14) / 86_400_000)
  const filled = Math.round(Date.UTC(2026, 6, 1) / 86_400_000)
  const days: DayCount[] = []
  for (let i = 13; i >= 0; i--) {
    const ed = end - i
    const dt = new Date(ed * 86_400_000)
    const date = `${dt.getUTCFullYear()}-${String(dt.getUTCMonth() + 1).padStart(2, "0")}-${String(dt.getUTCDate()).padStart(2, "0")}`
    days.push({
      date,
      count: ed === filled ? 10 : 0,
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

  it("retention heatmap uses a red/green scale anchored at requested retention (90%), with granularity within each side", () => {
    const answered = emptyGrid()
    const correct = emptyGrid()
    setCell(answered, 0, 0, 10)
    setCell(correct, 0, 0, 10) // 100% observed rate -> deep green
    setCell(answered, 1, 0, 10)
    setCell(correct, 1, 0, 9) // 90% observed rate -> lightest green (requested retention hinge)
    setCell(answered, 2, 0, 10)
    setCell(correct, 2, 0, 8) // 80% observed rate -> red-leaning (below requested retention)
    setCell(answered, 3, 0, 10)
    setCell(correct, 3, 0, 6) // 60% observed rate -> deep red
    setCell(answered, 4, 0, 100)
    setCell(correct, 4, 0, 87) // 87% observed rate -> red-leaning (below requested retention)
    const wrapper = helper
      .component(WeekdayHourHeatmap)
      .withProps({ mode: "retention", counts: answered, correct })
      .mount({ attachTo: document.body })

    const cells = wrapper.findAll('[data-testid="heatmap-cell"]')
    const byWeekday = (wd: number) =>
      cells.find((c) => c.attributes("data-weekday") === String(wd))!
    const deepGreen = byWeekday(0)
    const lightestGreen = byWeekday(1)
    const mildRed = byWeekday(2)
    const deepRed = byWeekday(3)
    const belowRequestedRetention = byWeekday(4)

    setTheme("light")

    // granularity: cells on the same side of 90% still render distinct colors
    expect(fillOf(deepGreen.element)).not.toBe(fillOf(lightestGreen.element))
    expect(fillOf(mildRed.element)).not.toBe(fillOf(deepRed.element))

    // hue direction: >= 90% is greenish, < 90% is reddish
    expect(hueOf(fillOf(deepGreen.element))).toBeGreaterThan(90)
    expect(hueOf(fillOf(lightestGreen.element))).toBeGreaterThan(90)
    expect(hueOf(fillOf(mildRed.element))).toBeLessThan(60)
    expect(hueOf(fillOf(deepRed.element))).toBeLessThan(60)
    expect(hueOf(fillOf(belowRequestedRetention.element))).toBeLessThan(60)
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
