/**
 * @jest-environment jsdom
 */

import NoteCardsView from "@/components/notes/views/NoteCardsView.vue";
import { useStore } from "@/store/index.js";
import { screen } from "@testing-library/vue";
import { renderWithStoreAndMockRoute } from "../helpers";
import makeMe from "../fixtures/makeMe";
import { createTestingPinia } from "@pinia/testing";

describe("note wth child cards", () => {
  const pinia = createTestingPinia();
  const store = useStore(pinia);

  it("should render note with one child", async () => {
    const notePosition = makeMe.aNotePosition.please()
    const noteParent = makeMe.aNote.title("parent").please();
    const noteChild = makeMe.aNote.title("child").under(noteParent).please();
    store.loadNotes([noteParent, noteChild]);
    renderWithStoreAndMockRoute(
      pinia,
      NoteCardsView,
      { props: { noteId: noteParent.id, notePosition, expandChildren: true } },
    )
    expect(screen.getAllByRole("title")).toHaveLength(1);
    await screen.findByText("parent");
    await screen.findByText("child");
  })
});