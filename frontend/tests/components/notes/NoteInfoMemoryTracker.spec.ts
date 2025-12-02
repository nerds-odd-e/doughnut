import NoteInfoMemoryTracker from "@/components/notes/NoteInfoMemoryTracker.vue"
import helper from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"

describe("NoteInfoMemoryTracker", () => {
  it("should display memory tracker information", () => {
    const memoryTracker = makeMe.aMemoryTracker
      .repetitionCount(5)
      .forgettingCurveIndex(3)
      .nextRecallAt("2024-01-01T12:00:00Z")
      .removedFromTracking(false)
      .please()

    const wrapper = helper
      .component(NoteInfoMemoryTracker)
      .withProps({
        modelValue: memoryTracker,
      })
      .mount()

    expect(wrapper.text()).toContain("normal")
    expect(wrapper.text()).toContain("5")
    expect(wrapper.text()).toContain("3")
  })

  it("should display spelling memory tracker", () => {
    const memoryTracker = makeMe.aMemoryTracker
      .removedFromTracking(false)
      .please()
    memoryTracker.spelling = true

    const wrapper = helper
      .component(NoteInfoMemoryTracker)
      .withProps({
        modelValue: memoryTracker,
      })
      .mount()

    expect(wrapper.text()).toContain("spelling")
  })

  it("should apply strikethrough styling to skipped memory trackers", () => {
    const memoryTracker = makeMe.aMemoryTracker
      .removedFromTracking(true)
      .please()

    const wrapper = helper
      .component(NoteInfoMemoryTracker)
      .withProps({
        modelValue: memoryTracker,
      })
      .mount()

    const cells = wrapper.findAll("td")
    cells.forEach((cell) => {
      expect(cell.classes()).toContain("strikethrough")
    })
  })

  it("should not apply strikethrough styling to active memory trackers", () => {
    const memoryTracker = makeMe.aMemoryTracker
      .removedFromTracking(false)
      .please()

    const wrapper = helper
      .component(NoteInfoMemoryTracker)
      .withProps({
        modelValue: memoryTracker,
      })
      .mount()

    const cells = wrapper.findAll("td")
    cells.forEach((cell) => {
      expect(cell.classes()).not.toContain("strikethrough")
    })
  })
})
