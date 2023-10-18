import { flushPromises } from "@vue/test-utils";
import { afterEach, beforeEach, describe, it } from "vitest";
import SuggestedQuestionList from "@/components/admin/SuggestedQuestionList.vue";
import helper from "../../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

describe("Edit Suggested Question", () => {
  describe("suggest question for fine tuning AI", () => {
    const suggestedQuestion: Generated.SuggestedQuestionForFineTuning = {
      id: 1357,
      preservedQuestion: {
        stem: "What is the capital of France?",
        choices: ["Paris", "London", "Berlin", "Madrid"],
        correctChoiceIndex: 0,
        confidence: 9,
      },
      comment: "",
      positiveFeedback: false,
      duplicated: false,
    };

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
