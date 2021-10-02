/**
 * @jest-environment jsdom
 */
import NoteViewedByUser from "@/components/notes/NoteViewedByUser.vue";
import store from "../../src/store/index.js";
import { screen } from "@testing-library/vue";
import _ from "lodash";
import { renderWithStoreAndMockRoute } from "../helpers";
import makeMe from "../fixtures/makeMe";

describe("a note not owned by viewer", () => {
  test("", async () => {
    const note = makeMe.aNote.please();
    const breadcrumb = makeMe.aBreadcrumb.inBazaar().please();
    store.commit("loadNotes", [note]);
    renderWithStoreAndMockRoute(
      store,
      NoteViewedByUser,
      {
        propsData: { id: note.id, breadcrumb },

      },
    );
    await screen.findByText("Bazaar");
    await screen.findByRole("button", { name: "Add to my learning" });
  });
});
