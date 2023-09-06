import { describe, it, expect, beforeEach, afterEach } from "vitest";
import { flushPromises } from "@vue/test-utils";
import QuizQuestion from "@/components/review/QuizQuestion.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

helper.resetWithApiMock(beforeEach, afterEach);

const goodQuestion = {
  question: "any question?",
  options: [
    { option: "option A" },
    { option: "option B" },
    { option: "option C" },
  ],
};

describe("QuizQuestion", () => {
  describe("AiQuestion", () => {
    it.skip("shows the progress", async () => {
      const quizQuestion: Generated.QuizQuestion = makeMe.aQuizQuestion
        .withQuestionType("AI_QUESTION")
        .please();
      helper.apiMock
        .expectingPost(
          `/api/ai/${quizQuestion.notebookPosition?.noteId}/completion`,
        )
        .andReturnOnce({
          moreCompleteContent: JSON.stringify(goodQuestion),
          finishReason: "stop",
        });
      const wrapper = helper
        .component(QuizQuestion)
        .withStorageProps({ quizQuestion })
        .mount();
      await flushPromises();
      expect(wrapper.text()).toContain("1/2");
    });

    it("should be able to mark a question as good", async () => {
      const notebook: Generated.NotePositionViewedByUser =
        makeMe.aNotePosition.please();
      const quizQuestion: Generated.QuizQuestion = makeMe.aQuizQuestion
        .withQuestionType("AI_QUESTION")
        .withNotebookPosition(notebook)
        .please();
      const expectation = helper.apiMock
        .expectingPost(`/api/reviews/mark_question`)
        .andReturnOnce({});

      const wrapper = helper
        .component(QuizQuestion)
        .withStorageProps({ quizQuestion })
        .mount();

      expect(wrapper.find(".thumb-up-filled").exists()).toBe(false);
      await wrapper.find(".thumb-up-hollow").trigger("click");

      await flushPromises();

      expect(expectation.actualRequestJsonBody()).toMatchObject({
        quizQuestionId: quizQuestion.quizQuestionId,
        noteId: notebook.noteId,
        isGood: true,
      });

      expect(wrapper.find(".thumb-up-filled").exists()).toBe(true);
      expect(wrapper.find(".thumb-up-hollow").exists()).toBe(false);
    });
  });
});
