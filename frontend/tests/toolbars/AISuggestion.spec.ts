import AISuggestion from "@/components/toolbars/AISuggestion.vue";
import { StoredApi } from "@/store/StoredApiCollection";
import { VueWrapper } from "@vue/test-utils";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

helper.resetWithApiMock(beforeEach, afterEach);

describe("AISuggestion", () => {
  helper.resetWithApiMock(beforeEach, afterEach);

  let wrapper: VueWrapper;

  const mountComponent = (
    note?: Generated.Note,
    updateTextContent?: StoredApi["updateTextContent"]
  ): VueWrapper => {
    return helper
      .component(AISuggestion)
      .withStorageProps({
        selectedNote: note,
        storageAccessor: {
          selectedNote: note,
          api: () => ({ updateTextContent }),
          peekUndo: () => [],
        },
      })
      .mount();
  };

  const mountComponentWithNote = (
    updateTextContent?: StoredApi["updateTextContent"]
  ) => {
    const note = makeMe.aNote.please();
    wrapper = mountComponent(note, updateTextContent);
  };

  it("has the suggest button when having selected note", () => {
    mountComponentWithNote();
    expect(wrapper.find(".btn").attributes("title")).toEqual("Suggest1");
  });

  it("ask api be called once when clicking the suggest button", () => {
    const updateTextContent: StoredApi["updateTextContent"] = async (
      _id,
      noteContent
    ) => {
      expect(noteContent.description).toBe("suggestion");
      return {} as Generated.NoteRealm;
    };

    mountComponentWithNote(updateTextContent);
    helper.apiMock
      .expectingPost(`/api/ai/ask-suggestions`)
      .andReturnOnce({ suggestion: "suggestion" });

    wrapper.find(".btn").trigger("click");
  });

  it('ask api be called many times until res.finishReason equal "stop" when clicking the suggest button', async () => {
    const updateTextContent: StoredApi["updateTextContent"] = async () => {
      helper.apiMock
        .expectingPost(`/api/ai/ask-suggestions`)
        .andReturnOnce({ suggestion: "suggestion", finishReason: "stop" });
      return {} as Generated.NoteRealm;
    };

    const mockFn = vi.fn().mockImplementation(updateTextContent);

    mountComponentWithNote(mockFn);

    helper.apiMock
      .expectingPost(`/api/ai/ask-suggestions`)
      .andReturnOnce({ suggestion: "suggestion", finishReason: "length" });

    await wrapper.find(".btn").trigger("click");

    setTimeout(() => expect(mockFn).toBeCalledTimes(2), 0);
  });
});
