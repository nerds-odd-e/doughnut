import ContestableQuestion from "@/components/review/ContestableQuestion.vue"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, it, vi, expect } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"

describe("ContestableQuestion", () => {
  const recallPrompt = makeMe.aRecallPrompt.please()
  const mockedContestCall = vi.fn()
  const mockedRegenerateCall = vi.fn()

  beforeEach(() => {
    vi.resetAllMocks()
    helper.managedApi.restRecallPromptController.contest = mockedContestCall
    helper.managedApi.restRecallPromptController.regenerate =
      mockedRegenerateCall
  })

  const mountComponent = async () => {
    const wrapper = helper
      .component(ContestableQuestion)
      .withRouter()
      .withStorageProps({
        recallPrompt,
      })
      .mount()
    await flushPromises()
    return wrapper
  }

  it("renders the recall prompt", async () => {
    const wrapper = await mountComponent()
    expect(
      wrapper.findComponent({ name: "RecallPromptComponent" }).exists()
    ).toBe(true)
  })

  it("shows contest button", async () => {
    const wrapper = await mountComponent()
    expect(wrapper.find("#try-again").exists()).toBe(true)
  })

  describe("when contesting a question", () => {
    it("calls contest API when contest button is clicked", async () => {
      mockedContestCall.mockResolvedValue({
        rejected: false,
        reason: "Bad question",
      })
      mockedRegenerateCall.mockResolvedValue(makeMe.aRecallPrompt.please())

      const wrapper = await mountComponent()
      await wrapper.find("#try-again").trigger("click")

      expect(mockedContestCall).toHaveBeenCalledWith(recallPrompt.id)
      expect(mockedRegenerateCall).toHaveBeenCalledWith(recallPrompt.id)
    })

    it("shows error message when contest is rejected", async () => {
      const rejectReason = "Question is legitimate"
      mockedContestCall.mockResolvedValue({
        rejected: true,
        reason: rejectReason,
      })

      const wrapper = await mountComponent()
      await wrapper.find("#try-again").trigger("click")

      expect(wrapper.text()).toContain(rejectReason)
    })
  })

  it("emits answered event when question is answered", async () => {
    const wrapper = await mountComponent()
    const answeredQuestion = makeMe.anAnsweredQuestion.please()

    await wrapper
      .findComponent({ name: "RecallPromptComponent" })
      .vm.$emit("answered", answeredQuestion)

    const emitted = wrapper.emitted()
    expect(emitted.answered).toBeTruthy()
    expect(emitted.answered![0]).toEqual([answeredQuestion])
  })
})
