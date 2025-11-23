import SuggestQuestionForFineTuning from "@/components/ai/SuggestQuestionForFineTuning.vue"
import type { PredefinedQuestion } from "@generated/backend"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, it, vi } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import * as sdk from "@generated/backend/sdk.gen"

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
      vi.spyOn(sdk, "suggestQuestionForFineTuning").mockResolvedValue({
        data: undefined,
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      } as unknown as Awaited<
        ReturnType<typeof sdk.suggestQuestionForFineTuning>
      >)
      wrapper.get(".negative-feedback-btn").trigger("click")
      wrapper.get("button.daisy-btn-success").trigger("click")
      await flushPromises()
      expect(sdk.suggestQuestionForFineTuning).toBeCalledWith({
        path: { predefinedQuestion: predefinedQuestion.id },
        body: {
          comment: "",
          isPositiveFeedback: false,
        },
      })
    })
  })
})
