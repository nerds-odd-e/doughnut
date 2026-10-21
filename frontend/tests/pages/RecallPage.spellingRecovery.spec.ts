import { RecallPromptController } from "@generated/donut-backend-api/sdk.gen"
import type {
  AnsweredQuestion,
  AnswerSpellingData,
} from "@generated/donut-backend-api"
import { mockSdkService, wrapSdkError, wrapSdkResponse } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, expect, it, vi } from "vitest"
import {
  moveCurrentRecallToEnd,
  useRecallPageSpecContext,
} from "./recallPageTestSupport"
import {
  givenSpellingRecoveryQueue,
  input,
  retry,
  progress,
  result,
  submit,
} from "./spellingRecoveryTestSupport"

const ctx = useRecallPageSpecContext({ fakeTimers: true })
beforeEach(() => {
  givenSpellingRecoveryQueue()
})

describe("RecallPage spelling submission recovery", () => {
  it.each([true, false])(
    "retains input and original timing through repeated failures and continues correct=%s once",
    async (correct) => {
      const accepted = result(correct)
      let finish!: (
        response: ReturnType<typeof wrapSdkResponse<AnsweredQuestion>>
      ) => void
      const answer = mockSdkService(
        RecallPromptController,
        "answerSpelling",
        accepted
      )
        .mockResolvedValueOnce(wrapSdkError("lost response"))
        .mockResolvedValueOnce(wrapSdkError("still unavailable"))
        .mockImplementationOnce(
          () =>
            new Promise((resolve) => {
              finish = resolve
            })
        )
      const wrapper = await ctx.mountPage()
      await submit(wrapper)
      expect(wrapper.text()).toContain("First spelling question")
      expect((input(wrapper).element as HTMLInputElement).value).toBe(
        "sedition"
      )
      expect(wrapper.find('[data-testid="answer-recovery"]').text()).toContain(
        "We couldn't confirm your answer. Retry to recover this attempt."
      )
      expect(progress(wrapper)).toContain("0/2")
      await vi.advanceTimersByTimeAsync(5000)
      await flushPromises()
      expect(answer).toHaveBeenCalledTimes(1)
      const original = {
        ...(answer.mock.calls[0]![0] as AnswerSpellingData).body,
      }
      expect(original).toEqual({
        spellingAnswer: "sedition",
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
      const control = retry(wrapper).element as HTMLButtonElement
      control.click()
      control.click()
      await wrapper.find("form").trigger("submit")
      await flushPromises()
      expect(input(wrapper).attributes("disabled")).toBeDefined()
      expect(answer).toHaveBeenCalledTimes(3)
      expect(
        answer.mock.calls.map(([options]) => ({
          path: (options as AnswerSpellingData).path,
          body: (options as AnswerSpellingData).body,
        }))
      ).toEqual(
        [1, 2, 3].map(() => ({ path: { recallPrompt: 101 }, body: original }))
      )
      finish(wrapSdkResponse(accepted))
      await flushPromises()
      expect(progress(wrapper)).toContain("1/2")
      if (correct) {
        expect(wrapper.text()).toContain("Next spelling question")
        await wrapper
          .find('button[title="view last answered question"]')
          .trigger("click")
        await flushPromises()
      }
      expect(wrapper.text()).toContain(
        correct ? "Correct!" : "Your answer `sedition` is incorrect."
      )
      expect(progress(wrapper)).toContain("1/2")
      expect(answer).toHaveBeenCalledTimes(3)
    }
  )

  it.each([false, true])(
    "ignores late spelling completion after navigation with recovery=%s pending",
    async (recovering) => {
      let finish!: (
        response: ReturnType<typeof wrapSdkResponse<AnsweredQuestion>>
      ) => void
      const answer = mockSdkService(
        RecallPromptController,
        "answerSpelling",
        result(true)
      )
      if (recovering)
        answer.mockResolvedValueOnce(wrapSdkError("lost response"))
      answer.mockImplementationOnce(
        () =>
          new Promise((resolve) => {
            finish = resolve
          })
      )
      const wrapper = await ctx.mountPage()
      await submit(wrapper)
      if (recovering) {
        await retry(wrapper).trigger("click")
        await flushPromises()
      }
      await moveCurrentRecallToEnd(wrapper)
      expect(wrapper.text()).toContain("Next spelling question")
      finish(wrapSdkResponse(result(true)))
      await flushPromises()
      expect(wrapper.text()).toContain("Next spelling question")
      expect(progress(wrapper)).toContain("0/2")
    }
  )
})
