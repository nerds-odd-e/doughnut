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
import { describe, expect, it } from "vitest"

function buildCalendar(): DayCount[] {
  return Array.from({ length: 365 }, (_, i) => ({
    date: `1989-01-${String((i % 28) + 1).padStart(2, "0")}`,
    count: i === 100 ? 10 : 0,
  }))
}

function emptyGrid(): number[][] {
  return Array.from({ length: 7 }, () => Array.from({ length: 24 }, () => 0))
}

// Responsive means the SVG fills its container (width 100%) instead of a fixed pixel
// width. Assert the rendered width matches the parent's content width within 1px.
function fillsParent(svg: Element): boolean {
  const parent = svg.parentElement
  if (!parent) return false
  return Math.abs(svg.getBoundingClientRect().width - parent.clientWidth) <= 1
}

function assertResponsive(svg: Element) {
  expect(svg.getAttribute("viewBox")).not.toBeNull()
  expect(svg.getAttribute("width")).toBeNull()
  expect(fillsParent(svg)).toBe(true)
}

describe("recall stats charts are responsive (fill their column, no fixed width)", () => {
  it("activity calendar svg fills its container", () => {
    const wrapper = helper
      .component(RecallActivityCalendar)
      .withProps({ calendar: buildCalendar() })
      .mount({ attachTo: document.body })

    assertResponsive(wrapper.find("svg").element)
  })

  it("response-time trend svg fills its container", () => {
    const trend: DayAvgResponseTime[] = [
      { date: "2026-07-23", avgMs: 4000, sampleSize: 4 },
      { date: "2026-07-24", avgMs: 5000, sampleSize: 4 },
    ]
    const wrapper = helper
      .component(ResponseTimeTrendChart)
      .withProps({ trend })
      .mount({ attachTo: document.body })

    assertResponsive(wrapper.find("svg").element)
  })

  it("AM/PM chart svg fills its container", () => {
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

    assertResponsive(wrapper.find("svg").element)
  })

  it("weekday-hour heatmap svg fills its container", () => {
    const counts = emptyGrid()
    counts[0]![0] = 10
    const wrapper = helper
      .component(WeekdayHourHeatmap)
      .withProps({ mode: "count", counts })
      .mount({ attachTo: document.body })

    assertResponsive(wrapper.find("svg").element)
  })
})
