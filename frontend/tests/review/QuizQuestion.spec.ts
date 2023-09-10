import { flushPromises } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import QuizQuestion from "@/components/review/QuizQuestion.vue";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

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

    describe("marking question as good", () => {
      const notebook: Generated.NotePositionViewedByUser =
        makeMe.aNotePosition.please();
      const quizQuestion: Generated.QuizQuestion = makeMe.aQuizQuestion
        .withQuestionType("AI_QUESTION")
        .withNotebookPosition(notebook)
        .please();

      let wrapper;
      const markedQuestionId: string = "1";
      const stubMarkQuestionCall = () =>
        helper.apiMock
          .expectingPost(`/api/reviews/mark_question`)
          .andReturnOnce(markedQuestionId);

      const clickSendQuestion = () =>
        wrapper
          .find(
            "button[title='send this question for fine tuning the question generation model']",
          )
          .trigger("click");

      beforeEach(() => {
        wrapper = helper
          .component(QuizQuestion)
          .withStorageProps({ quizQuestion })
          .mount();
      });

      it("should be able to mark a question as good", async () => {
        const markExpectation = stubMarkQuestionCall();
        wrapper.vm.popups.confirm = vitest.fn(() => Promise.resolve(true));
        await clickSendQuestion();
        await flushPromises();
        expect(markExpectation.actualRequestJsonBody()).toMatchObject({
          quizQuestionId: quizQuestion.quizQuestionId,
          noteId: notebook.noteId,
        });
      });

      it("should be able to skip marking a question as good", async () => {
        wrapper.vm.popups.confirm = vitest.fn(() => Promise.resolve(false));
        await clickSendQuestion();
        await flushPromises();
      });
    });
  });
});
