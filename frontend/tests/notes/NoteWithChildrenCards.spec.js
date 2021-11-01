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
      { props: { noteId: noteParent.id, notePosition, expandChildren: true } },
    )
    expect(screen.getAllByRole("title")).toHaveLength(1);
    await screen.findByText("parent");
    await screen.findByText("child");
  })
});