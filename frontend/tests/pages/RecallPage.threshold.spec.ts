import { MemoryTrackerController } from "@generated/doughnut-backend-api/sdk.gen"
import { useRecallData } from "@/composables/useRecallData"
import type { AnsweredQuestion } from "@generated/doughnut-backend-api"
import makeMe from "doughnut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import usePopups from "@/components/commons/Popups/usePopups"
import { flushPromises } from "@vue/test-utils"
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

describe("RecallPage re-assimilation threshold", () => {
  const memoryTrackerId = 123
  const ctx = useRecallPageSpecContext({ fakeTimers: true })
  let getThresholdExceededSpy: ReturnType<typeof mockSdkService>
  let confirmMock: ReturnType<typeof vi.fn<(msg: string) => Promise<boolean>>>

  beforeEach(() => {
    confirmMock = vi.fn<(msg: string) => Promise<boolean>>()
    vi.mocked(usePopups).mockReturnValue({
      popups: {
        options: vi.fn().mockResolvedValue(null),
        alert: vi.fn(),
        confirm: confirmMock,
        done: vi.fn(),
        register: vi.fn(),
        peek: vi.fn(),
      },
    })
    mockSdkService(
      MemoryTrackerController,
      "showMemoryTracker",
      makeMe.aMemoryTracker.please()
    )
    mockSdkService(
      MemoryTrackerController,
      "askAQuestion",
      makeMe.aRecallQuestion.please()
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

  it("should NOT call getThresholdExceeded when answer is correct", async () => {
    const correctAnswerResult: AnsweredQuestion = makeMe.anAnsweredQuestion
      .withNote(makeMe.aNote.please())
      .withPredefinedQuestion(makeMe.aPredefinedQuestion.please())
      .withAnswer({ id: 1, correct: true, choiceIndex: 0 })
      .withMemoryTrackerId(memoryTrackerId)
      .please()
    const wrapper = await ctx.mountPage()
    wrapper
      .findComponent({ name: "Quiz" })
      .vm.$emit("answered", correctAnswerResult)
    await flushPromises()
    expect(getThresholdExceededSpy).not.toHaveBeenCalled()
  })

  it("should call getThresholdExceeded when answer is wrong", async () => {
    const wrongAnswerResult: AnsweredQuestion = makeMe.anAnsweredQuestion
      .withNote(makeMe.aNote.please())
      .withPredefinedQuestion(makeMe.aPredefinedQuestion.please())
      .withAnswer({ id: 1, correct: false, choiceIndex: 1 })
      .withMemoryTrackerId(memoryTrackerId)
      .please()
    const wrapper = await ctx.mountPage()
    wrapper
      .findComponent({ name: "Quiz" })
      .vm.$emit("answered", wrongAnswerResult)
    await flushPromises()
    expect(getThresholdExceededSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { memoryTracker: memoryTrackerId },
      })
    )
  })

  it("should offer property-aware re-assimilation when threshold exceeded", async () => {
    getThresholdExceededSpy.mockResolvedValue(
      wrapSdkResponse({ thresholdExceeded: true })
    )
    confirmMock.mockResolvedValueOnce(false)
    const wrongAnswerResult: AnsweredQuestion = makeMe.anAnsweredQuestion
      .withNote(makeMe.aNote.please())
      .withPredefinedQuestion(makeMe.aPredefinedQuestion.please())
      .withAnswer({ id: 1, correct: false, choiceIndex: 1 })
      .withMemoryTrackerId(memoryTrackerId)
      .withPropertyKey("topic")
      .please()
    const wrapper = await ctx.mountPage()
    wrapper
      .findComponent({ name: "Quiz" })
      .vm.$emit("answered", wrongAnswerResult)
    await flushPromises()
    expect(confirmMock).toHaveBeenCalledWith(
      'You have answered the "topic" property incorrectly too many times. Would you like to re-assimilate it?'
    )
  })
})
