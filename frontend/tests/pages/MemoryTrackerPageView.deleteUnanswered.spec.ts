import makeMe from "doughnut-test-fixtures/makeMe"
import { beforeEach, describe, expect, it } from "vitest"
import {
  answeredRecallPrompt,
  clickDeleteUnanswered,
  contestedRecallPrompt,
  defaultMemoryTrackerId,
  deleteUnansweredButton,
  historyFromPrompts,
  mockDeleteUnansweredRecallPrompts,
  mockMemoryTrackerPageViewDefaults,
  mountMemoryTrackerPageViewReady,
  peekConfirmPopup,
  resolveConfirmPopup,
  unansweredRecallPrompt,
} from "./memoryTrackerPageViewTestSupport"

describe("MemoryTrackerPageView delete unanswered", () => {
  beforeEach(() => {
    mockMemoryTrackerPageViewDefaults()
  })

  it.each([
    {
      label: "unanswered prompts",
      recallHistory: historyFromPrompts([unansweredRecallPrompt()]),
      visible: true,
    },
    {
      label: "all answered prompts",
      recallHistory: historyFromPrompts([answeredRecallPrompt()]),
      visible: false,
    },
    {
      label: "no prompts",
      recallHistory: [],
      visible: false,
    },
    {
      label: "only contested unanswered prompts",
      recallHistory: historyFromPrompts([contestedRecallPrompt()]),
      visible: false,
    },
  ])(
    "delete button visibility when $label",
    async ({ recallHistory, visible }) => {
      const wrapper = await mountMemoryTrackerPageViewReady({ recallHistory })

      expect(deleteUnansweredButton(wrapper).exists()).toBe(visible)
    }
  )

  it("calls delete endpoint and emits refresh when confirmed", async () => {
    const deleteSpy = mockDeleteUnansweredRecallPrompts()

    const wrapper = await mountMemoryTrackerPageViewReady({
      recallHistory: historyFromPrompts([unansweredRecallPrompt()]),
    })

    await clickDeleteUnanswered(wrapper)
    await resolveConfirmPopup(true)

    expect(deleteSpy).toHaveBeenCalledWith({
      path: { memoryTracker: defaultMemoryTrackerId },
    })
    expect(wrapper.emitted("refresh")).toHaveLength(1)
  })

  it.each([
    {
      label: "single prompt",
      recallHistory: historyFromPrompts([unansweredRecallPrompt()]),
      expectedMessage:
        "Are you sure you want to delete 1 unanswered recall prompt?",
    },
    {
      label: "multiple prompts",
      recallHistory: historyFromPrompts([
        makeMe.aRecallPromptHistoryItem
          .withQuestionStem("Unanswered question 1")
          .please(),
        makeMe.aRecallPromptHistoryItem
          .withQuestionStem("Unanswered question 2")
          .please(),
      ]),
      expectedMessage:
        "Are you sure you want to delete 2 unanswered recall prompts?",
    },
    {
      label: "contested prompts excluded from count",
      recallHistory: historyFromPrompts([
        unansweredRecallPrompt(),
        contestedRecallPrompt(),
      ]),
      expectedMessage:
        "Are you sure you want to delete 1 unanswered recall prompt?",
    },
  ])(
    "confirmation message for $label",
    async ({ recallHistory, expectedMessage }) => {
      mockDeleteUnansweredRecallPrompts()

      const wrapper = await mountMemoryTrackerPageViewReady({ recallHistory })
      await clickDeleteUnanswered(wrapper)

      const popups = peekConfirmPopup()
      expect(popups).toHaveLength(1)
      expect(popups?.[0]?.message).toBe(expectedMessage)

      await resolveConfirmPopup(false)
    }
  )
})
