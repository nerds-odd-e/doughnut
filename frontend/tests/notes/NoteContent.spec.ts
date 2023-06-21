import { VueWrapper, flushPromises } from "@vue/test-utils";
import NoteContent from "@/components/notes/NoteContent.vue";
import ManagedApi from "@/managedApi/ManagedApi";
import { ComponentPublicInstance } from "vue";
import createNoteStorage from "../../src/store/createNoteStorage";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

describe("in place edit on title", () => {
  const note = makeMe.aNote.title("Dummy Title").please();
  let wrapper: VueWrapper<ComponentPublicInstance>;
  beforeEach(() => {
    wrapper = helper.component(NoteContent).withStorageProps({ note }).mount();
  });

  it("should display text field when one single click on title", async () => {
    expect(wrapper.findAll('[role="title"] input')).toHaveLength(0);
    await wrapper.find('[role="title"] h2').trigger("click");

    await flushPromises();

    expect(wrapper.findAll('[role="title"] input')).toHaveLength(1);
    expect(wrapper.findAll('[role="title"] h2')).toHaveLength(0);
  });

  it("should not save change when not unmount", async () => {
    await wrapper.find('[role="title"]').trigger("click");
    await wrapper.find('[role="title"] input').setValue("updated");
    // no api calls expected. Test will fail if there is any.
  });

  it("should save change when unmount", async () => {
    await wrapper.find('[role="title"]').trigger("click");
    await wrapper.find('[role="title"] input').setValue("updated");
    helper.apiMock.expectingPatch(`/api/text_content/${note.id}`);
    wrapper.unmount();
  });

  it("should save content when blur text field title", async () => {
    helper.apiMock.expectingPatch(`/api/text_content/${note.id}`);

    await wrapper.find('[role="title"]').trigger("click");
    await wrapper.find('[role="title"] input').setValue("updated");
    await wrapper.find('[role="title"] input').trigger("blur");
  });
});

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
