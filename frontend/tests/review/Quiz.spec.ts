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
      .withStorageProps({ toRepeat: [1, 2, 3] })
      .mount();
    await flushPromises();
    return wrapper;
  };

  describe('repeat page with "just review" quiz', () => {
    let quizQuestion: Generated.QuizQuestionViewedByUser;

    beforeEach(() => {
      vi.useFakeTimers();
      const reviewPoint = makeMe.aReviewPoint.please();
      quizQuestion = makeMe.aQuizQuestion
        .withReviewPointId(reviewPoint.id)
        .please();
      helper.apiMock
        .expectingGet(`/api/review-points/${1}/random-question`)
        .andReturnOnce(quizQuestion);
      helper.apiMock
        .expectingGet(`/api/review-points/${reviewPoint.id}`)
        .andReturnOnce(reviewPoint);
    });

    it("renders", async () => {
      await mountPage();
    });
  });
});
