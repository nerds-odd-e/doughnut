import NoteEditingHistory from "../src/store/NoteEditingHistory";
import makeMe from "./fixtures/makeMe";

describe("storeUndoCommand", () => {
  const note = makeMe.aNoteRealm.title("Dummy Title").please();
  const textContent = {
    title: note.note.title,
    description: note.note.description,
    updatedAt: note.note.updatedAt,
  };

  describe("addEditingToUndoHistory", () => {
    const histories = new NoteEditingHistory();
    it("should push textContent into store state noteUndoHistories ", () => {
      histories.addEditingToUndoHistory(note.id, textContent);

      expect(histories.noteUndoHistories.length).toEqual(1);
    });
  });

  describe("popUndoHistory", () => {
    let initialUndoCount;
    const histories = new NoteEditingHistory();

    beforeEach(() => {
      histories.addEditingToUndoHistory(note.id, textContent);
      initialUndoCount = histories.noteUndoHistories.length;
    });

    it("should undo to last history", () => {
      histories.popUndoHistory();

      expect(histories.noteUndoHistories.length).toEqual(initialUndoCount - 1);
    });

    it("should not undo to last history if there is no more history", () => {
      histories.popUndoHistory();
      histories.popUndoHistory();
      histories.popUndoHistory();

      expect(histories.noteUndoHistories.length).toEqual(0);
    });
  });
});
