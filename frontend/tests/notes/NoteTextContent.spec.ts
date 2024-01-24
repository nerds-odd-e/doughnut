import { VueWrapper, flushPromises } from "@vue/test-utils";
import { ComponentPublicInstance } from "vue";
import NoteTextContent from "@/components/notes/NoteTextContent.vue";
import TextContentWrapper from "@/components/notes/TextContentWrapper.vue";
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
        topicConstructor: n.topic,
        details: n.details,
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
      .spyOn(TextContentWrapper, "unmounted")
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
    helper.apiMock.expectingPatch(
      `/api/text_content/${note.id}/topic-constructor`,
    );
    wrapper.unmount();
  });

  const editTitle = async (wrapper: VueWrapper<ComponentPublicInstance>) => {
    await wrapper.find('[role="topic"]').trigger("click");
    await wrapper.find('[role="topic"] input').setValue("updated");
    await wrapper.find('[role="topic"] input').trigger("blur");
  };

  it("should save content when blur text field title", async () => {
    const wrapper = mountComponent(note);
    helper.apiMock.expectingPatch(
      `/api/text_content/${note.id}/topic-constructor`,
    );
    await editTitle(wrapper);
  });

  describe("saved and having error", () => {
    let wrapper: VueWrapper<ComponentPublicInstance>;
    beforeEach(async () => {
      wrapper = mountComponent(note);
      helper.apiMock
        .expectingPatch(`/api/text_content/${note.id}/topic-constructor`)
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
    });

    it("should display error when saving failed", async () => {
      expect(wrapper.find(".error-msg").text()).toBe(
        "size must be between 1 and 100",
      );
    });

    it("should clean up errors when editing", async () => {
      helper.apiMock.expectingPatch(
        `/api/text_content/${note.id}/topic-constructor`,
      );
      await editTitle(wrapper);
      await flushPromises();
      expect(wrapper.findAll(".error-msg")).toHaveLength(0);
    });
  });

  it("should not trigger changes for initial details content", async () => {
    note.details = "initial\n\ndescription";
    const wrapper = mountComponent(note);
    await flushPromises();
    wrapper.unmount();
    // no api calls expected. Test will fail if there is any.
    // because the initial details is not changed.
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
        .expectingPatch(`/api/text_content/${note.id}/topic-constructor`)
        .andRespondOnce({
          status: 401,
        });
      await editTitle(wrapper);
      await flushPromises();
      expect(wrapper.find(".error-msg").text()).toBe(
        "You are not authorized to edit this note. Perhaps you are not logged in?",
      );
    });
  });
});
