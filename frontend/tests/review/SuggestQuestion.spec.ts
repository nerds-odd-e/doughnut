import { flushPromises } from "@vue/test-utils";
import { afterEach, beforeEach, describe, it } from "vitest";
import SuggestQuestionForFineTuning from "@/components/review/SuggestQuestionForFineTuning.vue";
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

    let wrapper;

    beforeEach(() => {
      wrapper = helper
        .component(SuggestQuestionForFineTuning)
        .withStorageProps({ quizQuestion })
        .mount();
    });

    it("should be able to suggest a question as good example", async () => {
      helper.apiMock.expectingPost(
        `/api/quiz-questions/${quizQuestion.quizQuestionId}/suggest-fine-tuning`,
      );
      wrapper.get("button.btn-success").trigger("click");
      await flushPromises();
    });
  });
});
