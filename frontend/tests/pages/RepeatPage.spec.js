import RepeatPage from "@/pages/RepeatPage.vue";
import flushPromises from "flush-promises";
import { reviewPointViewedByUser } from "../notes/fixtures";
import _ from "lodash";
import { mountWithMockRoute } from "../helpers";

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
    fetch.mockResponseOnce(
      JSON.stringify({
        quizQuestion: {
          questionType: "CLOZE_SELECTION",
          options: [
            {
              note: {
                id: 1,
                notePicture: null,
                head: true,
                noteTypeDisplay: "Child Note",
                title: "question",
                shortDescription: "answer",
              },
              picture: false,
              display: "question",
            },
          ],
          description: "answer",
          mainTopic: "",
          pictureQuestion: false,
        },
        reviewPointViewedByUser,
      })
    );
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
