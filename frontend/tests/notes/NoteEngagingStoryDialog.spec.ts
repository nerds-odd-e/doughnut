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
    .withStorageProps({ selectedNoteId: note.note.id })
    .mount();
  await flushPromises();
  return wrapper;
};

describe("NoteEngagingStoryDialog", () => {
  it("fetches engaging story for review or single note", async () => {
    const wrapper = await createWrapper();
    expect(wrapper.find("textarea").element).toHaveValue(
      "This is an engaging story."
    );
  });
});
