/**
 * @jest-environment jsdom
 */
import storeUndoCommand from "../src/storeUndoCommand";
import store from "../src/store/index";
import makeMe from "./fixtures/makeMe";

describe("storeUndoCommand", () => {
  const note = makeMe.aNote.title("Dummy Title").please()

  describe("addUndoHistory", () => {
    beforeEach(() => {
      store.commit("loadNotes", [note]);
    });

    test("should push textContent into store state noteUndoHistories ",
        () => {
          storeUndoCommand.addUndoHistory(store,
              { noteId: note.id, });

          expect(store.state.noteUndoHistories.length).toEqual(1);
        });
  });

  describe('popUndoHistory', () => {
    const mockUpdatedNote = { noteId: note.id };
    let initialUndoCount;

    beforeEach(() => {
      store.commit('loadNotes', [note]);
      storeUndoCommand.addUndoHistory(store, mockUpdatedNote);
      initialUndoCount = store.state.noteUndoHistories.length;
    });

    it('should undo to last history', () => {
      storeUndoCommand.popUndoHistory(store);

      expect(store.state.noteUndoHistories.length).toEqual(initialUndoCount - 1);
    });

    it('should not undo to last history if there is no more history', () => {
      storeUndoCommand.popUndoHistory(store);
      storeUndoCommand.popUndoHistory(store);
      storeUndoCommand.popUndoHistory(store);

      expect(store.state.noteUndoHistories.length).toEqual(0);
    });
  })
});

