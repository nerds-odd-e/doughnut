/**
 * @jest-environment jsdom
 */
import NoteViewedByUser from "@/components/notes/NoteViewedByUser.vue";
import { noteViewedByUser } from "./fixtures";
import { screen } from "@testing-library/vue";
import _ from "lodash";
import { renderWithStoreAndMockRoute } from "../helpers";
import makeMe from "../fixtures/makeMe";

const noteView = _.merge(noteViewedByUser, {
  owns: false,
});

describe("a note not owned by viewer", () => {
  test("", async () => {
    const noteView = makeMe.aNote.deprecatingInBazaar().please();
    renderWithStoreAndMockRoute(
      NoteViewedByUser,
      {
        propsData: noteView,
      },
      null,
      (store) => {
        store.commit("loadNotes", [noteView]);
      }
    );
    screen.findByText("Bazaar");
    // expect(wrapper.findAll(".nav [title='Add to my learning']")).toHaveLength(
  });
});
