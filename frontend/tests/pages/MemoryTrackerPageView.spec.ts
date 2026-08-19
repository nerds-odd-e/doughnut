import makeMe from "doughnut-test-fixtures/makeMe"
import { describe, expect, it, beforeEach } from "vitest"
import {
  defaultMemoryTracker,
  focusedPropertyIndicator,
  historyFromPrompts,
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
      recallHistory: [],
      memoryTracker: makeMe.aMemoryTracker
        .withPropertyKey("a part of")
        .please(),
    })

    expect(focusedPropertyIndicator(wrapper).exists()).toBe(true)
    expect(wrapper.text()).toContain("Focused property: a part of")
  })

  it("hides focused property indicator for note-level memory trackers", async () => {
    const wrapper = await mountMemoryTrackerPageViewReady({
      recallHistory: [],
      memoryTracker: defaultMemoryTracker(),
    })

    expect(focusedPropertyIndicator(wrapper).exists()).toBe(false)
    expect(wrapper.text()).not.toContain("Focused property:")
  })

  it("shows memory tracker type", async () => {
    const wrapper = await mountMemoryTrackerPageViewReady({
      recallHistory: [],
      memoryTracker: makeMe.aMemoryTracker.spelling().please(),
    })

    expect(wrapper.text()).toContain("Type:")
    expect(wrapper.text()).toContain("SPELLING")
  })

  it("shows difficulty", async () => {
    const wrapper = await mountMemoryTrackerPageViewReady({
      recallHistory: [],
      memoryTracker: makeMe.aMemoryTracker.stability(72).difficulty(7).please(),
    })

    expect(wrapper.text()).toMatch(/Difficulty:\s*7/)
  })

  it("shows N/A when difficulty is unset", async () => {
    const wrapper = await mountMemoryTrackerPageViewReady({
      recallHistory: [],
    })

    expect(wrapper.text()).toMatch(/Difficulty:\s*N\/A/)
  })

  it("places Stability and Difficulty on the same row", async () => {
    const wrapper = await mountMemoryTrackerPageViewReady(
      { recallHistory: [] },
      { attachToBody: true }
    )

    const labels = wrapper.findAll("span.font-semibold")
    const texts = labels.map((label) => label.text())
    const stabilityIndex = texts.indexOf("Stability:")
    expect(texts[stabilityIndex + 1]).toBe("Difficulty:")
    expect(labels[stabilityIndex]!.element.getBoundingClientRect().top).toBe(
      labels[stabilityIndex + 1]!.element.getBoundingClientRect().top
    )
  })

  it.each([
    { thinkingTimeMs: 5234, expected: "Thinking time: 5.2s" },
    { thinkingTimeMs: 500, expected: "Thinking time: 500ms" },
    { thinkingTimeMs: 125000, expected: "Thinking time: 2m 5s" },
  ])(
    "formats thinking time as $expected",
    async ({ thinkingTimeMs, expected }) => {
      const wrapper = await mountMemoryTrackerPageViewReady({
        recallHistory: historyFromPrompts([
          recallPromptWithThinkingTime(thinkingTimeMs),
        ]),
      })

      expect(wrapper.text()).toContain(expected)
    }
  )

  it("does not display thinking time for unanswered questions", async () => {
    const wrapper = await mountMemoryTrackerPageViewReady({
      recallHistory: historyFromPrompts([
        makeMe.aRecallPromptHistoryItem
          .withQuestionStem("Test question")
          .withChoices(["A", "B", "C"])
          .please(),
      ]),
    })

    expect(wrapper.text()).not.toContain("Thinking time")
  })

  it("shows note under question only once", async () => {
    const wrapper = await mountMemoryTrackerPageViewReady({
      recallHistory: historyFromPrompts([
        makeMe.aRecallPromptHistoryItem.please(),
        makeMe.aRecallPromptHistoryItem.please(),
      ]),
    })

    expect(noteUnderQuestionSections(wrapper)).toHaveLength(1)
  })

  it("shows question generated time", async () => {
    const questionGeneratedTime = new Date("2024-01-01T10:00:00Z").toISOString()
    const wrapper = await mountMemoryTrackerPageViewReady({
      recallHistory: historyFromPrompts([
        makeMe.aRecallPromptHistoryItem
          .withQuestionGeneratedTime(questionGeneratedTime)
          .please(),
      ]),
    })

    expect(wrapper.text()).toContain("Generated:")
    expect(wrapper.text()).toContain(
      new Date(questionGeneratedTime).toLocaleString()
    )
  })

  it("shows contested badge for contested questions", async () => {
    const wrapper = await mountMemoryTrackerPageViewReady({
      recallHistory: historyFromPrompts([
        makeMe.aRecallPromptHistoryItem.withIsContested(true).please(),
      ]),
    })

    expect(wrapper.text()).toContain("Contested")
  })

  it("shows tested focus from the answered MCQ", async () => {
    const wrapper = await mountMemoryTrackerPageViewReady({
      recallHistory: historyFromPrompts([
        makeMe.aRecallPromptHistoryItem
          .withMcq(
            makeMe.anMcq
              .withQuestionStem("What is the capital of France?")
              .withChoices(["Paris", "London"])
              .testedFocus("capital city")
              .please()
          )
          .withAnswer({ id: 1, correct: true, choiceIndex: 0 })
          .withAnswerTime(new Date().toISOString())
          .please(),
      ]),
    })

    expect(wrapper.text()).toContain("capital city")
  })

  it("shows answer time for answered questions", async () => {
    const answerTime = new Date("2024-01-01T12:00:00Z").toISOString()
    const wrapper = await mountMemoryTrackerPageViewReady({
      recallHistory: historyFromPrompts([
        makeMe.aRecallPromptHistoryItem
          .withAnswerTime(answerTime)
          .withAnswer({ id: 1, correct: true, choiceIndex: 0 })
          .withMcq(makeMe.anMcq.please())
          .please(),
      ]),
    })

    expect(wrapper.text()).toContain("Answered:")
    expect(wrapper.text()).toContain(new Date(answerTime).toLocaleString())
  })

  it("shows unanswered status for unanswered questions", async () => {
    const wrapper = await mountMemoryTrackerPageViewReady({
      recallHistory: historyFromPrompts([
        makeMe.aRecallPromptHistoryItem.please(),
      ]),
    })

    expect(wrapper.text()).toContain("Unanswered")
  })
})
