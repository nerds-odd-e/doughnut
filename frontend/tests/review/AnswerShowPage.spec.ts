import { flushPromises } from "@vue/test-utils";
import AnsweredQuestionPage from "@/pages/AnsweredQuestionPage.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

describe("repetition page", () => {
  describe("repetition page for a link", () => {
    const link = makeMe.aLink.please();
    const reviewPoint = makeMe.aReviewPoint.ofLink(link).please();
    const notePosition = makeMe.aNotePosition.please();
    const mockedShowAnswerCall = vitest.fn();
    const mockedNotePositionCall = vitest.fn();

    beforeEach(async () => {
      vitest.resetAllMocks();
      helper.managedApi.restReviewsController.showAnswer =
        mockedShowAnswerCall.mockResolvedValue({
          answerResult: {
            answerId: 1,
            correct: true,
          },
          answerDisplay: "",
          reviewPoint,
        });
      helper.managedApi.restNoteController.getPosition =
        mockedNotePositionCall.mockResolvedValue(notePosition);
    });

    it("click on note when doing review", async () => {
      const wrapper = helper
        .component(AnsweredQuestionPage)
        .withStorageProps({ answerId: 1 })
        .currentRoute({ name: "repeat" })
        .mount();
      await flushPromises();
      wrapper.find(".review-point-abbr").trigger("click");
      await flushPromises();
      expect(mockedShowAnswerCall).toHaveBeenCalledWith(1);
      expect(mockedNotePositionCall).toHaveBeenCalledWith(
        reviewPoint.thing.note?.noteTopic.targetNoteTopic?.id,
      );
      expect(mockedNotePositionCall).toHaveBeenCalledWith(
        reviewPoint.thing.note?.parentId,
      );
      expect(
        JSON.parse(
          wrapper.find(".link-target .router-link").attributes().to as string,
        ).name,
      ).toEqual("notebooks");
    });

    it("click on note when doing review and in a nested page", async () => {
      const wrapper = helper
        .component(AnsweredQuestionPage)
        .withStorageProps({ answerId: 1 })
        .currentRoute({ name: "repeat-noteShow", params: { noteId: 123 } })
        .mount();
      await flushPromises();
      wrapper.find(".review-point-abbr").trigger("click");
      await flushPromises();
      expect(
        JSON.parse(
          wrapper.find(".link-target .router-link").attributes().to as string,
        ),
      ).toEqual({ name: "notebooks" });
    });
  });
});
