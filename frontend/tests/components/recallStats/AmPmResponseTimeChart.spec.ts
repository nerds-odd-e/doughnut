import AmPmResponseTimeChart from "@/components/recallStats/AmPmResponseTimeChart.vue"
import helper from "@tests/helpers"
import type { AmPmResponseTime } from "@generated/doughnut-backend-api"
import { describe, it, expect } from "vitest"

describe("AmPmResponseTimeChart", () => {
  it("renders a seconds y-axis, bar value labels, and time-of-day x labels", () => {
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
      .mount()

    const texts = wrapper.findAll("text").map((t) => t.text())
    expect(texts).toContain("0s")
    expect(texts).toContain("6.0s")
    expect(texts).toContain("5.0s")
    expect(texts).toContain("Morning")
    expect(texts).toContain("Afternoon")
    expect(texts).toContain("Evening")
    expect(texts).toContain("Night")
  })
})
