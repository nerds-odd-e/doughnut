import {
  MemoryTrackerController,
  RecallPromptController,
} from "@generated/donut-backend-api/sdk.gen"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkError, wrapSdkResponse } from "@tests/helpers"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { beforeEach, describe, expect, it } from "vitest"
import {
  createMemoryTrackerLite,
  deferredRecallPromptResponse,
  givenRecallQueue,
  moveCurrentRecallToEnd,
  useRecallPageSpecContext,
} from "./recallPageTestSupport"

const ctx = useRecallPageSpecContext()
const prompt = (id: number) =>
  makeMe.aRecallPrompt
    .withId(id)
    .withQuestionStem(`Question ${id}`)
    .withChoices(["One", "Two"])
    .please()
const retry = (wrapper: VueWrapper) =>
  wrapper.find('[data-testid="retry-question"]')
const progress = (wrapper: VueWrapper) => wrapper.find(".progress-bar").text()

beforeEach(() => {
  givenRecallQueue(createMemoryTrackerLite(1), createMemoryTrackerLite(2))
})

describe("RecallPage question recovery", () => {
  it("retries the same tracker through repeated failure and pending duplicate clicks without recording learning", async () => {
    const pending = deferredRecallPromptResponse()
    const getPrompt = mockSdkService(
      MemoryTrackerController,
      "getRecallPrompt",
      prompt(1)
    )
      .mockResolvedValueOnce(wrapSdkError("unavailable"))
      .mockResolvedValueOnce(wrapSdkError("still unavailable"))
      .mockImplementationOnce(() => pending.promise)
    const answer = mockSdkService(
      RecallPromptController,
      "answer",
      makeMe.anAnsweredQuestion.please()
    )
    const grade = mockSdkService(
      MemoryTrackerController,
      "markAsRecalled",
      makeMe.aMemoryTracker.please()
    )
    const wrapper = await ctx.mountPage()
    await retry(wrapper).trigger("click")
    await flushPromises()
    expect(retry(wrapper).text()).toBe("Retry")
    const control = retry(wrapper).element as HTMLButtonElement
    control.click()
    control.click()
    await flushPromises()
    expect(wrapper.find("[data-app-busy]").exists()).toBe(true)
    expect(getPrompt).toHaveBeenCalledTimes(3)
    expect(getPrompt.mock.calls.map(([options]) => options)).toEqual(
      Array(3).fill({ path: { memoryTracker: 1 } })
    )
    expect(progress(wrapper)).toContain("0/2")
    pending.resolve(wrapSdkResponse(prompt(1)))
    await flushPromises()
    expect(wrapper.text()).toContain("Question 1")
    expect(progress(wrapper)).toContain("0/2")
    expect(answer).not.toHaveBeenCalled()
    expect(grade).not.toHaveBeenCalled()
  })

  it.each([true, false])(
    "keeps navigation and a different question intact after late retry success=%s",
    async (success) => {
      const pending = deferredRecallPromptResponse()
      mockSdkService(MemoryTrackerController, "getRecallPrompt", prompt(2))
        .mockResolvedValueOnce(wrapSdkError("unavailable"))
        .mockImplementationOnce(() => pending.promise)
      const wrapper = await ctx.mountPage()
      await retry(wrapper).trigger("click")
      await flushPromises()
      await moveCurrentRecallToEnd(wrapper)
      expect(wrapper.text()).toContain("Question 2")
      pending.resolve(
        success ? wrapSdkResponse(prompt(1)) : wrapSdkError("retry failed")
      )
      await flushPromises()
      expect(wrapper.text()).toContain("Question 2")
      expect(progress(wrapper)).toContain("0/2")
      await moveCurrentRecallToEnd(wrapper)
      expect(
        success
          ? wrapper.text().includes("Question 1")
          : retry(wrapper).exists()
      ).toBe(true)
    }
  )

  it("keeps a failed future prefetch local and offers recovery when reached", async () => {
    const pending = deferredRecallPromptResponse()
    const getPrompt = mockSdkService(
      MemoryTrackerController,
      "getRecallPrompt",
      prompt(2)
    )
      .mockResolvedValueOnce(wrapSdkResponse(prompt(1)))
      .mockImplementationOnce(() => pending.promise)
    ctx.renderer.withProps({ eagerFetchCount: 2 })
    const wrapper = await ctx.mountPage()
    expect(wrapper.text()).toContain("Question 1")
    pending.resolve(wrapSdkError("prefetch failed"))
    await flushPromises()
    expect(wrapper.text()).toContain("Question 1")
    expect(
      wrapper.find("li.choice button").attributes("disabled")
    ).toBeUndefined()
    await moveCurrentRecallToEnd(wrapper)
    expect(retry(wrapper).text()).toBe("Retry")
    await retry(wrapper).trigger("click")
    await flushPromises()
    expect(wrapper.text()).toContain("Question 2")
    expect(getPrompt.mock.calls[2]![0]).toEqual({ path: { memoryTracker: 2 } })
    expect(progress(wrapper)).toContain("0/2")
  })
})
