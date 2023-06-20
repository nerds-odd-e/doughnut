import { flushPromises } from "@vue/test-utils";
import NoteQuestionDialog from "@/components/notes/NoteQuestionDialog.vue";
import { beforeEach, expect } from "vitest";
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
    .expectingPost(`/api/ai/${note.id}/ask-suggestions`)
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
