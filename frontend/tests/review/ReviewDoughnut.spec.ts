/**
 * @jest-environment jsdom
 */
import { flushPromises } from "@vue/test-utils";
import ReviewDoughnut from "@/components/review/ReviewDoughnut.vue";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

describe("repetition page", () => {
  describe("repetition page for a link", () => {
    beforeEach(async () => {
      helper.apiMock.expectingGet("/api/reviews/overview").andReturnOnce({
        toRepeatCount: 10,
        learntCount: 5,
        notLearntCount: 6,
        toInitialReviewCount: 3,
      } as Generated.ReviewStatus);
    });

    it("click on note when doing review", async () => {
      const wrapper = helper.component(ReviewDoughnut).mount();
      await flushPromises();
      expect(wrapper.find(".doughnut-initial-reviews").text()).toBe("3/6");
    });
  });
});
