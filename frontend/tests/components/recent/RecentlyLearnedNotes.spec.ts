import RecentlyLearnedNotes from "@/components/recent/RecentlyLearnedNotes.vue"
import { requestRecentMemoryTrackersRefresh } from "@/composables/useRecentMemoryTrackersRefresh"
import { flushPromises } from "@vue/test-utils"
import helper, { mockSdkService } from "@tests/helpers"
import makeMe from "doughnut-test-fixtures/makeMe"
import { describe, it, expect, beforeEach } from "vitest"

describe("RecentlyLearnedNotes", () => {
  const mockMemoryTrackers = [
    makeMe.aMemoryTracker
      .assimilatedAt("2024-01-01T00:00:00Z")
      .removedFromTracking(false)
      .please(),
    makeMe.aMemoryTracker
      .assimilatedAt("2024-01-02T00:00:00Z")
      .removedFromTracking(true)
      .please(),
  ]

  beforeEach(() => {
    mockSdkService("getRecentMemoryTrackers", mockMemoryTrackers)
  })

  it("fetches and displays recent memory trackers", async () => {
    const getRecentMemoryTrackersSpy = mockSdkService(
      "getRecentMemoryTrackers",
      mockMemoryTrackers
    )
    const wrapper = helper.component(RecentlyLearnedNotes).withRouter().mount()

    await flushPromises()

    // Verify API was called
    expect(getRecentMemoryTrackersSpy).toBeCalled()

    // Verify memory trackers are displayed
    const rows = wrapper.findAll("tbody tr")
    expect(rows).toHaveLength(2)

    // Verify removed memory tracker has correct styling
    const removedRow = rows[1]
    expect(removedRow?.classes()).toContain("removed")
  })

  it("refetches when recent memory trackers refresh is requested", async () => {
    const getRecentMemoryTrackersSpy = mockSdkService(
      "getRecentMemoryTrackers",
      mockMemoryTrackers
    )
    getRecentMemoryTrackersSpy.mockClear()
    helper.component(RecentlyLearnedNotes).withRouter().mount()

    await flushPromises()
    const callsAfterMount = getRecentMemoryTrackersSpy.mock.calls.length
    expect(callsAfterMount).toBeGreaterThanOrEqual(1)

    requestRecentMemoryTrackersRefresh()
    await flushPromises()

    expect(getRecentMemoryTrackersSpy.mock.calls.length).toBeGreaterThan(
      callsAfterMount
    )
  })
})
