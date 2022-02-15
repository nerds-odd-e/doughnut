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
              {
                id: note.id,
                textContent: {
                  description: "Desc",
                  title: 'updatedTitle',
                  updatedAt: "2021-08-24T08:46:44.000+00:00"
                }
              });

          expect(store.state.noteUndoHistories.length).toEqual(1);
        });
  });

  describe('popUndoHistory', () => {
    const mockUpdatedNote = {
      id: note.id,
      textContent: {
        description: "Desc",
        title: 'updatedTitle',
        updatedAt: "2021-08-24T08:46:44.000+00:00"
      }
    };

    beforeEach(() => {
      store.commit('loadNotes', [note]);
      storeUndoCommand.addUndoHistory(store, mockUpdatedNote);
    });

    it('should undo to last history', () => {
      storeUndoCommand.popUndoHistory(store);

      expect(store.state.noteUndoHistories.length).toEqual(1);
    });

    it('should not undo to last history if there is no more history', () => {
      storeUndoCommand.popUndoHistory(store);
      storeUndoCommand.popUndoHistory(store);

      expect(store.state.noteUndoHistories.length).toEqual(0);
    });
  })
});

