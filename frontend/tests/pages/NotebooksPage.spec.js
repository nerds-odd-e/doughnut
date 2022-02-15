/**
 * @jest-environment jsdom
 */
import { screen } from "@testing-library/vue";
import NotebooksPage from "@/pages/NotebooksPage.vue";
import store from "../../src/store/index.js";
import { renderWithStoreAndMockRoute } from "../helpers";
import _ from "lodash";
import makeMe from "../fixtures/makeMe";
 
jest.mock("snackbar-vue", () => ({
    useSnackbarPlugin: () => ({
        show: jest.fn(0)
    })
}));

beforeEach(() => {
   fetch.resetMocks();
   fetch.mockResponse(JSON.stringify({
            notebooks: [],
            subscriptions: []

   }));
});
 
describe("Notebooks Page", () => {

   test("fetch API to be called ONCE", async () => {
        const notebook = makeMe.aNotebook.please()
        const stubResponse = {
            notebooks: [notebook],
            subscriptions: []
        };

        fetch.mockResponse(JSON.stringify(stubResponse));
        renderWithStoreAndMockRoute(store, NotebooksPage, {});

        expect(fetch).toHaveBeenCalledTimes(1);
        expect(fetch).toHaveBeenCalledWith('/api/notebooks', {});
    });

   test("show undo when there is something to undo", async () => {
        const notebook = makeMe.aNotebook.please()
        store.commit('loadNotes', [notebook.headNote])
        store.commit('deleteNote', notebook.headNote.id)

        renderWithStoreAndMockRoute(store, NotebooksPage, {});

        await screen.findByTitle("undo");
    });

    xtest("call /undo-delete when snackbar button is pressed", async () => {
        const notebook = makeMe.aNotebook.please()
        const stubResponse = {
            notebooks: [notebook],
            subscriptions: []
        };
        const deletedNoteId = '1';

        const { wrapper } = renderWithStoreAndMockRoute(store, NotebooksPage, {});

        wrapper.find('.snackbar__action').trigger("click")
        expect(fetch).toHaveBeenCalledWith('/api/notebooks', {});
        expect(fetch).toHaveBeenCalledWith(`/api/notes/${deletedNoteId}/undo-delete`);

    })
});
