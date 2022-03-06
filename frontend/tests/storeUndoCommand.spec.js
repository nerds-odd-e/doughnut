/**
 * @jest-environment jsdom
 */
import store from "./fixtures/testingStore.js";
import makeMe from "./fixtures/makeMe";

describe("storeUndoCommand", () => {
  const note = makeMe.aNote.title("Dummy Title").please()

  describe("addEditingToUndoHistory", () => {
    beforeEach(() => {
      store.getters.ps().loadNotes([note]);
    });

    test("should push textContent into store state noteUndoHistories ",
        () => {
          store.getters.ps().addEditingToUndoHistory( { noteId: note.id, });

          expect(store.state.piniaStore.noteUndoHistories.length).toEqual(1);
        });
  });

  describe('popUndoHistory', () => {
    const mockUpdatedNote = { noteId: note.id };
    let initialUndoCount;

    beforeEach(() => {
      store.getters.ps().loadNotes([note]);
      store.getters.ps().addEditingToUndoHistory( mockUpdatedNote);
      initialUndoCount = store.state.piniaStore.noteUndoHistories.length;
    });

    it('should undo to last history', () => {
      store.getters.ps().popUndoHistory();

      expect(store.state.piniaStore.noteUndoHistories.length).toEqual(initialUndoCount - 1);
    });

    it('should not undo to last history if there is no more history', () => {
      store.getters.ps().popUndoHistory();
      store.getters.ps().popUndoHistory();
      store.getters.ps().popUndoHistory();

      expect(store.state.piniaStore.noteUndoHistories.length).toEqual(0);
    });
  })
});

