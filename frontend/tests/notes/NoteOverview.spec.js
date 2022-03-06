/**
 * @jest-environment jsdom
 */

import NoteOverview from "@/components/notes/NoteOverview.vue";
import makeMe from "../fixtures/makeMe";
import { renderWithStoreAndMockRoute } from "../helpers";
import { screen } from "@testing-library/vue";
import store from "../fixtures/testingStore.js";

describe("note overview", () => {
  it("should render one note", async () => {
    const note = makeMe.aNote.title("single note").please();
    store.getters.ps().loadNotes([note]);
    renderWithStoreAndMockRoute(
      store,
      NoteOverview,
      { props: { noteId: note.id, expandChildren: true } },
    );
    expect(screen.getByRole("title")).toHaveTextContent("single note");
    expect(screen.getAllByRole("title")).toHaveLength(1);
  });

  it("should render one note with links", async () => {
    const note = makeMe.aNote.title("source").linkToSomeNote().please();
    store.getters.ps().loadNotes([note]);
    renderWithStoreAndMockRoute(
      store,
      NoteOverview,
      { props: { noteId: note.id, expandChildren: true } },
    );
    await screen.findByText("a tool");
  });

  it("should render note with one child", async () => {
    const noteParent = makeMe.aNote.title("parent").please();
    const noteChild = makeMe.aNote.title("child").under(noteParent).please();
    store.getters.ps().loadNotes([noteParent, noteChild]);
    renderWithStoreAndMockRoute(
      store,
      NoteOverview,
      { props: { noteId: noteParent.id, expandChildren: true } },
    );
    expect(screen.getAllByRole("title")).toHaveLength(2);
    await screen.findByText("parent");
    await screen.findByText("child");
  });

  it("should render note with grandchild", async () => {
    const noteParent = makeMe.aNote.title("parent").please();
    const noteChild = makeMe.aNote.title("child").under(noteParent).please();
    const noteGrandchild = makeMe.aNote.title("grandchild").under(noteChild).please();
    store.getters.ps().loadNotes([noteParent, noteChild, noteGrandchild]);
    renderWithStoreAndMockRoute(
      store,
      NoteOverview,
      { props: { noteId: noteParent.id, expandChildren: true } },
    );
    await screen.findByText("parent");
    await screen.findByText("child");
    await screen.findByText("grandchild");
  });

});
