import ResponseTimeTrendChart from "@/components/recallStats/ResponseTimeTrendChart.vue"
import helper from "@tests/helpers"
import type { DayAvgResponseTime } from "@generated/doughnut-backend-api"
import { describe, it, expect } from "vitest"

describe("ResponseTimeTrendChart", () => {
  it("renders a polyline with one point per sufficient-data day; insufficient days are greyed", () => {
    const trend: DayAvgResponseTime[] = []
    let sufficient = 0
    let insufficient = 0
    for (let i = 0; i < 30; i++) {
      const isSufficient = i % 3 !== 0
      if (isSufficient) {
        sufficient++
        trend.push({
          date: `1989-02-${String(i + 1).padStart(2, "0")}`,
          avgMs: 3000 + i * 100,
          sampleSize: 3,
        })
      } else {
        insufficient++
        trend.push({
          date: `1989-02-${String(i + 1).padStart(2, "0")}`,
          avgMs: undefined,
          sampleSize: 1,
        })
      }
    }

    const wrapper = helper
      .component(ResponseTimeTrendChart)
      .withProps({ trend })
      .mount()

    const polyline = wrapper.find('[data-testid="response-time-polyline"]')
    expect(polyline.exists()).toBe(true)
    const points = (polyline.attributes("points") ?? "")
      .trim()
      .split(/\s+/)
      .filter(Boolean)
    expect(points).toHaveLength(sufficient)

    const greyed = wrapper.findAll('[data-testid="response-time-insufficient"]')
    expect(greyed).toHaveLength(insufficient)
  })
})
