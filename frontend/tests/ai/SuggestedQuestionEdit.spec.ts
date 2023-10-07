import { flushPromises } from "@vue/test-utils";
import { afterEach, beforeEach, describe, it } from "vitest";
import SuggestedQuestionEdit from "@/components/ai/SuggestedQuestionEdit.vue";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

describe("QuizQuestion", () => {
  describe("suggest question for fine tuning AI", () => {
    const notebook: Generated.NotePositionViewedByUser =
      makeMe.aNotePosition.please();
    const quizQuestion: Generated.QuizQuestion = makeMe.aQuizQuestion
      .withQuestionType("AI_QUESTION")
      .withNotebookPosition(notebook)
      .please();
    const suggestedQuestion: Generated.SuggestedQuestionForFineTuning = {
      preservedQuestion: quizQuestion,
      comment: "",
    };

    let wrapper;

    beforeEach(() => {
      wrapper = helper
        .component(SuggestedQuestionEdit)
        .withStorageProps({ suggestedQuestion })
        .mount();
    });

    it("should be able to suggest a question as good example", async () => {
      helper.apiMock.expectingPatch(
        `/api/fine-tuning/update-suggested-question-for-fine-tuning`,
      );
      wrapper.get("button.btn-success").trigger("click");
      await flushPromises();
    });
  });
});
