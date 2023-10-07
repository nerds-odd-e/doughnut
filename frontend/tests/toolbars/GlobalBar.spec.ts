import { flushPromises } from "@vue/test-utils";
import { screen } from "@testing-library/vue";
import ManagedApi from "@/managedApi/ManagedApi";
import GlobalBar from "@/components/toolbars/GlobalBar.vue";
import NoteEditingHistory from "@/store/NoteEditingHistory";
import createNoteStorage, { StorageAccessor } from "@/store/createNoteStorage";
import makeMe from "../fixtures/makeMe";
import usePopups, {
  PopupInfo,
} from "../../src/components/commons/Popups/usePopups";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

describe("global bar", () => {
  let noteEditingHistory: NoteEditingHistory;
  let histories: StorageAccessor;
  let user: Generated.User;

  beforeEach(() => {
    user = makeMe.aUser().please();
    noteEditingHistory = new NoteEditingHistory();
    histories = createNoteStorage(
      new ManagedApi({ states: [], errors: [] }),
      noteEditingHistory,
    );
  });

  const popupInfo = [] as PopupInfo[];
  beforeEach(() => {
    usePopups().popups.register({ popupInfo });
  });

  it("opens the circles selection", async () => {
    const wrapper = helper
      .component(GlobalBar)
      .withProps({
        storageAccessor: histories,
        user,
        apiStatus: { states: [] },
      })
      .mount();
    wrapper.find("[role='button']").trigger("click");
    await flushPromises();
    expect(popupInfo).toHaveLength(1);
  });

  it("fetch API to be called ONCE", async () => {
    helper
      .component(GlobalBar)
      .withProps({
        storageAccessor: histories,
        user,
        apiStatus: { states: [] },
      })
      .render();

    expect(screen.queryByTitle("undo")).toBeNull();
  });

  it("show undo when there is something to undo", async () => {
    const notebook = makeMe.aNotebook.please();
    noteEditingHistory.deleteNote(notebook.headNote.id);
    helper
      .component(GlobalBar)
      .withProps({
        storageAccessor: histories,
        user,
        apiStatus: { states: [] },
      })
      .render();

    expect(await screen.findByTitle("undo delete note")).not.toBeDisabled();
  });
});
