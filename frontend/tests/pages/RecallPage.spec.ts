import { useRecallData } from "@/composables/useRecallData"
import type { AnsweredQuestion } from "@generated/doughnut-backend-api"
import makeMe from "doughnut-test-fixtures/makeMe"
import { wrapSdkResponse } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it, vi } from "vitest"
import {
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

describe("repeat page loading", () => {
  const ctx = useRecallPageSpecContext()

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
      .withPredefinedQuestion(makeMe.aPredefinedQuestion.please())
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
})
