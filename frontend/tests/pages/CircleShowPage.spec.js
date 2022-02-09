/**
 * @jest-environment jsdom
 */
 import CircleShowPage from "@/pages/CircleShowPage.vue";
 import flushPromises from "flush-promises";
 import { renderWithStoreAndMockRoute } from "../helpers";
 import store from "../../src/store/index.js";
 import makeMe from "../fixtures/makeMe";
 
 beforeEach(() => {
   fetch.resetMocks();
 });

describe("circle show page", () => {
  test("fetch API to be called ONCE on mount", async () => {
    const notebook = makeMe.aNotebook.please();
    const circleNote = makeMe.aCircleNote.notebooks(notebook).please();
    fetch.mockResponseOnce(JSON.stringify(circleNote));

    renderWithStoreAndMockRoute(store, CircleShowPage, {
      propsData: { circleId: circleNote.id }
    });
    
    await flushPromises();
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith(`/api/circles/${circleNote.id}`, {});
  });

  test('fetch API to be called every 5 seconds', async () => {
    const notebook = makeMe.aNotebook.please();
    const circleNote = makeMe.aCircleNote.notebooks(notebook).please();
    fetch.mockResponse(JSON.stringify(circleNote));

    renderWithStoreAndMockRoute(store, CircleShowPage, {
        propsData: { circleId: circleNote.id }
    });

    await flushPromises();
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith(`/api/circles/${circleNote.id}`, {});
  });
});
