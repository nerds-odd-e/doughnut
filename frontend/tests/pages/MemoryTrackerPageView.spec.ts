import makeMe from "doughnut-test-fixtures/makeMe"
import { describe, expect, it, beforeEach } from "vitest"
import {
  defaultMemoryTracker,
  focusedPropertyIndicator,
  mockMemoryTrackerPageViewDefaults,
  mountMemoryTrackerPageViewReady,
  noteUnderQuestionSections,
  recallPromptWithThinkingTime,
} from "./memoryTrackerPageViewTestSupport"

describe("MemoryTrackerPageView display", () => {
  beforeEach(() => {
    mockMemoryTrackerPageViewDefaults()
  })

  it("shows focused property indicator for property memory trackers", async () => {
    const wrapper = await mountMemoryTrackerPageViewReady({
      recallPrompts: [],
      memoryTracker: makeMe.aMemoryTracker
        .withPropertyKey("a part of")
        .please(),
    })

    expect(focusedPropertyIndicator(wrapper).exists()).toBe(true)
    expect(wrapper.text()).toContain("Focused property: a part of")
  })

  it("hides focused property indicator for note-level memory trackers", async () => {
    const wrapper = await mountMemoryTrackerPageViewReady({
      recallPrompts: [],
      memoryTracker: defaultMemoryTracker(),
    })

    expect(focusedPropertyIndicator(wrapper).exists()).toBe(false)
    expect(wrapper.text()).not.toContain("Focused property:")
  })

  it("shows memory tracker type", async () => {
    const wrapper = await mountMemoryTrackerPageViewReady({
      recallPrompts: [],
      memoryTracker: makeMe.aMemoryTracker.spelling().please(),
    })

    expect(wrapper.text()).toContain("Type:")
    expect(wrapper.text()).toContain("SPELLING")
  })

  it.each([
    { thinkingTimeMs: 5234, expected: "Thinking time: 5.2s" },
    { thinkingTimeMs: 500, expected: "Thinking time: 500ms" },
    { thinkingTimeMs: 125000, expected: "Thinking time: 2m 5s" },
  ])(
    "formats thinking time as $expected",
    async ({ thinkingTimeMs, expected }) => {
      const wrapper = await mountMemoryTrackerPageViewReady({
        recallPrompts: [recallPromptWithThinkingTime(thinkingTimeMs)],
      })

      expect(wrapper.text()).toContain(expected)
    }
  )

  it("does not display thinking time for unanswered questions", async () => {
    const wrapper = await mountMemoryTrackerPageViewReady({
      recallPrompts: [
        makeMe.aRecallPrompt
          .withQuestionStem("Test question")
          .withChoices(["A", "B", "C"])
          .please(),
      ],
    })

    expect(wrapper.text()).not.toContain("Thinking time")
  })

  it("shows empty state when there are no recall prompts", async () => {
    const wrapper = await mountMemoryTrackerPageViewReady({
      recallPrompts: [],
    })

    expect(wrapper.text()).toContain("No recall prompts found")
  })

  it("shows note under question only once", async () => {
    const wrapper = await mountMemoryTrackerPageViewReady({
      recallPrompts: [
        makeMe.aRecallPrompt.please(),
        makeMe.aRecallPrompt.please(),
      ],
    })

    expect(noteUnderQuestionSections(wrapper)).toHaveLength(1)
  })

  it("shows question generated time", async () => {
    const questionGeneratedTime = new Date("2024-01-01T10:00:00Z").toISOString()
    const wrapper = await mountMemoryTrackerPageViewReady({
      recallPrompts: [
        makeMe.aRecallPrompt
          .withQuestionGeneratedTime(questionGeneratedTime)
          .please(),
      ],
    })

    expect(wrapper.text()).toContain("Generated:")
    expect(wrapper.text()).toContain(
      new Date(questionGeneratedTime).toLocaleString()
    )
  })

  it("shows contested badge for contested questions", async () => {
    const wrapper = await mountMemoryTrackerPageViewReady({
      recallPrompts: [makeMe.aRecallPrompt.withIsContested(true).please()],
    })

    expect(wrapper.text()).toContain("Contested")
  })

  it("shows answer time for answered questions", async () => {
    const answerTime = new Date("2024-01-01T12:00:00Z").toISOString()
    const wrapper = await mountMemoryTrackerPageViewReady({
      recallPrompts: [
        makeMe.aRecallPrompt
          .withAnswerTime(answerTime)
          .withAnswer({ id: 1, correct: true, choiceIndex: 0 })
          .withPredefinedQuestion(makeMe.aPredefinedQuestion.please())
          .please(),
      ],
    })

    expect(wrapper.text()).toContain("Answered:")
    expect(wrapper.text()).toContain(new Date(answerTime).toLocaleString())
  })

  it("shows unanswered status for unanswered questions", async () => {
    const wrapper = await mountMemoryTrackerPageViewReady({
      recallPrompts: [makeMe.aRecallPrompt.please()],
    })

    expect(wrapper.text()).toContain("Unanswered")
  })
})
