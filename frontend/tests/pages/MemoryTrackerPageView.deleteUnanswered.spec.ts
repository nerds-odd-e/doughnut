import makeMe from "doughnut-test-fixtures/makeMe"
import { beforeEach, describe, expect, it } from "vitest"
import {
  answeredRecallPrompt,
  clickDeleteUnanswered,
  contestedRecallPrompt,
  defaultMemoryTrackerId,
  deleteUnansweredButton,
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
      recallPrompts: [unansweredRecallPrompt()],
      visible: true,
    },
    {
      label: "all answered prompts",
      recallPrompts: [answeredRecallPrompt()],
      visible: false,
    },
    {
      label: "no prompts",
      recallPrompts: [],
      visible: false,
    },
    {
      label: "only contested unanswered prompts",
      recallPrompts: [contestedRecallPrompt()],
      visible: false,
    },
  ])(
    "delete button visibility when $label",
    async ({ recallPrompts, visible }) => {
      const wrapper = await mountMemoryTrackerPageViewReady({ recallPrompts })

      expect(deleteUnansweredButton(wrapper).exists()).toBe(visible)
    }
  )

  it("calls delete endpoint and emits refresh when confirmed", async () => {
    const deleteSpy = mockDeleteUnansweredRecallPrompts()

    const wrapper = await mountMemoryTrackerPageViewReady({
      recallPrompts: [unansweredRecallPrompt()],
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
      recallPrompts: [unansweredRecallPrompt()],
      expectedMessage:
        "Are you sure you want to delete 1 unanswered recall prompt?",
    },
    {
      label: "multiple prompts",
      recallPrompts: [
        makeMe.aRecallPromptHistoryItem
          .withQuestionStem("Unanswered question 1")
          .please(),
        makeMe.aRecallPromptHistoryItem
          .withQuestionStem("Unanswered question 2")
          .please(),
      ],
      expectedMessage:
        "Are you sure you want to delete 2 unanswered recall prompts?",
    },
    {
      label: "contested prompts excluded from count",
      recallPrompts: [unansweredRecallPrompt(), contestedRecallPrompt()],
      expectedMessage:
        "Are you sure you want to delete 1 unanswered recall prompt?",
    },
  ])(
    "confirmation message for $label",
    async ({ recallPrompts, expectedMessage }) => {
      mockDeleteUnansweredRecallPrompts()

      const wrapper = await mountMemoryTrackerPageViewReady({ recallPrompts })
      await clickDeleteUnanswered(wrapper)

      const popups = peekConfirmPopup()
      expect(popups).toHaveLength(1)
      expect(popups?.[0]?.message).toBe(expectedMessage)

      await resolveConfirmPopup(false)
    }
  )
})
