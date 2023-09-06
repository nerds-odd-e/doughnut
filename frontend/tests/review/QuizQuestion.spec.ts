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

      const stubMarkQuestionCall = () =>
        helper.apiMock
          .expectingPost(`/api/reviews/mark_question`)
          .andReturnOnce({});

      const stubUnmarkQuestionCall = () =>
        helper.apiMock
          .expectingDelete(`/api/reviews/mark_question`)
          .andReturnOnce({});

      beforeEach(() => {
        wrapper = helper
          .component(QuizQuestion)
          .withStorageProps({ quizQuestion })
          .mount();
      });

      it("should be able to mark a question as good", async () => {
        const markExpectation = stubMarkQuestionCall();
        expect(wrapper.find(".thumb-up-filled").exists()).toBe(false);
        await wrapper.find(".thumb-up-hollow").trigger("click");

        await flushPromises();

        expect(markExpectation.actualRequestJsonBody()).toMatchObject({
          quizQuestionId: quizQuestion.quizQuestionId,
          noteId: notebook.noteId,
          isGood: true,
        });

        expect(wrapper.find(".thumb-up-filled").exists()).toBe(true);
        expect(wrapper.find(".thumb-up-hollow").exists()).toBe(false);
      });

      it("should be able to undo marking a question as good", async () => {
        stubMarkQuestionCall();
        const unmarkExpectation = stubUnmarkQuestionCall();
        await wrapper.find(".thumb-up-hollow").trigger("click");
        await flushPromises();
        await wrapper.find(".thumb-up-filled").trigger("click");
        await flushPromises();

        expect(unmarkExpectation.actualRequestJsonBody()).toMatchObject({
          quizQuestionId: quizQuestion.quizQuestionId,
          noteId: notebook.noteId,
          isGood: true,
        });

        expect(wrapper.find(".thumb-up-filled").exists()).toBe(false);
        expect(wrapper.find(".thumb-up-hollow").exists()).toBe(true);
      });
    });
  });
});
