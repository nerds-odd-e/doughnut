import { RecallPromptController } from "@generated/doughnut-backend-api/sdk.gen"
import RecallPromptComponent from "@/components/recall/RecallPromptComponent.vue"
import { flushPromises } from "@vue/test-utils"
import helper, {
  mockSdkService,
  wrapSdkResponse,
  wrapSdkError,
} from "@tests/helpers"
import { mockCoarsePointer } from "@tests/helpers/mockCoarsePointer"
import {
  expectSoftKeyboardPrimerIsFocused,
  expectSoftKeyboardPrimerIsNotFocused,
  mountSoftKeyboardPrimer,
  softKeyboardPrimerElement,
} from "@tests/helpers/softKeyboardPrimerTestSupport"
import makeMe from "doughnut-test-fixtures/makeMe"
import { describe, it, expect, beforeEach, afterEach, vi } from "vitest"

describe("RecallPromptComponent", () => {
  let matchMediaSpy: ReturnType<typeof mockCoarsePointer> | undefined

  beforeEach(() => {
    vi.useFakeTimers()
    mockSdkService(
      RecallPromptController,
      "answerQuiz",
      makeMe.anAnsweredQuestion.please()
    )
  })

  afterEach(() => {
    matchMediaSpy?.mockRestore()
    matchMediaSpy = undefined
    Object.defineProperty(document, "hidden", { value: false, writable: true })
    vi.useRealTimers()
    document.body.innerHTML = ""
  })

  const mountComponent = (nextIsSpelling = false) => {
    const recallPrompt = makeMe.aRecallQuestion
      .withQuestionStem("Test question")
      .withChoices(["A", "B", "C"])
      .please()

    return helper
      .component(RecallPromptComponent)
      .withProps({ recallPrompt, nextIsSpelling })
      .mount({ attachTo: document.body })
  }

  describe("soft keyboard primer", () => {
    beforeEach(() => {
      mountSoftKeyboardPrimer()
    })

    it.each([
      {
        nextIsSpelling: true,
        expectPrimerFocused: true,
      },
      {
        nextIsSpelling: false,
        expectPrimerFocused: false,
      },
    ])("primer focus on choice when nextIsSpelling=$nextIsSpelling", async ({
      nextIsSpelling,
      expectPrimerFocused,
    }) => {
      matchMediaSpy = mockCoarsePointer(true)
      expect(softKeyboardPrimerElement()).toBeTruthy()

      let resolveAnswer: (value: ReturnType<typeof wrapSdkResponse>) => void
      const answerQuizSpy = mockSdkService(
        RecallPromptController,
        "answerQuiz",
        makeMe.anAnsweredQuestion.please()
      )
      if (expectPrimerFocused) {
        answerQuizSpy.mockImplementation(
          () =>
            new Promise((resolve) => {
              resolveAnswer = resolve
              // biome-ignore lint/suspicious/noExplicitAny: Promise type requires any for mock implementation
            }) as any
        )
      }

      const wrapper = mountComponent(nextIsSpelling)
      wrapper
        .findComponent({ name: "QuestionDisplay" })
        .vm.$emit("answer", { choiceIndex: 0 })

      if (expectPrimerFocused) {
        expectSoftKeyboardPrimerIsFocused()
        resolveAnswer!(wrapSdkResponse(makeMe.anAnsweredQuestion.please()))
        await flushPromises()
      } else {
        expectSoftKeyboardPrimerIsNotFocused()
      }
      wrapper.unmount()
    })
  })

  describe("answer submission", () => {
    it("shows loading state while submitting answer", async () => {
      // Setup API to delay response
      const answerQuizSpy = mockSdkService(
        RecallPromptController,
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
      expect(wrapper.find(".absolute.inset-0").exists()).toBe(true)
      expect(
        wrapper.find(".daisy-loading.daisy-loading-spinner").exists()
      ).toBe(true)

      await vi.advanceTimersByTimeAsync(100)
      await flushPromises()

      // Verify loading spinner is removed after response
      expect(
        wrapper.find(".daisy-loading.daisy-loading-spinner").exists()
      ).toBe(false)
      // The gray overlay should remain visible after answering (isAnswered stays true)
      expect(wrapper.find(".absolute.inset-0").exists()).toBe(true)
    })

    it("allows retrying on API error", async () => {
      // Setup API to fail first time
      const answerQuizSpy = mockSdkService(
        RecallPromptController,
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
      mockSdkService(RecallPromptController, "answerQuiz", answerResult)

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
