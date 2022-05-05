/**
 * @jest-environment jsdom
 */
import AnswerShowPage from "@/pages/AnswerShowPage.vue";
import { flushPromises } from "@vue/test-utils";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

helper.resetWithApiMock(beforeEach, afterEach);

describe("repetition page", () => {
  describe("repetition page for a link", () => {
    const linkViewedByUser = makeMe.aLinkViewedByUser.please();
    const reviewPointViewedByUser = makeMe.aReviewPoint
      .ofLink(linkViewedByUser)
      .please();

    beforeEach(async () => {
      helper.apiMock.expecting("/api/reviews/answers/1").andReturnOnce({
        answerId: 1,
        answerDisplay: "",
        correct: true,
        reviewPoint: reviewPointViewedByUser,
      });
      helper.apiMock
        .expecting(
          `/api/review-points/${reviewPointViewedByUser.reviewPoint.id}`
        )
        .andReturnOnce(reviewPointViewedByUser);
    });

    it("click on note when doing review", async () => {
      const wrapper = helper
        .component(AnswerShowPage)
        .withProps({ answerId: 1 })
        .currentRoute({ name: "repeat" })
        .mount();
      await flushPromises();
      expect(
        JSON.parse(wrapper.find(".link-source .router-link").attributes().to)
          .name
      ).toEqual("notebooks");
    });

    it("click on note when doing review and in a nested page", async () => {
      const wrapper = helper
        .component(AnswerShowPage)
        .withProps({ answerId: 1 })
        .currentRoute({ name: "repeat-noteShow", params: { rawNoteId: 123 } })
        .mount();
      await flushPromises();
      expect(
        JSON.parse(wrapper.find(".link-source .router-link").attributes().to)
      ).toEqual({ name: "notebooks" });
    });
  });
});
