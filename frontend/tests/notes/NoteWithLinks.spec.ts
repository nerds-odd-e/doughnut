import { flushPromises } from "@vue/test-utils";
import NoteWithLinks from "@/components/notes/core/NoteWithLinks.vue";
import ManagedApi from "@/managedApi/ManagedApi";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";
import createNoteStorage from "../../src/store/createNoteStorage";

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
    (updatedAt, expectedColor) => {
      const note = makeMe.aNoteRealm.updatedAt(updatedAt).please();

      const wrapper = helper
        .component(NoteWithLinks)
        .withStorageProps({
          note: note.note,
          links: note.links,
          readonly: false,
        })
        .mount();

      expect(wrapper.find(".note-body").element).toHaveStyle(
        `background-color: ${expectedColor};`,
      );
    },
  );
});

describe("undo editing", () => {
  it("should call addEditingToUndoHistory on submitChange", async () => {
    const histories = createNoteStorage(
      new ManagedApi({ errors: [], states: [] }),
    );

    const noteRealm = makeMe.aNoteRealm
      .topicConstructor("Dummy Title")
      .please();
    histories.refreshNoteRealm(noteRealm);

    const updatedTitle = "updated";
    const wrapper = helper
      .component(NoteWithLinks)
      .withProps({
        note: noteRealm.note,
        links: noteRealm.links,
        storageAccessor: histories,
      })
      .mount();

    await wrapper.find('[role="topic"]').trigger("click");
    await wrapper.find('[role="topic"] input').setValue(updatedTitle);
    await wrapper.find('[role="topic"] input').trigger("blur");
    await flushPromises();

    expect(histories.peekUndo()).toMatchObject({ type: "edit topic" });
  });
});
