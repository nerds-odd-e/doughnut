import { flushPromises } from "@vue/test-utils";
import { beforeEach, expect } from "vitest";
import AIGenerateImageDialog from "@/components/notes/AIGenerateImageDialog.vue";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

const createWrapper = async () => {
  const note = makeMe.aNoteRealm.please();
  helper.apiMock
    .expectingPost(`/api/ai/generate-image`)
    .andReturnOnce({ b64encoded: "This is an encoded image" });
  const wrapper = helper
    .component(AIGenerateImageDialog)
    .withStorageProps({ selectedNote: note.note })
    .mount();
  await flushPromises();
  return wrapper;
};

describe("AIGeneratedImageDialog", () => {
  it("fetches generated image", async () => {
    const wrapper = await createWrapper();
    expect(wrapper.find("img.ai-art").element).toBeDefined();
  });
});
