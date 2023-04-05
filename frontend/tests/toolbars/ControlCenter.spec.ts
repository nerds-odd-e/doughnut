import { screen } from "@testing-library/vue";
import ControlCenter from "@/components/toolbars/ControlCenter.vue";
import ManagedApi from "@/managedApi/ManagedApi";
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
