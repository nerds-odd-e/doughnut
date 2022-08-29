/**
 * @jest-environment jsdom
 */
import fetchMock from "jest-fetch-mock";
import makeMe from "./fixtures/makeMe";
import useStoredLoadingApi from "../src/managedApi/useStoredLoadingApi";
import createHistory from "../src/store/history";

beforeEach(() => {
  fetchMock.resetMocks();
});

describe("storedApiCollection", () => {
  const note = makeMe.aNoteRealm.please();
  const history = createHistory();
  const sa = useStoredLoadingApi(history).storedApi;

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
