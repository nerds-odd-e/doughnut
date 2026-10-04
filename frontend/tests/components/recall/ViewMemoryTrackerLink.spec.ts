import { flushPromises } from "@vue/test-utils"
import { describe, it, expect } from "vitest"
import helper, {
  countHistoryEntriesAdded,
  productionRouterAt,
} from "@tests/helpers"
import ViewMemoryTrackerLink from "@/components/recall/ViewMemoryTrackerLink.vue"

describe("ViewMemoryTrackerLink", () => {
  it("renders a button that navigates to the memory tracker page", async () => {
    const router = await productionRouterAt({ name: "recall" })
    const wrapper = helper
      .component(ViewMemoryTrackerLink)
      .withProps({ memoryTrackerId: 123 })
      .withRouter(router)
      .mount()

    const button = wrapper.find("button")
    expect(button.exists()).toBe(true)
    expect(button.text()).toBe("View Memory Tracker")

    const historyEntriesAdded = countHistoryEntriesAdded(router)
    await button.trigger("click")
    await flushPromises()
    expect(router.currentRoute.value).toMatchObject({
      name: "memoryTrackerShow",
      params: { memoryTrackerId: "123" },
    })
    expect(historyEntriesAdded()).toBe(1)
  })

  it("has the correct CSS classes", async () => {
    const wrapper = helper
      .component(ViewMemoryTrackerLink)
      .withProps({ memoryTrackerId: 456 })
      .withRouter(await productionRouterAt({ name: "recall" }))
      .mount()

    const button = wrapper.find("button")
    expect(button.classes()).toContain("daisy-btn")
    expect(button.classes()).toContain("daisy-btn-primary")
    expect(button.classes()).toContain("mt-4")
  })
})
