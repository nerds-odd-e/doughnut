import { afterEach, beforeEach, describe, it } from "vitest";
import SuggestedQuestionList from "@/components/admin/SuggestedQuestionList.vue";
import helper from "../../helpers";
import makeMe from "../../fixtures/makeMe";

helper.resetWithApiMock(beforeEach, afterEach);

describe("Edit Suggested Question", () => {
  describe("suggest question for fine tuning AI", () => {
    it("lists the suggestions", async () => {
      const suggestedQuestion = makeMe.aSuggestedQuestionForFineTuning.please();
      const wrapper = helper
        .component(SuggestedQuestionList)
        .withProps({ suggestedQuestions: [suggestedQuestion] })
        .mount();
      expect(wrapper.findAll("tr").length).toEqual(2);
      const btn = wrapper
        .findAll("button")
        .filter((node) => node.text().match(/Duplicate/));
      expect(btn.length).toBe(1);
    });

    it("cannot duplicate good suggestion", async () => {
      const suggestedQuestion = makeMe.aSuggestedQuestionForFineTuning
        .positive()
        .please();
      const wrapper = helper
        .component(SuggestedQuestionList)
        .withProps({ suggestedQuestions: [suggestedQuestion] })
        .mount();
      const btn = wrapper
        .findAll("button")
        .filter((node) => node.text().match(/Duplicate/));
      expect(btn.length).toBe(0);
    });
  });
});
