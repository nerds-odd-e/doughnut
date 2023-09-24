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
  });
});
