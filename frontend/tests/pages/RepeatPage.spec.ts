/**
 * @jest-environment jsdom
 */
import RepeatPage from "@/pages/RepeatPage.vue";
import flushPromises from "flush-promises";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";
import RenderingHelper from "../helpers/RenderingHelper";

let renderer: RenderingHelper;
let mockRouterPush = jest.fn();

helper.resetWithApiMock(beforeEach, afterEach);

beforeEach(() => {
  mockRouterPush = jest.fn();
  renderer = helper.component(RepeatPage).withMockRouterPush(mockRouterPush);
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
    helper.apiMock
      .expectingGet("/api/reviews/repeat")
      .andRespondOnce({ status: 404 });
    const wrapper = renderer.currentRoute({ name: "repeat" }).mount();
    await flushPromises();
    expect(wrapper.find(".alert-success").text()).toEqual(
      "You have finished all repetitions for this half a day!"
    );
  });

  it("replace route with repeat/quiz if there is a quiz", async () => {
    const repetition = makeMe.aRepetition.withQuestion().please();
    await mountPage(repetition);
    expect(mockRouterPush).toHaveBeenCalledWith({ name: "repeat-quiz" });
  });

  describe('repeat page with "just review" quiz', () => {
    let repetition: Generated.RepetitionForUser;

    beforeEach(() => {
      const reviewPoint = makeMe.aReviewPoint.please();
      repetition = makeMe.aRepetition
        .withReviewPointId(reviewPoint.id)
        .please();
      helper.apiMock
        .expectingGet(`/api/review-points/${reviewPoint.id}`)
        .andReturnOnce(reviewPoint);
    });

    it("stay at repeat page if there is no quiz", async () => {
      await mountPage(repetition);
      expect(mockRouterPush).toHaveBeenCalledWith({
        name: "repeat-quiz",
      });
    });

    it("should call the self-evaluate api", async () => {
      const wrapper = await mountPage(repetition);
      helper.apiMock.expectingPost(
        `/api/reviews/${repetition.reviewPoint}/self-evaluate`
      );
      helper.apiMock
        .expectingGet("/api/reviews/repeat")
        .andReturnOnce(repetition);
      await wrapper.find("#repeat-sad").trigger("click");
    });
  });
});
