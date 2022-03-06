/**
 * @jest-environment jsdom
 */
import { useStore } from "@/store";
import makeMe from "./fixtures/makeMe";
import { setActivePinia, createPinia } from "pinia";

describe("storeUndoCommand", () => {
  setActivePinia(createPinia());
  const store = useStore();
  const note = makeMe.aNote.title("Dummy Title").please()

  describe("addEditingToUndoHistory", () => {
    beforeEach(() => {
      store.loadNotes([note]);
    });

    test("should push textContent into store state noteUndoHistories ",
        () => {
          store.addEditingToUndoHistory({ noteId: note.id, });

          expect(store.noteUndoHistories.length).toEqual(1);
        });
  });

  describe('popUndoHistory', () => {
    const mockUpdatedNote = { noteId: note.id };
    let initialUndoCount;

    beforeEach(() => {
      store.loadNotes([note]);
      store.addEditingToUndoHistory(mockUpdatedNote);
      initialUndoCount = store.noteUndoHistories.length;
    });

    it('should undo to last history', () => {
      store.popUndoHistory();

      expect(store.noteUndoHistories.length).toEqual(initialUndoCount - 1);
    });

    it('should not undo to last history if there is no more history', () => {
      store.popUndoHistory();
      store.popUndoHistory();
      store.popUndoHistory();

      expect(store.noteUndoHistories.length).toEqual(0);
    });
  })
});

