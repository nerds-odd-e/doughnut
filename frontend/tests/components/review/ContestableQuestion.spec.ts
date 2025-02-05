import ContestableQuestion from "@/components/review/ContestableQuestion.vue"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, it, vi, expect } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import type { RecallPrompt } from "@/generated/backend"

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

    it("disables the component during contest", async () => {
      let resolveContest: (value: { rejected: boolean; reason: string }) => void
      const contestPromise = new Promise<{ rejected: boolean; reason: string }>(
        (resolve) => {
          resolveContest = resolve
        }
      )
      let resolveRegenerate: (value: RecallPrompt) => void
      const regeneratePromise = new Promise<RecallPrompt>((resolve) => {
        resolveRegenerate = resolve
      })
      mockedContestCall.mockReturnValue(contestPromise)
      mockedRegenerateCall.mockReturnValue(regeneratePromise)

      const wrapper = await mountComponent()
      const contestPromiseStarted = wrapper.find("#try-again").trigger("click")

      await flushPromises()
      const questionContainer = wrapper.find(".daisy-flex-col")
      expect(questionContainer.classes()).toContain("daisy-opacity-50")
      expect(questionContainer.classes()).toContain("daisy-pointer-events-none")

      resolveContest!({ rejected: false, reason: "Bad question" })
      await contestPromiseStarted
      await flushPromises()

      const newQuestion = makeMe.aRecallPrompt.please()
      resolveRegenerate!(newQuestion)
      await flushPromises()
      await wrapper.vm.$nextTick()
      await flushPromises()

      const updatedQuestionContainer = wrapper.find(".daisy-flex-col")
      expect(updatedQuestionContainer.classes()).not.toContain(
        "daisy-opacity-50"
      )
      expect(updatedQuestionContainer.classes()).not.toContain(
        "daisy-pointer-events-none"
      )
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
