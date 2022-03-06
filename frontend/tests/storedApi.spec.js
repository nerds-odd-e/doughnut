/**
 * @jest-environment jsdom
 */
import storedApi from "../src/managedApi/storedApi";
import store from "./fixtures/testingStore.js";
import makeMe from "./fixtures/makeMe";

beforeEach(() => {
  fetch.resetMocks();
});

describe("storedApi", () => {
  const note = makeMe.aNote.please()
  const sa = storedApi({$store: store})

  describe("delete note", () => {

    beforeEach(() => {
      fetch.mockResponseOnce(JSON.stringify({}));
      store.getters.ps().loadNotes([note]);
    });

    test("should call the api", async () => {
      await sa.deleteNote(note.id)
      expect(fetch).toHaveBeenCalledTimes(1);
      expect(fetch).toHaveBeenCalledWith(`/api/notes/${note.id}/delete`, expect.anything());
    });

    test("should change the store", async () => {
      await sa.deleteNote(note.id)
      expect(store.getters.ps().getNoteById(note.id)).toBeUndefined()
    });

    test("should remove children notes", async () => {
      const child = makeMe.aNote.under(note).please()
      store.getters.ps().loadNotes([child]);
      await sa.deleteNote(note.id)
      expect(store.getters.ps().getNoteById(child.id)).toBeUndefined()
    });

    test("should remove child from list", async () => {
      const child = makeMe.aNote.under(note).please()
      store.getters.ps().loadNotes([child]);
      const childrenCount = store.getters.ps().getChildrenIdsByParentId(note.id).length
      await sa.deleteNote(child.id)
      expect(store.getters.ps().getChildrenIdsByParentId(note.id)).toHaveLength(childrenCount - 1)
    });

  });
});

