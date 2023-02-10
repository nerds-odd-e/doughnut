import { flushPromises } from "@vue/test-utils";
import NoteEngagingStoryDialog from "@/components/notes/NoteEngagingStoryDialog.vue";
import { beforeEach, expect } from "vitest";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

const createWrapper = async (review = false) => {
  const note = makeMe.aNoteRealm.please();
  helper.apiMock
    .expectingGet(
      `/api/ai/ask-engaging-stories${
        review ? "/review" : `?notes=${note.note.id}`
      }`
    )
    .andReturnOnce({ engagingStory: "This is an engaging story." });
  const wrapper = helper
    .component(NoteEngagingStoryDialog)
    .withStorageProps({ selectedNote: review ? undefined : note })
    .mount();
  await flushPromises();
  return wrapper;
};

describe("NoteEngagingStoryDialog", () => {
  it("Engaging story dialog close button exits the dialog", async () => {
    const wrapper = await createWrapper();
    await wrapper.find("input[value='Close']").trigger("click");
    expect(wrapper.emitted("done")).toBeTruthy();
  });

  it.each([true, false])(
    "fetches engaging story for review or single note",
    async (review) => {
      const wrapper = await createWrapper(review);
      expect(wrapper.find(".engaging-story").text()).toEqual(
        "This is an engaging story."
      );
    }
  );
});
