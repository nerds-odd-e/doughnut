/**
 * @jest-environment jsdom
 */
import store from "./fixtures/testingStore";
import makeMe from "./fixtures/makeMe";

describe("storeUndoCommand", () => {
  const note = makeMe.aNoteRealm.title("Dummy Title").please();

  describe("addEditingToUndoHistory", () => {
    beforeEach(() => {
      store.loadNoteRealms([note]);
    });

    it("should push textContent into store state noteUndoHistories ", () => {
      store.addEditingToUndoHistory({ noteId: note.id });

      expect(store.noteUndoHistories.length).toEqual(1);
    });
  });

  describe("popUndoHistory", () => {
    const mockUpdatedNote = { noteId: note.id };
    let initialUndoCount;

    beforeEach(() => {
      store.loadNoteRealms([note]);
      store.addEditingToUndoHistory(mockUpdatedNote);
      initialUndoCount = store.noteUndoHistories.length;
    });

    it("should undo to last history", () => {
      store.popUndoHistory();

      expect(store.noteUndoHistories.length).toEqual(initialUndoCount - 1);
    });

    it("should not undo to last history if there is no more history", () => {
      store.popUndoHistory();
      store.popUndoHistory();
      store.popUndoHistory();

      expect(store.noteUndoHistories.length).toEqual(0);
    });
  });
});
