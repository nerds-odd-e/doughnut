import { screen } from "@testing-library/vue";
import ControlCenter from "@/components/toolbars/ControlCenter.vue";
import ManagedApi from "@/managedApi/ManagedApi";
import { StoredApi } from "@/store/StoredApiCollection";
import { VueWrapper } from "@vue/test-utils";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";
import createNoteStorage, {
  StorageAccessor,
} from "../../src/store/createNoteStorage";
import NoteEditingHistory from "../../src/store/NoteEditingHistory";

helper.resetWithApiMock(beforeEach, afterEach);

describe("Note Control Center", () => {
  let noteEditingHistory: NoteEditingHistory;
  let histories: StorageAccessor;

  beforeEach(() => {
    noteEditingHistory = new NoteEditingHistory();
    histories = createNoteStorage(
      new ManagedApi({ states: [], errors: [] }),
      noteEditingHistory
    );
  });

  it("fetch API to be called ONCE", async () => {
    helper
      .component(ControlCenter)
      .withProps({ storageAccessor: histories, apiStatus: { states: [] } })
      .render();

    expect(screen.queryByTitle("undo")).toBeNull();
  });

  it("show undo when there is something to undo", async () => {
    const notebook = makeMe.aNotebook.please();
    noteEditingHistory.deleteNote(notebook.headNote.id);
    helper
      .component(ControlCenter)
      .withProps({ storageAccessor: histories, apiStatus: { states: [] } })
      .render();

    expect(await screen.findByTitle("undo delete note")).not.toBeDisabled();
  });
});

describe("ControlCenter with manual mock", () => {
  helper.resetWithApiMock(beforeEach, afterEach);

  let wrapper: VueWrapper;

  const mountComponent = (
    note?: Generated.Note,
    updateTextContent?: StoredApi["updateTextContent"]
  ): VueWrapper => {
    return helper
      .component(ControlCenter)
      .withStorageProps({
        user: makeMe.aUser().please(),
        apiStatus: { states: [], errors: [] },
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

  it("has only the link-note button when no exist selected note", () => {
    wrapper = mountComponent();
    expect(wrapper.findAll(".btn")[0].attributes("title")).toEqual(
      "search note"
    );
  });

  it("has the suggest button when having selected note", () => {
    mountComponentWithNote();
    expect(wrapper.findAll(".btn")[4].attributes("title")).toEqual("Suggest1");
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

    wrapper.findAll(".btn")[4].trigger("click");
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

    await wrapper.findAll(".btn")[4].trigger("click");

    setTimeout(() => expect(mockFn).toBeCalledTimes(2), 0);
  });
});
