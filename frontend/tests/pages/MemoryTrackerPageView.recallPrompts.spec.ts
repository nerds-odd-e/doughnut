import makeMe from "donut-test-fixtures/makeMe"
import { beforeEach, describe, expect, it } from "vitest"
import {
  historyFromPrompts,
  mockMemoryTrackerPageViewDefaults,
  mountMemoryTrackerPageViewReady,
  noteUnderQuestionSections,
  recallPromptWithAnswer,
  spellingDetailsNotNeeded,
} from "./memoryTrackerPageViewTestSupport"

const mountWithPrompts = (
  ...prompts: Parameters<typeof historyFromPrompts>[0]
) =>
  mountMemoryTrackerPageViewReady({
    recallHistory: historyFromPrompts(prompts),
  })

describe("MemoryTrackerPageView recall prompts", () => {
  beforeEach(() => {
    mockMemoryTrackerPageViewDefaults()
  })

  it.each([
    { thinkingTimeMs: 5234, expected: "Thinking time: 5.2s" },
    { thinkingTimeMs: 500, expected: "Thinking time: 500ms" },
    { thinkingTimeMs: 125000, expected: "Thinking time: 2m 5s" },
  ])(
    "formats thinking time as $expected",
    async ({ thinkingTimeMs, expected }) => {
      const wrapper = await mountWithPrompts(
        recallPromptWithAnswer({ thinkingTimeMs })
      )

      expect(wrapper.text()).toContain(expected)
    }
  )

  it.each([
    { answer: { awayMs: 6500, awayCount: 2 }, expected: "Away: 6.5s (2x)" },
    {
      answer: { detourMs: 4200, detourCount: 1 },
      expected: "Detour: 4.2s (1x)",
    },
    { answer: { idleMs: 12000 }, expected: "Idle: 12.0s" },
  ])(
    "shows interruption $expected beside thinking time",
    async ({ answer, expected }) => {
      const wrapper = await mountWithPrompts(recallPromptWithAnswer(answer))

      expect(wrapper.text()).toContain(expected)
    }
  )

  it("does not display any interruption when uninstrumented", async () => {
    const wrapper = await mountWithPrompts(
      recallPromptWithAnswer({ thinkingTimeMs: 5234 })
    )

    expect(wrapper.text()).not.toContain("Away:")
    expect(wrapper.text()).not.toContain("Detour:")
    expect(wrapper.text()).not.toContain("Idle:")
  })

  it("shows unanswered status without thinking time for unanswered questions", async () => {
    const wrapper = await mountWithPrompts(
      makeMe.aRecallPromptHistoryItem
        .withQuestionStem("Test question")
        .withChoices(["A", "B", "C"])
        .please()
    )

    expect(wrapper.text()).toContain("Unanswered")
    expect(wrapper.text()).not.toContain("Thinking time")
  })

  it("shows note under question only once", async () => {
    const wrapper = await mountWithPrompts(
      makeMe.aRecallPromptHistoryItem.please(),
      makeMe.aRecallPromptHistoryItem.please()
    )

    expect(noteUnderQuestionSections(wrapper)).toHaveLength(1)
  })

  it("shows question generated time", async () => {
    const questionGeneratedTime = new Date("2024-01-01T10:00:00Z").toISOString()
    const wrapper = await mountWithPrompts(
      makeMe.aRecallPromptHistoryItem
        .withQuestionGeneratedTime(questionGeneratedTime)
        .please()
    )

    expect(wrapper.text()).toContain("Generated:")
    expect(wrapper.text()).toContain(
      new Date(questionGeneratedTime).toLocaleString()
    )
  })

  it("shows contested badge for contested questions", async () => {
    const wrapper = await mountWithPrompts(
      makeMe.aRecallPromptHistoryItem.withIsContested(true).please()
    )

    expect(wrapper.text()).toContain("Contested")
  })

  it("shows tested focus and answer time for an answered MCQ", async () => {
    const answerTime = new Date("2024-01-01T12:00:00Z").toISOString()
    const wrapper = await mountWithPrompts(
      makeMe.aRecallPromptHistoryItem
        .withMcq(
          makeMe.anMcq
            .withQuestionStem("What is the capital of France?")
            .withChoices(["Paris", "London"])
            .testedFocus("capital city")
            .please()
        )
        .withAnswer({ id: 1, correct: true, choiceIndex: 0 })
        .withAnswerTime(answerTime)
        .please()
    )

    expect(wrapper.text()).toContain("capital city")
    expect(wrapper.text()).toContain("Answered:")
    expect(wrapper.text()).toContain(new Date(answerTime).toLocaleString())
  })

  describe("spelling prompts", () => {
    it("shows details-not-needed message and no multiple-choice UI when unanswered", async () => {
      const wrapper = await mountWithPrompts(
        makeMe.aRecallPromptHistoryItem.withQuestionType("SPELLING").please()
      )

      expect(wrapper.text()).toContain(spellingDetailsNotNeeded)
      expect(wrapper.find('[data-test="question-section"]').exists()).toBe(
        false
      )
    })

    it.each([
      { spellingAnswer: "Sedition", correct: true, result: "Correct" },
      { spellingAnswer: "asdf", correct: false, result: "Incorrect" },
    ])(
      "displays the spelling answer $spellingAnswer as $result",
      async ({ spellingAnswer, correct, result }) => {
        const wrapper = await mountWithPrompts(
          makeMe.aRecallPromptHistoryItem
            .withQuestionType("SPELLING")
            .withAnswer({
              id: 1,
              spellingAnswer,
              correct,
              thinkingTimeMs: 3000,
            })
            .withAnswerTime(new Date().toISOString())
            .please()
        )

        expect(wrapper.text()).toContain("Your answer:")
        expect(wrapper.text()).toContain(spellingAnswer)
        expect(wrapper.text()).toContain(result)
      }
    )
  })
})
