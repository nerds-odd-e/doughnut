import RecentlyReviewedNotes from "@/components/recent/RecentlyReviewedNotes.vue"
import { flushPromises } from "@vue/test-utils"
import helper from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"

describe("RecentlyReviewedNotes", () => {
  const mockReviewPoints = [
    makeMe.aReviewPoint
      .initialReviewedAt("2024-01-01T00:00:00Z")
      .nextReviewAt("2024-01-08T00:00:00Z")
      .repetitionCount(3)
      .forgettingCurveIndex(2)
      .removedFromReview(false)
      .please(),
    makeMe.aReviewPoint
      .initialReviewedAt("2024-01-02T00:00:00Z")
      .nextReviewAt("2024-01-09T00:00:00Z")
      .repetitionCount(1)
      .forgettingCurveIndex(1)
      .removedFromReview(true)
      .please(),
  ]

  beforeEach(() => {
    helper.managedApi.restReviewPointController.getRecentlyReviewedPoints =
      vitest.fn().mockResolvedValue(mockReviewPoints)
  })

  it("fetches and displays recently reviewed points", async () => {
    const wrapper = helper.component(RecentlyReviewedNotes).withRouter().mount()

    await flushPromises()

    // Verify API was called
    expect(
      helper.managedApi.restReviewPointController.getRecentlyReviewedPoints
    ).toBeCalled()

    // Verify review points are displayed
    const rows = wrapper.findAll("tbody tr")
    expect(rows).toHaveLength(2)

    // Verify all columns are present
    const headers = wrapper.findAll("thead th")
    expect(headers).toHaveLength(6)

    // Verify removed review point has correct styling
    const removedRow = rows[1]
    expect(removedRow?.classes()).toContain("removed")
  })
})
