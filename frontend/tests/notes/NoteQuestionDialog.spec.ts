import { flushPromises } from "@vue/test-utils";
import { beforeEach, expect } from "vitest";
import NoteQuestionDialog from "@/components/notes/NoteQuestionDialog.vue";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

const goodQuestion = {
  question: "any question?",
  options: [
    { option: "option A" },
    { option: "option B" },
    { option: "option C" },
  ],
};

const createWrapper = async () => {
  const note = makeMe.aNoteRealm.please();
  helper.apiMock
    .expectingPost(`/api/ai/generate-question?note=${note.id}`)
    .andReturnOnce({
      suggestion: JSON.stringify(goodQuestion),
      finishReason: "stop",
    });
  const wrapper = helper
    .component(NoteQuestionDialog)
    .withStorageProps({ selectedNote: note.note })
    .mount();
  await flushPromises();
  return wrapper;
};

describe("NoteQuestionDialog", () => {
  it("render the question returned", async () => {
    const wrapper = await createWrapper();
    expect(wrapper.text()).toContain("any question?");
    expect(wrapper.text()).toContain("option A");
    expect(wrapper.text()).toContain("option C");
  });
});
