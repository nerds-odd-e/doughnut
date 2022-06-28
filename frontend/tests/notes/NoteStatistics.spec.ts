/**
 * @jest-environment jsdom
 */
import fetchMock from "jest-fetch-mock";
import NoteStatistics from "@/components/notes/NoteStatistics.vue";
import { mount } from "@vue/test-utils";
import flushPromises from "flush-promises";
import makeMe from "../fixtures/makeMe";

beforeEach(() => {
  fetchMock.resetMocks();
});

const stubResponse = {
  reviewPoint: makeMe.aReviewPoint.please(),
  note: makeMe.aNoteRealm.please(),
};

describe("note statistics", () => {
  it("fetch API to be called ONCE", async () => {
    fetchMock.mockResponseOnce(JSON.stringify({}));
    const wrapper = mount(NoteStatistics, { propsData: { noteId: 123 } });
    await flushPromises();
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith(
      "/api/notes/123/statistics",
      expect.anything()
    );
    expect(wrapper.findAll(".statistics-value")).toHaveLength(0);
  });

  it("should render values", async () => {
    fetchMock.mockResponseOnce(JSON.stringify(stubResponse));
    const wrapper = mount(NoteStatistics, { propsData: { noteId: 123 } });
    await flushPromises();
    expect(wrapper.findAll(".statistics-value")).toHaveLength(5);
  });
});
