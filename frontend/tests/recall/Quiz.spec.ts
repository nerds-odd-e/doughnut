import makeMe from "doughnut-test-fixtures/makeMe"
import { flushPromises } from "@vue/test-utils"
import { describe, it, expect } from "vitest"
import {
  askAQuestionSpy,
  contentLoaderVisible,
  contestableDummyInput,
  contestableQuestionVisible,
  createDeferredGate,
  getRecallPrompt,
  justReviewVisible,
  mockAnswerSpelling,
  mockSpellingRecallServices,
  mountQuizReady,
  setupQuizTests,
  spellingQuestionVisible,
  submitSpellingAnswerFromQuiz,
  wrapSdkError,
  wrapSdkResponse,
} from "./quizTestSupport"

describe("repeat page", () => {
  setupQuizTests()

  describe('repeat page with "just review" quiz', () => {
    it.each([
      {
        memoryTrackerIds: [1, 2, 3],
        eagerFetchCount: 1,
        expectedTrackerIds: [1],
      },
      {
        memoryTrackerIds: [111, 222, 333, 444],
        eagerFetchCount: 3,
        expectedTrackerIds: [111, 222, 333],
      },
    ])(
      "prefetches $eagerFetchCount question(s) on mount",
      async ({ memoryTrackerIds, eagerFetchCount, expectedTrackerIds }) => {
        await mountQuizReady(memoryTrackerIds, eagerFetchCount)
        for (const [index, memoryTrackerId] of expectedTrackerIds.entries()) {
          expect(askAQuestionSpy).toHaveBeenNthCalledWith(
            index + 1,
            expect.objectContaining({
              path: { memoryTracker: memoryTrackerId },
            })
          )
        }
      }
    )

    it("does not fetch question 2 again after prefetched", async () => {
      const quizWrapper = await mountQuizReady([1, 2, 3, 4], 2)
      expect(askAQuestionSpy).toBeCalledTimes(2)
      await quizWrapper.setProps({ currentIndex: 1 })
      expect(askAQuestionSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          path: { memoryTracker: 3 },
        })
      )
    })
  })

  describe("spelling questions", () => {
    it("shows spelling question input when question has no choices", async () => {
      mockSpellingRecallServices()

      const quizWrapper = await mountQuizReady([1], 1, true)

      expect(spellingQuestionVisible(quizWrapper)).toBe(true)
      expect(contestableQuestionVisible(quizWrapper)).toBe(false)
    })

    it("submits spelling answer correctly", async () => {
      const spellingRecallPrompt = mockSpellingRecallServices()
      const answerResult = makeMe.anAnsweredQuestion
        .spelling()
        .withAnswer({ id: 1, correct: true, spellingAnswer: "cat" })
        .please()
      const mockedAnswerSpelling = mockAnswerSpelling(answerResult)

      const quizWrapper = await mountQuizReady([1], 1, true)
      await submitSpellingAnswerFromQuiz(quizWrapper)

      expect(mockedAnswerSpelling).toHaveBeenCalledWith({
        path: { recallPrompt: spellingRecallPrompt.id },
        body: {
          spellingAnswer: "cat",
          thinkingTimeMs: 0,
        },
      })

      const emitted = quizWrapper.emitted()
      expect(emitted.answered).toBeTruthy()
      expect(emitted.answered![0]).toEqual([answerResult])
    })
  })

  describe("contestable dummy input", () => {
    it("clears when advancing to the next question", async () => {
      const recallPrompt = getRecallPrompt()
      const secondRecallPrompt = makeMe.aRecallQuestion
        .withQuestionStem("Second question")
        .please()
      askAQuestionSpy
        .mockResolvedValueOnce(wrapSdkResponse(recallPrompt))
        .mockResolvedValueOnce(wrapSdkResponse(secondRecallPrompt))

      const quizWrapper = await mountQuizReady([1, 2], 2)
      const dummyInput = contestableDummyInput(quizWrapper)
      await dummyInput.setValue("notes from previous question")

      await quizWrapper.setProps({ currentIndex: 1 })
      await flushPromises()

      expect(dummyInput.element.value).toBe("")
    })
  })

  describe("loading state when fetching recall prompt", () => {
    it("should show ContentLoader, not JustReview, when navigating to a memory tracker that previously failed", async () => {
      const recallPrompt = getRecallPrompt()
      let tracker1Calls = 0
      const { gate, resolve } = createDeferredGate()
      askAQuestionSpy.mockImplementation(async (options) => {
        const memoryTracker = (options as { path: { memoryTracker: number } })
          .path.memoryTracker
        if (memoryTracker === 1) {
          tracker1Calls += 1
          if (tracker1Calls === 1) {
            return wrapSdkError("Failed to fetch")
          }
          await gate
          return wrapSdkResponse(recallPrompt)
        }
        return wrapSdkResponse(recallPrompt)
      })

      const quizWrapper = await mountQuizReady([1, 2], 1)

      expect(justReviewVisible(quizWrapper)).toBe(true)

      await quizWrapper.setProps({ currentIndex: 1 })
      await flushPromises()
      await quizWrapper.setProps({ currentIndex: 0 })

      expect(justReviewVisible(quizWrapper)).toBe(false)
      expect(contentLoaderVisible(quizWrapper)).toBe(true)

      resolve()
      await flushPromises()
    })
  })
})
