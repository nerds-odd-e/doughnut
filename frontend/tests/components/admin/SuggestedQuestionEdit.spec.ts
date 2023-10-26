import { flushPromises } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import SuggestedQuestionEdit from "@/components/admin/SuggestedQuestionEdit.vue";
import helper from "../../helpers";
import makeMe from "../../fixtures/makeMe";

helper.resetWithApiMock(beforeEach, afterEach);

describe("Edit Suggested Question", () => {
  describe("suggest question for fine tuning AI", () => {
    const suggestedQuestion = makeMe.aSuggestedQuestionForFineTuning.please();

    let wrapper;

    beforeEach(() => {
      wrapper = helper
        .component(SuggestedQuestionEdit)
        .withStorageProps({ modelValue: suggestedQuestion })
        .mount();
    });

    it("call the api to make update", async () => {
      helper.apiMock.expectingPatch(
        `/api/fine-tuning/1357/update-suggested-question-for-fine-tuning`,
      );
      wrapper.get("button.btn-success").trigger("click");
      await flushPromises();
    });

    it("requires more than 1 choice", async () => {
      wrapper.get("#undefined-choice-1").setValue("");
      wrapper.get("#undefined-choice-2").setValue("");
      wrapper.get("#undefined-choice-3").setValue("");
      wrapper.get("button.btn-success").trigger("click");
      await flushPromises();
      expect(wrapper.get(".error-msg").text()).toContain(
        "At least 2 choices are required",
      );
    });

    it("validates the index", async () => {
      wrapper.get("#undefined-correctChoiceIndex").setValue("4");
      wrapper.get("button.btn-success").trigger("click");
      await flushPromises();
      expect(wrapper.get(".error-msg").text()).toContain(
        "Correct choice index is out of range",
      );
    });
  });
});
