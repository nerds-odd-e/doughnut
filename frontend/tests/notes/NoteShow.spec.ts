import { flushPromises } from "@vue/test-utils";
import { screen } from "@testing-library/vue";
import NoteShow from "@/components/notes/NoteShow.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

describe("new/updated pink banner", () => {
  beforeAll(() => {
    Date.now = vi.fn(() => new Date(Date.UTC(2017, 1, 14)).valueOf());
  });

  it.each([
    [new Date(Date.UTC(2017, 1, 15)), "rgb(208,237,23)"],
    [new Date(Date.UTC(2017, 1, 13)), "rgb(189,209,64)"],
    [new Date(Date.UTC(2017, 1, 12)), "rgb(181,197,82)"],
    [new Date(Date.UTC(2016, 1, 12)), "rgb(150,150,150)"],
  ])(
    "should show fresher color if recently updated",
    async (updatedAt, expectedColor) => {
      const note = makeMe.aNoteRealm.updatedAt(updatedAt).please();
      helper.managedApi.restNoteController.show1 = vitest
        .fn()
        .mockResolvedValue(note);

      const wrapper = helper
        .component(NoteShow)
        .withStorageProps({
          noteId: note.id,
          expandChildren: true,
          readonly: false,
        })
        .mount();
      await flushPromises();
      expect(wrapper.find(".note-recent-update-indicator").element).toHaveStyle(
        `background-color: ${expectedColor};`,
      );
    },
  );
});

describe("note wth child cards", () => {
  it("should render note with one child", async () => {
    const noteParent = makeMe.aNoteRealm.topicConstructor("parent").please();
    makeMe.aNoteRealm.topicConstructor("child").under(noteParent).please();
    helper.managedApi.restNoteController.show1 = vitest
      .fn()
      .mockResolvedValue(noteParent);
    helper
      .component(NoteShow)
      .withStorageProps({
        noteId: noteParent.id,
        expandChildren: true,
      })
      .render();
    await screen.findByText("parent");
    await screen.findByText("child");
    expect(screen.getAllByRole("topic")).toHaveLength(1);
    expect(helper.managedApi.restNoteController.show1).toBeCalledWith(
      noteParent.id,
    );
  });
});
