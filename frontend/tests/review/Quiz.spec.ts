import { describe, it, vi, beforeEach, afterEach } from "vitest";
import { flushPromises } from "@vue/test-utils";
import Quiz from "@/components/review/Quiz.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

helper.resetWithApiMock(beforeEach, afterEach);

const quizQuestion = makeMe.aQuizQuestion.withClozeSelectionQuestion().please();

describe("repeat page", () => {
  const mountPage = async (
    quizQuestions: number[],
    eagerFetchCount: number,
  ) => {
    const wrapper = helper
      .component(Quiz)
      .withStorageProps({
        quizQuestions,
        currentIndex: 0,
        eagerFetchCount,
      })
      .mount();
    await flushPromises();
    return wrapper;
  };

  describe('repeat page with "just review" quiz', () => {
    beforeEach(() => {
      vi.useFakeTimers();
    });

    it("fetch the first 1 question when mount", async () => {
      helper.apiMock
        .expectingGet(`/api/review-points/1/random-question`)
        .andReturnOnce(quizQuestion);
      await mountPage([1, 2, 3], 1);
    });

    it("fetch the first 3 question when mount", async () => {
      helper.apiMock
        .expectingGet(`/api/review-points/1/random-question`)
        .andReturnOnce(quizQuestion);
      helper.apiMock
        .expectingGet(`/api/review-points/2/random-question`)
        .andReturnOnce(quizQuestion);
      helper.apiMock
        .expectingGet(`/api/review-points/3/random-question`)
        .andReturnOnce(quizQuestion);
      await mountPage([1, 2, 3, 4], 3);
    });

    it("does not fetch question 2 again after prefetched", async () => {
      helper.apiMock
        .expectingGet(`/api/review-points/1/random-question`)
        .andReturnOnce(quizQuestion);
      helper.apiMock
        .expectingGet(`/api/review-points/2/random-question`)
        .andReturnOnce(quizQuestion);
      const wrapper = await mountPage([1, 2, 3, 4], 2);
      helper.apiMock
        .expectingGet(`/api/review-points/3/random-question`)
        .andReturnOnce(quizQuestion);
      await wrapper.setProps({ currentIndex: 1 });
    });
  });
});
