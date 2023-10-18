import { flushPromises } from "@vue/test-utils";
import { afterEach, beforeEach, describe, it } from "vitest";
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

    it("should be able to suggest a question as good example", async () => {
      helper.apiMock.expectingPatch(
        `/api/fine-tuning/1357/update-suggested-question-for-fine-tuning`,
      );
      wrapper.get("button.btn-success").trigger("click");
      await flushPromises();
    });
  });
});
