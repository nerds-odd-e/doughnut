/**
 * @jest-environment jsdom
 */
import NoteEditDialog from "@/components/notes/NoteEditDialog.vue";
import { renderWithStoreAndMockRoute } from "../helpers";
import flushPromises from "flush-promises";
import _ from "lodash";
import makeMe from "../fixtures/makeMe";
import { createTestingPinia } from "@pinia/testing";

beforeEach(() => {
  fetch.resetMocks();
});

describe("note show", () => {
  const pinia = createTestingPinia();
  it("fetch API to be called ONCE", async () => {
    const note = makeMe.aNote.please()
    const stubResponse = {
      notePosition: makeMe.aNotePosition.please(),
      notes: [ note ]
    };
    fetch.mockResponseOnce(JSON.stringify(stubResponse));
    renderWithStoreAndMockRoute(pinia, NoteEditDialog, {
      propsData: { noteId: note.id },
    });
    await flushPromises();
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith(`/api/notes/${note.id}`, expect.anything());
  });
});
