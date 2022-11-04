import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import InitialReviewPage from "@/pages/InitialReviewPage.vue";
import ShowReviewPoint from "@/components/review/ShowReviewPoint.vue";
import flushPromises from "flush-promises";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";
import RenderingHelper from "../helpers/RenderingHelper";

let renderer: RenderingHelper;
let mockRouterPush = vi.fn();

helper.resetWithApiMock(beforeEach, afterEach);

beforeEach(() => {
  mockRouterPush = vi.fn();
  renderer = helper
    .component(InitialReviewPage)
    .withStorageProps({})
    .withMockRouterPush(mockRouterPush);
});

describe("repeat page", () => {
  it("redirect to review page if nothing to review", async () => {
    helper.apiMock.expectingGet("/api/reviews/initial").andReturnOnce([]);
    renderer.currentRoute({ name: "initial" }).mount();
    await flushPromises();
    expect(mockRouterPush).toHaveBeenCalledWith({ name: "reviews" });
  });

  describe("normal view", () => {
    const noteRealm = makeMe.aNoteRealm.please();
    const reviewPoint = makeMe.aReviewPoint.ofNote(noteRealm).please();

    beforeEach(() => {
      helper.apiMock
        .expectingGet("/api/reviews/initial")
        .andReturnOnce([reviewPoint, reviewPoint]);
      helper.apiMock.expectingGet(`/api/notes/${noteRealm.id}`).andReturnOnce({
        notePosition: makeMe.aNotePosition.please(),
        notes: [noteRealm],
      });
    });

    it("normal view", async () => {
      const wrapper = renderer.currentRoute({ name: "initial" }).mount();
      await flushPromises();
      expect(mockRouterPush).toHaveBeenCalledTimes(1);
      expect(wrapper.findAll(".initial-review-paused")).toHaveLength(0);
      expect(wrapper.findAll(".pause-stop")).toHaveLength(1);
      expect(wrapper.find(".progress-text").text()).toContain(
        "Initial Review: 0/2"
      );
    });

    (["levelChanged"] as "levelChanged"[]).forEach((event) => {
      it(`reloads when ${event}`, async () => {
        const wrapper = renderer.currentRoute({ name: "initial" }).mount();
        await flushPromises();
        helper.apiMock.expectingGet("/api/reviews/initial").andReturnOnce([]);
        wrapper.findComponent(ShowReviewPoint).vm.$emit(event);
      });
    });
  });

  it("minimized view", async () => {
    const noteRealm = makeMe.aNoteRealm.please();
    const reviewPoint = makeMe.aReviewPoint.ofNote(noteRealm).please();
    helper.apiMock
      .expectingGet("/api/reviews/initial")
      .andReturnOnce([reviewPoint]);
    const wrapper = renderer
      .withStorageProps({ minimized: true })
      .currentRoute({ name: "initial" })
      .mount();
    await flushPromises();
    expect(mockRouterPush).toHaveBeenCalledTimes(0);
    expect(wrapper.findAll(".initial-review-paused")).toHaveLength(1);
    expect(wrapper.find(".review-point-abbr span").text()).toContain(
      noteRealm.note.title
    );
  });

  it("minimized view for link", async () => {
    const link = makeMe.aLink.please();
    const reviewPoint = makeMe.aReviewPoint.ofLink(link).please();
    helper.apiMock
      .expectingGet("/api/reviews/initial")
      .andReturnOnce([reviewPoint]);
    const wrapper = renderer
      .withStorageProps({ minimized: true })
      .currentRoute({ name: "initial" })
      .mount();
    await flushPromises();
    expect(mockRouterPush).toHaveBeenCalledTimes(0);
    expect(wrapper.findAll(".initial-review-paused")).toHaveLength(1);
    expect(wrapper.find(".review-point-abbr span").text()).toContain(
      link.sourceNote.title
    );
    expect(wrapper.find(".review-point-abbr span").text()).toContain(
      link.targetNote.title
    );
  });
});
