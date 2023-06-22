import { VueWrapper, flushPromises } from "@vue/test-utils";
import { ComponentPublicInstance } from "vue";
import NoteTextContent from "@/components/notes/NoteTextContent.vue";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

describe("in place edit on title", () => {
  const note = makeMe.aNote.title("Dummy Title").please();
  const mountComponent = (
    n: Generated.Note
  ): VueWrapper<ComponentPublicInstance> => {
    return helper
      .component(NoteTextContent)
      .withStorageProps({ noteId: n.id, textContent: n.textContent })
      .mount();
  };

  it("should display text field when one single click on title", async () => {
    const wrapper = mountComponent(note);
    expect(wrapper.findAll('[role="title"] input')).toHaveLength(0);
    await wrapper.find('[role="title"] h2').trigger("click");

    await flushPromises();

    expect(wrapper.findAll('[role="title"] input')).toHaveLength(1);
    expect(wrapper.findAll('[role="title"] h2')).toHaveLength(0);
  });

  it("should not save change when not unmount", async () => {
    // because the components always get unmounted after each test
    // we simulate the before unmount siutation by replacing the unmounted method
    // with an empty function.

    const mockUnmounted = vitest
      .spyOn(NoteTextContent, "unmounted")
      // eslint-disable-next-line @typescript-eslint/no-empty-function
      .mockImplementation(() => {});
    const wrapper = mountComponent(note);
    await wrapper.find('[role="title"]').trigger("click");
    await wrapper.find('[role="title"] input').setValue("updated");
    wrapper.unmount();
    mockUnmounted.mockRestore();
  });

  it("should save change when unmount", async () => {
    const wrapper = mountComponent(note);
    await wrapper.find('[role="title"]').trigger("click");
    await wrapper.find('[role="title"] input').setValue("updated");
    helper.apiMock.expectingPatch(`/api/text_content/${note.id}`);
    wrapper.unmount();
  });

  it("should save content when blur text field title", async () => {
    const wrapper = mountComponent(note);
    helper.apiMock.expectingPatch(`/api/text_content/${note.id}`);

    await wrapper.find('[role="title"]').trigger("click");
    await wrapper.find('[role="title"] input').setValue("updated");
    await wrapper.find('[role="title"] input').trigger("blur");
  });

  it("should not trigger changes for initial description content", async () => {
    note.textContent.description = "initial\n\ndescription";
    const wrapper = mountComponent(note);
    await flushPromises();
    wrapper.unmount();
    // no api calls expected. Test will fail if there is any.
    // because the initial description is not changed.
  });
});
