import { render, screen } from "@testing-library/vue";
import NoteOverview from "@/components/notes/NoteOverview.vue";
import { createMockNote } from "./NoteShow-fixtures";
import { renderWithMockRoute } from "../helpers";

describe("note overview", () => {
  test("should show pink banner if the note was updated within the last 12 hours", async () => {
    const note = createMockNote({ recentlyUpdated: true });
    renderWithMockRoute(NoteOverview, { props: note });

    await screen.findByText('Desc')
  });

});
