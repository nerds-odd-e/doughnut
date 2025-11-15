import Quiz from "@/components/review/Quiz.vue"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, it, vi } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import type { MemoryTrackerLite } from "@generated/backend"

describe("repeat page", () => {
  const recallPrompt = makeMe.aRecallPrompt.please()
  const mockedRandomQuestionCall = vi.fn().mockResolvedValue(recallPrompt)

  beforeEach(() => {
    vi.resetAllMocks()
    vi.useFakeTimers()
    vi.spyOn(helper.managedApi.services, "show").mockResolvedValue(
      makeMe.aNote.please() as never
    )
    vi.spyOn(helper.managedApi.services, "show1").mockResolvedValue(
      makeMe.aMemoryTracker.please() as never
    )
    vi.spyOn(
      helper.managedApi.silent.services,
      "askAquestion"
    ).mockImplementation(mockedRandomQuestionCall)
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
        memoryTracker: 1,
      })
    })

    it("fetch the first 3 question when mount", async () => {
      await mountPage([111, 222, 333, 444], 3)
      expect(mockedRandomQuestionCall).nthCalledWith(1, { memoryTracker: 111 })
      expect(mockedRandomQuestionCall).nthCalledWith(2, { memoryTracker: 222 })
      expect(mockedRandomQuestionCall).nthCalledWith(3, { memoryTracker: 333 })
    })

    it("does not fetch question 2 again after prefetched", async () => {
      const wrapper = await mountPage([1, 2, 3, 4], 2)
      expect(mockedRandomQuestionCall).toBeCalledTimes(2)
      await wrapper.setProps({ currentIndex: 1 })
      expect(mockedRandomQuestionCall).toHaveBeenCalledWith({
        memoryTracker: 3,
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
      vi.spyOn(
        helper.managedApi.services,
        "answerSpelling"
      ).mockResolvedValue(answerResult as never)

      const wrapper = await mountPage([1], 1, true)

      await wrapper
        .findComponent({ name: "SpellingQuestionComponent" })
        .vm.$emit("answer", { spellingAnswer: "cat" })
      await flushPromises()

      expect(
        helper.managedApi.services.answerSpelling
      ).toHaveBeenCalledWith({
        memoryTracker: 1,
        requestBody: {
          spellingAnswer: "cat",
        },
      })

      const emitted = wrapper.emitted()
      expect(emitted["answered-spelling"]).toBeTruthy()
      expect(emitted["answered-spelling"]![0]).toEqual([answerResult])
    })
  })
})
