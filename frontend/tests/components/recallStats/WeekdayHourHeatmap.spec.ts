import WeekdayHourHeatmap from "@/components/recallStats/WeekdayHourHeatmap.vue"
import helper from "@tests/helpers"
import { describe, it, expect } from "vitest"

function emptyGrid(): number[][] {
  return Array.from({ length: 7 }, () => Array.from({ length: 24 }, () => 0))
}

describe("WeekdayHourHeatmap", () => {
  it("count mode: renders 168 cells and the peak cell gets the darkest fill", () => {
    const counts = emptyGrid()
    counts[3]![14] = 10 // peak at Thursday 14:00
    counts[1]![9] = 3

    const wrapper = helper
      .component(WeekdayHourHeatmap)
      .withProps({ mode: "count", counts })
      .mount()

    const cells = wrapper.findAll('[data-testid="heatmap-cell"]')
    expect(cells).toHaveLength(168)

    const peak = cells.find((c) => c.attributes("data-count") === "10")
    expect(peak).toBeDefined()
    expect(peak!.classes()).toContain("rs-hm-c4")
  })

  it("retention mode: high-retention cell gets the greenest fill and an insufficient cell is greyed", () => {
    const answered = emptyGrid()
    const correct = emptyGrid()
    // High retention: Wed 10:00, 5/5 correct -> 100%
    answered[2]![10] = 5
    correct[2]![10] = 5
    // Insufficient: Fri 09:00, 1/2 correct -> answered < 3
    answered[4]![9] = 2
    correct[4]![9] = 1

    const wrapper = helper
      .component(WeekdayHourHeatmap)
      .withProps({ mode: "retention", counts: answered, correct })
      .mount()

    const cells = wrapper.findAll('[data-testid="heatmap-cell"]')
    expect(cells).toHaveLength(168)

    const high = cells.find(
      (c) =>
        c.attributes("data-weekday") === "2" &&
        c.attributes("data-hour") === "10"
    )
    expect(high).toBeDefined()
    expect(high!.classes()).toContain("rs-hm-r4")

    const insufficient = cells.find(
      (c) =>
        c.attributes("data-weekday") === "4" &&
        c.attributes("data-hour") === "9"
    )
    expect(insufficient).toBeDefined()
    expect(insufficient!.classes()).toContain("rs-hm-insufficient")
  })

  it("renders weekday and hour axis captions and a caption label", () => {
    const counts = emptyGrid()
    const wrapper = helper
      .component(WeekdayHourHeatmap)
      .withProps({ mode: "count", counts, label: "Reviews" })
      .mount()

    const texts = wrapper.findAll("text").map((t) => t.text())
    expect(texts).toContain("Reviews")
    expect(texts).toContain("Mon")
    expect(texts).toContain("Wed")
    expect(texts).toContain("Fri")
    expect(texts).toContain("0")
    expect(texts).toContain("12")
  })
})
