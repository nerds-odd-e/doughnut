/**
 * @jest-environment jsdom
 */
import { screen } from "@testing-library/vue";
import NoteCard from "@/components/notes/NoteCard.vue";
import MindmapSector from "@/components/notes/MindmapSector.ts";
import makeMe from "../fixtures/makeMe";
import { renderWithMockRoute } from "../helpers";

describe("note mindmap", () => {
  it("render single note", async () => {
    const note = makeMe.aNote.title("single note").shortDescription('not long').please();
    const mindmapSector = new MindmapSector(0, 0, 0, 0)
    renderWithMockRoute(
      NoteCard,
      { props: { note, mindmapSector } },
    );
    expect(screen.getByRole("card")).toHaveTextContent("single note");
    expect(screen.getByRole("card")).toHaveTextContent("not long");
  });
})
