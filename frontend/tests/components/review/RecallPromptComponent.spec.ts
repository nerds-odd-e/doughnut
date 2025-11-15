import RecallPromptComponent from "@/components/review/RecallPromptComponent.vue"
import { flushPromises } from "@vue/test-utils"
import helper from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"

describe("RecallPromptComponent", () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.spyOn(
      helper.managedApi.services,
      "answerQuiz"
    ).mockResolvedValue({} as never)
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  const mountComponent = () => {
    const recallPrompt = makeMe.aRecallPrompt
      .withQuestionStem("Test question")
      .withChoices(["A", "B", "C"])
      .please()

    return helper
      .component(RecallPromptComponent)
      .withProps({ recallPrompt })
      .mount()
  }

  describe("answer submission", () => {
    it("shows loading state while submitting answer", async () => {
      // Setup API to delay response
      vi.spyOn(
        helper.managedApi.services,
        "answerQuiz"
      ).mockImplementation(
        () => new Promise((resolve) => setTimeout(resolve, 100)) as never
      )

      const wrapper = mountComponent()

      // Submit an answer
      await wrapper
        .findComponent({ name: "QuestionDisplay" })
        .vm.$emit("answer", { choiceIndex: 0 })

      // Verify loading overlay is shown
      expect(wrapper.find(".daisy-absolute.daisy-inset-0").exists()).toBe(true)
      expect(
        wrapper.find(".daisy-loading.daisy-loading-spinner").exists()
      ).toBe(true)

      vi.runAllTimers()
      await flushPromises()

      // Verify loading state is removed after response
      expect(wrapper.find(".daisy-absolute.daisy-inset-0").exists()).toBe(false)
    })

    it("allows retrying on API error", async () => {
      // Setup API to fail first time
      vi.spyOn(
        helper.managedApi.services,
        "answerQuiz"
      )
        .mockRejectedValueOnce(new Error("API Error"))
        .mockResolvedValueOnce({ correct: true } as never)

      const wrapper = mountComponent()

      // Submit first answer (will fail)
      await wrapper
        .findComponent({ name: "QuestionDisplay" })
        .vm.$emit("answer", { choiceIndex: 0 })
      await flushPromises()

      // Verify error state
      expect(wrapper.find(".daisy-alert.daisy-alert-error").exists()).toBe(true)

      // Submit second answer (should succeed)
      await wrapper
        .findComponent({ name: "QuestionDisplay" })
        .vm.$emit("answer", { choiceIndex: 1 })
      await flushPromises()

      // Verify success
      expect(wrapper.emitted().answered).toBeTruthy()
      expect(wrapper.find(".daisy-alert.daisy-alert-error").exists()).toBe(
        false
      )
    })

    it("emits answered event on successful submission", async () => {
      const answerResult = { correct: true }
      vi.spyOn(
        helper.managedApi.services,
        "answerQuiz"
      ).mockResolvedValue(answerResult as never)

      const wrapper = mountComponent()

      await wrapper
        .findComponent({ name: "QuestionDisplay" })
        .vm.$emit("answer", { choiceIndex: 0 })
      await flushPromises()

      const emitted = wrapper.emitted()
      expect(emitted.answered).toBeTruthy()
      expect(emitted.answered![0]).toEqual([answerResult])
    })
  })
})
