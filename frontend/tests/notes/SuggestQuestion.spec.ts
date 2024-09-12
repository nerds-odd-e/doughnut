import SuggestQuestionForFineTuning from "@/components/ai/SuggestQuestionForFineTuning.vue"
import { PredefinedQuestion } from "@/generated/backend"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, it } from "vitest"
import makeMe from "../fixtures/makeMe"
import helper from "../helpers"

describe("SuggestQuestion", () => {
  describe("suggest question for fine tuning AI", () => {
    const predefinedQuestion: PredefinedQuestion =
      makeMe.aPredefinedQuestion.please()

    let wrapper

    beforeEach(() => {
      wrapper = helper
        .component(SuggestQuestionForFineTuning)
        .withProps({ predefinedQuestion })
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
      ).toBeCalledWith(predefinedQuestion.id, {
        comment: "",
        isPositiveFeedback: false,
      })
    })
  })
})
