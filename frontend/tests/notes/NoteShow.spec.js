import { render, screen } from "@testing-library/vue";
import NoteShow from "@/components/notes/NoteShow.vue";
import { createMockNote } from "./NoteShow-fixtures";
import makeMe from "../fixtures/makeMe"

describe("new/updated pink banner", () => {
  const getBannerCount = async () =>
    screen.queryAllByText("This note has been changed recently.");

  test("should show pink banner if the note was updated within the last 12 hours", async () => {
    const note = makeMe.aNote.recentlyUpdated(true).please()
    render(NoteShow, { props: note });

    expect(await getBannerCount()).toHaveLength(1);
  });

  test("should show 0 pink banners if the note was updated more than 12 hours ago", async () => {
    const note = makeMe.aNote.recentlyUpdated(false).please()
    render(NoteShow, { props: note });

    expect(await getBannerCount()).toHaveLength(0);
  });
});
