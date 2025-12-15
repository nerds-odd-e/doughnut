import { describe, expect, it, beforeEach, afterEach, vi } from "vitest"
import { flushPromises } from "@vue/test-utils"
import ContestableQuestion from "@/components/recall/ContestableQuestion.vue"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import type { AnswerDto } from "@generated/backend"

describe("ContestableQuestion.vue", () => {
  let performanceNowSpy: ReturnType<typeof vi.spyOn>
  let rafCallbacks: Array<FrameRequestCallback> = []

  beforeEach(() => {
    vi.useFakeTimers()
    performanceNowSpy = vi.spyOn(performance, "now").mockReturnValue(0)
    rafCallbacks = []
    global.requestAnimationFrame = vi.fn((callback: FrameRequestCallback) => {
      rafCallbacks.push(callback)
      return 1
    }) as unknown as typeof requestAnimationFrame
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  const flushRAF = () => {
    const callbacks = [...rafCallbacks]
    rafCallbacks = []
    callbacks.forEach((cb) => cb(performance.now()))
  }

  const mountComponent = (recallPrompt = makeMe.aRecallPrompt.please()) => {
    return helper
      .component(ContestableQuestion)
      .withRouter()
      .withCleanStorage()
      .withProps({
        recallPrompt,
      })
      .mount()
  }

  it("renders recall prompt for multiple choice questions", () => {
    const wrapper = mountComponent()
    expect(
      wrapper.findComponent({ name: "RecallPromptComponent" }).exists()
    ).toBe(true)
  })

  it("emits answered event when question is answered", async () => {
    const wrapper = mountComponent()
    const answerResult = makeMe.anAnsweredQuestion.answerCorrect(true).please()

    await wrapper
      .findComponent({ name: "RecallPromptComponent" })
      .vm.$emit("answered", answerResult)

    const emitted = wrapper.emitted()
    expect(emitted.answered).toBeTruthy()
    expect(emitted.answered![0]).toEqual([answerResult])
  })

  it("resets thinking time when question is contested", async () => {
    const initialQuestion = makeMe.aRecallPrompt
      .withQuestionStem("First question")
      .withChoices(["A", "B", "C"])
      .please()

    const regeneratedQuestion = makeMe.aRecallPrompt
      .withQuestionStem("Second question")
      .withChoices(["X", "Y", "Z"])
      .please()

    // Mock contest API to return a successful contest result
    const contestSpy = mockSdkService("contest", {
      rejected: false,
      advice: "Question was contested",
      isContested: true,
    })

    // Mock regenerate API to return a new question
    const regenerateSpy = mockSdkService("regenerate", regeneratedQuestion)

    // Mock answerQuiz API
    const answerQuizSpy = mockSdkService(
      "answerQuiz",
      makeMe.anAnsweredQuestion.answerCorrect(true).please()
    )

    const wrapper = mountComponent(initialQuestion)

    await flushPromises()
    flushRAF()

    // Simulate thinking about the first question for 5 seconds
    // Time: 0ms -> 5000ms
    performanceNowSpy.mockReturnValue(5000)
    vi.advanceTimersByTime(5000)

    // Contest the question (still at time 5000ms)
    const contestButton = wrapper.find("#try-again")
    await contestButton.trigger("click")
    await flushPromises()

    // Verify contest was called
    expect(contestSpy).toHaveBeenCalled()

    // Wait for regeneration to complete
    // The new QuestionDisplay should mount and start tracking from the current time (5000ms)
    await flushPromises()
    await wrapper.vm.$nextTick()
    flushRAF()

    // Verify regenerate was called
    expect(regenerateSpy).toHaveBeenCalled()

    // The new QuestionDisplay component should have mounted and started tracking
    // Since it's a new component instance, it should start tracking from the current performance.now()
    // which is 5000ms. We keep it at 5000ms to represent the moment the new question appears.
    performanceNowSpy.mockReturnValue(5000)

    // Wait for the new question to fully mount and start tracking
    await flushPromises()
    await wrapper.vm.$nextTick()
    flushRAF()

    // Simulate thinking about the new question for 3 seconds
    // Time: 5000ms -> 8000ms
    // The new QuestionDisplay started tracking at 5000ms, so thinking time should be 3000ms (8000 - 5000)
    performanceNowSpy.mockReturnValue(8000)
    vi.advanceTimersByTime(3000)

    // Find the new QuestionDisplay component after regeneration
    const recallPromptComponent = wrapper.findComponent({
      name: "RecallPromptComponent",
    })
    const questionDisplay = recallPromptComponent.findComponent({
      name: "QuestionDisplay",
    })

    // Answer the new question
    const choiceButton = questionDisplay.find("li.choice button")
    await choiceButton.trigger("click")
    await flushPromises()

    // Verify answerQuiz was called with thinking time that only includes time for the new question
    expect(answerQuizSpy).toHaveBeenCalled()
    const answerCall = answerQuizSpy.mock.calls[0]
    expect(answerCall).toBeDefined()
    const answerData = (answerCall?.[0] as { body: AnswerDto })?.body as {
      thinkingTimeMs?: number
    }

    // The thinking time should be approximately 3000ms (time spent on new question: 8000 - 5000)
    // It should NOT include the 5000ms from the first question
    // If thinking time didn't reset, it would be ~8000ms (5000 + 3000)
    expect(answerData?.thinkingTimeMs).toBeDefined()
    expect(answerData?.thinkingTimeMs).toBeGreaterThanOrEqual(3000)
    expect(answerData?.thinkingTimeMs).toBeLessThan(4000) // Should be close to 3000ms, not 8000ms
  })
})
