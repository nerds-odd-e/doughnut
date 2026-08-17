import { MemoryTrackerController } from "@generated/doughnut-backend-api/sdk.gen"
import RecentlyAssimilatedNotes from "@/components/recent/RecentlyAssimilatedNotes.vue"
import { flushPromises } from "@vue/test-utils"
import helper, { mockSdkService } from "@tests/helpers"
import makeMe from "doughnut-test-fixtures/makeMe"
import { describe, it, expect } from "vitest"

describe("RecentlyAssimilatedNotes", () => {
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

  it("fetches and displays recent memory trackers", async () => {
    const getRecentMemoryTrackersSpy = mockSdkService(
      MemoryTrackerController,
      "getRecentMemoryTrackers",
      mockMemoryTrackers
    )
    const wrapper = helper
      .component(RecentlyAssimilatedNotes)
      .withRouter()
      .mount()

    await flushPromises()

    expect(getRecentMemoryTrackersSpy).toBeCalled()

    const rows = wrapper.findAll("tbody tr")
    expect(rows).toHaveLength(2)

    const removedRow = rows[1]
    expect(removedRow?.classes()).toContain("removed")
  })
})
