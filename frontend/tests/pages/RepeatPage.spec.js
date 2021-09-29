/**
 * @jest-environment jsdom
 */
import RepeatPage from "@/pages/RepeatPage.vue";
import flushPromises from "flush-promises";
import _ from "lodash";
import { mountWithMockRoute } from "../helpers";
import makeMe from "../fixtures/makeMe";

beforeEach(() => {
  fetch.resetMocks();
});

describe("repeat page", () => {
  test("redirect to review page if nothing to repeat", async () => {
    fetch.mockResponseOnce(JSON.stringify({}));
    const { wrapper, mockRouter } = mountWithMockRoute(
      RepeatPage,
      {},
      { name: "repeat" }
    );
    await flushPromises();
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith("/api/reviews/repeat", {});
    expect(mockRouter.push).toHaveBeenCalledWith({ name: "reviews" });
  });

  test("replace route with repeat/quiz if there is a quiz", async () => {
    const note = makeMe.aNote.please()
    const repetition = makeMe.aRepetition.ofNote(note).please()
    fetch.mockResponseOnce(JSON.stringify(repetition))
    const { wrapper, mockRouter } = mountWithMockRoute(
      RepeatPage,
      {},
      { name: "repeat" }
    );
    await flushPromises();
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith("/api/reviews/repeat", {});
    expect(mockRouter.push).toHaveBeenCalledWith({ name: "repeat-quiz" });
    expect(wrapper.findAll(".pause-repeat")).toHaveLength(1);
  });
});
