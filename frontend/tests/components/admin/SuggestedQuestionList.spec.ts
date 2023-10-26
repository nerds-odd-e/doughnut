import { afterEach, beforeEach, describe, expect, it } from "vitest";
import SuggestedQuestionList from "@/components/admin/SuggestedQuestionList.vue";
import usePopups from "../../../src/components/commons/Popups/usePopups";
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

    it("can download chat gpt conversation starter", async () => {
      const suggestedQuestion = makeMe.aSuggestedQuestionForFineTuning
        .positive()
        .please();
      const wrapper = helper
        .component(SuggestedQuestionList)
        .withProps({ suggestedQuestions: [suggestedQuestion] })
        .mount();
      const btn = wrapper
        .findAll("button")
        .filter((node) => node.text().match(/Chat/))[0];
      btn!.trigger("click");
      const alertMsg = usePopups().popups.peek()[0]!.message;
      expect(alertMsg).toContain(suggestedQuestion.preservedQuestion.stem);
      expect(alertMsg).toContain(suggestedQuestion.preservedNoteContent);
    });
  });
});
