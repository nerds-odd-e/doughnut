import { VueWrapper, flushPromises } from "@vue/test-utils";
import { ComponentPublicInstance } from "vue";
import NoteTextContent from "@/components/notes/NoteTextContent.vue";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

describe("in place edit on title", () => {
  const note = makeMe.aNote.topic("Dummy Title").please();
  const mountComponent = (
    n: Generated.Note,
  ): VueWrapper<ComponentPublicInstance> => {
    return helper
      .component(NoteTextContent)
      .withStorageProps({
        noteId: n.id,
        textContent: { topic: n.topic, details: n.details },
      })
      .mount();
  };

  it("should display text field when one single click on title", async () => {
    const wrapper = mountComponent(note);
    expect(wrapper.findAll('[role="topic"] input')).toHaveLength(0);
    await wrapper.find('[role="topic"] h2').trigger("click");

    await flushPromises();

    expect(wrapper.findAll('[role="topic"] input')).toHaveLength(1);
    expect(wrapper.findAll('[role="topic"] h2')).toHaveLength(0);
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
    await wrapper.find('[role="topic"]').trigger("click");
    await wrapper.find('[role="topic"] input').setValue("updated");
    wrapper.unmount();
    mockUnmounted.mockRestore();
  });

  it("should save change when unmount", async () => {
    const wrapper = mountComponent(note);
    await wrapper.find('[role="topic"]').trigger("click");
    await wrapper.find('[role="topic"] input').setValue("updated");
    helper.apiMock.expectingPatch(`/api/text_content/${note.id}`);
    wrapper.unmount();
  });

  const editTitle = async (wrapper: VueWrapper<ComponentPublicInstance>) => {
    await wrapper.find('[role="topic"]').trigger("click");
    await wrapper.find('[role="topic"] input').setValue("updated");
    await wrapper.find('[role="topic"] input').trigger("blur");
  };

  it("should save content when blur text field title", async () => {
    const wrapper = mountComponent(note);
    helper.apiMock.expectingPatch(`/api/text_content/${note.id}`);
    await editTitle(wrapper);
  });

  it("should display error when saving failed", async () => {
    const wrapper = mountComponent(note);
    helper.apiMock
      .expectingPatch(`/api/text_content/${note.id}`)
      .andRespondOnce({
        status: 400,
        body: JSON.stringify({
          message: "binding error",
          errors: {
            topic: "size must be between 1 and 100",
          },
          errorType: "BINDING_ERROR",
        }),
      });
    await editTitle(wrapper);
    await flushPromises();
    const { errors } = wrapper.vm.$data as { errors: { topic: string } };
    expect(errors.topic).toBe("size must be between 1 and 100");
    expect(wrapper.find(".error-msg").text()).toBe(
      "size must be between 1 and 100",
    );
  });

  it("should clean up errors when editing", async () => {
    const wrapper = mountComponent(note);
    const { errors } = wrapper.vm.$data as { errors: { topic: string } };
    errors.topic = "size must be between 1 and 100";
    helper.apiMock.expectingPatch(`/api/text_content/${note.id}`);
    await editTitle(wrapper);
    await flushPromises();
    const { errors: expectedErrors } = wrapper.vm.$data as {
      errors: { topic: string };
    };
    expect(expectedErrors.topic).toBeUndefined();
  });

  it("should not trigger changes for initial details content", async () => {
    note.details = "initial\n\ndescription";
    const wrapper = mountComponent(note);
    await flushPromises();
    wrapper.unmount();
    // no api calls expected. Test will fail if there is any.
    // because the initial details is not changed.
  });

  it("when there is pending changes not saved but the previous change trigger refresh", async () => {
    note.details = "initial\n\ndescription";
    const wrapper = mountComponent(note);
    await wrapper.find('[role="topic"]').trigger("click");
    await wrapper.find('[role="topic"] input').setValue("updated");
    await wrapper.setProps({ textContent: { topic: "change from outside." } });
    expect(
      wrapper.find<HTMLInputElement>('[role="topic"] input').element.value,
    ).toBe("updated");
    helper.apiMock.expectingPatch(`/api/text_content/${note.id}`);
  });

  describe("with mocked window.confirm", () => {
    // eslint-disable-next-line no-alert
    const jsdomConfirm = window.confirm;
    beforeEach(() => {
      // eslint-disable-next-line no-alert
      window.confirm = () => false;
    });

    afterEach(() => {
      // eslint-disable-next-line no-alert
      window.confirm = jsdomConfirm;
    });

    it("should display error when no authorization to save", async () => {
      const wrapper = mountComponent(note);
      helper.apiMock
        .expectingPatch(`/api/text_content/${note.id}`)
        .andRespondOnce({
          status: 401,
        });
      await editTitle(wrapper);
      await flushPromises();
      const { errors } = wrapper.vm.$data as { errors: { topic: string } };
      expect(errors.topic).toBe(
        "You are not authorized to edit this note. Perhaps you are not logged in?",
      );
    });
  });
});
