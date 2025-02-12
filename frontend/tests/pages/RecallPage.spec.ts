import RecallPage from "@/pages/RecallPage.vue"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { useRouter } from "vue-router"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import RenderingHelper from "@tests/helpers/RenderingHelper"
import mockBrowserTimeZone from "@tests/helpers/mockBrowserTimeZone"

vitest.mock("vue-router", () => ({
  useRouter: () => ({
    currentRoute: {
      value: {
        name: "recall",
      },
    },
  }),
}))

useRouter().currentRoute.value.name = "recall"

let renderer: RenderingHelper<typeof RecallPage>
const mockedRepeatCall = vi.fn()

let teleportTarget: HTMLDivElement

beforeEach(() => {
  teleportTarget = document.createElement("div")
  teleportTarget.id = "head-status"
  document.body.appendChild(teleportTarget)
})
afterEach(() => {
  document.body.innerHTML = ""
})

beforeEach(() => {
  vitest.resetAllMocks()
  helper.managedApi.restNoteController.show = vi
    .fn()
    .mockResolvedValue(makeMe.aNote.please())
  helper.managedApi.restRecallsController.recalling = mockedRepeatCall
  renderer = helper
    .component(RecallPage)
    .withStorageProps({ eagerFetchCount: 1 })
})

describe("repeat page", () => {
  const mountPage = async () => {
    const wrapper = renderer.currentRoute({ name: "recall" }).mount()
    await flushPromises()
    return wrapper
  }

  mockBrowserTimeZone("Asia/Shanghai", beforeEach, afterEach)

  it("redirect to review page if nothing to repeat", async () => {
    const repetition = makeMe.aDueMemoryTrackersList.please()
    mockedRepeatCall.mockResolvedValue(repetition)
    await mountPage()
    expect(mockedRepeatCall).toHaveBeenCalledWith("Asia/Shanghai", 0)
  })

  describe('repeat page with "just review" quiz', () => {
    const firstMemoryTrackerId = 123
    const secondMemoryTrackerId = 456
    const mockedRandomQuestionCall = vi.fn()
    const mockedMemoryTrackerCall = vi.fn()

    beforeEach(() => {
      vi.useFakeTimers()
      helper.managedApi.restMemoryTrackerController.show1 =
        mockedMemoryTrackerCall.mockResolvedValue(
          makeMe.aMemoryTracker.please()
        )
      helper.managedApi.silent.restRecallPromptController.askAQuestion =
        mockedRandomQuestionCall
      mockedRandomQuestionCall.mockRejectedValueOnce(makeMe.anApiError.please())
      mockedRepeatCall.mockResolvedValue(
        makeMe.aDueMemoryTrackersList
          .toRepeat([firstMemoryTrackerId, secondMemoryTrackerId, 3])
          .please()
      )
    })

    it("shows the progress", async () => {
      await mountPage()
      expect(teleportTarget.textContent).toContain("0/3")
      expect(mockedRandomQuestionCall).toHaveBeenCalledWith(
        firstMemoryTrackerId
      )
    })

    it("should show progress", async () => {
      const wrapper = await mountPage()
      const answerResult = makeMe.anAnsweredQuestion
        .withRecallPromptId(1)
        .answerCorrect(false)
        .please()
      const mockedMarkAsRepeatedCall = vi.fn().mockResolvedValue(answerResult)
      helper.managedApi.restMemoryTrackerController.markAsRepeated =
        mockedMarkAsRepeatedCall
      const recallPrompt = makeMe.aRecallPrompt.please()
      mockedRandomQuestionCall.mockResolvedValueOnce(recallPrompt)
      vi.runOnlyPendingTimers()
      await flushPromises()
      await wrapper.find("button.daisy-btn-primary").trigger("click")
      expect(mockedMarkAsRepeatedCall).toHaveBeenCalledWith(
        firstMemoryTrackerId,
        true
      )
      await flushPromises()
      expect(teleportTarget.textContent).toContain("1/3")
      expect(mockedRandomQuestionCall).toHaveBeenCalledWith(
        secondMemoryTrackerId
      )
    })

    it("should move current memory tracker to end when requested", async () => {
      const wrapper = await mountPage()

      // Initial order should be [123, 456, 3]
      expect(wrapper.vm.toRepeat).toEqual([123, 456, 3])

      // Click the "Move to end" button
      await wrapper.find('button[title="Move to end of list"]').trigger("click")

      // New order should be [456, 3, 123]
      expect(wrapper.vm.toRepeat).toEqual([456, 3, 123])
    })

    it("should not show move to end button for last item", async () => {
      const wrapper = await mountPage()

      // Move to last item
      wrapper.vm.currentIndex = 2
      await wrapper.vm.$nextTick()

      const quiz = wrapper.findComponent({ name: "Quiz" })
      expect(quiz.vm.canMoveToEnd).toBe(false)
    })
  })

  describe('repeat page with "spelling" quiz', () => {
    const firstMemoryTrackerId = 123
    const mockedRandomQuestionCall = vi.fn()
    const mockedMemoryTrackerCall = vi.fn()

    beforeEach(() => {
      vi.useFakeTimers()
      helper.managedApi.restMemoryTrackerController.show1 =
        mockedMemoryTrackerCall.mockResolvedValue(
          makeMe.aMemoryTracker.please()
        )
      helper.managedApi.silent.restRecallPromptController.askAQuestion =
        mockedRandomQuestionCall
      const recallPrompt = makeMe.aRecallPrompt.please()
      recallPrompt.id = 1
      recallPrompt.bareQuestion.checkSpell = true
      recallPrompt.bareQuestion.multipleChoicesQuestion.choices = []
      mockedRandomQuestionCall.mockResolvedValueOnce(recallPrompt)

      mockedRepeatCall.mockResolvedValue(
        makeMe.aDueMemoryTrackersList.toRepeat([firstMemoryTrackerId]).please()
      )
    })

    it("should handle spelling questions correctly", async () => {
      const answerResult = makeMe.anAnsweredQuestion
        .withRecallPromptId(1)
        .answerCorrect(false)
        .please()

      // Add spelling-specific properties
      answerResult.predefinedQuestion = makeMe.aPredefinedQuestion
        .withSpellCheck()
        .withQuestionStem("test question")
        .please()
      const note = makeMe.aNote.please()
      note.id = 42
      answerResult.note = note
      answerResult.answerDisplay = "test answer"
      answerResult.answer = { id: 1, correct: false }

      const mockedAnswerSpellingCall = vi.fn().mockResolvedValue(answerResult)
      helper.managedApi.restRecallPromptController.answerSpelling =
        mockedAnswerSpellingCall

      const wrapper = await mountPage()
      await flushPromises()

      // Debug: print the wrapper's HTML
      await wrapper.find("input#memory_tracker-answer").setValue("test answer")
      await flushPromises()
      await wrapper.find("form").trigger("submit")
      await flushPromises()
      expect(mockedAnswerSpellingCall).toHaveBeenCalled()

      // Verify that a spelling result was created and displayed correctly
      const answeredSpellingQuestion = wrapper.findComponent({
        name: "AnsweredSpellingQuestion",
      })
      expect(answeredSpellingQuestion.exists()).toBe(true)
      const spellingAlert = answeredSpellingQuestion.find(".daisy-alert-error")
      expect(spellingAlert.exists()).toBe(true)
      expect(spellingAlert.text()).toContain(
        'Your answer "test answer" is incorrect.'
      )

      // Verify note is displayed
      const noteShow = answeredSpellingQuestion.findComponent({
        name: "NoteShow",
      })
      expect(noteShow.exists()).toBe(true)
      expect(noteShow.props("noteId")).toBe(42)
    })
  })
})
