import AccuracyTile from "@/components/recallStats/AccuracyTile.vue"
import helper from "@tests/helpers"
import { describe, it, expect } from "vitest"

describe("AccuracyTile", () => {
  it("shows recalling better than expected for a clearly positive residual", () => {
    const wrapper = helper
      .component(AccuracyTile)
      .withProps({
        accuracy: { standardizedResidual: 1.8, sampleSize: 6 },
      })
      .mount()

    const tile = wrapper.find('[data-testid="accuracy-tile"]')
    expect(tile.text()).toContain("Recalling better than expected")
    expect(tile.text()).toContain("based on 6 answered this morning")
  })

  it("shows recalling worse than expected for a clearly negative residual", () => {
    const wrapper = helper
      .component(AccuracyTile)
      .withProps({
        accuracy: { standardizedResidual: -1.8, sampleSize: 4 },
      })
      .mount()

    expect(wrapper.find('[data-testid="accuracy-tile"]').text()).toContain(
      "Recalling worse than expected"
    )
  })

  it("shows an about-as-expected message for a small residual", () => {
    const wrapper = helper
      .component(AccuracyTile)
      .withProps({
        accuracy: { standardizedResidual: 0.1, sampleSize: 6 },
      })
      .mount()

    expect(wrapper.find('[data-testid="accuracy-tile"]').text()).toContain(
      "Recalling about as expected"
    )
  })

  it("shows a calm message when the statistic is absent (e.g. zero denominator)", () => {
    const wrapper = helper
      .component(AccuracyTile)
      .withProps({ accuracy: { sampleSize: 0 } })
      .mount()

    expect(wrapper.find('[data-testid="accuracy-tile"]').text()).toContain(
      "Not enough recall history yet for an accuracy comparison"
    )
  })
})
