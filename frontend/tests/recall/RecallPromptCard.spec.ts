import makeMe from "donut-test-fixtures/makeMe"
import { flushPromises } from "@vue/test-utils"
import { describe, it, expect } from "vitest"
import {
  getRecallPromptSpy,
  contentLoaderVisible,
  contestableQuestionVisible,
  createDeferredGate,
  createMemoryTrackerLite,
  getRecallPrompt,
  justReviewVisible,
  mockAnswerSpelling,
  mockSpellingRecallServices,
  mountRecallPromptCard,
  mountRecallPromptCardReady,
  setupRecallPromptCardTests,
  spellingQuestionVisible,
  submitSpellingAnswerFromRecallPromptCard,
  wrapSdkError,
  wrapSdkResponse,
} from "./recallPromptCardTestSupport"

describe("repeat page", () => {
  setupRecallPromptCardTests()

  describe('repeat page with "just review" recall prompt card', () => {
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
        await mountRecallPromptCardReady(memoryTrackerIds, eagerFetchCount)
        for (const [index, memoryTrackerId] of expectedTrackerIds.entries()) {
          expect(getRecallPromptSpy).toHaveBeenNthCalledWith(
            index + 1,
            expect.objectContaining({
              path: { memoryTracker: memoryTrackerId },
            })
          )
        }
      }
    )

    it("fetches the new current prompt when the tracker list changes during prefetch", async () => {
      const { gate, resolve } = createDeferredGate()
      getRecallPromptSpy.mockImplementation(async (options) => {
        const memoryTracker = (options as { path: { memoryTracker: number } })
          .path.memoryTracker
        if (memoryTracker === 1) {
          await gate
        }
        return wrapSdkResponse(getRecallPrompt())
      })

      const recallPromptCardWrapper = mountRecallPromptCard([1, 2, 3, 4, 5], 5)
      await recallPromptCardWrapper.vm.$nextTick()

      await recallPromptCardWrapper.setProps({
        memoryTrackers: [6, 7, 8, 9, 10].map((id) =>
          createMemoryTrackerLite(id)
        ),
      })

      resolve()
      await flushPromises()

      expect(getRecallPromptSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          path: { memoryTracker: 6 },
        })
      )
      expect(contestableQuestionVisible(recallPromptCardWrapper)).toBe(true)
    })

    it("does not fetch question 2 again after prefetched", async () => {
      const recallPromptCardWrapper = await mountRecallPromptCardReady(
        [1, 2, 3, 4],
        2
      )
      expect(getRecallPromptSpy).toBeCalledTimes(2)
      await recallPromptCardWrapper.setProps({ currentIndex: 1 })
      expect(getRecallPromptSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          path: { memoryTracker: 3 },
        })
      )
    })
  })

  describe("spelling questions", () => {
    it("shows spelling question input when question has no choices", async () => {
      mockSpellingRecallServices()

      const recallPromptCardWrapper = await mountRecallPromptCardReady(
        [1],
        1,
        true
      )

      expect(spellingQuestionVisible(recallPromptCardWrapper)).toBe(true)
      expect(contestableQuestionVisible(recallPromptCardWrapper)).toBe(false)
    })

    it("submits spelling answer correctly", async () => {
      const spellingRecallPrompt = mockSpellingRecallServices()
      const answerResult = makeMe.anAnsweredQuestion
        .spelling()
        .withAnswer({ id: 1, correct: true, spellingAnswer: "cat" })
        .please()
      const mockedAnswerSpelling = mockAnswerSpelling(answerResult)

      const recallPromptCardWrapper = await mountRecallPromptCardReady(
        [1],
        1,
        true
      )
      await submitSpellingAnswerFromRecallPromptCard(recallPromptCardWrapper)

      expect(mockedAnswerSpelling).toHaveBeenCalledWith({
        path: { recallPrompt: spellingRecallPrompt.id },
        body: {
          recallPromptId: spellingRecallPrompt.id,
          spellingAnswer: "cat",
          thinkingTimeMs: 0,
          awayMs: 0,
          awayCount: 0,
          detourMs: 0,
          detourCount: 0,
          idleMs: 0,
        },
      })

      const emitted = recallPromptCardWrapper.emitted()
      expect(emitted.answered).toBeTruthy()
      expect(emitted.answered![0]).toEqual([answerResult])
    })
  })

  describe("loading state when fetching recall prompt", () => {
    it("should show ContentLoader, not JustReview, when navigating to a memory tracker that previously failed", async () => {
      const recallPrompt = getRecallPrompt()
      let tracker1Calls = 0
      const { gate, resolve } = createDeferredGate()
      getRecallPromptSpy.mockImplementation(async (options) => {
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

      const recallPromptCardWrapper = await mountRecallPromptCardReady(
        [1, 2],
        1
      )

      expect(justReviewVisible(recallPromptCardWrapper)).toBe(true)

      await recallPromptCardWrapper.setProps({ currentIndex: 1 })
      await flushPromises()
      await recallPromptCardWrapper.setProps({ currentIndex: 0 })

      expect(contentLoaderVisible(recallPromptCardWrapper)).toBe(true)

      resolve()
      await flushPromises()
    })
  })
})
