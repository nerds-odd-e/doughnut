import RecallPage from "@/pages/RecallPage.vue"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { useRouter } from "vue-router"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import RenderingHelper from "@tests/helpers/RenderingHelper"
import mockBrowserTimeZone from "@tests/helpers/mockBrowserTimeZone"
import type { SpellingResultDto, MemoryTrackerLite } from "@generated/backend"
import * as sdk from "@generated/backend/sdk.gen"

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
  vi.spyOn(helper.managedApi.services, "showNote").mockResolvedValue(
    makeMe.aNote.please() as never
  )
  vi.spyOn(sdk, "recalling").mockImplementation(async (options) => {
    const result = await mockedRepeatCall(options)
    return {
      data: result,
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    }
  })
  vi.spyOn(sdk, "getSpellingQuestion").mockResolvedValue({
    data: { stem: "Spell the word 'cat'" } as never,
    error: undefined,
    request: {} as Request,
    response: {} as Response,
  })
  renderer = helper
    .component(RecallPage)
    .withStorageProps({ eagerFetchCount: 1 })
})

describe("repeat page", () => {
  const createMemoryTrackerLite = (
    id: number,
    spelling = false
  ): MemoryTrackerLite => ({
    memoryTrackerId: id,
    spelling,
  })

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
    expect(vi.spyOn(sdk, "recalling")).toHaveBeenCalledWith({
      query: {
        timezone: "Asia/Shanghai",
        dueindays: 0,
      },
    })
  })

  describe('repeat page with "just review" quiz', () => {
    const firstMemoryTrackerId = 123
    const secondMemoryTrackerId = 456
    const mockedRandomQuestionCall = vi.fn()

    beforeEach(() => {
      vi.useFakeTimers()
      vi.spyOn(sdk, "showMemoryTracker").mockResolvedValue({
        data: makeMe.aMemoryTracker.please(),
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })
      vi.spyOn(sdk, "askAQuestion").mockImplementation(async (options) => {
        try {
          const result = await mockedRandomQuestionCall(options)
          return {
            data: result,
            error: undefined,
            request: {} as Request,
            response: {} as Response,
          }
        } catch (error) {
          return {
            data: undefined,
            error: error instanceof Error ? error.message : String(error),
            request: {} as Request,
            response: {} as Response,
          }
        }
      })
      mockedRandomQuestionCall.mockRejectedValueOnce(makeMe.anApiError.please())
      mockedRepeatCall.mockResolvedValue(
        makeMe.aDueMemoryTrackersList
          .toRepeat([
            createMemoryTrackerLite(firstMemoryTrackerId),
            createMemoryTrackerLite(secondMemoryTrackerId),
            createMemoryTrackerLite(3),
          ])
          .please()
      )
    })

    it("shows the progress", async () => {
      await mountPage()
      expect(teleportTarget.textContent).toContain("0/3")
      expect(mockedRandomQuestionCall).toHaveBeenCalledWith(
        expect.objectContaining({
          path: { memoryTracker: firstMemoryTrackerId },
        })
      )
    })

    it("should show progress", async () => {
      const wrapper = await mountPage()
      const answerResult = makeMe.anAnsweredQuestion
        .withRecallPromptId(1)
        .answerCorrect(false)
        .please()
      const mockedMarkAsRepeatedCall = vi
        .spyOn(sdk, "markAsRepeated")
        .mockResolvedValue({
          data: answerResult as never,
          error: undefined,
          request: {} as Request,
          response: {} as Response,
        })
      const recallPrompt = makeMe.aRecallPrompt.please()
      mockedRandomQuestionCall.mockResolvedValueOnce(recallPrompt)
      vi.runOnlyPendingTimers()
      await flushPromises()
      await wrapper.find("button.daisy-btn-primary").trigger("click")
      expect(mockedMarkAsRepeatedCall).toHaveBeenCalledWith({
        path: { memoryTracker: firstMemoryTrackerId },
        query: { successful: true },
      })
      await flushPromises()
      expect(teleportTarget.textContent).toContain("1/3")
      expect(mockedRandomQuestionCall).toHaveBeenCalledWith(
        expect.objectContaining({
          path: { memoryTracker: secondMemoryTrackerId },
        })
      )
    })

    it("should move current memory tracker to end when requested", async () => {
      const wrapper = await mountPage()
      type ExposedVM = { toRepeat?: MemoryTrackerLite[]; currentIndex: number }
      const vm = wrapper.vm as unknown as ExposedVM

      // Initial order should be [123, 456, 3]
      expect(vm.toRepeat?.map((t) => t.memoryTrackerId)).toEqual([123, 456, 3])

      // Click the "Move to end" button
      await wrapper.find('button[title="Move to end of list"]').trigger("click")

      // New order should be [456, 3, 123]
      expect(vm.toRepeat?.map((t) => t.memoryTrackerId)).toEqual([456, 3, 123])
    })

    it("should not show move to end button for last item", async () => {
      const wrapper = await mountPage()
      type ExposedVM = { toRepeat?: MemoryTrackerLite[]; currentIndex: number }
      const vm = wrapper.vm as unknown as ExposedVM

      // Move to last item
      vm.currentIndex = 2
      await wrapper.vm.$nextTick()

      const quiz = wrapper.findComponent({ name: "Quiz" })
      expect(quiz.vm.canMoveToEnd).toBe(false)
    })
  })

  describe('repeat page with "spelling" quiz', () => {
    const firstMemoryTrackerId = 123
    const mockedRandomQuestionCall = vi.fn()

    beforeEach(() => {
      vi.useFakeTimers()
      vi.spyOn(sdk, "showMemoryTracker").mockResolvedValue({
        data: makeMe.aMemoryTracker.please(),
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })
      vi.spyOn(sdk, "askAQuestion").mockImplementation(async (options) => {
        const result = await mockedRandomQuestionCall(options)
        return {
          data: result,
          error: undefined,
          request: {} as Request,
          response: {} as Response,
        }
      })

      mockedRepeatCall.mockResolvedValue(
        makeMe.aDueMemoryTrackersList
          .toRepeat([createMemoryTrackerLite(firstMemoryTrackerId, true)])
          .please()
      )
    })

    it("should handle spelling questions correctly", async () => {
      const note = makeMe.aNote.please()
      note.id = 42
      const answerResult: SpellingResultDto = {
        note,
        answer: "test answer",
        isCorrect: false,
      }

      const mockedAnswerSpellingCall = vi
        .spyOn(sdk, "answerSpelling")
        .mockResolvedValue({
          data: answerResult,
          error: undefined,
          request: {} as Request,
          response: {} as Response,
        })

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
        "Your answer `test answer` is incorrect."
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
