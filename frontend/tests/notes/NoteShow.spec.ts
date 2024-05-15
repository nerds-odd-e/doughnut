import { flushPromises } from "@vue/test-utils";
import { screen } from "@testing-library/vue";
import NoteShow from "@/components/notes/NoteShow.vue";
import { NoteRealm } from "@/generated/backend";
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

describe("note wth children", () => {
  const note = makeMe.aNoteRealm.please();

  const render = (n: NoteRealm) => {
    helper.managedApi.restNoteController.show1 = vitest
      .fn()
      .mockResolvedValue(n);
    helper
      .component(NoteShow)
      .withStorageProps({
        noteId: n.id,
        expandChildren: true,
      })
      .render();
  };

  it("should call the api", async () => {
    render(note);
    expect(helper.managedApi.restNoteController.show1).toBeCalledWith(note.id);
  });

  it("should not render children control if no child", async () => {
    render(note);
    await flushPromises();
    expect(screen.queryAllByTitle("collapse children")).toHaveLength(0);
  });

  describe("with children", () => {
    const parentNote = makeMe.aNoteRealm.topicConstructor("parent").please();
    makeMe.aNoteRealm.topicConstructor("child").under(parentNote).please();

    it("should not render children control if no child", async () => {
      render(parentNote);
      await flushPromises();
      expect(screen.queryAllByTitle("collapse children")).toHaveLength(1);
    });

    it("should render note with one child", async () => {
      render(parentNote);
      await screen.findByText("parent");
      await screen.findByText("child");
      expect(screen.getAllByRole("topic")).toHaveLength(1);
    });
  });
});
