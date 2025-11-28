import RecentlyReviewedNotes from "@/components/recent/RecentlyReviewedNotes.vue"
import { flushPromises } from "@vue/test-utils"
import helper, { mockSdkService } from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"
import { createRouter, createWebHistory } from "vue-router"
import routes from "@/routes/routes"

describe("RecentlyReviewedNotes", () => {
  const mockMemoryTrackers = [
    makeMe.aMemoryTracker
      .assimilatedAt("2024-01-01T00:00:00Z")
      .nextRecallAt("2024-01-08T00:00:00Z")
      .repetitionCount(3)
      .forgettingCurveIndex(2)
      .removedFromTracking(false)
      .please(),
    makeMe.aMemoryTracker
      .assimilatedAt("2024-01-02T00:00:00Z")
      .nextRecallAt("2024-01-09T00:00:00Z")
      .repetitionCount(1)
      .forgettingCurveIndex(1)
      .removedFromTracking(true)
      .please(),
  ]

  beforeEach(() => {
    mockSdkService("getRecentlyReviewed", mockMemoryTrackers)
  })

  it("fetches and displays recently reviewed points", async () => {
    const getRecentlyReviewedSpy = mockSdkService(
      "getRecentlyReviewed",
      mockMemoryTrackers
    )
    const wrapper = helper.component(RecentlyReviewedNotes).withRouter().mount()

    await flushPromises()

    // Verify API was called
    expect(getRecentlyReviewedSpy).toBeCalled()

    // Verify memory trackers are displayed
    const rows = wrapper.findAll("tbody tr")
    expect(rows).toHaveLength(2)

    // Verify all columns are present
    const headers = wrapper.findAll("thead th")
    expect(headers).toHaveLength(6)

    // Verify removed memory tracker has correct styling
    const removedRow = rows[1]
    expect(removedRow?.classes()).toContain("removed")
  })

  it("navigates to memory tracker page when row is clicked", async () => {
    const router = createRouter({
      history: createWebHistory(),
      routes,
    })
    const pushSpy = vi.spyOn(router, "push")

    mockSdkService("getRecentlyReviewed", mockMemoryTrackers)
    const wrapper = helper
      .component(RecentlyReviewedNotes)
      .withRouter(router)
      .mount()

    await flushPromises()

    const rows = wrapper.findAll("tbody tr")
    const firstRow = rows[0]
    expect(firstRow).toBeDefined()
    await firstRow!.trigger("click")

    expect(pushSpy).toHaveBeenCalledWith({
      name: "memoryTrackerShow",
      params: { memoryTrackerId: mockMemoryTrackers[0]!.id },
    })
  })
})
