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
    const note = makeMe.aNote.title("single note").shortDescription('not long').please();
    store.commit("loadNotes", [note]);
    renderWithStoreAndMockRoute(
      store,
      NoteMinmap,
      { props: { noteId: note.id } },
    );
    expect(screen.getByRole("card")).toHaveTextContent("single note");
    expect(screen.getByRole("card")).toHaveTextContent("not long");
  });
})
