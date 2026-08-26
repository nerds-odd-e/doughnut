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

  it("shows a low-confidence badge when confidence is below 0.5", () => {
    const wrapper = helper
      .component(PaceTile)
      .withProps({
        pace: {
          pctVsUsual: 42,
          sampleSize: 5,
          totalAnsweredToday: 8,
          confidence: 0.3,
        },
      })
      .mount()

    const badge = wrapper.find('[data-testid="recall-pace-low-confidence"]')
    expect(badge.exists()).toBe(true)
    expect(badge.text()).toContain("low confidence")
  })

  it("does not show a low-confidence badge when confidence is 0.5 or above", () => {
    const wrapper = helper
      .component(PaceTile)
      .withProps({
        pace: {
          pctVsUsual: 42,
          sampleSize: 5,
          totalAnsweredToday: 8,
          confidence: 0.5,
        },
      })
      .mount()

    expect(
      wrapper.find('[data-testid="recall-pace-low-confidence"]').exists()
    ).toBe(false)
  })

  it("does not show a low-confidence badge when confidence is absent", () => {
    const wrapper = helper
      .component(PaceTile)
      .withProps({
        pace: { pctVsUsual: 42, sampleSize: 5, totalAnsweredToday: 8 },
      })
      .mount()

    expect(
      wrapper.find('[data-testid="recall-pace-low-confidence"]').exists()
    ).toBe(false)
  })

  it("shows the retrieval-lapse count when there is at least one lapse today", () => {
    const wrapper = helper
      .component(PaceTile)
      .withProps({
        pace: {
          pctVsUsual: 42,
          sampleSize: 5,
          totalAnsweredToday: 8,
          lapseCount: 2,
        },
      })
      .mount()

    const lapseReadout = wrapper.find('[data-testid="recall-pace-lapse-count"]')
    expect(lapseReadout.exists()).toBe(true)
    expect(lapseReadout.text()).toContain("2 retrieval lapses today")
  })

  it("does not show the retrieval-lapse readout when lapseCount is 0", () => {
    const wrapper = helper
      .component(PaceTile)
      .withProps({
        pace: {
          pctVsUsual: 42,
          sampleSize: 5,
          totalAnsweredToday: 8,
          lapseCount: 0,
        },
      })
      .mount()

    expect(
      wrapper.find('[data-testid="recall-pace-lapse-count"]').exists()
    ).toBe(false)
  })

  it("shows a more-erratic-than-usual badge when consistencyZScore is above 1", () => {
    const wrapper = helper
      .component(PaceTile)
      .withProps({
        pace: {
          pctVsUsual: 42,
          sampleSize: 5,
          totalAnsweredToday: 8,
          consistencyZScore: 1.5,
        },
      })
      .mount()

    const badge = wrapper.find('[data-testid="recall-pace-inconsistent"]')
    expect(badge.exists()).toBe(true)
    expect(badge.text()).toContain("more erratic than usual")
  })

  it("does not show the more-erratic-than-usual badge when consistencyZScore is 1 or below", () => {
    const wrapper = helper
      .component(PaceTile)
      .withProps({
        pace: {
          pctVsUsual: 42,
          sampleSize: 5,
          totalAnsweredToday: 8,
          consistencyZScore: 1,
        },
      })
      .mount()

    expect(
      wrapper.find('[data-testid="recall-pace-inconsistent"]').exists()
    ).toBe(false)
  })

  it("does not show the more-erratic-than-usual badge when consistencyZScore is absent", () => {
    const wrapper = helper
      .component(PaceTile)
      .withProps({
        pace: { pctVsUsual: 42, sampleSize: 5, totalAnsweredToday: 8 },
      })
      .mount()

    expect(
      wrapper.find('[data-testid="recall-pace-inconsistent"]').exists()
    ).toBe(false)
  })
})
