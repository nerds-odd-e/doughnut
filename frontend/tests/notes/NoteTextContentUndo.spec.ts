import { flushPromises } from "@vue/test-utils";
import NoteTextContent from "@/components/notes/core/NoteTextContent.vue";
import ManagedApi from "@/managedApi/ManagedApi";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";
import createNoteStorage from "../../src/store/createNoteStorage";

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
      .component(NoteTextContent)
      .withProps({
        note: noteRealm.note,
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
