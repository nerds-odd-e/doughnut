import {
  MemoryTrackerController,
  RecallPromptController,
  RecallsController,
} from "@generated/donut-backend-api/sdk.gen"
import { useRecallData } from "@/composables/useRecallData"
import type { AnsweredQuestion, AnswerData } from "@generated/donut-backend-api"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
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

describe("thinking time while viewing a previously answered question", () => {
  const ctx = useRecallPageSpecContext({ fakeTimers: true })
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

  afterEach(() => {
    vi.restoreAllMocks()
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

  const setupMemoryTrackerAndPreviousAnswer = () => {
    const mockData = createUseRecallDataMock({
      toRepeat: [createMemoryTrackerLite(1, false)],
    })
    vi.mocked(useRecallData).mockReturnValue(mockData)
    const previousQuestion: AnsweredQuestion = makeMe.anAnsweredQuestion
      .withId(1)
      .withMemoryTrackerId(99)
      .please()
    ctx.previouslyAnsweredSpy.mockResolvedValueOnce(
      wrapSdkResponse([previousQuestion])
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
    return mockData
  }

  it("excludes time spent viewing the last answered question from the current question's thinking time", async () => {
    const mockData = setupMemoryTrackerAndPreviousAnswer()
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
