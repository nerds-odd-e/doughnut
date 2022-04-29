/**
 * @jest-environment jsdom
 */
import InitialReviewPage from "@/pages/InitialReviewPage.vue";
import flushPromises from "flush-promises";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";
import RenderingHelper from "../helpers/RenderingHelper";

let renderer: RenderingHelper;
let mockRouterPush = jest.fn();

helper.resetWithApiMock(beforeEach, afterEach);

beforeEach(() => {
  mockRouterPush = jest.fn();
  renderer = helper
    .component(InitialReviewPage)
    .withMockRouterPush(mockRouterPush);
});

describe("repeat page", () => {
  it("redirect to review page if nothing to review", async () => {
    helper.apiMock.expecting("/api/reviews/initial", []);
    renderer.currentRoute({ name: "initial" }).mount();
    await flushPromises();
    expect(mockRouterPush).toHaveBeenCalledWith({ name: "reviews" });
  });

  it("normal view", async () => {
    const note = makeMe.aNoteRealm.please();
    const reviewPoint = makeMe.aReviewPoint.ofNote(note).please();
    helper.apiMock.expecting("/api/reviews/initial", [
      reviewPoint,
      reviewPoint,
    ]);

    const wrapper = renderer.currentRoute({ name: "initial" }).mount();
    await flushPromises();
    expect(mockRouterPush).toHaveBeenCalledTimes(0);
    expect(wrapper.findAll(".initial-review-paused")).toHaveLength(0);
    expect(wrapper.findAll(".pause-stop")).toHaveLength(1);
    expect(wrapper.find(".progress-text").text()).toContain(
      "Initial Review: 0/2"
    );
  });

  it("minimized view", async () => {
    const noteRealm = makeMe.aNoteRealm.please();
    const reviewPoint = makeMe.aReviewPoint.ofNote(noteRealm).please();
    helper.apiMock.expecting("/api/reviews/initial", [reviewPoint]);
    const wrapper = renderer
      .withProps({ nested: true })
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
    const link = makeMe.aLinkViewedByUser.please();
    const reviewPoint = makeMe.aReviewPoint.ofLink(link).please();
    helper.apiMock.expecting("/api/reviews/initial", [reviewPoint]);
    const wrapper = renderer
      .withProps({ nested: true })
      .currentRoute({ name: "initial" })
      .mount();
    await flushPromises();
    expect(mockRouterPush).toHaveBeenCalledTimes(0);
    expect(wrapper.findAll(".initial-review-paused")).toHaveLength(1);
    expect(wrapper.find(".review-point-abbr span").text()).toContain(
      link.sourceNoteWithPosition.note.note.title
    );
    expect(wrapper.find(".review-point-abbr span").text()).toContain(
      link.targetNoteWithPosition.note.note.title
    );
  });
});
