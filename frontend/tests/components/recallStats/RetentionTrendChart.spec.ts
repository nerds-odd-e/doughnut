import RetentionTrendChart from "@/components/recallStats/RetentionTrendChart.vue"
import helper from "@tests/helpers"
import type { DayRetention } from "@generated/doughnut-backend-api"
import { describe, it, expect } from "vitest"

describe("RetentionTrendChart", () => {
  it("renders a polyline per sufficient day on 0-100% Y; insufficient days greyed (not 0% or 100%)", () => {
    const retentionTrend: DayRetention[] = []
    let sufficient = 0
    let insufficient = 0
    for (let i = 0; i < 30; i++) {
      const isSufficient = i % 4 !== 0
      if (isSufficient) {
        sufficient++
        retentionTrend.push({
          date: `1989-02-${String(i + 1).padStart(2, "0")}`,
          retentionPct: 60 + (i % 30),
          correctCount: 3,
          answeredCount: 4,
          sampleSize: 4,
        })
      } else {
        insufficient++
        // Insufficient: retentionPct null (NOT 0 or 100), answeredCount < 3
        retentionTrend.push({
          date: `1989-02-${String(i + 1).padStart(2, "0")}`,
          retentionPct: undefined,
          correctCount: 1,
          answeredCount: 1,
          sampleSize: 1,
        })
      }
    }

    const wrapper = helper
      .component(RetentionTrendChart)
      .withProps({ retentionTrend })
      .mount()

    const polyline = wrapper.find('[data-testid="retention-trend-polyline"]')
    expect(polyline.exists()).toBe(true)
    const points = (polyline.attributes("points") ?? "")
      .trim()
      .split(/\s+/)
      .filter(Boolean)
    expect(points).toHaveLength(sufficient)

    const greyed = wrapper.findAll(
      '[data-testid="retention-trend-insufficient"]'
    )
    expect(greyed).toHaveLength(insufficient)

    // Insufficient days are never rendered as 0% or 100%: they have no polyline point,
    // only a grey marker. The polyline points count equals the sufficient days only.
    expect(points.length).toBe(sufficient)
    expect(greyed.length).toBe(insufficient)
  })

  it("renders a titled 0-100% axis with x-date labels", () => {
    const retentionTrend: DayRetention[] = []
    for (let i = 0; i < 30; i++) {
      retentionTrend.push({
        date: `1989-02-${String(i + 1).padStart(2, "0")}`,
        retentionPct: 60 + (i % 30),
        correctCount: 3,
        answeredCount: 4,
        sampleSize: 4,
      })
    }
    const wrapper = helper
      .component(RetentionTrendChart)
      .withProps({ retentionTrend })
      .mount()

    const texts = wrapper.findAll("text").map((t) => t.text())
    expect(texts).toContain("Retention (%/day)")
    expect(texts).toContain("0%")
    expect(texts).toContain("50%")
    expect(texts).toContain("100%")
    expect(texts).toContain("2/1")
  })
})
