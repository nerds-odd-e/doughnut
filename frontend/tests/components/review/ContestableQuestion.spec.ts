import ContestableQuestion from "@/components/review/ContestableQuestion.vue"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, it, vi, expect } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import type { RecallPrompt } from "@/generated/backend"

describe("ContestableQuestion", () => {
  const recallPrompt = makeMe.aRecallPrompt
    .withQuestionStem("Test question")
    .withChoices(["A", "B", "C"])
    .please()
  const mockedContestCall = vi.fn()
  const mockedRegenerateCall = vi.fn()

  beforeEach(() => {
    vi.resetAllMocks()
    helper.managedApi.restRecallPromptController.contest = mockedContestCall
    helper.managedApi.restRecallPromptController.regenerate =
      mockedRegenerateCall
  })

  const mountComponent = async (customRecallPrompt = recallPrompt) => {
    const wrapper = helper
      .component(ContestableQuestion)
      .withRouter()
      .withStorageProps({
        recallPrompt: customRecallPrompt,
      })
      .mount()
    await flushPromises()
    return wrapper
  }

  it("renders the recall prompt component for multiple choice questions", async () => {
    const wrapper = await mountComponent()
    expect(
      wrapper.findComponent({ name: "RecallPromptComponent" }).exists()
    ).toBe(true)
    expect(
      wrapper.findComponent({ name: "SpellingQuestionDisplay" }).exists()
    ).toBe(false)
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
      expect(mockedRegenerateCall).toHaveBeenCalledWith(recallPrompt.id, {
        rejected: false,
        reason: "Bad question",
      })
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

  describe("spelling questions", () => {
    it("shows spelling question input when question has no choices", async () => {
      const recallPromptWithoutChoices = makeMe.aRecallPrompt
        .withQuestionStem("Spell the word 'cat'")
        .please()

      const wrapper = helper
        .component(ContestableQuestion)
        .withRouter()
        .withStorageProps({
          recallPrompt: recallPromptWithoutChoices,
        })
        .mount()
      await flushPromises()

      expect(
        wrapper.findComponent({ name: "SpellingQuestionDisplay" }).exists()
      ).toBe(true)
      expect(
        wrapper.findComponent({ name: "RecallPromptComponent" }).exists()
      ).toBe(false)
    })

    it("submits spelling answer correctly", async () => {
      const recallPromptWithoutChoices = makeMe.aRecallPrompt
        .withQuestionStem("Spell the word 'cat'")
        .please()

      const answerResult = makeMe.anAnsweredQuestion
        .answerCorrect(true)
        .please()
      helper.managedApi.restRecallPromptController.answerSpelling = vi
        .fn()
        .mockResolvedValue(answerResult)

      const wrapper = helper
        .component(ContestableQuestion)
        .withRouter()
        .withStorageProps({
          recallPrompt: recallPromptWithoutChoices,
        })
        .mount()
      await flushPromises()

      await wrapper
        .findComponent({ name: "SpellingQuestionDisplay" })
        .vm.$emit("answer", { spellingAnswer: "cat" })
      await flushPromises()

      expect(
        helper.managedApi.restRecallPromptController.answerSpelling
      ).toHaveBeenCalledWith(recallPromptWithoutChoices.id, {
        spellingAnswer: "cat",
      })

      const emitted = wrapper.emitted()
      expect(emitted.answered).toBeTruthy()
      expect(emitted.answered![0]).toEqual([answerResult])
    })
  })

  it("emits answered event when question is answered", async () => {
    const wrapper = await mountComponent()
    const answeredQuestion = makeMe.anAnsweredQuestion
      .withChoiceIndex(0)
      .answerCorrect(true)
      .please()

    await wrapper
      .findComponent({ name: "RecallPromptComponent" })
      .vm.$emit("answered", answeredQuestion)

    const emitted = wrapper.emitted()
    expect(emitted.answered).toBeTruthy()
    expect(emitted.answered![0]).toEqual([answeredQuestion])
  })
})
