import RecallPromptComponent from "@/components/review/RecallPromptComponent.vue"
import { flushPromises } from "@vue/test-utils"
import helper from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"

describe("RecallPromptComponent", () => {
  const recallPrompt = makeMe.aRecallPrompt.please()

  beforeEach(() => {
    vi.useFakeTimers()
    helper.managedApi.restRecallPromptController.answerQuiz = vi.fn()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  const mountComponent = () => {
    return helper
      .component(RecallPromptComponent)
      .withProps({ recallPrompt })
      .mount()
  }

  describe("answer submission", () => {
    it("shows loading state while submitting answer", async () => {
      // Setup API to delay response
      helper.managedApi.restRecallPromptController.answerQuiz = vi
        .fn()
        .mockImplementation(
          () => new Promise((resolve) => setTimeout(resolve, 100))
        )

      const wrapper = mountComponent()

      // Submit an answer
      await wrapper
        .findComponent({ name: "QuestionDisplay" })
        .vm.$emit("answer", { choiceIndex: 0 })

      // Verify loading overlay is shown
      expect(wrapper.find(".loading-overlay").exists()).toBe(true)
      expect(wrapper.find(".loading-spinner").exists()).toBe(true)

      vi.runAllTimers()
      await flushPromises()

      // Verify loading state is removed after response
      expect(wrapper.find(".loading-overlay").exists()).toBe(false)
    })

    it("allows retrying on API error", async () => {
      // Setup API to fail first time
      helper.managedApi.restRecallPromptController.answerQuiz = vi
        .fn()
        .mockRejectedValueOnce(new Error("API Error"))
        .mockResolvedValueOnce({ correct: true })

      const wrapper = mountComponent()

      // Submit first answer (will fail)
      await wrapper
        .findComponent({ name: "QuestionDisplay" })
        .vm.$emit("answer", { choiceIndex: 0 })
      await flushPromises()

      // Verify error state
      expect(wrapper.find(".error-message").exists()).toBe(true)

      // Submit second answer (should succeed)
      await wrapper
        .findComponent({ name: "QuestionDisplay" })
        .vm.$emit("answer", { choiceIndex: 1 })
      await flushPromises()

      // Verify success
      expect(wrapper.emitted().answered).toBeTruthy()
      expect(wrapper.find(".error-message").exists()).toBe(false)
    })

    it("emits answered event on successful submission", async () => {
      const answerResult = { correct: true }
      helper.managedApi.restRecallPromptController.answerQuiz = vi
        .fn()
        .mockResolvedValue(answerResult)

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
