/**
 * @jest-environment jsdom
 */
import { screen } from "@testing-library/vue";
import NoteMinmap from "@/components/notes/NoteMindmap.vue";
import store from "../../src/store/index.js";
import { renderWithStoreAndMockRoute } from "../helpers";
import makeMe from "../fixtures/makeMe";

describe("note mindmap", () => {
  it("should render one note", async () => {
    const note = makeMe.aNote.title("single note").please();
    store.commit("loadNotes", [note]);
    renderWithStoreAndMockRoute(
      store,
      NoteMinmap,
      { props: { noteId: note.id } },
    );
    expect(screen.getByRole("title")).toHaveTextContent("single note");
    expect(screen.getAllByRole("title")).toHaveLength(1);
  });
})
