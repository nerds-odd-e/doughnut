import Quiz from "@/components/review/Quiz.vue"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, it, vi } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import type { MemoryTrackerLite } from "@generated/backend"
import * as sdk from "@generated/backend/sdk.gen"

describe("repeat page", () => {
  const recallPrompt = makeMe.aRecallPrompt.please()
  const mockedRandomQuestionCall = vi.fn().mockResolvedValue(recallPrompt)

  beforeEach(() => {
    vi.resetAllMocks()
    vi.useFakeTimers()
    vi.spyOn(helper.managedApi.services, "showNote").mockResolvedValue(
      makeMe.aNote.please() as never
    )
    vi.spyOn(sdk, "showMemoryTracker").mockResolvedValue({
      data: makeMe.aMemoryTracker.please(),
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })
    vi.spyOn(
      helper.managedApi.silent.services,
      "askAQuestion"
    ).mockImplementation(mockedRandomQuestionCall)
    vi.spyOn(sdk, "getSpellingQuestion").mockResolvedValue({
      data: { stem: "Spell the word 'cat'" } as never,
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })
  })

  const createMemoryTrackerLite = (
    id: number,
    spelling = false
  ): MemoryTrackerLite => ({
    memoryTrackerId: id,
    spelling,
  })

  const mountPage = async (
    memoryTrackerIds: number[],
    eagerFetchCount: number,
    spelling = false
  ) => {
    const memoryTrackers = memoryTrackerIds.map((id) =>
      createMemoryTrackerLite(id, spelling)
    )
    const wrapper = helper
      .component(Quiz)
      .withRouter()
      .withStorageProps({
        memoryTrackers,
        currentIndex: 0,
        eagerFetchCount,
      })
      .mount()
    await flushPromises()
    return wrapper
  }

  describe('repeat page with "just review" quiz', () => {
    it("fetch the first 1 question when mount", async () => {
      await mountPage([1, 2, 3], 1)
      expect(mockedRandomQuestionCall).toHaveBeenCalledWith({
        path: { memoryTracker: 1 },
      })
    })

    it("fetch the first 3 question when mount", async () => {
      await mountPage([111, 222, 333, 444], 3)
      expect(mockedRandomQuestionCall).nthCalledWith(1, {
        path: { memoryTracker: 111 },
      })
      expect(mockedRandomQuestionCall).nthCalledWith(2, {
        path: { memoryTracker: 222 },
      })
      expect(mockedRandomQuestionCall).nthCalledWith(3, {
        path: { memoryTracker: 333 },
      })
    })

    it("does not fetch question 2 again after prefetched", async () => {
      const wrapper = await mountPage([1, 2, 3, 4], 2)
      expect(mockedRandomQuestionCall).toBeCalledTimes(2)
      await wrapper.setProps({ currentIndex: 1 })
      expect(mockedRandomQuestionCall).toHaveBeenCalledWith({
        path: { memoryTracker: 3 },
      })
    })
  })

  describe("spelling questions", () => {
    it("shows spelling question input when question has no choices", async () => {
      const recallPromptWithoutChoices = makeMe.aRecallPrompt
        .withQuestionStem("Spell the word 'cat'")
        .please()
      mockedRandomQuestionCall.mockResolvedValue(recallPromptWithoutChoices)

      const wrapper = await mountPage([1], 1, true)

      expect(
        wrapper.findComponent({ name: "SpellingQuestionComponent" }).exists()
      ).toBe(true)
      expect(
        wrapper.findComponent({ name: "ContestableQuestion" }).exists()
      ).toBe(false)
    })

    it("submits spelling answer correctly", async () => {
      const recallPromptWithoutChoices = makeMe.aRecallPrompt
        .withQuestionStem("Spell the word 'cat'")
        .please()
      mockedRandomQuestionCall.mockResolvedValue(recallPromptWithoutChoices)

      const answerResult = makeMe.anAnsweredQuestion
        .answerCorrect(true)
        .please()
      vi.spyOn(helper.managedApi.services, "answerSpelling").mockResolvedValue(
        answerResult as never
      )

      const wrapper = await mountPage([1], 1, true)

      await wrapper
        .findComponent({ name: "SpellingQuestionComponent" })
        .vm.$emit("answer", { spellingAnswer: "cat" })
      await flushPromises()

      expect(helper.managedApi.services.answerSpelling).toHaveBeenCalledWith({
        path: { memoryTracker: 1 },
        body: {
          spellingAnswer: "cat",
        },
      })

      const emitted = wrapper.emitted()
      expect(emitted["answered-spelling"]).toBeTruthy()
      expect(emitted["answered-spelling"]![0]).toEqual([answerResult])
    })
  })
})
