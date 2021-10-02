/**
 * @jest-environment jsdom
 */
import InitialReviewPage from "@/pages/InitialReviewPage.vue";
import flushPromises from "flush-promises";
import _ from "lodash";
import { mountWithMockRoute } from "../helpers";
import makeMe from "../fixtures/makeMe";

beforeEach(() => {
  fetch.resetMocks();
});

describe("repeat page", () => {
  test("redirect to review page if nothing to review", async () => {
    fetch.mockResponseOnce(JSON.stringify({}));
    const { mockRouter } = mountWithMockRoute(
      InitialReviewPage,
      {},
      { name: "initial" }
    );
    await flushPromises();
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith("/api/reviews/initial", {});
    expect(mockRouter.push).toHaveBeenCalledWith({ name: "reviews" });
  });

  test("normal view", async () => {

    const note = makeMe.aNote.please()
    const reviewPoint = makeMe.aReviewPoint.ofNote(note).remainingInitialReviewCountForToday(53).please()
    fetch.mockResponseOnce(JSON.stringify(reviewPoint))

    const { wrapper, mockRouter } = mountWithMockRoute(
      InitialReviewPage,
      {},
      { name: "initial" }
    );
    await flushPromises();
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith("/api/reviews/initial", {});
    expect(mockRouter.push).toHaveBeenCalledTimes(0);
    expect(wrapper.findAll(".initial-review-container")).toHaveLength(0);
    expect(wrapper.findAll(".pause-stop")).toHaveLength(1);
    expect(wrapper.find(".progress-text").text()).toContain(
      "Initial Review: 0/53"
    );
  });

  test("minimized view", async () => {
    const note = makeMe.aNote.please()
    const reviewPoint = makeMe.aReviewPoint.ofNote(note).please()
    fetch.mockResponseOnce(JSON.stringify(reviewPoint));
    const { wrapper, mockRouter } = mountWithMockRoute(
      InitialReviewPage,
      { propsData: { nested: true } },
      { name: "initial" }
    );
    await flushPromises();
    expect(mockRouter.push).toHaveBeenCalledTimes(0);
    expect(wrapper.findAll(".initial-review-container")).toHaveLength(1);
    expect(wrapper.find(".review-point-abbr span").text()).toContain(note.title);
  });
});
