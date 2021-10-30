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

    test("should call the api", async () => {
      await storedApiDeleteNote(store, note.id)
      expect(store.getters.getNoteById(note.id)).toBeUndefined()
    });

  });
});

