import { flushPromises } from "@vue/test-utils";
import NoteSuggestDescriptionDialog from "@/components/notes/NoteSuggestDescriptionDialog.vue";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

describe("NoteSuggestDescriptionDialog", () => {
  const note = makeMe.aNoteRealm.please();

  beforeEach(() => {
    helper.apiMock
      .expectingPost(`/api/ai/ask-suggestions`)
      .andReturnOnce({ suggestion: "suggestion" });
  });

  it("fetches from api", async () => {
    const wrapper = helper
      .component(NoteSuggestDescriptionDialog)
      .withStorageProps({ selectedNote: note })
      .mount();
    await flushPromises();
    expect(wrapper.find("textarea").element).toHaveValue("suggestion");
  });

  it("uses right parameters", async () => {
    helper
      .component(NoteSuggestDescriptionDialog)
      .withStorageProps({ selectedNote: note })
      .mount();
    helper.apiMock.verifyCall(
      "/api/ai/ask-suggestions",
      expect.objectContaining({
        body: expect.stringContaining(
          `"title":"In one paragraph, tell me about undefined"`
        ),
      })
    );
  });
});
