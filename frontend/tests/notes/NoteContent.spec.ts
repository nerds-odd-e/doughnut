import { flushPromises } from "@vue/test-utils";
import NoteContent from "@/components/notes/NoteContent.vue";
import ManagedApi from "@/managedApi/ManagedApi";
import createNoteStorage from "../../src/store/createNoteStorage";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

describe("undo editing", () => {
  it("should call addEditingToUndoHistory on submitChange", async () => {
    const histories = createNoteStorage(
      new ManagedApi({ errors: [], states: [] })
    );

    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please();
    helper.apiMock.expectingPatch(`/api/text_content/${noteRealm.id}`);

    const updatedTitle = "updated";
    const wrapper = helper
      .component(NoteContent)
      .withProps({
        note: noteRealm.note,
        storageAccessor: histories,
      })
      .mount();

    await wrapper.find('[role="title"]').trigger("click");
    await wrapper.find('[role="title"] input').setValue(updatedTitle);
    await wrapper.find('[role="title"] input').trigger("blur");
    await flushPromises();

    expect(histories.peekUndo()).toMatchObject({ type: "editing" });
  });
});
