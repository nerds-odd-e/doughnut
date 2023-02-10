import { flushPromises } from "@vue/test-utils";
import NoteEngagingStoryDialog from "@/components/notes/NoteEngagingStoryDialog.vue";
import { beforeEach, expect } from "vitest";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

const createWrapper = async () => {
  const note = makeMe.aNoteRealm.please();
  helper.apiMock
    .expectingGet(`/api/ai/ask-engaging-stories?notes=${note.note.id}`)
    .andReturnOnce({ engagingStory: "This is an engaging story." });
  const wrapper = helper
    .component(NoteEngagingStoryDialog)
    .withStorageProps({ selectedNote: note })
    .mount();
  await flushPromises();
  return wrapper;
};

describe("NoteEngagingStoryDialog", () => {
  it("fetches engaging story from api", async () => {
    const wrapper = await createWrapper();
    expect(wrapper.find(".engaging-story").text()).toEqual(
      "This is an engaging story."
    );
  });

  it("Engaging story dialog close button exits the dialog", async () => {
    const wrapper = await createWrapper();
    await wrapper.find("input[value='Close']").trigger("click");
    expect(wrapper.emitted("done")).toBeTruthy();
  });
});
