/**
 * @jest-environment jsdom
 */
import Repetition from "@/components/review/Repetition.vue";
import {
  noteViewedByUser,
  linkViewedByUser,
  reviewPointViewedByUser,
} from "../notes/fixtures";
import { mountWithMockRoute } from "../helpers";

describe("repetition page", () => {
  beforeEach(async () => {
    fetch.resetMocks();
    fetch.mockResponseOnce(JSON.stringify({}));
  });

  describe("repetition page for a note", () => {
    const reviewPointForView = {
      ...reviewPointViewedByUser,
      noteViewedByUser: noteViewedByUser,
    };

    test("for note", async () => {
      const { wrapper } = mountWithMockRoute(
        Repetition,
        { propsData: reviewPointForView },
        { name: "root" }
      );
      expect(wrapper.findAll(".btn-toolbar")).toHaveLength(1);
    });
  });

  describe("repetition page for a link", () => {
    const reviewPointForView = {
      ...reviewPointViewedByUser,
      linkViewedByUser: linkViewedByUser,
    };

    test("for link", async () => {
      const { wrapper } = mountWithMockRoute(
        Repetition,
        { propsData: reviewPointForView },
        { name: "root" }
      );
      expect(wrapper.findAll(".btn-toolbar")).toHaveLength(1);
    });

    test("click on note when doing review", async () => {
      const { wrapper } = mountWithMockRoute(
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
      const { wrapper } = mountWithMockRoute(
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
