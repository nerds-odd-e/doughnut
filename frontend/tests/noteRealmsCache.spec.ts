/**
 * @jest-environment jsdom
 */
import store from "./fixtures/testingStore";
import makeMe from "./fixtures/makeMe";
import NoteRealmCache from "../src/store/NoteRealmCache";

beforeEach(() => {
  fetch.resetMocks();
});

describe("storedApiCollection", () => {
  const note = makeMe.aNoteRealm.please();

  const toNoteRealmsCache = (notes: Generated.NoteRealm[]) => {
    return new NoteRealmCache({ notes });
  };

  describe("delete note", () => {

    it("should change the store", async () => {
      const cache = toNoteRealmsCache([note]);
      cache.deleteNoteAndDescendents(note.id);
      expect(store.getNoteRealmById(note.id)).toBeUndefined();
    });

    it("should remove children notes", async () => {
      const child = makeMe.aNoteRealm.under(note).please();
      const cache = toNoteRealmsCache([note, child]);
      cache.deleteNoteAndDescendents(note.id);
      expect(cache.getNoteRealmById(child.id)).toBeUndefined();
    });

    it("should remove child from list", async () => {
      const child = makeMe.aNoteRealm.under(note).please();
      const cache = toNoteRealmsCache([note, child]);
      const childrenCount = cache.getNoteRealmById(note.id)?.children.length;
      cache.deleteNoteAndDescendents(child.id);
      expect(cache.getNoteRealmById(note.id)?.children).toHaveLength(
        childrenCount - 1
      );
    });

  });
});
