import NoteInfoMemoryTracker from "@/components/notes/NoteInfoMemoryTracker.vue"
import helper from "@tests/helpers"
import makeMe from "doughnut-test-fixtures/makeMe"
import { describe, it, expect, afterEach } from "vitest"
import type { VueWrapper } from "@vue/test-utils"

describe("NoteInfoMemoryTracker", () => {
  let wrapper: VueWrapper

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  it("should display memory tracker information", () => {
    const memoryTracker = makeMe.aMemoryTracker
      .recallCount(5)
      .stability(3)
      .nextRecallAt("2024-01-01T12:00:00Z")
      .removedFromTracking(false)
      .please()

    wrapper = helper
      .component(NoteInfoMemoryTracker)
      .withProps({
        modelValue: memoryTracker,
      })
      .mount({ attachTo: document.body })

    expect(wrapper.text()).toContain("normal")
    expect(wrapper.text()).toContain("5")
    expect(wrapper.text()).toContain("3")
  })

  it("should display property memory tracker type", () => {
    const memoryTracker = makeMe.aMemoryTracker
      .removedFromTracking(false)
      .withPropertyKey("topic")
      .please()

    wrapper = helper
      .component(NoteInfoMemoryTracker)
      .withProps({
        modelValue: memoryTracker,
      })
      .mount({ attachTo: document.body })

    expect(wrapper.text()).toContain("property: topic")
  })

  it("should display spelling memory tracker", () => {
    const memoryTracker = makeMe.aMemoryTracker
      .removedFromTracking(false)
      .spelling()
      .please()

    wrapper = helper
      .component(NoteInfoMemoryTracker)
      .withProps({
        modelValue: memoryTracker,
      })
      .mount({ attachTo: document.body })

    expect(wrapper.text()).toContain("spelling")
  })

  it("should display commissioned memory tracker type", () => {
    const memoryTracker = makeMe.aMemoryTracker
      .removedFromTracking(false)
      .commissioned()
      .please()

    wrapper = helper
      .component(NoteInfoMemoryTracker)
      .withProps({
        modelValue: memoryTracker,
      })
      .mount({ attachTo: document.body })

    expect(wrapper.text()).toContain("Commissioned")
  })

  it("shows tutor feedback score for commissioned tracker", () => {
    const memoryTracker = makeMe.aMemoryTracker
      .removedFromTracking(false)
      .commissioned()
      .latestTutorFeedbackScore(4)
      .please()

    wrapper = helper
      .component(NoteInfoMemoryTracker)
      .withProps({
        modelValue: memoryTracker,
      })
      .mount({ attachTo: document.body })

    expect(wrapper.find('[data-test="tutor-feedback-score-4"]').exists()).toBe(
      true
    )
    expect(wrapper.text()).toContain(
      "tutor feedback score 4 from a learning session"
    )
  })

  it("should apply strikethrough styling to skipped memory trackers", () => {
    const memoryTracker = makeMe.aMemoryTracker
      .removedFromTracking(true)
      .please()

    wrapper = helper
      .component(NoteInfoMemoryTracker)
      .withProps({
        modelValue: memoryTracker,
      })
      .mount({ attachTo: document.body })

    const cells = wrapper.findAll("td")
    cells.forEach((cell) => {
      expect(cell.classes()).toContain("strikethrough")
    })
  })

  it("should not apply strikethrough styling to active memory trackers", () => {
    const memoryTracker = makeMe.aMemoryTracker
      .removedFromTracking(false)
      .please()

    wrapper = helper
      .component(NoteInfoMemoryTracker)
      .withProps({
        modelValue: memoryTracker,
      })
      .mount({ attachTo: document.body })

    const cells = wrapper.findAll("td")
    cells.forEach((cell) => {
      expect(cell.classes()).not.toContain("strikethrough")
    })
  })
})
