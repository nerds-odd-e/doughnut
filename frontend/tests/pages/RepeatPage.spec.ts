import { describe, it, vi, expect, beforeEach, afterEach } from "vitest";
import { flushPromises } from "@vue/test-utils";
import { useRouter } from "vue-router";
import RepeatPage from "@/pages/RepeatPage.vue";
import { AnsweredQuestion } from "@/generated/backend";
import mockBrowserTimeZone from "../helpers/mockBrowserTimeZone";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";
import RenderingHelper from "../helpers/RenderingHelper";

vitest.mock("vue-router", () => ({
  useRouter: () => ({
    currentRoute: {
      value: {
        name: "repeat",
      },
    },
  }),
}));

useRouter().currentRoute.value.name = "repeat";

let renderer: RenderingHelper;
const mockRouterPush = vi.fn();
const mockedRepeatCall = vi.fn();

let teleportTarget: HTMLDivElement;

beforeEach(() => {
  teleportTarget = document.createElement("div");
  teleportTarget.id = "head-status";
  document.body.appendChild(teleportTarget);
});
afterEach(() => {
  document.body.innerHTML = "";
});

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
    await mountPage();
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
      mockedRandomQuestionCall.mockRejectedValueOnce(
        makeMe.anApiError.please(),
      );
      mockedRepeatCall.mockResolvedValue(
        makeMe.aDueReviewPointsList
          .toRepeat([firstReviewPointId, secondReviewPointId, 3])
          .please(),
      );
    });

    it("shows the progress", async () => {
      await mountPage();
      expect(teleportTarget.textContent).toContain("0/3");
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
      expect(teleportTarget.textContent).toContain("1/3");
      expect(mockedRandomQuestionCall).toHaveBeenCalledWith(
        secondReviewPointId,
      );
    });
  });
});
