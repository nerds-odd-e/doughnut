/**
 * @jest-environment jsdom
 */
import { screen } from "@testing-library/vue";
import NoteWithChildrenCards from "@/components/notes/NoteWithChildrenCards.vue";
import store from "../../src/store/index.js";
import { renderWithStoreAndMockRoute } from "../helpers";
import makeMe from "../fixtures/makeMe";

describe("note wth child cards", () => {

  it("should render note with one child", async () => {
    const notePosition = makeMe.aNotePosition.please()
    const noteParent = makeMe.aNote.title("parent").please();
    const noteChild = makeMe.aNote.title("child").under(noteParent).please();
    store.commit("loadNotes", [noteParent, noteChild]);
    renderWithStoreAndMockRoute(
      store,
      NoteWithChildrenCards,
      { props: { id: noteParent.id, notePosition } },
    )
    expect(screen.getAllByRole("title")).toHaveLength(1);
    await screen.findByText("parent");
    await screen.findByText("child");
  })

  it("view note belongs to other people in bazaar", async () => {
    const note = makeMe.aNote.please();
    const notePosition = makeMe.aNotePosition.inBazaar().please();
    store.commit("loadNotes", [note]);
    renderWithStoreAndMockRoute(
      store,
      NoteWithChildrenCards,
      {
        propsData: { id: note.id, notePosition },

      },
    );
    await screen.findByText("Bazaar");
  });
});