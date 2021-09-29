/**
 * @jest-environment jsdom
 */
import NoteViewedByUser from "@/components/notes/NoteViewedByUser.vue";
import { screen } from "@testing-library/vue";
import _ from "lodash";
import { renderWithStoreAndMockRoute } from "../helpers";
import makeMe from "../fixtures/makeMe";

describe("a note not owned by viewer", () => {
  test("", async () => {
    const noteView = makeMe.aNote.deprecatingInBazaar().please();
    const breadcrumb = makeMe.aBreadcrumb.inBazaar().please();
    renderWithStoreAndMockRoute(
      NoteViewedByUser,
      {
        propsData: { id: noteView.id, breadcrumb },

      },
      null,
      (store) => {
        store.commit("loadNotes", [noteView]);
      }
    );
    await screen.findByText("Bazaar");
    await screen.findByRole("button", { name: "Add to my learning" });
  });
});
