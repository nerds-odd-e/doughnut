import { flushPromises } from "@vue/test-utils";
import AISuggestDetailsButton from "@/components/toolbars/AISuggestDetailsButton.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

describe("AISuggestDetailsButton", () => {
  helper.resetWithApiMock(beforeEach, afterEach);

  const triggerSuggestionwithoutFlushPromises = async (
    note: Generated.Note,
  ) => {
    const wrapper = helper
      .component(AISuggestDetailsButton)
      .withStorageProps({
        selectedNote: note,
      })
      .mount();
    await wrapper.find(".btn").trigger("click");
    return wrapper;
  };

  const triggerSuggestion = async (note: Generated.Note) => {
    const wrapper = triggerSuggestionwithoutFlushPromises(note);
    await flushPromises();
    return wrapper;
  };

  it("ask api to generate suggested details when details is empty", async () => {
    const note = makeMe.aNote.details("").please();
    const expectation = helper.apiMock
      .expectingPost(`/api/ai/${note.id}/completion`)
      .andReturnOnce({ moreCompleteContent: "suggestion" });
    helper.apiMock.expectingPatch(`/api/text_content/${note.id}`);
    await triggerSuggestion(note);
    expect(expectation.actualRequestJsonBody()).toMatchObject({
      detailsToComplete: "",
    });
  });

  it("ask api be called once when clicking the suggest button", async () => {
    const note = makeMe.aNote.please();
    const expectation = helper.apiMock
      .expectingPost(`/api/ai/${note.id}/completion`)
      .andReturnOnce({ moreCompleteContent: "suggestion" });
    helper.apiMock.expectingPatch(`/api/text_content/${note.id}`);
    await triggerSuggestion(note);
    expect(expectation.actualRequestJsonBody()).toMatchObject({
      detailsToComplete: "<p>Desc</p>",
    });
  });

  it('ask api be called many times until res.finishReason equal "stop" when clicking the suggest button', async () => {
    const note = makeMe.aNote.please();

    helper.apiMock
      .expectingPost(`/api/ai/${note.id}/completion`)
      .andReturnOnce({
        moreCompleteContent: "suggestion",
        finishReason: "length",
      });

    helper.apiMock
      .expectingPost(`/api/ai/${note.id}/completion`)
      .andReturnOnce({
        moreCompleteContent: "suggestion",
        finishReason: "stop",
      });
    helper.apiMock.expectingPatch(`/api/text_content/${note.id}`);
    helper.apiMock.expectingPatch(`/api/text_content/${note.id}`);

    await triggerSuggestion(note);
  });

  it("stop calling if the component is unmounted", async () => {
    const note = makeMe.aNote.please();

    helper.apiMock
      .expectingPost(`/api/ai/${note.id}/completion`)
      .andReturnOnce({
        moreCompleteContent: "suggestion",
        finishReason: "length",
      });

    helper.apiMock.expectingPatch(`/api/text_content/${note.id}`);

    const wrapper = await triggerSuggestionwithoutFlushPromises(note);
    wrapper.unmount();
    await flushPromises();
    // AI completion API should be called only once, although the finishReaon is "length."
    // Because the component is unmounted.
  });
});
