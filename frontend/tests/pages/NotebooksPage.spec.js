/**
 * @jest-environment jsdom
 */
import NotebooksPage from "@/pages/NotebooksPage.vue";
import { useStore } from "@/store";
import { screen } from "@testing-library/vue";
import { renderWithStoreAndMockRoute } from "../helpers";
import _ from "lodash";
import makeMe from "../fixtures/makeMe";
import { createTestingPinia } from "@pinia/testing";

beforeEach(() => {
   fetch.resetMocks();
   fetch.mockResponse(JSON.stringify({notebooks: [], subscriptions: []}));
});
 
describe("Notebooks Page", () => {
  const pinia = createTestingPinia();

  test("fetch API to be called ONCE", async () => {
      const notebook = makeMe.aNotebook.please()
      const stubResponse = {
        notebooks: [notebook],
        subscriptions: []
      };

      fetch.mockResponse(JSON.stringify(stubResponse));
      renderWithStoreAndMockRoute(pinia, NotebooksPage, {});

      expect(fetch).toHaveBeenCalledTimes(1);
      expect(fetch).toHaveBeenCalledWith("/api/notebooks", expect.anything());
      expect(await screen.findByTitle("undo")).toBeDisabled();
    });

  test("show undo when there is something to undo", async () => {
      const store = useStore(pinia);
      const notebook = makeMe.aNotebook.please()
      store.loadNotes([notebook.headNote])
      store.deleteNote(notebook.headNote.id)

      renderWithStoreAndMockRoute(store, NotebooksPage, {});
      const domElem = await screen.findByTitle("undo delete note");
      expect(domElem).not.toBeDisabled();
    });
});