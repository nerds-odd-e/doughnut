/**
 * @jest-environment jsdom
 */
import { screen } from "@testing-library/vue";
import NoteShowPage from "@/pages/NoteShowPage.vue";
import { renderWithStoreAndMockRoute } from "../helpers";
import flushPromises from "flush-promises";
import _ from "lodash";
import makeMe from "../fixtures/makeMe";

beforeEach(() => {
  fetch.resetMocks();
});

describe("note show", () => {
  test("fetch API to be called ONCE", async () => {
    const stubResponse = makeMe.aNote.deprecatingFromCircle('a circle').please()
    fetch.mockResponseOnce(JSON.stringify(stubResponse));
    renderWithStoreAndMockRoute(NoteShowPage, {
      propsData: { noteId: 123 },
    });
    await flushPromises();
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith("/api/notes/123", {});
    await screen.findByText('a circle')
  });
});
