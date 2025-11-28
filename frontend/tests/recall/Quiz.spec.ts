import Quiz from "@/components/review/Quiz.vue"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, it, vi, afterEach } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper, {
  mockSdkService,
  wrapSdkResponse,
  wrapSdkError,
} from "@tests/helpers"
import type { MemoryTrackerLite, SpellingResultDto } from "@generated/backend"

describe("repeat page", () => {
  const recallPrompt = makeMe.aRecallPrompt.please()
  let askAQuestionSpy: ReturnType<typeof mockSdkService<"askAQuestion">>

  beforeEach(() => {
    vi.resetAllMocks()
    vi.useFakeTimers()
    mockSdkService("showNote", makeMe.aNoteRealm.please())
    mockSdkService("showMemoryTracker", makeMe.aMemoryTracker.please())
    askAQuestionSpy = mockSdkService("askAQuestion", recallPrompt)
    mockSdkService("getSpellingQuestion", { stem: "Spell the word 'cat'" })
  })

  afterEach(() => {
    vi.useRealTimers()
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
      expect(askAQuestionSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          path: { memoryTracker: 1 },
        })
      )
    })

    it("fetch the first 3 question when mount", async () => {
      await mountPage([111, 222, 333, 444], 3)
      expect(askAQuestionSpy).nthCalledWith(
        1,
        expect.objectContaining({
          path: { memoryTracker: 111 },
        })
      )
      expect(askAQuestionSpy).nthCalledWith(
        2,
        expect.objectContaining({
          path: { memoryTracker: 222 },
        })
      )
      expect(askAQuestionSpy).nthCalledWith(
        3,
        expect.objectContaining({
          path: { memoryTracker: 333 },
        })
      )
    })

    it("does not fetch question 2 again after prefetched", async () => {
      const wrapper = await mountPage([1, 2, 3, 4], 2)
      expect(askAQuestionSpy).toBeCalledTimes(2)
      await wrapper.setProps({ currentIndex: 1 })
      expect(askAQuestionSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          path: { memoryTracker: 3 },
        })
      )
    })
  })

  describe("spelling questions", () => {
    it("shows spelling question input when question has no choices", async () => {
      const recallPromptWithoutChoices = makeMe.aRecallPrompt
        .withQuestionStem("Spell the word 'cat'")
        .please()
      askAQuestionSpy.mockResolvedValue(
        wrapSdkResponse(recallPromptWithoutChoices)
      )

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
      askAQuestionSpy.mockResolvedValue(
        wrapSdkResponse(recallPromptWithoutChoices)
      )

      const answerResult: SpellingResultDto = {
        note: makeMe.aNote.please(),
        answer: "cat",
        isCorrect: true,
      }
      const mockedAnswerSpelling = mockSdkService(
        "answerSpelling",
        answerResult
      )

      const wrapper = await mountPage([1], 1, true)
      await wrapper
        .findComponent({ name: "SpellingQuestionComponent" })
        .vm.$emit("answer", { spellingAnswer: "cat" })
      await flushPromises()

      expect(mockedAnswerSpelling).toHaveBeenCalledWith({
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

  describe("loading state when fetching recall prompt", () => {
    it("should show ContentLoader, not JustReview, when navigating to a memory tracker that previously failed", async () => {
      askAQuestionSpy.mockResolvedValueOnce(wrapSdkError("Failed to fetch"))
      const wrapper = await mountPage([1, 2], 1)
      await flushPromises()

      expect(wrapper.findComponent({ name: "JustReview" }).exists()).toBe(true)

      await wrapper.setProps({ currentIndex: 1 })
      await flushPromises()
      await wrapper.setProps({ currentIndex: 0 })

      expect(wrapper.findComponent({ name: "JustReview" }).exists()).toBe(false)
      expect(wrapper.findComponent({ name: "ContentLoader" }).exists()).toBe(
        true
      )

      await flushPromises()
    })
  })
})
