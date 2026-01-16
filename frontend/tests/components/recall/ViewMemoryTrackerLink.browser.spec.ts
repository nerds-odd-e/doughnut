import { describe, it, expect, vi } from "vitest"
import helper from "@tests/helpers"
import ViewMemoryTrackerLink from "@/components/recall/ViewMemoryTrackerLink.vue"

const mockedPush = vi.fn()
vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRouter: () => ({
      push: mockedPush,
    }),
  }
})

describe("ViewMemoryTrackerLink", () => {
  it("renders a button that navigates to the memory tracker page", async () => {
    mockedPush.mockClear()
    const wrapper = helper
      .component(ViewMemoryTrackerLink)
      .withProps({ memoryTrackerId: 123 })
      .mount()

    const button = wrapper.find("button")
    expect(button.exists()).toBe(true)
    expect(button.text()).toBe("View Memory Tracker")

    await button.trigger("click")
    expect(mockedPush).toHaveBeenCalledWith({
      name: "memoryTrackerShow",
      params: { memoryTrackerId: 123 },
    })
  })

  it("has the correct CSS classes", () => {
    const wrapper = helper
      .component(ViewMemoryTrackerLink)
      .withProps({ memoryTrackerId: 456 })
      .mount()

    const button = wrapper.find("button")
    expect(button.classes()).toContain("daisy-btn")
    expect(button.classes()).toContain("daisy-btn-primary")
    expect(button.classes()).toContain("daisy-mt-4")
  })
})
