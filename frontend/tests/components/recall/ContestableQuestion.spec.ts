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
    vi.spyOn(window, "requestAnimationFrame").mockImplementation(
      (callback: FrameRequestCallback) => {
        rafCallbacks.push(callback)
        return 1
      }
    )
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

  const waitForUpdates = async (wrapper: ReturnType<typeof mountComponent>) => {
    await flushPromises()
    await wrapper.vm.$nextTick()
    flushRAF()
  }

  const setTimeAndAdvance = (timeMs: number, advanceMs: number) => {
    performanceNowSpy.mockReturnValue(timeMs)
    vi.advanceTimersByTime(advanceMs)
  }

  const mountComponent = (recallPrompt = makeMe.aRecallPrompt.please()) =>
    helper
      .component(ContestableQuestion)
      .withRouter()
      .withCleanStorage()
      .withProps({
        recallPrompt,
      })
      .mount()

  const findRecallPrompt = (wrapper: ReturnType<typeof mountComponent>) =>
    wrapper.findComponent({ name: "RecallPromptComponent" })

  const getAnswerThinkingTime = (spy: ReturnType<typeof mockSdkService>) => {
    const answerCall = spy.mock.calls[0]
    return (answerCall?.[0] as { body: AnswerDto })?.body?.thinkingTimeMs
  }

  it("renders recall prompt for multiple choice questions", () => {
    const wrapper = mountComponent()
    expect(findRecallPrompt(wrapper).exists()).toBe(true)
  })

  it("emits answered event when question is answered", async () => {
    const wrapper = mountComponent()
    const answerResult = makeMe.anAnsweredQuestion.answerCorrect(true).please()

    await findRecallPrompt(wrapper).vm.$emit("answered", answerResult)

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

    const contestSpy = mockSdkService("contest", {
      rejected: false,
      advice: "Question was contested",
      isContested: true,
    })

    const regenerateSpy = mockSdkService("regenerate", regeneratedQuestion)

    const answerQuizSpy = mockSdkService(
      "answerQuiz",
      makeMe.anAnsweredQuestion.answerCorrect(true).please()
    )

    const wrapper = mountComponent(initialQuestion)
    await waitForUpdates(wrapper)

    setTimeAndAdvance(5000, 5000)

    await wrapper.find("#try-again").trigger("click")
    await flushPromises()

    expect(contestSpy).toHaveBeenCalled()
    await waitForUpdates(wrapper)
    expect(regenerateSpy).toHaveBeenCalled()

    performanceNowSpy.mockReturnValue(5000)
    await waitForUpdates(wrapper)

    setTimeAndAdvance(8000, 3000)

    const questionDisplay = findRecallPrompt(wrapper).findComponent({
      name: "QuestionDisplay",
    })

    await questionDisplay.find("li.choice button").trigger("click")
    await flushPromises()

    expect(answerQuizSpy).toHaveBeenCalled()
    const thinkingTime = getAnswerThinkingTime(answerQuizSpy)
    expect(thinkingTime).toBeDefined()
    expect(thinkingTime).toBeGreaterThanOrEqual(3000)
    expect(thinkingTime).toBeLessThan(4000)
  })
})
