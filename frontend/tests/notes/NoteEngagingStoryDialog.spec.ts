import { flushPromises } from "@vue/test-utils";
import NoteEngagingStoryDialog from "@/components/notes/NoteEngagingStoryDialog.vue";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

describe("NoteEngagingStoryDialog", () => {
  it("fetches engaging story from api", async () => {
    const note = makeMe.aNoteRealm.please();
    helper.apiMock
      .expectingGet(`/api/ai/ask-engaging-stories/${note.note.id}`)
      .andReturnOnce({ engagingStory: "This is an engaging story." });
    const wrapper = helper
      .component(NoteEngagingStoryDialog)
      .withStorageProps({ selectedNote: note })
      .mount();
    await flushPromises();
    expect(wrapper.find("textarea").element).toHaveValue(
      "This is an engaging story."
    );
  });
});
