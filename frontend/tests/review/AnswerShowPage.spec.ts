import { flushPromises } from "@vue/test-utils";
import AnswerShowPage from "@/pages/AnswerShowPage.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

helper.resetWithApiMock(beforeEach, afterEach);

describe("repetition page", () => {
  describe("repetition page for a link", () => {
    const link = makeMe.aLink.please();
    const reviewPoint = makeMe.aReviewPoint.ofLink(link).please();
    const notePosition = makeMe.aNotePosition.please();

    beforeEach(async () => {
      helper.apiMock.expectingGet("/api/reviews/answers/1").andReturnOnce({
        answerId: 1,
        answerDisplay: "",
        correct: true,
        reviewPoint,
      });
      helper.apiMock
        .expectingGet(
          `/api/notes/${reviewPoint.thing.link?.targetNote.id}/position`
        )
        .andReturnOnce(notePosition);
      helper.apiMock.expectingGet(
        `/api/notes/${reviewPoint.thing.link?.sourceNote.id}/position`
      );
    });

    it("click on note when doing review", async () => {
      const wrapper = helper
        .component(AnswerShowPage)
        .withStorageProps({ answerId: 1 })
        .currentRoute({ name: "repeat" })
        .mount();
      await flushPromises();
      expect(
        JSON.parse(
          wrapper.find(".link-target .router-link").attributes().to as string
        ).name
      ).toEqual("notebooks");
    });

    it("click on note when doing review and in a nested page", async () => {
      const wrapper = helper
        .component(AnswerShowPage)
        .withStorageProps({ answerId: 1 })
        .currentRoute({ name: "repeat-noteShow", params: { noteId: 123 } })
        .mount();
      await flushPromises();
      expect(
        JSON.parse(
          wrapper.find(".link-target .router-link").attributes().to as string
        )
      ).toEqual({ name: "notebooks" });
    });
  });
});
