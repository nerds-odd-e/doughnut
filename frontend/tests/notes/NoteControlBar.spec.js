/**
 * @jest-environment jsdom
 */
import { screen } from "@testing-library/vue";
import Breadcrumb from "@/components/notes/Breadcrumb.vue";
import store from "../fixtures/testingStore.js";
import { renderWithStoreAndMockRoute } from "../helpers";
import makeMe from "../fixtures/makeMe";

describe("note wth child cards", () => {

  it("view note belongs to other people in bazaar", async () => {
    const note = makeMe.aNote.please();
    const notePosition = makeMe.aNotePosition.inBazaar().please();
    store.commit("loadNotes", [note]);
    renderWithStoreAndMockRoute(
      store,
      Breadcrumb,
      {
        propsData: notePosition,

      },
    );
    await screen.findByText("Bazaar");
  });
});