import { MemoryTrackerController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { mockSdkService } from "@tests/helpers"
import makeMe from "doughnut-test-fixtures/makeMe"
import { vi, describe, it, expect, beforeEach } from "vitest"
import {
  answeredRecallPrompt,
  clickDeleteUnanswered,
  contestedRecallPrompt,
  defaultMemoryTracker,
  defaultMemoryTrackerId,
  deleteUnansweredButton,
  focusedPropertyIndicator,
  mockDeleteUnansweredRecallPrompts,
  mountMemoryTrackerPageViewReady,
  peekConfirmPopup,
  recallPromptWithThinkingTime,
  removeFromRecallButton,
  resolveConfirmPopup,
  reviveButton,
  skippedBannerText,
  unansweredRecallPrompt,
} from "./memoryTrackerPageViewTestSupport"

const mockedPush = vi.fn()
vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRouter: () => ({
      push: mockedPush,
    }),
  }
})

describe("MemoryTrackerPageView", () => {
  beforeEach(() => {
    mockSdkService(
      MemoryTrackerController,
      "removeFromRepeating",
      makeMe.aMemoryTracker.please()
    )
  })

  it.each([
    {
      label: "property memory trackers",
      memoryTracker: makeMe.aMemoryTracker
        .withPropertyKey("a part of")
        .please(),
      shouldShow: true,
      expectedText: "Focused property: a part of",
    },
    {
      label: "note-level memory trackers",
      memoryTracker: defaultMemoryTracker(),
      shouldShow: false,
      expectedText: null,
    },
  ])(
    "$label focused property indicator visibility",
    async ({ memoryTracker, shouldShow, expectedText }) => {
      const wrapper = await mountMemoryTrackerPageViewReady({
        recallPrompts: [],
        memoryTracker,
      })

      expect(focusedPropertyIndicator(wrapper).exists()).toBe(shouldShow)
      if (expectedText) {
        expect(wrapper.text()).toContain(expectedText)
      } else {
        expect(wrapper.text()).not.toContain("Focused property:")
      }
    }
  )

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
    const recallPrompt = makeMe.aRecallPrompt
      .withQuestionStem("Test question")
      .withChoices(["A", "B", "C"])
      .please()

    const wrapper = await mountMemoryTrackerPageViewReady({
      recallPrompts: [recallPrompt],
    })

    expect(wrapper.text()).not.toContain("Thinking time")
  })

  it("works correctly when there are no recall prompts (spelling tracker)", async () => {
    const wrapper = await mountMemoryTrackerPageViewReady({
      recallPrompts: [],
      memoryTrackerId: 456,
    })

    expect(wrapper.text()).toContain("No recall prompts found")
  })

  describe("delete unanswered prompts", () => {
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
          makeMe.aRecallPrompt
            .withQuestionStem("Unanswered question 1")
            .please(),
          makeMe.aRecallPrompt
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
        expect(popups?.[0]?.type).toBe("confirm")
        expect(popups?.[0]?.message).toBe(expectedMessage)

        await resolveConfirmPopup(false)
      }
    )
  })

  describe("skipped memory tracker", () => {
    const skippedMemoryTracker = () =>
      makeMe.aMemoryTracker.removedFromTracking(true).please()

    it("shows skipped banner, revive button, and hides remove-from-recall", async () => {
      const recallPrompt = makeMe.aRecallPrompt
        .withQuestionStem("Test question")
        .please()

      const wrapper = await mountMemoryTrackerPageViewReady({
        recallPrompts: [recallPrompt],
        memoryTracker: skippedMemoryTracker(),
      })

      expect(wrapper.text()).toContain(skippedBannerText)
      expect(reviveButton(wrapper).exists()).toBe(true)
      expect(reviveButton(wrapper).text()).toContain("Revive")
      expect(removeFromRecallButton(wrapper).exists()).toBe(false)
    })

    it("shows recall prompts even when memory tracker is skipped", async () => {
      const recallPrompt = makeMe.aRecallPrompt
        .withQuestionStem("Test question")
        .withChoices(["A", "B", "C"])
        .please()

      const wrapper = await mountMemoryTrackerPageViewReady({
        recallPrompts: [recallPrompt],
        memoryTracker: skippedMemoryTracker(),
      })

      expect(wrapper.text()).toContain("Test question")
      expect(wrapper.text()).toContain("A")
      expect(wrapper.text()).toContain("B")
      expect(wrapper.text()).toContain("C")
    })

    it("calls re-enable endpoint and emits refresh when revive button is clicked", async () => {
      const memoryTracker = skippedMemoryTracker()
      const recallPrompt = makeMe.aRecallPrompt
        .withQuestionStem("Test question")
        .please()

      const reEnableSpy = mockSdkService(MemoryTrackerController, "reEnable", {
        ...memoryTracker,
        removedFromTracking: false,
      })

      const wrapper = await mountMemoryTrackerPageViewReady({
        recallPrompts: [recallPrompt],
        memoryTracker,
      })

      await reviveButton(wrapper).trigger("click")
      await flushPromises()

      expect(reEnableSpy).toHaveBeenCalledWith({
        path: { memoryTracker: defaultMemoryTrackerId },
      })
      expect(wrapper.emitted("refresh")).toHaveLength(1)
    })

    it("does not show skipped indicator when memory tracker is not skipped", async () => {
      const recallPrompt = makeMe.aRecallPrompt
        .withQuestionStem("Test question")
        .please()

      const wrapper = await mountMemoryTrackerPageViewReady({
        recallPrompts: [recallPrompt],
        memoryTracker: makeMe.aMemoryTracker
          .removedFromTracking(false)
          .please(),
      })

      expect(wrapper.text()).not.toContain(
        "This memory tracker is currently skipped"
      )
      expect(reviveButton(wrapper).exists()).toBe(false)
    })
  })

  describe("spelling question type", () => {
    it("displays spelling question message when question type is SPELLING and unanswered", async () => {
      const spellingPrompt = makeMe.aRecallPrompt
        .withQuestionType("SPELLING")
        .please()

      const wrapper = await mountMemoryTrackerPageViewReady({
        recallPrompts: [spellingPrompt],
      })

      expect(wrapper.text()).toContain(
        "This is a spelling question. Details are not needed."
      )
    })

    it.each([
      {
        label: "correct",
        answer: {
          id: 1,
          spellingAnswer: "Sedition",
          correct: true,
          thinkingTimeMs: 3000,
        },
        resultText: "Correct",
        thinkingTime: "Thinking time: 3.0s",
        hidesUnansweredMessage: true,
      },
      {
        label: "incorrect",
        answer: {
          id: 1,
          spellingAnswer: "asdf",
          correct: false,
          thinkingTimeMs: 1500,
        },
        resultText: "Incorrect",
        thinkingTime: "Thinking time: 1.5s",
        hidesUnansweredMessage: false,
      },
    ])(
      "displays spelling answer information when answered $label",
      async ({ answer, resultText, thinkingTime, hidesUnansweredMessage }) => {
        const spellingPrompt = makeMe.aRecallPrompt
          .withQuestionType("SPELLING")
          .withAnswer(answer)
          .withAnswerTime(new Date().toISOString())
          .please()

        const wrapper = await mountMemoryTrackerPageViewReady({
          recallPrompts: [spellingPrompt],
        })

        expect(wrapper.text()).toContain("Your answer:")
        expect(wrapper.text()).toContain(answer.spellingAnswer)
        expect(wrapper.text()).toContain("Result:")
        expect(wrapper.text()).toContain(resultText)
        expect(wrapper.text()).toContain(thinkingTime)
        if (hidesUnansweredMessage) {
          expect(wrapper.text()).not.toContain(
            "This is a spelling question. Details are not needed."
          )
        }
      }
    )

    it("does not display supplemental question text for spelling questions", async () => {
      const spellingPrompt = makeMe.aRecallPrompt
        .withQuestionType("SPELLING")
        .please()

      const wrapper = await mountMemoryTrackerPageViewReady({
        recallPrompts: [spellingPrompt],
      })

      expect(wrapper.findComponent({ name: "QuestionDisplay" }).exists()).toBe(
        false
      )
    })
  })
})
