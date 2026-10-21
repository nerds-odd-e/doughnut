import {
  MemoryTrackerController,
  RecallPromptController,
} from "@generated/donut-backend-api/sdk.gen"
import type { AnswerData, AnsweredQuestion } from "@generated/donut-backend-api"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkError, wrapSdkResponse } from "@tests/helpers"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { beforeEach, describe, expect, it, vi } from "vitest"
import {
  createMemoryTrackerLite,
  givenRecallQueue,
  useRecallPageSpecContext,
} from "./recallPageTestSupport"

const ctx = useRecallPageSpecContext({ fakeTimers: true })
const choices = ["First choice", "Second choice"]
const firstPrompt = () =>
  makeMe.aRecallPrompt
    .withId(101)
    .withQuestionStem("First question")
    .withChoices(choices)
    .please()
const secondPrompt = () =>
  makeMe.aRecallPrompt
    .withId(102)
    .withQuestionStem("Next question")
    .withChoices(["Next A", "Next B"])
    .please()
const acceptedAnswer = (correct: boolean) =>
  makeMe.anAnsweredQuestion
    .withId(101)
    .withMemoryTrackerId(1)
    .withMcq(
      makeMe.anMcq
        .withQuestionStem("First question")
        .withChoices(choices)
        .please()
    )
    .withAnswer({ id: 201, correct, choiceIndex: correct ? 0 : 1 })
    .please()
const progress = (wrapper: VueWrapper) => wrapper.find(".progress-bar").text()
const retry = (wrapper: VueWrapper) =>
  wrapper.find('[data-testid="retry-answer"]')

beforeEach(() => {
  givenRecallQueue(createMemoryTrackerLite(1), createMemoryTrackerLite(2))
  mockSdkService(
    MemoryTrackerController,
    "getRecallPrompt",
    secondPrompt()
  ).mockResolvedValueOnce(wrapSdkResponse(firstPrompt()))
  mockSdkService(
    MemoryTrackerController,
    "showMemoryTracker",
    makeMe.aMemoryTracker.please()
  )
  mockSdkService(MemoryTrackerController, "getThresholdExceeded", {
    thresholdExceeded: false,
  })
})

describe("RecallPage MCQ answer recovery", () => {
  it.each([true, false])(
    "retains the submission through repeated failures and recovers the accepted correct=%s answer once",
    async (correct) => {
      const accepted = acceptedAnswer(correct)
      let finishRetry!: (
        value: ReturnType<typeof wrapSdkResponse<AnsweredQuestion>>
      ) => void
      const answer = mockSdkService(RecallPromptController, "answer", accepted)
        .mockResolvedValueOnce(wrapSdkError("response lost"))
        .mockResolvedValueOnce(wrapSdkError("still unavailable"))
        .mockImplementationOnce(
          () =>
            new Promise((resolve) => {
              finishRetry = resolve
            })
        )
      const wrapper = await ctx.mountPage()
      const selected = correct ? 0 : 1
      await wrapper.findAll("li.choice button")[selected]!.trigger("click")
      await flushPromises()

      expect(wrapper.text()).toContain("First question")
      expect(wrapper.find('[data-testid="answer-recovery"]').text()).toContain(
        "We couldn't confirm your answer. Retry to recover this attempt."
      )
      expect(progress(wrapper)).toContain("0/2")
      const retainedChoice = wrapper.find("button.is-selected")
      expect(retainedChoice.text()).toBe(choices[selected])
      expect(retainedChoice.classes()).not.toContain("is-correct")
      expect(retainedChoice.classes()).not.toContain("is-incorrect")
      expect(retainedChoice.attributes("disabled")).toBeDefined()
      await vi.advanceTimersByTimeAsync(5000)
      await flushPromises()
      expect(answer).toHaveBeenCalledTimes(1)
      const original = { ...(answer.mock.calls[0]![0] as AnswerData).body }
      expect(original).toEqual({
        choiceIndex: selected,
        thinkingTimeMs: expect.any(Number),
        awayMs: expect.any(Number),
        awayCount: expect.any(Number),
        detourMs: expect.any(Number),
        detourCount: expect.any(Number),
        idleMs: expect.any(Number),
      })

      await retry(wrapper).trigger("click")
      await flushPromises()
      expect(retry(wrapper).text()).toBe("Retry")
      const retryControl = retry(wrapper).element as HTMLButtonElement
      retryControl.click()
      retryControl.click()
      await flushPromises()
      expect(
        wrapper.find("button.is-selected").attributes("disabled")
      ).toBeDefined()
      await wrapper.findAll("li.choice button")[1 - selected]!.trigger("click")
      expect(answer).toHaveBeenCalledTimes(3)
      expect(
        answer.mock.calls.map(([options]) => (options as AnswerData).body)
      ).toEqual([original, original, original])
      finishRetry(wrapSdkResponse(accepted))
      await flushPromises()

      expect(progress(wrapper)).toContain("1/2")
      if (correct) {
        expect(wrapper.text()).toContain("Next question")
        await wrapper
          .find('button[title="view last answered question"]')
          .trigger("click")
        await flushPromises()
      }
      expect(wrapper.find("button.is-selected").classes()).toContain(
        correct ? "is-correct" : "is-incorrect"
      )
      expect(wrapper.find("button.is-selected").text()).toBe(choices[selected])
      expect(progress(wrapper)).toContain("1/2")
      expect(answer).toHaveBeenCalledTimes(3)
    }
  )

  it.each([false, true])(
    "keeps navigation available and ignores a late response while recovery=%s is pending",
    async (recovering) => {
      let finish!: (
        value: ReturnType<typeof wrapSdkResponse<AnsweredQuestion>>
      ) => void
      const answer = mockSdkService(
        RecallPromptController,
        "answer",
        acceptedAnswer(true)
      )
      if (recovering)
        answer.mockResolvedValueOnce(wrapSdkError("response lost"))
      answer.mockImplementationOnce(
        () =>
          new Promise((resolve) => {
            finish = resolve
          })
      )
      const wrapper = await ctx.mountPage()
      await wrapper.find("li.choice button").trigger("click")
      await flushPromises()
      if (recovering) {
        await retry(wrapper).trigger("click")
        await flushPromises()
      }
      await wrapper.find(".progress-bar").trigger("click")
      await flushPromises()
      const moveToEnd = document.body.querySelector<HTMLButtonElement>(
        'button[title="Move to end of list"]'
      )!
      expect(moveToEnd.disabled).toBe(false)
      moveToEnd.click()
      await flushPromises()
      expect(wrapper.text()).toContain("Next question")
      finish(wrapSdkResponse(acceptedAnswer(true)))
      await flushPromises()
      expect(wrapper.text()).toContain("Next question")
      expect(progress(wrapper)).toContain("0/2")
    }
  )
})
