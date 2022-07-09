/**
 * @jest-environment jsdom
 */
import { flushPromises } from "@vue/test-utils";
import ReviewDoughnut from "@/components/review/ReviewDoughnut.vue";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

describe("review doughnut", () => {
  describe("repetition page for a link", () => {
    beforeEach(async () => {
      jest.useFakeTimers();
      helper.apiMock.expectingGet("/api/reviews/overview").andReturnOnce({
        toRepeatCount: 10,
        learntCount: 5,
        notLearntCount: 6,
        toInitialReviewCount: 3,
      } as Generated.ReviewStatus);
    });
    afterEach(() => {
      jest.clearAllTimers();
      jest.useRealTimers();
    });

    it("fetches the data", async () => {
      const wrapper = helper.component(ReviewDoughnut).mount();
      await flushPromises();
      expect(wrapper.find(".doughnut-ring .initial-review").text()).toBe("3/6");
    });

    it("refreshes", async () => {
      jest.setSystemTime(new Date(2020, 1, 1, 10, 30));
      helper.component(ReviewDoughnut).mount();
      await flushPromises();
      helper.apiMock.expectingGet("/api/reviews/overview");
      jest.advanceTimersToNextTimer();
      expect(new Date()).toEqual(new Date(2020, 1, 1, 12));
    });
  });
});
