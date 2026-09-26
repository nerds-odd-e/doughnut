import { MemoryTrackerController } from "@generated/donut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import { beforeEach, describe, expect, it } from "vitest"
import {
  answeredRecallPrompt,
  clickDeleteUnanswered,
  contestedRecallPrompt,
  defaultMemoryTracker,
  defaultMemoryTrackerId,
  deleteUnansweredButton,
  focusedPropertyIndicator,
  historyFromPrompts,
  mockDeleteUnansweredRecallPrompts,
  mockMemoryTrackerPageViewDefaults,
  mountMemoryTrackerPageViewReady,
  peekConfirmPopup,
  removeFromRecallButton,
  resolveConfirmPopup,
  reviveButton,
  skippedBannerText,
  skippedMemoryTracker,
  unansweredRecallPrompt,
} from "./memoryTrackerPageViewTestSupport"

describe("MemoryTrackerPageView tracker", () => {
  beforeEach(() => {
    mockMemoryTrackerPageViewDefaults()
  })

  describe("details", () => {
    it("shows focused property indicator for property memory trackers", async () => {
      const wrapper = await mountMemoryTrackerPageViewReady({
        memoryTracker: makeMe.aMemoryTracker
          .withPropertyKey("a part of")
          .please(),
      })

      expect(focusedPropertyIndicator(wrapper).exists()).toBe(true)
      expect(wrapper.text()).toContain("Focused property: a part of")
    })

    it("hides focused property indicator for note-level memory trackers", async () => {
      const wrapper = await mountMemoryTrackerPageViewReady({
        memoryTracker: defaultMemoryTracker(),
      })

      expect(focusedPropertyIndicator(wrapper).exists()).toBe(false)
      expect(wrapper.text()).not.toContain("Focused property:")
    })

    it.each([
      {
        label: "memory tracker type",
        memoryTracker: makeMe.aMemoryTracker.spelling().please(),
        expected: /Type:\s*SPELLING/,
      },
      {
        label: "difficulty",
        memoryTracker: makeMe.aMemoryTracker
          .stability(72)
          .difficulty(7)
          .please(),
        expected: /Difficulty:\s*7/,
      },
      {
        label: "N/A when difficulty is unset",
        memoryTracker: defaultMemoryTracker(),
        expected: /Difficulty:\s*N\/A/,
      },
    ])("shows $label", async ({ memoryTracker, expected }) => {
      const wrapper = await mountMemoryTrackerPageViewReady({ memoryTracker })

      expect(wrapper.text()).toMatch(expected)
    })

    it("places Stability and Difficulty on the same row", async () => {
      const wrapper = await mountMemoryTrackerPageViewReady(
        {},
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
  })

  describe("skipped tracker", () => {
    const skippedWithPrompt = () =>
      mountMemoryTrackerPageViewReady({
        recallHistory: historyFromPrompts([
          makeMe.aRecallPromptHistoryItem
            .withQuestionStem("Test question")
            .withChoices(["A", "B", "C"])
            .please(),
        ]),
        memoryTracker: skippedMemoryTracker(),
      })

    it("shows skipped banner and revive, hides remove-from-recall, still shows prompts", async () => {
      const wrapper = await skippedWithPrompt()

      expect(wrapper.text()).toContain(skippedBannerText)
      expect(reviveButton(wrapper).exists()).toBe(true)
      expect(removeFromRecallButton(wrapper).exists()).toBe(false)
      expect(wrapper.text()).toContain("Test question")
      expect(wrapper.text()).toContain("A")
    })

    it("calls re-enable and emits refresh when revive is clicked", async () => {
      const reEnableSpy = mockSdkService(MemoryTrackerController, "reEnable", {
        ...skippedMemoryTracker(),
        removedFromTracking: false,
      })
      const wrapper = await skippedWithPrompt()

      await reviveButton(wrapper).trigger("click")
      await flushPromises()

      expect(reEnableSpy).toHaveBeenCalledWith({
        path: { memoryTracker: defaultMemoryTrackerId },
      })
      expect(wrapper.emitted("refresh")).toHaveLength(1)
    })
  })

  describe("delete unanswered", () => {
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
      { label: "no prompts", recallHistory: [], visible: false },
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

    it("confirms with the unanswered prompt count and deletes when confirmed", async () => {
      const deleteSpy = mockDeleteUnansweredRecallPrompts()
      const wrapper = await mountMemoryTrackerPageViewReady({
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
        recallHistory: historyFromPrompts([
          unansweredRecallPrompt(),
          unansweredRecallPrompt(),
        ]),
      })
      await clickDeleteUnanswered(wrapper)
      expect(peekConfirmPopup()?.[0]?.message).toBe(
        "Are you sure you want to delete 2 unanswered recall prompts?"
      )
      await resolveConfirmPopup(true)

      expect(deleteSpy).toHaveBeenCalledWith({
        path: { memoryTracker: defaultMemoryTrackerId },
      })
      expect(wrapper.emitted("refresh")).toHaveLength(1)
    })
  })
})
