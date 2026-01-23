import SuggestQuestionForFineTuning from "@/components/ai/SuggestQuestionForFineTuning.vue"
import type { PredefinedQuestion } from "@generated/backend"
import { type VueWrapper, flushPromises } from "@vue/test-utils"
import { beforeEach, describe, it, expect, afterEach } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"

describe("SuggestQuestion", () => {
  describe("suggest question for fine tuning AI", () => {
    const predefinedQuestion: PredefinedQuestion =
      makeMe.aPredefinedQuestion.please()

    // biome-ignore lint/suspicious/noExplicitAny: wrapper for testing
    let wrapper: VueWrapper<any>

    beforeEach(() => {
      wrapper = helper
        .component(SuggestQuestionForFineTuning)
        .withProps({ predefinedQuestion })
        .mount({ attachTo: document.body })
    })

    afterEach(() => {
      wrapper?.unmount()
      document.body.innerHTML = ""
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
