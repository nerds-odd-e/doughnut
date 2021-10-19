/**
 * @jest-environment jsdom
 */
import NoteStatistics from "@/components/notes/NoteStatistics.vue";
import { mount } from "@vue/test-utils";
import flushPromises from "flush-promises";
import makeMe from "../fixtures/makeMe";

beforeEach(() => {
  fetch.resetMocks();
});

const stubResponse = {
  reviewPoint: makeMe.aReviewPoint.please(),
  note: makeMe.aNote.please(),
};

const stubLinkResponse = {
  reviewPoint: makeMe.aReviewPoint.please(),
  link: makeMe.aLinkViewedByUser.please(),
};

describe("note statistics", () => {
  test("fetch API to be called ONCE", async () => {
    fetch.mockResponseOnce(JSON.stringify({}));
    const wrapper = mount(NoteStatistics, { propsData: { noteId: "123" } });
    await flushPromises();
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith("/api/notes/123/statistics");
    expect(wrapper.findAll(".statistics-value")).toHaveLength(0);
  });

  test("should render values", async () => {
    fetch.mockResponseOnce(JSON.stringify(stubResponse));
    const wrapper = mount(NoteStatistics, { propsData: { noteId: "123" } });
    await flushPromises();
    expect(wrapper.findAll(".statistics-value")).toHaveLength(5);
  });

  test("should render values for link as well", async () => {
    fetch.mockResponseOnce(JSON.stringify(stubLinkResponse));
    const wrapper = mount(NoteStatistics, { propsData: { linkid: "123" } });
    await flushPromises();
    expect(fetch).toHaveBeenCalledWith("/api/links/123/statistics");
    expect(wrapper.findAll(".statistics-value")).toHaveLength(4);
  });
});
