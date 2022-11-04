import { describe, it, expect, beforeEach } from "vitest";
import makeMe from "./fixtures/makeMe";
import createNoteStorage from "../src/store/createNoteStorage";

beforeEach(() => {
  fetchMock.resetMocks();
});

describe("storedApiCollection", () => {
  const note = makeMe.aNoteRealm.please();
  const history = createNoteStorage();
  const sa = history.api();

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
