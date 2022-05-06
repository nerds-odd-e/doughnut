/**
 * @jest-environment jsdom
 */
import { screen } from "@testing-library/vue";
import NotebooksPage from "@/pages/NotebooksPage.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

helper.resetWithApiMock(beforeEach, afterEach);

describe("Notebooks Page", () => {
  it("fetch API to be called ONCE", async () => {
    const notebook = makeMe.aNotebook.please();

    helper.apiMock.expectingGet("/api/notebooks").andReturnOnce({
      notebooks: [notebook],
      subscriptions: [],
    });
    helper.component(NotebooksPage).render();

    helper.apiMock.verifyCall("/api/notebooks");
    expect(await screen.findByTitle("undo")).toBeDisabled();
  });

  it("show undo when there is something to undo", async () => {
    const notebook = makeMe.aNotebook.please();
    helper.store.deleteNote(notebook.headNote.id);
    helper.apiMock.expectingGet("/api/notebooks").andReturnOnce({
      notebooks: [],
      subscriptions: [],
    });

    helper.component(NotebooksPage).render();

    expect(await screen.findByTitle("undo delete note")).not.toBeDisabled();
  });
});
