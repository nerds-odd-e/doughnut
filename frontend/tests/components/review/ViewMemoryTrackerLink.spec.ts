import { describe, it, expect } from "vitest"
import helper from "@tests/helpers"
import ViewMemoryTrackerLink from "@/components/review/ViewMemoryTrackerLink.vue"

describe("ViewMemoryTrackerLink", () => {
  it("renders a link to the memory tracker page", () => {
    const wrapper = helper
      .component(ViewMemoryTrackerLink)
      .withProps({ memoryTrackerId: 123 })
      .mount()

    const link = wrapper.find("a.router-link")
    expect(link.exists()).toBe(true)
    expect(link.text()).toBe("View Memory Tracker")
    expect(JSON.parse(link.attributes("to")!)).toMatchObject({
      name: "memoryTrackerShow",
      params: { memoryTrackerId: 123 },
    })
  })

  it("has the correct CSS classes", () => {
    const wrapper = helper
      .component(ViewMemoryTrackerLink)
      .withProps({ memoryTrackerId: 456 })
      .mount()

    const link = wrapper.find("a")
    expect(link.classes()).toContain("daisy-link")
    expect(link.classes()).toContain("daisy-link-primary")
    expect(link.classes()).toContain("daisy-mt-4")
    expect(link.classes()).toContain("daisy-inline-block")
  })
})
