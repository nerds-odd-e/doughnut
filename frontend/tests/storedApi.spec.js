/**
 * @jest-environment jsdom
 */
import store from './fixtures/testingStore';
import makeMe from './fixtures/makeMe';
import useStoredLoadingApi from '../src/managedApi/useStoredLoadingApi';

beforeEach(() => {
  fetch.resetMocks();
});

describe('storedApiCollection', () => {
  const note = makeMe.aNoteRealm.please();
  const sa = useStoredLoadingApi().storedApi;

  describe('delete note', () => {
    beforeEach(() => {
      fetch.mockResponseOnce(JSON.stringify({}));
      store.loadNoteRealms([note]);
    });

    it('should call the api', async () => {
      await sa.deleteNote(note.id);
      expect(fetch).toHaveBeenCalledTimes(1);
      expect(fetch).toHaveBeenCalledWith(
        `/api/notes/${note.id}/delete`,
        expect.anything()
      );
    });

    it('should change the store', async () => {
      await sa.deleteNote(note.id);
      expect(store.getNoteRealmById(note.id)).toBeUndefined();
    });

    it('should remove children notes', async () => {
      const child = makeMe.aNoteRealm.under(note).please();
      store.loadNoteRealms([child]);
      await sa.deleteNote(note.id);
      expect(store.getNoteRealmById(child.id)).toBeUndefined();
    });

    it('should remove child from list', async () => {
      const child = makeMe.aNoteRealm.under(note).please();
      store.loadNoteRealms([child]);
      const childrenCount = store.getNoteRealmById(note.id).childrenIds.length;
      await sa.deleteNote(child.id);
      expect(store.getNoteRealmById(note.id).childrenIds).toHaveLength(
        childrenCount - 1
      );
    });
  });
});
