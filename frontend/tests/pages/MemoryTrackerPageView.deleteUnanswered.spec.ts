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

  it("confirmation messages and deletes when confirmed", async () => {
    const deleteSpy = mockDeleteUnansweredRecallPrompts()
    const wrapper = await mountMemoryTrackerPageViewReady({
      recallHistory: historyFromPrompts([unansweredRecallPrompt()]),
    })

    await clickDeleteUnanswered(wrapper)
    expect(peekConfirmPopup()?.[0]?.message).toBe(
      "Are you sure you want to delete 1 unanswered recall prompt?"
    )
    await resolveConfirmPopup(false)

    await wrapper.setProps({
      recallHistory: historyFromPrompts([
        makeMe.aRecallPromptHistoryItem
          .withQuestionStem("Unanswered question 1")
          .please(),
        makeMe.aRecallPromptHistoryItem
          .withQuestionStem("Unanswered question 2")
          .please(),
      ]),
    })
    await clickDeleteUnanswered(wrapper)
    expect(peekConfirmPopup()?.[0]?.message).toBe(
      "Are you sure you want to delete 2 unanswered recall prompts?"
    )
    await resolveConfirmPopup(false)

    await wrapper.setProps({
      recallHistory: historyFromPrompts([
        unansweredRecallPrompt(),
        contestedRecallPrompt(),
      ]),
    })
    await clickDeleteUnanswered(wrapper)
    expect(peekConfirmPopup()?.[0]?.message).toBe(
      "Are you sure you want to delete 1 unanswered recall prompt?"
    )
    await resolveConfirmPopup(false)

    await wrapper.setProps({
      recallHistory: historyFromPrompts([unansweredRecallPrompt()]),
    })
    await clickDeleteUnanswered(wrapper)
    await resolveConfirmPopup(true)

    expect(deleteSpy).toHaveBeenCalledWith({
      path: { memoryTracker: defaultMemoryTrackerId },
    })
    expect(wrapper.emitted("refresh")).toHaveLength(1)
  })
})
