/**
 * @jest-environment jsdom
 */
import { storedApi } from "../src/storedApi";
import store from "../src/store/index.js";
import makeMe from "./fixtures/makeMe";

beforeEach(() => {
  fetch.resetMocks();
});

describe("storedApi", () => {
  const note = makeMe.aNote.please()
  const sa = storedApi(store)

  describe("delete note", () => {

    beforeEach(() => {
      fetch.mockResponseOnce(JSON.stringify({}));
      store.commit("loadNotes", [note]);
    });

    test("should call the api", async () => {
      await sa.deleteNote(note.id)
      expect(fetch).toHaveBeenCalledTimes(1);
      expect(fetch).toHaveBeenCalledWith(`/api/notes/${note.id}/delete`, expect.anything());
    });

    test("should change the store", async () => {
      await sa.deleteNote(note.id)
      expect(store.getters.getNoteById(note.id)).toBeUndefined()
    });

    test("should remove children notes", async () => {
      const child = makeMe.aNote.under(note).please()
      store.commit("loadNotes", [child]);
      await sa.deleteNote(note.id)
      expect(store.getters.getNoteById(child.id)).toBeUndefined()
    });

    test("should remove child from list", async () => {
      const child = makeMe.aNote.under(note).please()
      store.commit("loadNotes", [child]);
      const childrenCount = store.getters.getChildrenIdsByParentId(note.id).length
      await sa.deleteNote(child.id)
      expect(store.getters.getChildrenIdsByParentId(note.id)).toHaveLength(childrenCount - 1)
    });

  });
});

