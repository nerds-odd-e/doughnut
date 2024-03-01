import { describe, it, vi, expect, beforeEach, afterEach } from "vitest";
import { flushPromises } from "@vue/test-utils";
import RepeatPage from "@/pages/RepeatPage.vue";
import { AnsweredQuestion } from "@/generated/backend";
import mockBrowserTimeZone from "../helpers/mockBrowserTimeZone";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";
import RenderingHelper from "../helpers/RenderingHelper";

let renderer: RenderingHelper;
const mockRouterPush = vi.fn();
const mockedRepeatCall = vi.fn();

helper.resetWithApiMock(beforeEach, afterEach);

beforeEach(() => {
  vitest.resetAllMocks();
  helper.managedApi.restReviewsController.repeatReview = mockedRepeatCall;
  renderer = helper
    .component(RepeatPage)
    .withMockRouterPush(mockRouterPush)
    .withStorageProps({ eagerFetchCount: 1 });
});

describe("repeat page", () => {
  const mountPage = async () => {
    const wrapper = renderer.currentRoute({ name: "repeat" }).mount();
    await flushPromises();
    return wrapper;
  };

  mockBrowserTimeZone("Asia/Shanghai", beforeEach, afterEach);

  it("redirect to review page if nothing to repeat", async () => {
    const repetition = makeMe.aDueReviewPointsList.please();
    mockedRepeatCall.mockResolvedValue(repetition);
    const wrapper = await mountPage();
    expect(wrapper.findAll("button").length).toBe(4);
    expect(mockedRepeatCall).toHaveBeenCalledWith("Asia/Shanghai", 0);
  });

  describe('repeat page with "just review" quiz', () => {
    const firstReviewPointId = 123;
    const secondReviewPointId = 456;
    const mockedRandomQuestionCall = vi.fn();
    const mockedReviewPointCall = vi.fn();

    beforeEach(() => {
      vi.useFakeTimers();
      helper.managedApi.restReviewPointController.show =
        mockedReviewPointCall.mockResolvedValue(makeMe.aReviewPoint.please());
      helper.managedApi.silent.restReviewPointController.generateRandomQuestion =
        mockedRandomQuestionCall;
      mockedRandomQuestionCall.mockRejectedValueOnce(makeMe.a404Error.please());
      mockedRepeatCall.mockResolvedValue(
        makeMe.aDueReviewPointsList
          .toRepeat([firstReviewPointId, secondReviewPointId, 3])
          .please(),
      );
    });

    it("shows the progress", async () => {
      const wrapper = await mountPage();
      expect(wrapper.find(".progress-text").text()).toContain("0/3");
      expect(mockedRandomQuestionCall).toHaveBeenCalledWith(firstReviewPointId);
    });

    it("should show progress", async () => {
      const wrapper = await mountPage();
      const answerResult: AnsweredQuestion = {
        answerId: 1,
        correct: false,
        answerDisplay: "my answer",
        quizQuestion: makeMe.aQuizQuestion.please(),
      };
      const mockedMarkAsRepeatedCall = vi.fn().mockResolvedValue(answerResult);
      helper.managedApi.restReviewPointController.markAsRepeated =
        mockedMarkAsRepeatedCall;
      const quizQuestion = makeMe.aQuizQuestion.please();
      mockedRandomQuestionCall.mockResolvedValueOnce(quizQuestion);
      vi.runOnlyPendingTimers();
      await flushPromises();
      await wrapper.find("button.btn-primary").trigger("click");
      expect(mockedMarkAsRepeatedCall).toHaveBeenCalledWith(
        firstReviewPointId,
        true,
      );
      await flushPromises();
      expect(wrapper.find(".progress-text").text()).toContain("1/3");
      expect(mockedRandomQuestionCall).toHaveBeenCalledWith(
        secondReviewPointId,
      );
    });
  });
});
