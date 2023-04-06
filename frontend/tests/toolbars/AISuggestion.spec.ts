import AISuggestion from "@/components/toolbars/AISuggestion.vue";
import { flushPromises, VueWrapper } from "@vue/test-utils";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

helper.resetWithApiMock(beforeEach, afterEach);

describe("AISuggestion", () => {
  const note = makeMe.aNote.please();
  helper.resetWithApiMock(beforeEach, afterEach);

  let wrapper: VueWrapper;

  const mountComponentWithNote = () => {
    wrapper = helper
      .component(AISuggestion)
      .withStorageProps({
        selectedNote: note,
      })
      .mount();
  };

  it("has the suggest button when having selected note", () => {
    mountComponentWithNote();
    expect(wrapper.find(".btn").attributes("title")).toEqual(
      "suggest description"
    );
  });

  it("ask api be called once when clicking the suggest button", async () => {
    mountComponentWithNote();
    const expectation = helper.apiMock
      .expectingPost(`/api/ai/ask-suggestions`)
      .andReturnOnce({ suggestion: "suggestion" });
    helper.apiMock.expectingPatch(`/api/text_content/${note.id}`);

    await wrapper.find(".btn").trigger("click");
    await flushPromises();
    expect(expectation.actualRequestJsonBody()).toMatchObject({
      prompt: "Desc",
    });
  });

  it('ask api be called many times until res.finishReason equal "stop" when clicking the suggest button', async () => {
    mountComponentWithNote();

    helper.apiMock
      .expectingPost(`/api/ai/ask-suggestions`)
      .andReturnOnce({ suggestion: "suggestion", finishReason: "length" });

    helper.apiMock
      .expectingPost(`/api/ai/ask-suggestions`)
      .andReturnOnce({ suggestion: "suggestion", finishReason: "stop" });
    helper.apiMock.expectingPatch(`/api/text_content/${note.id}`);
    helper.apiMock.expectingPatch(`/api/text_content/${note.id}`);

    await wrapper.find(".btn").trigger("click");
    await flushPromises();
  });
});
