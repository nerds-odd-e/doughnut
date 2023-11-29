import { flushPromises } from "@vue/test-utils";
import AISuggestDetailsButton from "@/components/toolbars/AISuggestDetailsButton.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

describe("AISuggestDetailsButton", () => {
  const note = makeMe.aNote.please();

  helper.resetWithApiMock(beforeEach, afterEach);

  const triggerSuggestionwithoutFlushPromises = async (
    selectedNote: Generated.Note,
  ) => {
    const wrapper = helper
      .component(AISuggestDetailsButton)
      .withStorageProps({ selectedNote })
      .mount();
    await wrapper.find(".btn").trigger("click");
    return wrapper;
  };

  const triggerSuggestion = async (n: Generated.Note) => {
    const wrapper = triggerSuggestionwithoutFlushPromises(n);
    await flushPromises();
    return wrapper;
  };

  it("ask api to generate suggested details when details is empty", async () => {
    const noteWithNoDetails = makeMe.aNote.details("").please();
    const expectation = helper.apiMock
      .expectingPost(`/api/ai/${noteWithNoDetails.id}/completion`)
      .andReturnOnce({ moreCompleteContent: "suggestion" });
    helper.apiMock.expectingPatch(`/api/text_content/${noteWithNoDetails.id}`);
    await triggerSuggestion(noteWithNoDetails);
    expect(expectation.actualRequestJsonBody()).toMatchObject({
      detailsToComplete: "",
    });
  });

  it("ask api be called once when clicking the suggest button", async () => {
    const expectation = helper.apiMock
      .expectingPost(`/api/ai/${note.id}/completion`)
      .andReturnOnce({ moreCompleteContent: "suggestion" });
    helper.apiMock.expectingPatch(`/api/text_content/${note.id}`);
    await triggerSuggestion(note);
    expect(expectation.actualRequestJsonBody()).toMatchObject({
      detailsToComplete: "<p>Desc</p>",
    });
  });

  it("get more completed content and update", async () => {
    helper.apiMock
      .expectingPost(`/api/ai/${note.id}/completion`)
      .andReturnOnce({
        moreCompleteContent: "suggestion",
        finishReason: "stop",
      });
    helper.apiMock.expectingPatch(`/api/text_content/${note.id}`);

    await triggerSuggestion(note);
  });

  it("stop updating if the component is unmounted", async () => {
    helper.apiMock
      .expectingPost(`/api/ai/${note.id}/completion`)
      .andReturnOnce({
        moreCompleteContent: "suggestion",
        finishReason: "stop",
      });

    const wrapper = await triggerSuggestionwithoutFlushPromises(note);
    wrapper.unmount();
    await flushPromises();
    // no future api call expected.
    // Because the component is unmounted.
  });
});
