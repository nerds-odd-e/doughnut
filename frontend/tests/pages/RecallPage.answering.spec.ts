import {
  MemoryTrackerController,
  RecallPromptController,
  RecallsController,
} from "@generated/donut-backend-api/sdk.gen"
import { useRecallData } from "@/composables/useRecallData"
import type { AnsweredQuestion, AnswerData } from "@generated/donut-backend-api"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import usePopups from "@/components/commons/Popups/usePopups"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { beforeEach, describe, expect, it, vi } from "vitest"
import {
  createMemoryTrackerLite,
  createUseRecallDataMock,
  useRecallPageSpecContext,
} from "./recallPageTestSupport"

vi.mock("@/composables/useRecallData")
vi.mock("@/components/commons/Popups/usePopups")

vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRoute: () => ({ path: "/", fullPath: "/" }),
    useRouter: () => ({ currentRoute: { value: { name: "recall" } } }),
  }
})

const memoryTrackerId = 123
const ctx = useRecallPageSpecContext({ fakeTimers: true })
let getThresholdExceededSpy: ReturnType<typeof mockSdkService>

beforeEach(() => {
  mockSdkService(
    MemoryTrackerController,
    "showMemoryTracker",
    makeMe.aMemoryTracker.please()
  )
  mockSdkService(
    MemoryTrackerController,
    "getRecallPrompt",
    makeMe.aRecallPrompt.please()
  )
  getThresholdExceededSpy = mockSdkService(
    MemoryTrackerController,
    "getThresholdExceeded",
    { thresholdExceeded: false }
  )
  vi.mocked(useRecallData).mockReturnValue(
    createUseRecallDataMock({
      toRepeat: [createMemoryTrackerLite(memoryTrackerId)],
    })
  )
})

const answeredMcq = (correct: boolean) =>
  makeMe.anAnsweredQuestion
    .withNote(makeMe.aNote.please())
    .withMcq(makeMe.anMcq.please())
    .withAnswer({ id: 1, correct, choiceIndex: correct ? 0 : 1 })
    .withMemoryTrackerId(memoryTrackerId)

const answer = async (wrapper: VueWrapper, answered: AnsweredQuestion) => {
  wrapper
    .findComponent({ name: "RecallPromptCard" })
    .vm.$emit("answered", answered)
  await flushPromises()
}

describe("RecallPage frequent failure warning", () => {
  let alertMock: ReturnType<typeof vi.fn<(msg: string) => Promise<boolean>>>

  beforeEach(() => {
    alertMock = vi
      .fn<(msg: string) => Promise<boolean>>()
      .mockResolvedValue(true)
    vi.mocked(usePopups).mockReturnValue({
      popups: {
        options: vi.fn().mockResolvedValue(null),
        alert: alertMock,
        confirm: vi.fn(),
        done: vi.fn(),
        register: vi.fn(),
        peek: vi.fn(),
      },
    })
  })

  const thresholdExceeded = (wrongCount: number) =>
    getThresholdExceededSpy.mockResolvedValue(
      wrapSdkResponse({ thresholdExceeded: true, wrongCount, periodDays: 14 })
    )

  it("should NOT call getThresholdExceeded when answer is correct", async () => {
    await answer(await ctx.mountPage(), answeredMcq(true).please())
    expect(getThresholdExceededSpy).not.toHaveBeenCalled()
  })

  it("should call getThresholdExceeded when answer is wrong", async () => {
    await answer(await ctx.mountPage(), answeredMcq(false).please())
    expect(getThresholdExceededSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { memoryTracker: memoryTrackerId },
      })
    )
  })

  it("should show note-level frequent failure warning when threshold exceeded", async () => {
    thresholdExceeded(5)
    await answer(await ctx.mountPage(), answeredMcq(false).please())
    expect(alertMock).toHaveBeenCalledWith(
      "You've answered incorrectly 5 times within the last 14 days."
    )
  })

  it("should show property-aware frequent failure warning when threshold exceeded", async () => {
    thresholdExceeded(7)
    await answer(
      await ctx.mountPage(),
      answeredMcq(false).withPropertyKey("topic").please()
    )
    expect(alertMock).toHaveBeenCalledWith(
      'You\'ve answered the "topic" property incorrectly 7 times within the last 14 days.'
    )
  })
})

describe("RecallPage speaking practice input", () => {
  const speakingPracticeInput = '[data-testid="speaking-practice-input"]'

  it("shows speaking practice input under answered MCQ, not during quiz", async () => {
    const wrapper = await ctx.mountPage()
    expect(wrapper.find(speakingPracticeInput).exists()).toBe(false)

    await answer(wrapper, answeredMcq(false).please())

    expect(wrapper.find(speakingPracticeInput).exists()).toBe(true)
  })
})

describe("thinking time while viewing a previously answered question", () => {
  let performanceNowSpy: ReturnType<typeof vi.spyOn>
  let rafCallbacks: Array<FrameRequestCallback>

  beforeEach(() => {
    rafCallbacks = []
    performanceNowSpy = vi.spyOn(performance, "now").mockReturnValue(0)
    vi.spyOn(window, "requestAnimationFrame").mockImplementation(
      (callback: FrameRequestCallback) => {
        rafCallbacks.push(callback)
        return 1
      }
    )
  })

  const flushRAF = () => {
    const callbacks = [...rafCallbacks]
    rafCallbacks = []
    callbacks.forEach((cb) => cb(performance.now()))
  }

  const setTime = (ms: number) => {
    performanceNowSpy.mockReturnValue(ms)
    vi.advanceTimersByTime(ms)
  }

  it("excludes time spent viewing the last answered question from the current question's thinking time", async () => {
    const mockData = createUseRecallDataMock({
      toRepeat: [createMemoryTrackerLite(1, false)],
    })
    vi.mocked(useRecallData).mockReturnValue(mockData)
    ctx.previouslyAnsweredSpy.mockResolvedValueOnce(
      wrapSdkResponse([
        makeMe.anAnsweredQuestion.withId(1).withMemoryTrackerId(99).please(),
      ])
    )
    mockSdkService(
      RecallsController,
      "recalling",
      makeMe.aDueMemoryTrackersList
        .toRepeat([createMemoryTrackerLite(1, false)])
        .please()
    )
    mockSdkService(
      MemoryTrackerController,
      "getRecallPrompt",
      makeMe.aRecallPrompt.withChoices(["A", "B", "C"]).please()
    )
    const answerSpy = mockSdkService(
      RecallPromptController,
      "answer",
      makeMe.anAnsweredQuestion.please()
    )

    const wrapper = await ctx.mountPage()
    await wrapper.vm.$nextTick()
    flushRAF()

    setTime(2000)

    await wrapper
      .find('button[title="view last answered question"]')
      .trigger("click")
    await flushPromises()

    setTime(7000)

    // Return to the current question the same way the app does: via resumeRecall.
    mockData.shouldResumeRecall.value = true
    await flushPromises()

    setTime(7500)

    await wrapper.find("li.choice button").trigger("click")
    await flushPromises()

    const [firstCall] = answerSpy.mock.calls
    if (firstCall === undefined) throw new Error("answer was not called")
    const sentThinkingTimeMs = (firstCall[0] as AnswerData).body.thinkingTimeMs
    expect(sentThinkingTimeMs).toBeGreaterThanOrEqual(2000)
    expect(sentThinkingTimeMs).toBeLessThan(3000)
  })
})
