import { VueWrapper, flushPromises } from "@vue/test-utils";
import { ComponentPublicInstance } from "vue";
import TextContentWrapper from "@/components/notes/core/TextContentWrapper.vue";
import { Note } from "@/generated/backend";
import NoteTextContent from "@/components/notes/core/NoteTextContent.vue";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

const mockedUpdateTopicCall = vi.fn();

describe("in place edit on title", () => {
  const note = makeMe.aNote.topicConstructor("Dummy Title").please();
  const mountComponent = (n: Note): VueWrapper<ComponentPublicInstance> => {
    return helper
      .component(NoteTextContent)
      .withStorageProps({
        note: n,
      })
      .mount();
  };

  beforeEach(() => {
    vi.resetAllMocks();
    helper.managedApi.restTextContentController.updateNoteTopicConstructor =
      mockedUpdateTopicCall;
  });

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
    wrapper.unmount();
    expect(mockedUpdateTopicCall).toBeCalledWith(note.id, {
      topicConstructor: "updated",
    });
  });

  const editTitle = async (
    wrapper: VueWrapper<ComponentPublicInstance>,
    newValue: string,
  ) => {
    await wrapper.find('[role="topic"]').trigger("click");
    await wrapper.find('[role="topic"] input').setValue(newValue);
  };

  const editTitleThenBlur = async (
    wrapper: VueWrapper<ComponentPublicInstance>,
  ) => {
    await editTitle(wrapper, "updated");
    await wrapper.find('[role="topic"] input').trigger("blur");
  };

  it("should save content when blur text field title", async () => {
    const wrapper = mountComponent(note);
    await editTitle(wrapper, "updated");
    await wrapper.find('[role="topic"] input').trigger("blur");
    expect(mockedUpdateTopicCall).toBeCalledWith(note.id, {
      topicConstructor: "updated",
    });
  });

  it("should not change content if there's unsaved changed", async () => {
    const wrapper = mountComponent(note);
    await editTitle(wrapper, "updated");

    await wrapper.setProps({
      note: { ...note, topicConstructor: "different value" },
    });
    expect(
      wrapper.find<HTMLInputElement>('[role="topic"] input').element.value,
    ).toBe("updated");

    expect(mockedUpdateTopicCall).not.toBeCalled();
  });

  it("should change content if there's no unsaved changed but change from prop", async () => {
    const wrapper = mountComponent(note);
    await wrapper.setProps({
      note: {
        ...note,
        noteTopic: { ...note.noteTopic, topicConstructor: "different value" },
      },
    });
    await wrapper.find('[role="topic"]').trigger("click");
    expect(
      wrapper.find<HTMLInputElement>('[role="topic"] input').element.value,
    ).toBe("different value");
  });

  describe("saved and having error", () => {
    let wrapper: VueWrapper<ComponentPublicInstance>;
    beforeEach(async () => {
      wrapper = mountComponent(note);
      mockedUpdateTopicCall.mockRejectedValueOnce(
        makeMe.anApiError
          .ofBindingError({
            topic: "size must be between 1 and 100",
          })
          .please(),
      );
      await editTitleThenBlur(wrapper);
      await flushPromises();
    });

    it("should display error when saving failed", async () => {
      expect(wrapper.find(".error-msg").text()).toBe(
        "size must be between 1 and 100",
      );
    });

    it("should clean up errors when editing", async () => {
      await editTitleThenBlur(wrapper);
      await flushPromises();
      expect(wrapper.findAll(".error-msg")).toHaveLength(0);
      expect(mockedUpdateTopicCall).toBeCalledTimes(2);
    });
  });

  it("should not trigger changes for initial details content", async () => {
    note.details = "initial\n\ndescription";
    const wrapper = mountComponent(note);
    await flushPromises();
    wrapper.unmount();
    expect(mockedUpdateTopicCall).toBeCalledTimes(0);
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
      mockedUpdateTopicCall.mockRejectedValueOnce(
        makeMe.anApiError.of401().please(),
      );
      await editTitleThenBlur(wrapper);
      await flushPromises();
      expect(wrapper.find(".error-msg").text()).toBe(
        "You are not authorized to edit this note. Perhaps you are not logged in?",
      );
    });
  });
});
