import { useRecallData } from "@/composables/useRecallData"
import { DailyProbeController } from "@generated/donut-backend-api/sdk.gen"
import type { AnsweredQuestion } from "@generated/donut-backend-api"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkError, wrapSdkResponse } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it, vi } from "vitest"
import { ref } from "vue"
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

const ctx = useRecallPageSpecContext()

describe("repeat page loading", () => {
  it("should call previouslyAnswered on mount", async () => {
    await ctx.mountPage()
    expect(ctx.previouslyAnsweredSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        query: { timezone: "Asia/Shanghai" },
      })
    )
  })

  it("should prepend previously answered recall prompts to the list", async () => {
    const note = makeMe.aNote.please()
    const previousQuestionResult: AnsweredQuestion = makeMe.anAnsweredQuestion
      .withId(1)
      .withNote(note)
      .withMcq(makeMe.anMcq.please())
      .withAnswer({ id: 1, correct: true, choiceIndex: 0 })
      .withMemoryTrackerId(1)
      .please()
    ctx.previouslyAnsweredSpy.mockResolvedValueOnce(
      wrapSdkResponse([previousQuestionResult])
    )

    const wrapper = await ctx.mountPage()
    expect(wrapper.findComponent({ name: "GlobalBar" }).text()).toContain("1/")
  })

  it("calls recalling when dueRecallsRefreshNonce increments", async () => {
    const mockData = createUseRecallDataMock({ toRepeat: undefined })
    vi.mocked(useRecallData).mockReturnValue(mockData)
    await ctx.mountPage()
    ctx.recallingSpy.mockClear()
    mockData.dueRecallsRefreshNonce.value += 1
    await flushPromises()
    expect(ctx.recallingSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        query: expect.objectContaining({
          timezone: "Asia/Shanghai",
          dueindays: 0,
        }),
      })
    )
  })

  it("redirect to recall page if nothing to repeat", async () => {
    const repetition = makeMe.aDueMemoryTrackersList.please()
    vi.mocked(useRecallData).mockReturnValue(
      createUseRecallDataMock({ toRepeat: repetition.toRepeat })
    )
    await ctx.mountPage()
    expect(ctx.recallingSpy).not.toHaveBeenCalled()
  })

  it("shows learning session actions when useRecallData has potential sessions", async () => {
    vi.mocked(useRecallData).mockReturnValue(
      createUseRecallDataMock({
        toRepeat: [],
        potentialLearningSessions: [
          {
            notebookId: 10,
            notebookName: "Spanish conversation",
          },
        ],
      })
    )
    const wrapper = await ctx.mountPage()
    expect(
      wrapper.find('[data-test="learning-session-actions"]').exists()
    ).toBe(true)
  })
})

describe("RecallPage Daily probe entry", () => {
  const mountRecall = async (dailyProbeEnabled: boolean) => {
    ctx.renderer.withCurrentUserRef(
      ref(makeMe.aUser.dailyProbeEnabled(dailyProbeEnabled).please())
    )
    vi.mocked(useRecallData).mockReturnValue(
      createUseRecallDataMock({
        toRepeat: [createMemoryTrackerLite(1)],
      })
    )
    return ctx.mountPage()
  }

  const showsDailyProbe = (wrapper: Awaited<ReturnType<typeof mountRecall>>) =>
    wrapper.findComponent({ name: "DailyProbe" }).exists()
  const showsRecallPrompt = (
    wrapper: Awaited<ReturnType<typeof mountRecall>>
  ) => wrapper.findComponent({ name: "RecallPromptCard" }).exists()

  it("shows Daily probe instead of the quiz when the learner has opted in", async () => {
    const wrapper = await mountRecall(true)

    expect(showsDailyProbe(wrapper)).toBe(true)
    expect(showsRecallPrompt(wrapper)).toBe(false)
  })

  it("loads ordinary recall when Daily probe is off", async () => {
    const wrapper = await mountRecall(false)

    expect(showsDailyProbe(wrapper)).toBe(false)
    expect(showsRecallPrompt(wrapper)).toBe(true)
  })

  it("skips Daily probe when today's run is already completed", async () => {
    mockSdkService(DailyProbeController, "getDailyProbeToday", {
      completed: true,
    })
    const wrapper = await mountRecall(true)

    expect(showsDailyProbe(wrapper)).toBe(false)
    expect(showsRecallPrompt(wrapper)).toBe(true)
  })

  it("shows retry when today's Daily probe check fails", async () => {
    mockSdkService(DailyProbeController, "getDailyProbeToday", {
      completed: false,
    }).mockResolvedValue(wrapSdkError("unavailable"))
    const wrapper = await mountRecall(true)

    expect(
      wrapper.find('[data-testid="daily-probe-offer-retry"]').exists()
    ).toBe(true)
    expect(showsDailyProbe(wrapper)).toBe(false)
    expect(showsRecallPrompt(wrapper)).toBe(false)
  })

  it("offers Daily probe after retry succeeds with today's run still due", async () => {
    const getToday = mockSdkService(
      DailyProbeController,
      "getDailyProbeToday",
      {
        completed: false,
      }
    )
    getToday.mockResolvedValueOnce(wrapSdkError("unavailable"))
    const wrapper = await mountRecall(true)
    await wrapper
      .find('[data-testid="daily-probe-offer-retry"]')
      .trigger("click")
    await flushPromises()

    expect(getToday).toHaveBeenCalledTimes(2)
    expect(showsDailyProbe(wrapper)).toBe(true)
  })
})
