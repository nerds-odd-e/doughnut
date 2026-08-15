import makeMe from "doughnut-test-fixtures/makeMe"
import { beforeEach, describe, expect, it } from "vitest"
import {
  mockMemoryTrackerPageViewDefaults,
  mountMemoryTrackerPageViewReady,
  spellingDetailsNotNeeded,
} from "./memoryTrackerPageViewTestSupport"

describe("MemoryTrackerPageView spelling prompts", () => {
  beforeEach(() => {
    mockMemoryTrackerPageViewDefaults()
  })

  it("shows spelling details-not-needed message when unanswered", async () => {
    const wrapper = await mountMemoryTrackerPageViewReady({
      recallPrompts: [
        makeMe.aRecallPromptHistoryItem.withQuestionType("SPELLING").please(),
      ],
    })

    expect(wrapper.text()).toContain(spellingDetailsNotNeeded)
  })

  it("displays spelling answer and Correct result when answered correctly", async () => {
    const wrapper = await mountMemoryTrackerPageViewReady({
      recallPrompts: [
        makeMe.aRecallPromptHistoryItem
          .withQuestionType("SPELLING")
          .withAnswer({
            id: 1,
            spellingAnswer: "Sedition",
            correct: true,
            thinkingTimeMs: 3000,
          })
          .withAnswerTime(new Date().toISOString())
          .please(),
      ],
    })

    expect(wrapper.text()).toContain("Your answer:")
    expect(wrapper.text()).toContain("Sedition")
    expect(wrapper.text()).toContain("Correct")
  })

  it("displays Incorrect result when spelling answer is wrong", async () => {
    const wrapper = await mountMemoryTrackerPageViewReady({
      recallPrompts: [
        makeMe.aRecallPromptHistoryItem
          .withQuestionType("SPELLING")
          .withAnswer({
            id: 1,
            spellingAnswer: "asdf",
            correct: false,
            thinkingTimeMs: 1500,
          })
          .withAnswerTime(new Date().toISOString())
          .please(),
      ],
    })

    expect(wrapper.text()).toContain("asdf")
    expect(wrapper.text()).toContain("Incorrect")
  })

  it("does not show multiple-choice question UI for spelling prompts", async () => {
    const wrapper = await mountMemoryTrackerPageViewReady({
      recallPrompts: [
        makeMe.aRecallPromptHistoryItem.withQuestionType("SPELLING").please(),
      ],
    })

    expect(wrapper.find('[data-test="question-section"]').exists()).toBe(false)
  })
})
