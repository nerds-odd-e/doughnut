import { describe, it, vi, expect, beforeEach, afterEach } from "vitest";
import { flushPromises } from "@vue/test-utils";
import RepeatPage from "@/pages/RepeatPage.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";
import RenderingHelper from "../helpers/RenderingHelper";

let renderer: RenderingHelper;
let mockRouterPush = vi.fn();

helper.resetWithApiMock(beforeEach, afterEach);

expect.extend({
  toContainEither(received, arg1, arg2) {
    const pass = received.includes(arg1) || received.includes(arg2);
    if (pass) {
      return {
        message: () =>
          `expected "${received}" not to contain either "${arg1}" or "${arg2}"`,
        pass: true,
      };
    }
    return {
      message: () =>
        `expected "${received}" to contain either "${arg1}" or "${arg2}"`,
      pass: false,
    };
  },
});

beforeEach(() => {
  mockRouterPush = vi.fn();
  renderer = helper
    .component(RepeatPage)
    .withMockRouterPush(mockRouterPush)
    .withStorageProps({ eagerFetchCount: 1 });
});

describe("repeat page", () => {
  const mountPage = async (
    repetition: Generated.DueReviewPoints | Record<string, never>,
  ) => {
    helper.apiMock
      .expectingGet("/api/reviews/repeat?dueindays=0")
      .andReturnOnce(repetition);
    const wrapper = renderer.currentRoute({ name: "repeat" }).mount();
    await flushPromises();
    return wrapper;
  };

  it("redirect to review page if nothing to repeat", async () => {
    const repetition = makeMe.aDueReviewPointsList.please();
    const wrapper = await mountPage(repetition);
    await flushPromises();
    expect(wrapper.findAll("button").length).toBe(4);
  });

  describe('repeat page with "just review" quiz', () => {
    let repetition: Generated.DueReviewPoints;
    let quizQuestion: Generated.QuizQuestion;
    const reviewPointId = 123;
    const secondReviewPointId = 456;

    beforeEach(() => {
      vi.useFakeTimers();
      const reviewPoint = makeMe.aReviewPoint.please();
      repetition = makeMe.aDueReviewPointsList.please();
      quizQuestion = makeMe.aQuizQuestion.please();
      repetition.toRepeat = [reviewPointId, secondReviewPointId, 3];
      helper.apiMock
        .expectingGet(`/api/review-points/${reviewPointId}/random-question`)
        .andReturnOnce(quizQuestion);
      helper.apiMock
        .expectingGet(`/api/review-points/${reviewPointId}`)
        .andReturnOnce(reviewPoint);
    });

    it("shows the progress", async () => {
      const wrapper = await mountPage(repetition);
      expect(wrapper.find(".progress-text").text()).toContain("0/3");
    });

    it("should show progress", async () => {
      const wrapper = await mountPage(repetition);
      const answerResult: Generated.AnswerResult = {
        answerId: 1,
        correct: false,
      };
      helper.apiMock
        .expectingPost(
          `/api/quiz-questions/${quizQuestion.quizQuestionId}/answer`,
        )
        .andReturnOnce(answerResult);
      vi.runOnlyPendingTimers();
      await flushPromises();
      await wrapper.find("button.btn-primary").trigger("click");
      helper.apiMock
        .expectingGet(
          `/api/review-points/${secondReviewPointId}/random-question`,
        )
        .andReturnOnce(quizQuestion);

      for (let i = 0; i < 10; i += 1) {
        // eslint-disable-next-line no-await-in-loop
        await wrapper.vm.$nextTick();
        expect(wrapper.find(".progress-text").text()).toContainEither(
          "0/3",
          "1/3",
        );
      }
    });
  });
});
