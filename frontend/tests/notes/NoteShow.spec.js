import { render, screen } from "@testing-library/vue";
import NoteShow from "@/components/notes/NoteShow.vue";
import { createMockNote } from "./NoteShow-fixtures";

describe("new/updated pink banner", () => {
  const getBannerCount = async () =>
    screen.queryAllByText("This note has been changed recently.");

  test("should show pink banner if the note was updated within the last 12 hours", async () => {
    const note = createMockNote({ recentlyUpdated: true });
    const view = render(NoteShow, { props: note });

    expect(await getBannerCount()).toHaveLength(1);
  });

  test("should show 0 pink banners if the note was updated more than 12 hours ago", async () => {
    const note = createMockNote({ recentlyUpdated: false });
    const view = render(NoteShow, { props: note });

    expect(await getBannerCount()).toHaveLength(0);
  });
});
