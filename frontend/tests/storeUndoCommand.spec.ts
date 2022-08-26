/**
 * @jest-environment jsdom
 */
import createHistory from "../src/store/history";
import makeMe from "./fixtures/makeMe";

describe("storeUndoCommand", () => {
  const note = makeMe.aNoteRealm.title("Dummy Title").please();

  describe("addEditingToUndoHistory", () => {
    const histories = createHistory();
    it("should push textContent into store state noteUndoHistories ", () => {
      histories.addEditingToUndoHistory(note.id, note.note.textContent);

      expect(histories.noteUndoHistories.length).toEqual(1);
    });
  });

  describe("popUndoHistory", () => {
    let initialUndoCount;
    const histories = createHistory();

    beforeEach(() => {
      histories.addEditingToUndoHistory(note.id, note.note.textContent);
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
