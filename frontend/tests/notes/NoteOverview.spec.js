/**
 * @jest-environment jsdom
 */
import { screen, within } from "@testing-library/vue";
import NoteOverview from "@/components/notes/NoteOverview.vue";
import store from "../../src/store/index.js";
import { renderWithStoreAndMockRoute } from "../helpers";
import makeMe from "../fixtures/makeMe";

describe("note overview", () => {
  it("should render one note", async () => {
    const note = makeMe.aNote.title("single note").please();
    store.commit("loadNotes", [note]);
    renderWithStoreAndMockRoute(
      store,
      NoteOverview,
      { props: { noteId: note.id } },
    );
    expect(screen.getByRole("title")).toHaveTextContent("single note");
    expect(screen.getAllByRole("title")).toHaveLength(1);
  });

  it("should render one note with links", async () => {
    const note = makeMe.aNote.title("source").linkTo().please();
    store.commit("loadNotes", [note]);
    renderWithStoreAndMockRoute(
      store,
      NoteOverview,
      { props: { noteId: note.id } },
    );
    await screen.findByText("a tool");
  });

  it("should render note with one child", async () => {
    const noteParent = makeMe.aNote.title("parent").please();
    const noteChild = makeMe.aNote.title("child").under(noteParent).please();
    store.commit("loadNotes", [noteParent, noteChild]);
    renderWithStoreAndMockRoute(
      store,
      NoteOverview,
      { props: { noteId: noteParent.id } },
    );
    expect(screen.getAllByRole("title")).toHaveLength(2);
    await screen.findByText("parent");
    await screen.findByText("child");
  });

  it("should render note with grandchild", async () => {
    const noteParent = makeMe.aNote.title("parent").please();
    const noteChild = makeMe.aNote.title("child").under(noteParent).please();
    const noteGrandchild = makeMe.aNote.title("grandchild").under(noteChild).please();
    store.commit("loadNotes", [noteParent, noteChild, noteGrandchild]);
    renderWithStoreAndMockRoute(
      store,
      NoteOverview,
      { props: { noteId: noteParent.id } },
    );
    await screen.findByText("parent");
    await screen.findByText("child");
    await screen.findByText("grandchild");
  });
});
