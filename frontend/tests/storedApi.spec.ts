/**
 * @jest-environment jsdom
 */
import fetchMock from "jest-fetch-mock";
import store from "./fixtures/testingStore";
import makeMe from "./fixtures/makeMe";
import useStoredLoadingApi from "../src/managedApi/useStoredLoadingApi";

beforeEach(() => {
  fetchMock.resetMocks();
  store.$reset();
});

describe("storedApiCollection", () => {
  const note = makeMe.aNoteRealm.please();
  const sa = useStoredLoadingApi().storedApi;

  describe("delete note", () => {
    beforeEach(() => {
      fetchMock.mockResponseOnce(JSON.stringify({}));
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
