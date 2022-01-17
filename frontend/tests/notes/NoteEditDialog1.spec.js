/**
 * @jest-environment jsdom
 */
import NoteEditDialog from "@/components/dialogs/NoteEditDialog.vue";
import store from "../../src/store/index.js";
import { renderWithStoreAndMockRoute } from "../helpers";
import flushPromises from "flush-promises";
import _ from "lodash";
import makeMe from "../fixtures/makeMe";

beforeEach(() => {
  fetch.resetMocks();
});

describe("note show", () => {
  test("fetch API to be called ONCE", async () => {
    const note = makeMe.aNote.please()
    const stubResponse = {
      notePosition: makeMe.aNotePosition.please(),
      notes: [ note ]
    };
    fetch.mockResponseOnce(JSON.stringify(stubResponse));
    renderWithStoreAndMockRoute(store, NoteEditDialog, {
      propsData: { noteId: note.id },
    });
    await flushPromises();
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith(`/api/notes/${note.id}`, {});
  });
});
