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
  const mountPage = async (repetition)=>{
    fetch.mockResponseOnce(JSON.stringify(repetition))
    const { wrapper, mockRouter }  = mountWithMockRoute(
      RepeatPage,
      {},
      { name: "repeat" }
    );
    await flushPromises();
    return { wrapper, mockRouter }
  }

  test("redirect to review page if nothing to repeat", async () => {
    const { mockRouter } = await mountPage({})
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith("/api/reviews/repeat", {});
    expect(mockRouter.push).toHaveBeenCalledWith({ name: "reviews" });
  });

  test("replace route with repeat/quiz if there is a quiz", async () => {
    const note = makeMe.aNote.please()
    const repetition = makeMe.aRepetition.ofNote(note).please()
    const { wrapper, mockRouter } = await mountPage(repetition)
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith("/api/reviews/repeat", {});
    expect(mockRouter.push).toHaveBeenCalledWith({ name: "repeat-quiz" });
    expect(wrapper.findAll(".pause-repeat")).toHaveLength(1);
  });

});
