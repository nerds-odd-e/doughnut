import { VueWrapper } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import SuggestedQuestionList from "@/components/admin/SuggestedQuestionList.vue";
import usePopups from "../../../src/components/commons/Popups/usePopups";
import helper from "../../helpers";
import makeMe from "../../fixtures/makeMe";

helper.resetWithApiMock(beforeEach, afterEach);

describe("Edit Suggested Question", () => {
  const matchByText = (wrapper: VueWrapper, reg: RegExp, selector: string) => {
    const btns = wrapper
      .findAll(selector)
      .filter((node) => node.text().match(reg));
    return btns.length === 1 ? btns[0] : undefined;
  };
  describe("suggest question for fine tuning AI", () => {
    it("lists the suggestions", async () => {
      const suggestedQuestion = makeMe.aSuggestedQuestionForFineTuning.please();
      const wrapper = helper
        .component(SuggestedQuestionList)
        .withProps({ suggestedQuestions: [suggestedQuestion] })
        .mount();
      expect(wrapper.findAll("tr").length).toEqual(2);
      expect(matchByText(wrapper, /Duplicate/, "button")).not.toBeUndefined();
    });

    describe("with a positive feedback", () => {
      const suggestedQuestion = makeMe.aSuggestedQuestionForFineTuning
        .positive()
        .please();

      it("cannot duplicate good suggestion", async () => {
        const wrapper = helper
          .component(SuggestedQuestionList)
          .withProps({ suggestedQuestions: [suggestedQuestion] })
          .mount();
        expect(matchByText(wrapper, /Duplicate/, "button")).toBeUndefined();
      });

      it("can download chat gpt conversation starter", async () => {
        const wrapper = helper
          .component(SuggestedQuestionList)
          .withProps({ suggestedQuestions: [suggestedQuestion] })
          .mount();
        matchByText(wrapper, /Chat/, "button")!.trigger("click");
        const alertMsg = usePopups().popups.peek()[0]!.message;
        expect(alertMsg).toContain(suggestedQuestion.preservedQuestion.stem);
        expect(alertMsg).toContain(suggestedQuestion.preservedNoteContent);
      });

      it("can delete", async () => {
        const wrapper = helper
          .component(SuggestedQuestionList)
          .withProps({ suggestedQuestions: [suggestedQuestion] })
          .mount();
        matchByText(wrapper, /Del/, "button")!.trigger("click");
        const confirm = usePopups().popups.peek()[0]!;
        confirm.doneResolve(true);
      });
    });
  });
});
