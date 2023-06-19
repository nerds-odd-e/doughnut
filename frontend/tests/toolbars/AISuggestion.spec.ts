import AISuggestion from "@/components/toolbars/AISuggestion.vue";
import { flushPromises } from "@vue/test-utils";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

helper.resetWithApiMock(beforeEach, afterEach);

describe("AISuggestion", () => {
  helper.resetWithApiMock(beforeEach, afterEach);

  const triggerSuggestion = async (note: Generated.Note) => {
    const wrapper = helper
      .component(AISuggestion)
      .withStorageProps({
        selectedNote: note,
      })
      .mount();
    await wrapper.find(".btn").trigger("click");
    await flushPromises();
  };

  it("ask api to generate suggested description when description is empty", async () => {
    const note = makeMe.aNote.description("").please();
    const expectation = helper.apiMock
      .expectingPost(`/api/ai/${note.id}/ask-suggestions`)
      .andReturnOnce({ suggestion: "suggestion" });
    helper.apiMock.expectingPatch(`/api/text_content/${note.id}`);
    await triggerSuggestion(note);
    expect(expectation.actualRequestJsonBody()).toMatchObject({
      prompt: "Describe the note with title: Note1.1.1",
    });
  });

  it("ask api be called once when clicking the suggest button", async () => {
    const note = makeMe.aNote.please();
    const expectation = helper.apiMock
      .expectingPost(`/api/ai/${note.id}/ask-suggestions`)
      .andReturnOnce({ suggestion: "suggestion" });
    helper.apiMock.expectingPatch(`/api/text_content/${note.id}`);
    await triggerSuggestion(note);
    expect(expectation.actualRequestJsonBody()).toMatchObject({
      prompt: expect.stringContaining("Desc"),
    });
  });

  it('ask api be called many times until res.finishReason equal "stop" when clicking the suggest button', async () => {
    const note = makeMe.aNote.please();

    helper.apiMock
      .expectingPost(`/api/ai/${note.id}/ask-suggestions`)
      .andReturnOnce({ suggestion: "suggestion", finishReason: "length" });

    helper.apiMock
      .expectingPost(`/api/ai/${note.id}/ask-suggestions`)
      .andReturnOnce({ suggestion: "suggestion", finishReason: "stop" });
    helper.apiMock.expectingPatch(`/api/text_content/${note.id}`);
    helper.apiMock.expectingPatch(`/api/text_content/${note.id}`);

    await triggerSuggestion(note);
  });
});
