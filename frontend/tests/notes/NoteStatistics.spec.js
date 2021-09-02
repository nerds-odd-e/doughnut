import NoteStatistics from "@/components/notes/NoteStatistics.vue";
import { mount } from "@vue/test-utils";
import flushPromises from "flush-promises";

beforeEach(() => {
  fetch.resetMocks();
});

const stubResponse = {
  reviewPoint: {
    id: 1,
    lastReviewedAt: "1976-06-01T17:00:00.000+00:00",
    nextReviewAt: "1976-06-01T17:00:00.000+00:00",
    initialReviewedAt: "1976-06-01T17:00:00.000+00:00",
    repetitionCount: 2,
    forgettingCurveIndex: 110,
    removedFromReview: false,
  },
  note: {
    id: 1,
    noteContent: {
      id: 1,
      title: "Fungible",
      description: null,
      url: null,
      urlIsVideo: false,
      pictureUrl: null,
      pictureMask: null,
      useParentPicture: false,
      skipReview: false,
      hideTitleInArticle: false,
      showAsBulletInArticle: false,
      updatedAt: "2021-06-12T04:17:51.000+00:00",
      notePicture: null,
    },
    createdAt: "2021-06-12T04:17:51.000+00:00",
    title: "Fungible",
    head: true,
  },
};

const stubLinkResponse = {
  reviewPoint: {
    id: 1,
    lastReviewedAt: "1976-06-01T17:00:00.000+00:00",
    nextReviewAt: "1976-06-01T17:00:00.000+00:00",
    initialReviewedAt: "1976-06-01T17:00:00.000+00:00",
    repetitionCount: 2,
    forgettingCurveIndex: 110,
    removedFromReview: false,
  },
  link: {
    createdAt: "2021-06-12T04:17:51.000+00:00",
  },
};

describe("note statistics", () => {
  test("fetch API to be called ONCE", async () => {
    fetch.mockResponseOnce(JSON.stringify({}));
    const wrapper = mount(NoteStatistics, { propsData: { noteId: 123 } });
    await flushPromises();
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith("/api/notes/123/statistics");
    expect(wrapper.findAll(".statistics-value")).toHaveLength(0);
  });

  test("should render values", async () => {
    fetch.mockResponseOnce(JSON.stringify(stubResponse));
    const wrapper = mount(NoteStatistics, { propsData: { noteId: 123 } });
    await flushPromises();
    expect(wrapper.findAll(".statistics-value")).toHaveLength(5);
  });

  test("should render values for link as well", async () => {
    fetch.mockResponseOnce(JSON.stringify(stubLinkResponse));
    const wrapper = mount(NoteStatistics, { propsData: { linkid: 123 } });
    await flushPromises();
    expect(fetch).toHaveBeenCalledWith("/api/links/123/statistics");
    expect(wrapper.findAll(".statistics-value")).toHaveLength(4);
  });
});
