import {
  MemoryTrackerController,
  RecallPromptController,
} from "@generated/donut-backend-api/sdk.gen"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkError, wrapSdkResponse } from "@tests/helpers"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { focusDirective } from "@/utils/focusTarget"
import {
  captureRequestAnimationFrame,
  flushCapturedAnimationFrames,
} from "@tests/components/recall/spellingQuestionDisplayTestSupport"
import { beforeEach, describe, expect, it, vi } from "vitest"
import {
  createMemoryTrackerLite,
  deferredRecallPromptResponse,
  givenRecallQueue,
  moveCurrentRecallToEnd,
  useRecallPageSpecContext,
} from "./recallPageTestSupport"

const ctx = useRecallPageSpecContext({ fakeTimers: true })
const prompt = (id: number) =>
  makeMe.aRecallPrompt.withId(id).withSpellingStem(`Spell ${id}`).please()
const retry = (wrapper: VueWrapper) =>
  wrapper.find('[data-testid="retry-question"]')
const input = (wrapper: VueWrapper) =>
  wrapper.find("input#memory_tracker-answer")
beforeEach(() =>
  givenRecallQueue(
    createMemoryTrackerLite(1, true),
    createMemoryTrackerLite(2, true)
  )
)

describe("RecallPage spelling question recovery", () => {
  it("requires a loaded prompt for entry and submits the recovered prompt after repeated failure", async () => {
    const pending = deferredRecallPromptResponse()
    const load = mockSdkService(
      MemoryTrackerController,
      "getRecallPrompt",
      prompt(71)
    )
      .mockResolvedValueOnce(wrapSdkError("unavailable"))
      .mockResolvedValueOnce(wrapSdkError("still unavailable"))
      .mockImplementationOnce(() => pending.promise)
    const answer = mockSdkService(
      RecallPromptController,
      "answerSpelling",
      makeMe.anAnsweredQuestion.spelling().withMemoryTrackerId(1).please()
    )
    const grade = mockSdkService(
      MemoryTrackerController,
      "markAsRecalled",
      makeMe.aMemoryTracker.please()
    )
    const wrapper = await ctx.mountPage()
    expect(input(wrapper).exists()).toBe(false)
    expect(retry(wrapper).text()).toBe("Retry")
    await retry(wrapper).trigger("click")
    await flushPromises()
    expect(retry(wrapper).text()).toBe("Retry")
    const button = retry(wrapper).element as HTMLButtonElement
    button.click()
    button.click()
    await flushPromises()
    expect(wrapper.find("[data-app-busy]").exists()).toBe(true)
    expect(input(wrapper).exists()).toBe(false)
    expect(load).toHaveBeenCalledTimes(3)
    expect(wrapper.find(".progress-bar").text()).toContain("0/2")
    expect(answer).not.toHaveBeenCalled()
    expect(grade).not.toHaveBeenCalled()
    pending.resolve(wrapSdkResponse(prompt(71)))
    await flushPromises()
    expect(wrapper.text()).toContain("Spell 71")
    await input(wrapper).setValue("moon")
    await wrapper.find("form").trigger("submit")
    await flushPromises()
    expect(answer.mock.calls[0]![0]).toMatchObject({
      path: { recallPrompt: 71 },
      body: { spellingAnswer: "moon" },
    })
  })

  it("preserves typing when a future spelling prefetch fails and offers Retry when reached", async () => {
    const pending = deferredRecallPromptResponse()
    mockSdkService(MemoryTrackerController, "getRecallPrompt", prompt(2))
      .mockResolvedValueOnce(wrapSdkResponse(prompt(1)))
      .mockImplementationOnce(() => pending.promise)
    ctx.renderer.withProps({ eagerFetchCount: 2 })
    const wrapper = await ctx.mountPage()
    await input(wrapper).setValue("still typing")
    pending.resolve(wrapSdkError("prefetch failed"))
    await flushPromises()
    expect((input(wrapper).element as HTMLInputElement).value).toBe(
      "still typing"
    )
    await moveCurrentRecallToEnd(wrapper)
    expect(retry(wrapper).text()).toBe("Retry")
    await retry(wrapper).trigger("click")
    await flushPromises()
    expect(wrapper.text()).toContain("Spell 2")
  })

  it.each([true, false])(
    "keeps another spelling input intact after late Retry success=%s",
    async (success) => {
      const pending = deferredRecallPromptResponse()
      mockSdkService(MemoryTrackerController, "getRecallPrompt", prompt(2))
        .mockResolvedValueOnce(wrapSdkError("unavailable"))
        .mockImplementationOnce(() => pending.promise)
      const wrapper = await ctx.mountPage()
      await retry(wrapper).trigger("click")
      await flushPromises()
      await moveCurrentRecallToEnd(wrapper)
      await input(wrapper).setValue("other answer")
      pending.resolve(
        success ? wrapSdkResponse(prompt(1)) : wrapSdkError("still unavailable")
      )
      await flushPromises()
      expect(wrapper.text()).toContain("Spell 2")
      expect((input(wrapper).element as HTMLInputElement).value).toBe(
        "other answer"
      )
      expect(wrapper.find(".progress-bar").text()).toContain("0/2")
      await moveCurrentRecallToEnd(wrapper)
      expect(
        success ? wrapper.text().includes("Spell 1") : retry(wrapper).exists()
      ).toBe(true)
    }
  )

  it("recovers a fresh overlap attempt with feedback, focus and thinking time starting at presentation", async () => {
    const now = vi.spyOn(performance, "now").mockReturnValue(0)
    const frames = captureRequestAnimationFrame()
    const pending = deferredRecallPromptResponse()
    const load = mockSdkService(
      MemoryTrackerController,
      "getRecallPrompt",
      prompt(72)
    )
      .mockResolvedValueOnce(wrapSdkResponse(prompt(71)))
      .mockResolvedValueOnce(wrapSdkError("next attempt unavailable"))
      .mockImplementationOnce(() => pending.promise)
    const answer = mockSdkService(
      RecallPromptController,
      "answerSpelling",
      makeMe.anAnsweredQuestion.spelling().withMemoryTrackerId(1).please()
    ).mockResolvedValueOnce(
      wrapSdkResponse(
        makeMe.anAnsweredQuestion
          .overlap("Shared")
          .withMemoryTrackerId(1)
          .please()
      )
    )
    const wrapper = await ctx.mountPage({
      attachTo: document.body,
      global: { directives: { focus: focusDirective } },
    })
    const page = wrapper.element as HTMLElement
    page.style.width = "375px"
    page.style.height = "667px"
    flushCapturedAnimationFrames(frames)
    await input(wrapper).setValue("Shared")
    await wrapper.find("form").trigger("submit")
    await flushPromises()
    expect(retry(wrapper).text()).toBe("Retry")
    expect(
      wrapper.find('[data-testid="spelling-overlap-feedback"]').text()
    ).toContain("overlapped note")
    expect(input(wrapper).exists()).toBe(false)
    now.mockReturnValue(10000)
    await retry(wrapper).trigger("click")
    await flushPromises()
    now.mockReturnValue(20000)
    pending.resolve(wrapSdkResponse(prompt(72)))
    await flushPromises()
    flushCapturedAnimationFrames(frames)
    const field = input(wrapper).element as HTMLInputElement
    expect(field.value).toBe("")
    expect(document.activeElement).toBe(field)
    expect(field.getBoundingClientRect().bottom).toBeLessThanOrEqual(
      page.getBoundingClientRect().bottom
    )
    expect(
      wrapper.find('[data-testid="spelling-overlap-feedback"]').text()
    ).toContain("overlapped note")
    expect(wrapper.find(".progress-bar").text()).toContain("0/2")
    expect(load.mock.calls.slice(0, 3).map(([options]) => options)).toEqual(
      Array(3).fill({ path: { memoryTracker: 1 } })
    )
    now.mockReturnValue(23000)
    await input(wrapper).setValue("more specific")
    await wrapper.find("form").trigger("submit")
    await flushPromises()
    expect(answer.mock.calls[1]![0]).toMatchObject({
      path: { recallPrompt: 72 },
      body: { spellingAnswer: "more specific", thinkingTimeMs: 3000 },
    })
  })
})
