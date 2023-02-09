import { flushPromises } from "@vue/test-utils";
import NoteSuggestDescriptionDialog from "@/components/notes/NoteSuggestDescriptionDialog.vue";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

describe("NoteSuggestDescriptionDialog", () => {
  it("fetch from api", async () => {
    const note = makeMe.aNoteRealm.please();
    helper.apiMock
      .expectingPost(`/api/ai/ask-suggestions`)
      .andReturnOnce({ suggestion: "suggestion" });
    const wrapper = helper
      .component(NoteSuggestDescriptionDialog)
      .withStorageProps({ selectedNote: note })
      .mount();
    await flushPromises();
    expect(wrapper.find("textarea").element).toHaveValue("suggestion");
  });

  it.skip("opens an alert when the api returns an empty string", async () => {
    const note = makeMe.aNoteRealm.please();
    helper.apiMock
      .expectingPost(`/api/ai/ask-suggestions`)
      .andReturnOnce({ suggestion: "" });
    const wrapper = helper
      .component(NoteSuggestDescriptionDialog)
      .withStorageProps({ selectedNote: note })
      .mount();
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    const spy = vi.spyOn(wrapper.vm, "openAlert");
    expect(spy).not.toHaveBeenCalled();
    await flushPromises();
    expect(spy).toHaveBeenCalled();
  });
});
