import SuggestedQuestionEdit from "@/components/admin/SuggestedQuestionEdit.vue"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, expect, it, vi } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import * as sdk from "@generated/backend/sdk.gen"

describe("Edit Suggested Question", () => {
  describe("suggest question for fine tuning AI", () => {
    const suggestedQuestion = makeMe.aSuggestedQuestionForFineTuning.please()

    let wrapper

    beforeEach(() => {
      wrapper = helper
        .component(SuggestedQuestionEdit)
        .withStorageProps({ modelValue: suggestedQuestion })
        .mount()
    })

    it("call the api to make update", async () => {
      const mockUpdate = vi
        .spyOn(sdk, "updateSuggestedQuestionForFineTuning")
        .mockResolvedValue({
          data: {} as never,
          error: undefined,
          request: {} as Request,
          response: {} as Response,
        })
      wrapper.get("button.daisy-btn-success").trigger("click")
      await flushPromises()
      expect(mockUpdate).toHaveBeenCalledWith({
        path: { suggestedQuestion: 1357 },
        body: expect.anything(),
      })
    })

    it("requires more than 1 choice", async () => {
      wrapper.get("#undefined-choice-1").setValue("")
      wrapper.get("#undefined-choice-2").setValue("")
      wrapper.get("#undefined-choice-3").setValue("")
      wrapper.get("button.daisy-btn-success").trigger("click")
      await flushPromises()
      expect(wrapper.get(".daisy-text-error").text()).toContain(
        "At least 2 choices are required"
      )
    })

    it("validates the correct answer index", async () => {
      wrapper.get("#undefined-correctChoiceIndex").setValue("4")
      wrapper.get("button.daisy-btn-success").trigger("click")
      await flushPromises()
      expect(wrapper.get(".daisy-text-error").text()).toContain(
        "Correct choice index is out of range"
      )
    })

    it("real correct answers has to be number lists", async () => {
      wrapper.get("#undefined-realCorrectAnswers").setValue("a,b")
      wrapper.get("button.daisy-btn-success").trigger("click")
      await flushPromises()
      expect(wrapper.get(".daisy-text-error").text()).toContain(
        "must be a number list"
      )
    })
  })
})
