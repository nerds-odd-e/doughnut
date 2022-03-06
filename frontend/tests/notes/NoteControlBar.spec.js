/**
 * @jest-environment jsdom
 */
import Breadcrumb from "@/components/notes/Breadcrumb.vue";
import { useStore } from "@/store";
import { screen } from "@testing-library/vue";
import { renderWithStoreAndMockRoute } from "../helpers";
import makeMe from "../fixtures/makeMe";
import { createTestingPinia } from "@pinia/testing";

describe("note wth child cards", () => {
  const pinia = createTestingPinia();
  const store = useStore(pinia);

  it("view note belongs to other people in bazaar", async () => {
    const note = makeMe.aNote.please();
    const notePosition = makeMe.aNotePosition.inBazaar().please();
    store.loadNotes([note]);
    renderWithStoreAndMockRoute(
      pinia,
      Breadcrumb,
      {
        propsData: notePosition,

      },
    );
    await screen.findByText("Bazaar");
  });
});