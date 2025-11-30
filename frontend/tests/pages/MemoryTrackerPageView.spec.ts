import { flushPromises } from "@vue/test-utils"
import helper, { mockSdkService } from "@tests/helpers"
import MemoryTrackerPageView from "@/pages/MemoryTrackerPageView.vue"
import makeMe from "@tests/fixtures/makeMe"

describe("MemoryTrackerPageView", () => {
  beforeEach(() => {
    mockSdkService("removeFromRepeating", { removedFromTracking: false })
  })

  it("displays thinking time for answered questions", async () => {
    const recallPrompt = makeMe.aRecallPrompt
      .withQuestionStem("Test question")
      .withChoices(["A", "B", "C"])
      .withAnswer({
        id: 1,
        choiceIndex: 0,
        correct: true,
        thinkingTimeMs: 5234,
      })
      .please()

    const wrapper = helper
      .component(MemoryTrackerPageView)
      .withProps({
        recallPrompts: [recallPrompt],
        memoryTrackerId: 1,
      })
      .mount()

    await flushPromises()

    expect(wrapper.text()).toContain("Thinking time: 5.2s")
  })

  it("formats thinking time in milliseconds when less than 1 second", async () => {
    const recallPrompt = makeMe.aRecallPrompt
      .withQuestionStem("Test question")
      .withChoices(["A", "B", "C"])
      .withAnswer({
        id: 1,
        choiceIndex: 0,
        correct: true,
        thinkingTimeMs: 500,
      })
      .please()

    const wrapper = helper
      .component(MemoryTrackerPageView)
      .withProps({
        recallPrompts: [recallPrompt],
        memoryTrackerId: 1,
      })
      .mount()

    await flushPromises()

    expect(wrapper.text()).toContain("Thinking time: 500ms")
  })

  it("formats thinking time in minutes and seconds when over 1 minute", async () => {
    const recallPrompt = makeMe.aRecallPrompt
      .withQuestionStem("Test question")
      .withChoices(["A", "B", "C"])
      .withAnswer({
        id: 1,
        choiceIndex: 0,
        correct: true,
        thinkingTimeMs: 125000,
      })
      .please()

    const wrapper = helper
      .component(MemoryTrackerPageView)
      .withProps({
        recallPrompts: [recallPrompt],
        memoryTrackerId: 1,
      })
      .mount()

    await flushPromises()

    expect(wrapper.text()).toContain("Thinking time: 2m 5s")
  })

  it("does not display thinking time for unanswered questions", async () => {
    const recallPrompt = makeMe.aRecallPrompt
      .withQuestionStem("Test question")
      .withChoices(["A", "B", "C"])
      .please()

    const wrapper = helper
      .component(MemoryTrackerPageView)
      .withProps({
        recallPrompts: [recallPrompt],
        memoryTrackerId: 1,
      })
      .mount()

    await flushPromises()

    expect(wrapper.text()).not.toContain("Thinking time")
  })
})
