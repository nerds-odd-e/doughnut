/**
 * @jest-environment jsdom
 */
import { screen } from "@testing-library/vue";
import NoteControlCenter from "@/components/toolbars/NoteControlCenter.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";
import createNoteStorage, {
  NoteEditingHistory,
  StorageAccessor,
} from "../../src/store/createNoteStorage";

helper.resetWithApiMock(beforeEach, afterEach);

describe("Note Control Center", () => {
  let noteEditingHistory: NoteEditingHistory;
  let histories: StorageAccessor;

  beforeEach(() => {
    noteEditingHistory = new NoteEditingHistory();
    histories = createNoteStorage(noteEditingHistory);
  });

  it("fetch API to be called ONCE", async () => {
    helper
      .component(NoteControlCenter)
      .withProps({ storageAccessor: histories })
      .render();

    expect(await screen.findByTitle("undo")).toBeDisabled();
  });

  it("show undo when there is something to undo", async () => {
    const notebook = makeMe.aNotebook.please();
    noteEditingHistory.deleteNote(notebook.headNote.id);
    helper
      .component(NoteControlCenter)
      .withProps({ storageAccessor: histories })
      .render();

    expect(await screen.findByTitle("undo delete note")).not.toBeDisabled();
  });
});
