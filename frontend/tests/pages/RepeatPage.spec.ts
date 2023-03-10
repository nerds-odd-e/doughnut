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
    .withStorageProps({});
});

describe("repeat page", () => {
  const mountPage = async (
    repetition: Generated.RepetitionForUser | Record<string, never>
  ) => {
    helper.apiMock
      .expectingGet("/api/reviews/repeat")
      .andReturnOnce(repetition);
    const wrapper = renderer.currentRoute({ name: "repeat" }).mount();
    await flushPromises();
    return wrapper;
  };

  it("redirect to review page if nothing to repeat", async () => {
    helper.apiMock.expectingGet("/api/reviews/repeat").andRespondOnceWith404();
    renderer.currentRoute({ name: "repeat" }).mount();
    await flushPromises();
    expect(mockRouterPush).toHaveBeenCalledWith({ name: "reviews" });
  });

  describe('repeat page with "just review" quiz', () => {
    let repetition: Generated.RepetitionForUser;

    beforeEach(() => {
      vi.useFakeTimers();
      const reviewPoint = makeMe.aReviewPoint.please();
      repetition = makeMe.aRepetition
        .withReviewPointId(reviewPoint.id)
        .please();
      repetition.toRepeat = [1, 2, 3];
      helper.apiMock
        .expectingGet(`/api/review-points/${reviewPoint.id}`)
        .andReturnOnce(reviewPoint);
    });

    it("shows the progress", async () => {
      const wrapper = await mountPage(repetition);
      expect(wrapper.find(".progress-text").text()).toContain("0/3");
    });

    it("should call the answer api", async () => {
      const wrapper = await mountPage(repetition);
      helper.apiMock.expectingPost(`/api/reviews/answer`);
      vi.runOnlyPendingTimers();
      await flushPromises();
      await wrapper.find("button.btn-primary").trigger("click");
    });

    it("should show progress", async () => {
      const wrapper = await mountPage(repetition);
      const answerResult: Generated.AnswerResult = {
        answerId: 1,
        correct: false,
      };
      helper.apiMock
        .expectingPost(`/api/reviews/answer`)
        .andReturnOnce(answerResult);
      vi.runOnlyPendingTimers();
      await flushPromises();
      await wrapper.find("button.btn-primary").trigger("click");
      helper.apiMock.expectingGet("/api/reviews/repeat");

      for (let i = 0; i < 10; i += 1) {
        // eslint-disable-next-line no-await-in-loop
        await wrapper.vm.$nextTick();
        expect(wrapper.find(".progress-text").text()).toContainEither(
          "0/3",
          "1/4"
        );
      }
    });
  });
});
