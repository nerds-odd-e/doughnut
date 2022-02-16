/**
 * @jest-environment jsdom
 */
import storeUndoCommand from "../src/storeUndoCommand";
import store from "../src/store/index";
import makeMe from "./fixtures/makeMe";

describe("storeUndoCommand", () => {
  const note = makeMe.aNote.title("Dummy Title").please()

  describe("addEditingToUndoHistory", () => {
    beforeEach(() => {
      store.commit("loadNotes", [note]);
    });

    test("should push textContent into store state noteUndoHistories ",
        () => {
          storeUndoCommand.addEditingToUndoHistory(store,
              { noteId: note.id, });

          expect(store.state.noteUndoHistories.length).toEqual(1);
        });
  });

  describe('popUndoHistory', () => {
    const mockUpdatedNote = { noteId: note.id };
    let initialUndoCount;

    beforeEach(() => {
      store.commit('loadNotes', [note]);
      storeUndoCommand.addEditingToUndoHistory(store, mockUpdatedNote);
      initialUndoCount = store.state.noteUndoHistories.length;
    });

    it('should undo to last history', () => {
      store.commit('popUndoHistory');

      expect(store.state.noteUndoHistories.length).toEqual(initialUndoCount - 1);
    });

    it('should not undo to last history if there is no more history', () => {
      store.commit('popUndoHistory');
      store.commit('popUndoHistory');
      store.commit('popUndoHistory');

      expect(store.state.noteUndoHistories.length).toEqual(0);
    });
  })
});

