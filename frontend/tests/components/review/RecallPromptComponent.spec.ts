import RecallPromptComponent from "@/components/review/RecallPromptComponent.vue"
import { flushPromises } from "@vue/test-utils"
import helper, {
  mockSdkService,
  wrapSdkResponse,
  wrapSdkError,
} from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"

describe("RecallPromptComponent", () => {
  beforeEach(() => {
    vi.useFakeTimers()
    mockSdkService("answerQuiz", makeMe.anAnsweredQuestion.please())
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  const mountComponent = () => {
    const predefinedQuestion = makeMe.aPredefinedQuestion
      .withQuestionStem("Test question")
      .withChoices(["A", "B", "C"])
      .please()

    return helper
      .component(RecallPromptComponent)
      .withProps({ predefinedQuestion })
      .mount()
  }

  describe("answer submission", () => {
    it("shows loading state while submitting answer", async () => {
      // Setup API to delay response
      const answerQuizSpy = mockSdkService(
        "answerQuiz",
        makeMe.anAnsweredQuestion.please()
      )
      answerQuizSpy.mockImplementation(
        () =>
          new Promise(
            (resolve) =>
              setTimeout(
                () =>
                  resolve(wrapSdkResponse(makeMe.anAnsweredQuestion.please())),
                100
              )
            // biome-ignore lint/suspicious/noExplicitAny: Promise type requires any for mock implementation
          ) as any
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

      // Verify loading spinner is removed after response
      expect(
        wrapper.find(".daisy-loading.daisy-loading-spinner").exists()
      ).toBe(false)
      // The gray overlay should remain visible after answering (isAnswered stays true)
      expect(wrapper.find(".daisy-absolute.daisy-inset-0").exists()).toBe(true)
    })

    it("allows retrying on API error", async () => {
      // Setup API to fail first time
      const answerQuizSpy = mockSdkService(
        "answerQuiz",
        makeMe.anAnsweredQuestion.answerCorrect(true).please()
      )
      answerQuizSpy
        .mockResolvedValueOnce(wrapSdkError("API Error"))
        .mockResolvedValueOnce(
          wrapSdkResponse(
            makeMe.anAnsweredQuestion.answerCorrect(true).please()
          )
        )

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
      const answerResult = makeMe.anAnsweredQuestion
        .answerCorrect(true)
        .please()
      mockSdkService("answerQuiz", answerResult)

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
