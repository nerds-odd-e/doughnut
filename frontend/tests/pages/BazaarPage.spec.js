/**
 * @jest-environment jsdom
 */
 import BazaarPage from "@/pages/BazaarPage.vue";
 import flushPromises from "flush-promises";
 import _ from "lodash";
 import { renderWithStoreAndMockRoute } from "../helpers";
 import store from "../../src/store/index.js";
 import makeMe from "../fixtures/makeMe";
 
 jest.useFakeTimers();

 beforeEach(() => {
   fetch.resetMocks();
 });

describe("bazaar page", () => {
  test("fetch API to be called ONCE on mount", async () => {
    const bazaarNote = makeMe.aNotePosition.inBazaar().shortDescription('a description').please();
    const stubResponse = {
      notebooks: [bazaarNote],
    };
    fetch.mockResponseOnce(JSON.stringify(stubResponse));

    renderWithStoreAndMockRoute(store, BazaarPage);
    
    await flushPromises();
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith("/api/bazaar", {});
  });

  test('fetch API to be called every 5 seconds', async () => {
    const bazaarNote = makeMe.aNotePosition.inBazaar().shortDescription('a description').please();
    const stubResponse = {
      notebooks: [bazaarNote],
    };
    fetch.mockResponse(JSON.stringify(stubResponse));

    renderWithStoreAndMockRoute(store, BazaarPage);

    await flushPromises();
    jest.advanceTimersByTime(6000);
    expect(fetch).toHaveBeenCalledTimes(2);
    expect(fetch).toHaveBeenCalledWith("/api/bazaar", {});
  });
});
