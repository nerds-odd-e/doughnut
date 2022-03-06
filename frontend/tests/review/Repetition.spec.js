/**
 * @jest-environment jsdom
 */
import Repetition from "@/components/review/Repetition.vue";
import store from "../fixtures/testingStore.js";
import { mountWithStoreAndMockRoute } from "../helpers";
import makeMe from "../fixtures/makeMe";

describe("repetition page", () => {
  beforeEach(async () => {
    fetch.resetMocks();
    fetch.mockResponseOnce(JSON.stringify({}));
  });

  describe("repetition page for a note", () => {
    const note = makeMe.aNote.please()
    const reviewPointForView = makeMe.aReviewPoint.ofNote(note).please()

    test("for note", async () => {
      const { wrapper } = mountWithStoreAndMockRoute(
        store,
        Repetition,
        { propsData: reviewPointForView },
        { name: "root" }
      );
      expect(wrapper.findAll(".btn-toolbar")).toHaveLength(1);
    });
  });

  describe("repetition page for a link", () => {
    const linkViewedByUser = makeMe.aLinkViewedByUser.please()
    const reviewPointForView = makeMe.aReviewPoint.ofLink(linkViewedByUser).please()

    test("for link", async () => {
      const { wrapper } = mountWithStoreAndMockRoute(
        store,
        Repetition,
        { propsData: reviewPointForView },
        { name: "root" }
      );
      expect(wrapper.findAll(".btn-toolbar")).toHaveLength(1);
    });

    test("click on note when doing review", async () => {
      const { wrapper } = mountWithStoreAndMockRoute(
        store,
        Repetition,
        { propsData: reviewPointForView },
        { name: "repeat" }
      );
      expect(
        JSON.parse(wrapper.find(".link-source .router-link").attributes().to)
          .name
      ).toEqual("notebooks");
    });

    test("click on note when doing review and in a nested page", async () => {
      const { wrapper } = mountWithStoreAndMockRoute(
        store,
        Repetition,
        { propsData: reviewPointForView },
        { name: "repeat-noteShow", params: { noteId: 123 } }
      );
      expect(
        JSON.parse(wrapper.find(".link-source .router-link").attributes().to)
      ).toEqual({ name: "notebooks" });
    });
  });
});
