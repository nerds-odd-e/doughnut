import { describe, it, vi, beforeEach, afterEach } from "vitest";
import { flushPromises } from "@vue/test-utils";
import Quiz from "@/components/review/Quiz.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

helper.resetWithApiMock(beforeEach, afterEach);

describe("repeat page", () => {
  const mountPage = async () => {
    const wrapper = helper
      .component(Quiz)
      .withStorageProps({ quizQuestions: [1, 2, 3] })
      .mount();
    await flushPromises();
    return wrapper;
  };

  describe('repeat page with "just review" quiz', () => {
    beforeEach(() => {
      vi.useFakeTimers();
    });

    it("fetch the first question when mount", async () => {
      const quizQuestion = makeMe.aQuizQuestion
        .withClozeSelectionQuestion()
        .please();
      helper.apiMock
        .expectingGet(`/api/review-points/${1}/random-question`)
        .andReturnOnce(quizQuestion);
      await mountPage();
    });
  });
});
