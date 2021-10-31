/**
 * @jest-environment jsdom
 */
import {storedApiDeleteNote} from "../src/storedApi";
import store from "../src/store/index.js";
import makeMe from "./fixtures/makeMe";

beforeEach(() => {
  fetch.resetMocks();
});

describe("storedApi", () => {
  const note = makeMe.aNote.please()

  describe("delete note", () => {

    beforeEach(() => {
      fetch.mockResponseOnce(JSON.stringify({}));
      store.commit("loadNotes", [note]);
    });

    test("should call the api", async () => {
      await storedApiDeleteNote(store, note.id)
      expect(fetch).toHaveBeenCalledTimes(1);
      expect(fetch).toHaveBeenCalledWith(`/api/notes/${note.id}/delete`, expect.anything());
    });

    test("should change the store", async () => {
      await storedApiDeleteNote(store, note.id)
      expect(store.getters.getNoteById(note.id)).toBeUndefined()
    });

    test("should remove children notes", async () => {
      const child = makeMe.aNote.under(note).please()
      store.commit("loadNotes", [child]);
      await storedApiDeleteNote(store, note.id)
      expect(store.getters.getNoteById(child.id)).toBeUndefined()
    });

    test("should remove child from list", async () => {
      const child = makeMe.aNote.under(note).please()
      store.commit("loadNotes", [child]);
      const childrenCount = store.getters.getChildrenIdsByParentId(note.id).length
      await storedApiDeleteNote(store, child.id)
      expect(store.getters.getChildrenIdsByParentId(note.id)).toHaveLength(childrenCount - 1)
    });

  });
});

