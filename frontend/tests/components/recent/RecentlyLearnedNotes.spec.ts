import RecentlyLearnedNotes from "@/components/recent/RecentlyLearnedNotes.vue"
import { flushPromises } from "@vue/test-utils"
import helper from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"

describe("RecentlyLearnedNotes", () => {
  const mockReviewPoints = [
    makeMe.aReviewPoint
      .initialReviewedAt("2024-01-01T00:00:00Z")
      .removedFromReview(false)
      .please(),
    makeMe.aReviewPoint
      .initialReviewedAt("2024-01-02T00:00:00Z")
      .removedFromReview(true)
      .please(),
  ]

  beforeEach(() => {
    helper.managedApi.restReviewPointController.getRecentReviewPoints = vitest
      .fn()
      .mockResolvedValue(mockReviewPoints)
  })

  it("fetches and displays recent review points", async () => {
    const wrapper = helper.component(RecentlyLearnedNotes).withRouter().mount()

    await flushPromises()

    // Verify API was called
    expect(
      helper.managedApi.restReviewPointController.getRecentReviewPoints
    ).toBeCalled()

    // Verify review points are displayed
    const rows = wrapper.findAll("tbody tr")
    expect(rows).toHaveLength(2)

    // Verify removed memory tracker has correct styling
    const removedRow = rows[1]
    expect(removedRow?.classes()).toContain("removed")
  })
})
