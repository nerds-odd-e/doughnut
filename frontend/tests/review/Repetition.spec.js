/**
 * @jest-environment jsdom
 */
import Repetition from "@/components/review/Repetition.vue";
import { useStore } from "@/store";
import { mountWithStoreAndMockRoute } from "../helpers";
import makeMe from "../fixtures/makeMe";
import { createTestingPinia } from "@pinia/testing";

describe("repetition page", () => {
  const pinia = createTestingPinia();
  beforeEach(async () => {
    fetch.resetMocks();
    fetch.mockResponseOnce(JSON.stringify({}));
  });

  describe("repetition page for a note", () => {
    const note = makeMe.aNote.please()
    const reviewPointForView = makeMe.aReviewPoint.ofNote(note).please()

    it("for note", async () => {
      const { wrapper } = mountWithStoreAndMockRoute(
        useStore(pinia),
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

    it("for link", async () => {
      const { wrapper } = mountWithStoreAndMockRoute(
        useStore(pinia),
        Repetition,
        { propsData: reviewPointForView },
        { name: "root" }
      );
      expect(wrapper.findAll(".btn-toolbar")).toHaveLength(1);
    });

    it("click on note when doing review", async () => {
      const { wrapper } = mountWithStoreAndMockRoute(
        useStore(pinia),
        Repetition,
        { propsData: reviewPointForView },
        { name: "repeat" }
      );
      expect(
        JSON.parse(wrapper.find(".link-source .router-link").attributes().to)
          .name
      ).toEqual("notebooks");
    });

    it("click on note when doing review and in a nested page", async () => {
      const { wrapper } = mountWithStoreAndMockRoute(
        useStore(pinia),
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
