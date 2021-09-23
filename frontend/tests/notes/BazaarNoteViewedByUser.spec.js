/**
 * @jest-environment jsdom
 */
import NoteViewedByUser from "@/components/notes/NoteViewedByUser.vue";
import { noteViewedByUser } from "./fixtures";
import _ from "lodash";
import { mountWithStoreAndMockRoute } from "../helpers";

const noteView = _.merge(noteViewedByUser, {
  owns: false,
});

describe("a note not owned by viewer", () => {
  test("", async () => {
    const { wrapper } = mountWithStoreAndMockRoute(NoteViewedByUser, {
      propsData: noteView,
    });
    expect(wrapper.find(".breadcrumb").text()).toContain("Bazaar");
    expect(wrapper.findAll(".nav [title='Add to my learning']")).toHaveLength(
      1
    );
  });
});
