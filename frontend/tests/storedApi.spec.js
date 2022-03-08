/**
 * @jest-environment jsdom
 */
import store from "./fixtures/testingStore.js";
import makeMe from "./fixtures/makeMe";
import useStoredLoadingApi from "../src/managedApi/useStoredLoadingApi";

beforeEach(() => {
  fetch.resetMocks();
});

describe("storedApiCollection", () => {
  const note = makeMe.aNote.please()
  const sa = useStoredLoadingApi().storedApi;

  describe("delete note", () => {

    beforeEach(() => {
      fetch.mockResponseOnce(JSON.stringify({}));
      store.loadNotes([note]);
    });

    test("should call the api", async () => {
      await sa.deleteNote(note.id)
      expect(fetch).toHaveBeenCalledTimes(1);
      expect(fetch).toHaveBeenCalledWith(`/api/notes/${note.id}/delete`, expect.anything());
    });

    test("should change the store", async () => {
      await sa.deleteNote(note.id)
      expect(store.getNoteById(note.id)).toBeUndefined()
    });

    test("should remove children notes", async () => {
      const child = makeMe.aNote.under(note).please()
      store.loadNotes([child]);
      await sa.deleteNote(note.id)
      expect(store.getNoteById(child.id)).toBeUndefined()
    });

    test("should remove child from list", async () => {
      const child = makeMe.aNote.under(note).please()
      store.loadNotes([child]);
      const childrenCount = store.getChildrenIdsByParentId(note.id).length
      await sa.deleteNote(child.id)
      expect(store.getChildrenIdsByParentId(note.id)).toHaveLength(childrenCount - 1)
    });

  });
});

