import { Router } from "vue-router";
import ManagedApi from "@/managedApi/ManagedApi";
import makeMe from "./fixtures/makeMe";
import createNoteStorage from "../src/store/createNoteStorage";

beforeEach(() => {
  fetchMock.resetMocks();
});

describe("storedApiCollection", () => {
  const note = makeMe.aNoteRealm.please();
  const history = createNoteStorage(new ManagedApi({ states: [], errors: [] }));
  const routerReplace = vitest.fn();
  const sa = history.api({ replace: routerReplace } as unknown as Router);

  describe("delete note", () => {
    beforeEach(() => {
      fetchMock.mockResponseOnce(JSON.stringify({}));
    });

    it("should call the api", async () => {
      await sa.deleteNote(note.id);
      expect(fetch).toHaveBeenCalledTimes(1);
      expect(fetch).toHaveBeenCalledWith(
        `/api/notes/${note.id}/delete`,
        expect.anything(),
      );
      expect(routerReplace).toHaveBeenCalledTimes(1);
    });
  });
});
