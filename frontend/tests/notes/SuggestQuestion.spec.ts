import SuggestQuestionForFineTuning from "@/components/ai/SuggestQuestionForFineTuning.vue"
import type { PredefinedQuestion } from "generated/backend"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, it } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"

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
      helper.managedApi.restPredefinedQuestionController.suggestQuestionForFineTuning =
        vi.fn().mockResolvedValue({})
      wrapper.get(".negative-feedback-btn").trigger("click")
      wrapper.get("button.daisy-btn-success").trigger("click")
      await flushPromises()
      expect(
        helper.managedApi.restPredefinedQuestionController
          .suggestQuestionForFineTuning
      ).toBeCalledWith(predefinedQuestion.id, {
        comment: "",
        isPositiveFeedback: false,
      })
    })
  })
})
