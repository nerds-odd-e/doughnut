import SuggestQuestionForFineTuning from "@/components/ai/SuggestQuestionForFineTuning.vue"
import type { PredefinedQuestion } from "@generated/backend"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, it, expect } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"

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
      const suggestQuestionSpy = mockSdkService(
        "suggestQuestionForFineTuning",
        undefined
      )
      wrapper.get(".negative-feedback-btn").trigger("click")
      wrapper.get("button.daisy-btn-success").trigger("click")
      await flushPromises()
      expect(suggestQuestionSpy).toBeCalledWith({
        path: { predefinedQuestion: predefinedQuestion.id },
        body: {
          comment: "",
          isPositiveFeedback: false,
        },
      })
    })
  })
})
