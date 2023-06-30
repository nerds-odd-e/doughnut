import { flushPromises } from "@vue/test-utils";
import { beforeEach, expect } from "vitest";
import NoteEngagingStoryDialog from "@/components/notes/NoteEngagingStoryDialog.vue";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

const createWrapper = async () => {
  const note = makeMe.aNoteRealm.please();
  helper.apiMock
    .expectingPost(`/api/ai/generate-image`)
    .andReturnOnce({ engagingStory: "This is an engaging story." });
  const wrapper = helper
    .component(NoteEngagingStoryDialog)
    .withStorageProps({ selectedNote: note.note })
    .mount();
  await flushPromises();
  return wrapper;
};

describe("NoteEngagingStoryDialog", () => {
  it("fetches engaging story for review or single note", async () => {
    const wrapper = await createWrapper();
    expect(wrapper.find("img.ai-art").element).toBeDefined();
  });
});
