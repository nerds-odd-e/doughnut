/**
 * @jest-environment jsdom
 */
import store from "./fixtures/testingStore";
import makeMe from "./fixtures/makeMe";
import useStoredLoadingApi from "../src/managedApi/useStoredLoadingApi";

beforeEach(() => {
  fetch.resetMocks();
  store.$reset();
});

describe("storedApiCollection", () => {
  const note = makeMe.aNoteRealm.please();
  const sa = useStoredLoadingApi().storedApi;

  describe("delete note", () => {
    beforeEach(() => {
      fetch.mockResponseOnce(JSON.stringify({}));
    });

    it("should call the api", async () => {
      await sa.deleteNote(note.id);
      expect(fetch).toHaveBeenCalledTimes(1);
      expect(fetch).toHaveBeenCalledWith(
        `/api/notes/${note.id}/delete`,
        expect.anything()
      );
    });
  });
});
