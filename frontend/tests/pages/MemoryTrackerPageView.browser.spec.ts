import { flushPromises } from "@vue/test-utils"
import helper, { mockSdkService } from "@tests/helpers"
import MemoryTrackerPageView from "@/pages/MemoryTrackerPageView.vue"
import makeMe from "@tests/fixtures/makeMe"
import usePopups from "@/components/commons/Popups/usePopups"
import { vi, describe, it, expect, beforeEach } from "vitest"

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
    const memoryTracker = makeMe.aMemoryTracker.please()

    const wrapper = helper
      .component(MemoryTrackerPageView)
      .withProps({
        recallPrompts: [recallPrompt],
        memoryTracker,
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
    const memoryTracker = makeMe.aMemoryTracker.please()

    const wrapper = helper
      .component(MemoryTrackerPageView)
      .withProps({
        recallPrompts: [recallPrompt],
        memoryTracker,
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
    const memoryTracker = makeMe.aMemoryTracker.please()

    const wrapper = helper
      .component(MemoryTrackerPageView)
      .withProps({
        recallPrompts: [recallPrompt],
        memoryTracker,
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
    const memoryTracker = makeMe.aMemoryTracker.please()

    const wrapper = helper
      .component(MemoryTrackerPageView)
      .withProps({
        recallPrompts: [recallPrompt],
        memoryTracker,
        memoryTrackerId: 1,
      })
      .mount()

    await flushPromises()

    expect(wrapper.text()).not.toContain("Thinking time")
  })

  it("works correctly when there are no recall prompts (spelling tracker)", async () => {
    const memoryTracker = makeMe.aMemoryTracker.please()
    const wrapper = helper
      .component(MemoryTrackerPageView)
      .withProps({
        recallPrompts: [],
        memoryTracker,
        memoryTrackerId: 456,
      })
      .mount()

    await flushPromises()

    expect(wrapper.text()).toContain("No recall prompts found")
  })

  describe("delete unanswered prompts", () => {
    it("shows delete button when there are unanswered prompts", async () => {
      const unansweredPrompt = makeMe.aRecallPrompt
        .withQuestionStem("Unanswered question")
        .please()
      const memoryTracker = makeMe.aMemoryTracker.please()

      mockSdkService("deleteUnansweredRecallPrompts", undefined)

      const wrapper = helper
        .component(MemoryTrackerPageView)
        .withProps({
          recallPrompts: [unansweredPrompt],
          memoryTracker,
          memoryTrackerId: 1,
        })
        .mount()

      await flushPromises()

      expect(wrapper.text()).toContain("Delete Unanswered Prompts")
    })

    it("does not show delete button when all prompts are answered", async () => {
      const answeredPrompt = makeMe.aRecallPrompt
        .withQuestionStem("Answered question")
        .withAnswer({
          id: 1,
          choiceIndex: 0,
          correct: true,
        })
        .withAnswerTime(new Date().toISOString())
        .please()
      const memoryTracker = makeMe.aMemoryTracker.please()

      const wrapper = helper
        .component(MemoryTrackerPageView)
        .withProps({
          recallPrompts: [answeredPrompt],
          memoryTracker,
          memoryTrackerId: 1,
        })
        .mount()

      await flushPromises()

      const deleteButton = wrapper.find(
        'button[title="delete all unanswered recall prompts"]'
      )
      expect(deleteButton.exists()).toBe(false)
    })

    it("does not show delete button when there are no prompts", async () => {
      const memoryTracker = makeMe.aMemoryTracker.please()

      const wrapper = helper
        .component(MemoryTrackerPageView)
        .withProps({
          recallPrompts: [],
          memoryTracker,
          memoryTrackerId: 1,
        })
        .mount()

      await flushPromises()

      expect(wrapper.text()).not.toContain("Delete Unanswered Prompts")
    })

    it("calls delete endpoint and emits refresh when confirmed", async () => {
      const unansweredPrompt = makeMe.aRecallPrompt
        .withQuestionStem("Unanswered question")
        .please()
      const memoryTracker = makeMe.aMemoryTracker.please()

      const deleteSpy = mockSdkService(
        "deleteUnansweredRecallPrompts",
        undefined
      )

      const wrapper = helper
        .component(MemoryTrackerPageView)
        .withProps({
          recallPrompts: [unansweredPrompt],
          memoryTracker,
          memoryTrackerId: 1,
        })
        .mount()

      await flushPromises()

      const deleteButton = wrapper.find(
        'button[title="delete all unanswered recall prompts"]'
      )
      expect(deleteButton.exists()).toBe(true)
      await deleteButton.trigger("click")
      await flushPromises()
      usePopups().popups.done(true)
      await flushPromises()

      expect(deleteSpy).toHaveBeenCalledWith({
        path: { memoryTracker: 1 },
      })
      expect(wrapper.emitted("refresh")).toBeTruthy()
      expect(wrapper.emitted("refresh")?.length).toBe(1)
    })

    it("shows confirmation dialog when delete button is clicked", async () => {
      const unansweredPrompt = makeMe.aRecallPrompt
        .withQuestionStem("Unanswered question")
        .please()
      const memoryTracker = makeMe.aMemoryTracker.please()

      const wrapper = helper
        .component(MemoryTrackerPageView)
        .withProps({
          recallPrompts: [unansweredPrompt],
          memoryTracker,
          memoryTrackerId: 1,
        })
        .mount()

      await flushPromises()

      const deleteButton = wrapper.find(
        'button[title="delete all unanswered recall prompts"]'
      )
      expect(deleteButton.exists()).toBe(true)
      await deleteButton.trigger("click")
      await flushPromises()

      // Verify confirmation dialog is shown
      const popups = usePopups().popups.peek()
      expect(popups?.length).toBe(1)
      expect(popups?.[0]?.type).toBe("confirm")
      // Clean up
      usePopups().popups.done(false)
      await flushPromises()
    })

    it("shows correct count in confirmation message for single prompt", async () => {
      const unansweredPrompt = makeMe.aRecallPrompt
        .withQuestionStem("Unanswered question")
        .please()
      const memoryTracker = makeMe.aMemoryTracker.please()

      mockSdkService("deleteUnansweredRecallPrompts", undefined)

      const wrapper = helper
        .component(MemoryTrackerPageView)
        .withProps({
          recallPrompts: [unansweredPrompt],
          memoryTracker,
          memoryTrackerId: 1,
        })
        .mount()

      await flushPromises()

      const deleteButton = wrapper.find(
        'button[title="delete all unanswered recall prompts"]'
      )
      expect(deleteButton.exists()).toBe(true)
      await deleteButton.trigger("click")
      await flushPromises()

      const popups = usePopups().popups.peek()
      expect(popups?.length).toBe(1)
      expect(popups?.[0]?.message).toBe(
        "Are you sure you want to delete 1 unanswered recall prompt?"
      )
      usePopups().popups.done(false)
      await flushPromises()
    })

    it("shows correct count in confirmation message for multiple prompts", async () => {
      const unansweredPrompt1 = makeMe.aRecallPrompt
        .withQuestionStem("Unanswered question 1")
        .please()
      const unansweredPrompt2 = makeMe.aRecallPrompt
        .withQuestionStem("Unanswered question 2")
        .please()
      const memoryTracker = makeMe.aMemoryTracker.please()

      mockSdkService("deleteUnansweredRecallPrompts", undefined)

      const wrapper = helper
        .component(MemoryTrackerPageView)
        .withProps({
          recallPrompts: [unansweredPrompt1, unansweredPrompt2],
          memoryTracker,
          memoryTrackerId: 1,
        })
        .mount()

      await flushPromises()

      const deleteButton = wrapper.find(
        'button[title="delete all unanswered recall prompts"]'
      )
      expect(deleteButton.exists()).toBe(true)
      await deleteButton.trigger("click")
      await flushPromises()

      const popups = usePopups().popups.peek()
      expect(popups?.length).toBe(1)
      expect(popups?.[0]?.message).toBe(
        "Are you sure you want to delete 2 unanswered recall prompts?"
      )
      usePopups().popups.done(false)
      await flushPromises()
    })

    it("does not show delete button when only contested unanswered prompts exist", async () => {
      const contestedPrompt = makeMe.aRecallPrompt
        .withQuestionStem("Contested question")
        .withIsContested(true)
        .please()
      const memoryTracker = makeMe.aMemoryTracker.please()

      const wrapper = helper
        .component(MemoryTrackerPageView)
        .withProps({
          recallPrompts: [contestedPrompt],
          memoryTracker,
          memoryTrackerId: 1,
        })
        .mount()

      await flushPromises()

      const deleteButton = wrapper.find(
        'button[title="delete all unanswered recall prompts"]'
      )
      expect(deleteButton.exists()).toBe(false)
    })

    it("excludes contested prompts from unanswered count in confirmation message", async () => {
      const unansweredPrompt = makeMe.aRecallPrompt
        .withQuestionStem("Unanswered question")
        .please()
      const contestedPrompt = makeMe.aRecallPrompt
        .withQuestionStem("Contested question")
        .withIsContested(true)
        .please()
      const memoryTracker = makeMe.aMemoryTracker.please()

      mockSdkService("deleteUnansweredRecallPrompts", undefined)

      const wrapper = helper
        .component(MemoryTrackerPageView)
        .withProps({
          recallPrompts: [unansweredPrompt, contestedPrompt],
          memoryTracker,
          memoryTrackerId: 1,
        })
        .mount()

      await flushPromises()

      const deleteButton = wrapper.find(
        'button[title="delete all unanswered recall prompts"]'
      )
      expect(deleteButton.exists()).toBe(true)
      await deleteButton.trigger("click")
      await flushPromises()

      const popups = usePopups().popups.peek()
      expect(popups?.length).toBe(1)
      expect(popups?.[0]?.message).toBe(
        "Are you sure you want to delete 1 unanswered recall prompt?"
      )
      usePopups().popups.done(false)
      await flushPromises()
    })
  })

  describe("skipped memory tracker", () => {
    it("shows skipped indicator at the top when memory tracker is skipped", async () => {
      const memoryTracker = makeMe.aMemoryTracker
        .removedFromTracking(true)
        .please()
      const recallPrompt = makeMe.aRecallPrompt
        .withQuestionStem("Test question")
        .please()

      const wrapper = helper
        .component(MemoryTrackerPageView)
        .withProps({
          recallPrompts: [recallPrompt],
          memoryTracker,
          memoryTrackerId: 1,
        })
        .mount()

      await flushPromises()

      expect(wrapper.text()).toContain(
        "This memory tracker is currently skipped and will not appear in review sessions."
      )
    })

    it("shows re-enable button when memory tracker is skipped", async () => {
      const memoryTracker = makeMe.aMemoryTracker
        .removedFromTracking(true)
        .please()
      const recallPrompt = makeMe.aRecallPrompt
        .withQuestionStem("Test question")
        .please()

      const wrapper = helper
        .component(MemoryTrackerPageView)
        .withProps({
          recallPrompts: [recallPrompt],
          memoryTracker,
          memoryTrackerId: 1,
        })
        .mount()

      await flushPromises()

      const reEnableButton = wrapper.find(
        'button[title="Re-enable this memory tracker"]'
      )
      expect(reEnableButton.exists()).toBe(true)
      expect(reEnableButton.text()).toContain("Re-enable")
    })

    it("shows recall prompts even when memory tracker is skipped", async () => {
      const memoryTracker = makeMe.aMemoryTracker
        .removedFromTracking(true)
        .please()
      const recallPrompt = makeMe.aRecallPrompt
        .withQuestionStem("Test question")
        .withChoices(["A", "B", "C"])
        .please()

      const wrapper = helper
        .component(MemoryTrackerPageView)
        .withProps({
          recallPrompts: [recallPrompt],
          memoryTracker,
          memoryTrackerId: 1,
        })
        .mount()

      await flushPromises()

      expect(wrapper.text()).toContain("Test question")
      expect(wrapper.text()).toContain("A")
      expect(wrapper.text()).toContain("B")
      expect(wrapper.text()).toContain("C")
    })

    it("hides remove from review button when memory tracker is skipped", async () => {
      const memoryTracker = makeMe.aMemoryTracker
        .removedFromTracking(true)
        .please()
      const recallPrompt = makeMe.aRecallPrompt
        .withQuestionStem("Test question")
        .please()

      const wrapper = helper
        .component(MemoryTrackerPageView)
        .withProps({
          recallPrompts: [recallPrompt],
          memoryTracker,
          memoryTrackerId: 1,
        })
        .mount()

      await flushPromises()

      const removeButton = wrapper.find(
        'button[title="remove this note from review"]'
      )
      expect(removeButton.exists()).toBe(false)
    })

    it("calls re-enable endpoint and emits refresh when re-enable button is clicked", async () => {
      const memoryTracker = makeMe.aMemoryTracker
        .removedFromTracking(true)
        .please()
      const recallPrompt = makeMe.aRecallPrompt
        .withQuestionStem("Test question")
        .please()

      const reEnableSpy = mockSdkService("reEnable", {
        ...memoryTracker,
        removedFromTracking: false,
      })

      const wrapper = helper
        .component(MemoryTrackerPageView)
        .withProps({
          recallPrompts: [recallPrompt],
          memoryTracker,
          memoryTrackerId: 1,
        })
        .mount()

      await flushPromises()

      const reEnableButton = wrapper.find(
        'button[title="Re-enable this memory tracker"]'
      )
      expect(reEnableButton.exists()).toBe(true)
      await reEnableButton.trigger("click")
      await flushPromises()

      expect(reEnableSpy).toHaveBeenCalledWith({
        path: { memoryTracker: 1 },
      })
      expect(wrapper.emitted("refresh")).toBeTruthy()
      expect(wrapper.emitted("refresh")?.length).toBe(1)
    })

    it("does not show skipped indicator when memory tracker is not skipped", async () => {
      const memoryTracker = makeMe.aMemoryTracker
        .removedFromTracking(false)
        .please()
      const recallPrompt = makeMe.aRecallPrompt
        .withQuestionStem("Test question")
        .please()

      const wrapper = helper
        .component(MemoryTrackerPageView)
        .withProps({
          recallPrompts: [recallPrompt],
          memoryTracker,
          memoryTrackerId: 1,
        })
        .mount()

      await flushPromises()

      expect(wrapper.text()).not.toContain(
        "This memory tracker is currently skipped"
      )
      const reEnableButton = wrapper.find(
        'button[title="Re-enable this memory tracker"]'
      )
      expect(reEnableButton.exists()).toBe(false)
    })
  })

  describe("spelling question type", () => {
    it("displays spelling question message when question type is SPELLING and unanswered", async () => {
      const memoryTracker = makeMe.aMemoryTracker.please()
      const spellingPrompt = makeMe.aRecallPrompt
        .withQuestionType("SPELLING")
        .please()

      const wrapper = helper
        .component(MemoryTrackerPageView)
        .withProps({
          recallPrompts: [spellingPrompt],
          memoryTracker,
          memoryTrackerId: 1,
        })
        .mount()

      await flushPromises()

      expect(wrapper.text()).toContain(
        "This is a spelling question. Details are not needed."
      )
    })

    it("displays spelling answer information when answered correctly", async () => {
      const memoryTracker = makeMe.aMemoryTracker.please()
      const spellingPrompt = makeMe.aRecallPrompt
        .withQuestionType("SPELLING")
        .withAnswer({
          id: 1,
          spellingAnswer: "Sedition",
          correct: true,
          thinkingTimeMs: 3000,
        })
        .withAnswerTime(new Date().toISOString())
        .please()

      const wrapper = helper
        .component(MemoryTrackerPageView)
        .withProps({
          recallPrompts: [spellingPrompt],
          memoryTracker,
          memoryTrackerId: 1,
        })
        .mount()

      await flushPromises()

      expect(wrapper.text()).toContain("Your answer:")
      expect(wrapper.text()).toContain("Sedition")
      expect(wrapper.text()).toContain("Result:")
      expect(wrapper.text()).toContain("Correct")
      expect(wrapper.text()).toContain("Thinking time: 3.0s")
      expect(wrapper.text()).not.toContain(
        "This is a spelling question. Details are not needed."
      )
    })

    it("displays spelling answer information when answered incorrectly", async () => {
      const memoryTracker = makeMe.aMemoryTracker.please()
      const spellingPrompt = makeMe.aRecallPrompt
        .withQuestionType("SPELLING")
        .withAnswer({
          id: 1,
          spellingAnswer: "asdf",
          correct: false,
          thinkingTimeMs: 1500,
        })
        .withAnswerTime(new Date().toISOString())
        .please()

      const wrapper = helper
        .component(MemoryTrackerPageView)
        .withProps({
          recallPrompts: [spellingPrompt],
          memoryTracker,
          memoryTrackerId: 1,
        })
        .mount()

      await flushPromises()

      expect(wrapper.text()).toContain("Your answer:")
      expect(wrapper.text()).toContain("asdf")
      expect(wrapper.text()).toContain("Result:")
      expect(wrapper.text()).toContain("Incorrect")
      expect(wrapper.text()).toContain("Thinking time: 1.5s")
    })

    it("does not display question details for spelling questions", async () => {
      const memoryTracker = makeMe.aMemoryTracker.please()
      const spellingPrompt = makeMe.aRecallPrompt
        .withQuestionType("SPELLING")
        .please()

      const wrapper = helper
        .component(MemoryTrackerPageView)
        .withProps({
          recallPrompts: [spellingPrompt],
          memoryTracker,
          memoryTrackerId: 1,
        })
        .mount()

      await flushPromises()

      // Should not display multiple choice question
      expect(wrapper.findComponent({ name: "QuestionDisplay" }).exists()).toBe(
        false
      )
    })
  })
})
