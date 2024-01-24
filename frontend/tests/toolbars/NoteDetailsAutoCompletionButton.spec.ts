import { flushPromises } from "@vue/test-utils";
import NoteDetailsAutoCompletionButton from "@/components/toolbars/NoteDetailsAutoCompletionButton.vue";
import AIClarifyingQuestionDialog from "@/components/notes/AIClarifyingQuestionDialog.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

describe("NoteDetailsAutoCompletionButton", () => {
  const note = makeMe.aNote.please();

  helper.resetWithApiMock(beforeEach, afterEach);

  const triggerAutoCompletionWithoutFlushPromises = async (
    n: Generated.Note,
  ) => {
    const wrapper = helper
      .component(NoteDetailsAutoCompletionButton)
      .withStorageProps({ note: n })
      .mount();
    await wrapper.find(".btn").trigger("click");
    return wrapper;
  };

  const triggerAutoCompletion = async (n: Generated.Note) => {
    const wrapper = triggerAutoCompletionWithoutFlushPromises(n);
    await flushPromises();
    return wrapper;
  };

  it("ask api to generate details when details is empty", async () => {
    const noteWithNoDetails = makeMe.aNote.details("").please();
    const expectation = helper.apiMock
      .expectingPost(`/api/ai/${noteWithNoDetails.id}/completion`)
      .andReturnOnce({
        requiredAction: { contentToAppend: "auto completed content" },
      });
    helper.apiMock.expectingPatch(
      `/api/text_content/${noteWithNoDetails.id}/details`,
    );
    await triggerAutoCompletion(noteWithNoDetails);
    expect(expectation.actualRequestJsonBody()).toMatchObject({
      detailsToComplete: "",
    });
  });

  it("ask api be called once when clicking the auto-complete button", async () => {
    const expectation = helper.apiMock
      .expectingPost(`/api/ai/${note.id}/completion`)
      .andReturnOnce({
        requiredAction: { contentToAppend: "auto completed content" },
      });
    helper.apiMock.expectingPatch(`/api/text_content/${note.id}/details`);
    await triggerAutoCompletion(note);
    expect(expectation.actualRequestJsonBody()).toMatchObject({
      detailsToComplete: "<p>Desc</p>",
    });
  });

  it("get more completed content and update", async () => {
    helper.apiMock
      .expectingPost(`/api/ai/${note.id}/completion`)
      .andReturnOnce({
        requiredAction: {
          contentToAppend: "auto completed content",
        },
      });
    helper.apiMock.expectingPatch(`/api/text_content/${note.id}/details`);

    await triggerAutoCompletion(note);
  });

  it("ask for clarifying question", async () => {
    helper.apiMock
      .expectingPost(`/api/ai/${note.id}/completion`)
      .andReturnOnce({
        runId: "run-id",
        threadId: "thread-id",
        requiredAction: {
          toolCallId: "tool-call-id",
          clarifyingQuestion: {
            question: "what do you mean?",
          },
        },
      });
    const wrapper = await triggerAutoCompletion(note);
    const dialog = wrapper.getComponent(AIClarifyingQuestionDialog);
    dialog.find("input#note-answerToAI").setValue("I mean this");
    helper.apiMock.expectingPost(`/api/ai/answer-clarifying-question`);
    dialog.find("input[type='submit']").trigger("click");
    helper.apiMock.verifyCall(
      `/api/ai/answer-clarifying-question`,
      expect.objectContaining({
        body: expect.stringContaining("tool-call-id"),
      }),
    );
  });

  it("stop updating if the component is unmounted", async () => {
    helper.apiMock
      .expectingPost(`/api/ai/${note.id}/completion`)
      .andReturnOnce({
        requiredAction: {
          contentToAppend: "auto completed content",
        },
      });

    const wrapper = await triggerAutoCompletionWithoutFlushPromises(note);
    wrapper.unmount();
    await flushPromises();
    // no future api call expected.
    // Because the component is unmounted.
  });
});
