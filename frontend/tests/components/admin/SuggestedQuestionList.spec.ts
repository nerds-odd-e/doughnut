import { afterEach, beforeEach, describe, it } from "vitest";
import SuggestedQuestionList from "@/components/admin/SuggestedQuestionList.vue";
import helper from "../../helpers";
import makeMe from "../../fixtures/makeMe";

helper.resetWithApiMock(beforeEach, afterEach);

describe("Edit Suggested Question", () => {
  describe("suggest question for fine tuning AI", () => {
    const suggestedQuestion = makeMe.aSuggestedQuestionForFineTuning.please();
    let wrapper;

    beforeEach(() => {
      wrapper = helper
        .component(SuggestedQuestionList)
        .withStorageProps({ suggestedQuestions: [suggestedQuestion] })
        .mount();
    });

    it("lists the suggestions", async () => {
      expect(wrapper.findAll("tr").length).toEqual(2);
    });
  });
});
