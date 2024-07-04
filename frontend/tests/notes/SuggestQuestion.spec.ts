import SuggestQuestionForFineTuning from "@/components/ai/SuggestQuestionForFineTuning.vue"
import { QuizQuestion } from "@/generated/backend"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, it } from "vitest"
import makeMe from "../fixtures/makeMe"
import helper from "../helpers"

describe("QuizQuestion", () => {
  describe("suggest question for fine tuning AI", () => {
    const quizQuestion: QuizQuestion = makeMe.aQuizQuestion.please()

    let wrapper

    beforeEach(() => {
      wrapper = helper
        .component(SuggestQuestionForFineTuning)
        .withProps({ quizQuestion })
        .mount()
    })

    it("should be able to suggest a question as good example", async () => {
      helper.managedApi.restQuizQuestionController.suggestQuestionForFineTuning =
        vi.fn().mockResolvedValue({})
      wrapper.get(".negative-feedback-btn").trigger("click")
      wrapper.get("button.btn-success").trigger("click")
      await flushPromises()
      expect(
        helper.managedApi.restQuizQuestionController
          .suggestQuestionForFineTuning
      ).toBeCalledWith(quizQuestion.id, {
        comment: "",
        isPositiveFeedback: false,
      })
    })
  })
})
