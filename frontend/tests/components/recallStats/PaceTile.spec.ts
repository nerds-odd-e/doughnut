import PaceTile from "@/components/recallStats/PaceTile.vue"
import helper from "@tests/helpers"
import { describe, it, expect } from "vitest"

describe("PaceTile", () => {
  it("shows a slower-than-usual pace with the sample context", () => {
    const wrapper = helper
      .component(PaceTile)
      .withProps({
        pace: { pctVsUsual: 42, sampleSize: 5, totalAnsweredToday: 8 },
      })
      .mount()

    const tile = wrapper.find('[data-testid="pace-tile"]')
    expect(tile.text()).toContain("42% slower than usual")
    expect(tile.text()).toContain("based on 5 of 8 answered this morning")
  })

  it("shows a faster-than-usual pace", () => {
    const wrapper = helper
      .component(PaceTile)
      .withProps({
        pace: { pctVsUsual: -20, sampleSize: 3, totalAnsweredToday: 4 },
      })
      .mount()

    expect(wrapper.find('[data-testid="pace-tile"]').text()).toContain(
      "20% faster than usual"
    )
  })

  it("shows a calm message when there is not enough history for a comparison", () => {
    const wrapper = helper
      .component(PaceTile)
      .withProps({ pace: { sampleSize: 0 } })
      .mount()

    expect(wrapper.find('[data-testid="pace-tile"]').text()).toContain(
      "Not enough recall history yet for a pace comparison"
    )
  })
})
