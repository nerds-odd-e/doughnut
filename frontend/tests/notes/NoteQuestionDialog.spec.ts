import { flushPromises } from "@vue/test-utils";
import { beforeEach, expect } from "vitest";
import NoteQuestionDialog from "@/components/notes/NoteQuestionDialog.vue";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

const note = makeMe.aNoteRealm.please();
const createWrapper = async () => {
  const quizQuestion = makeMe.aQuizQuestion
    .withQuestionType("AI_QUESTION")
    .withQuestionStem("any question?")
    .withChoices(["option A", "option B", "option C"])
    .please();
  helper.apiMock
    .expectingPost(`/api/ai/generate-question?note=${note.id}`)
    .andReturnOnce(quizQuestion);
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

  it("regenerate question when asked", async () => {
    const wrapper = await createWrapper();

    const quizQuestion = makeMe.aQuizQuestion
      .withQuestionType("AI_QUESTION")
      .withQuestionStem("is it raining?")
      .please();
    helper.apiMock
      .expectingPost(`/api/ai/generate-question?note=${note.id}`)
      .andReturnOnce(quizQuestion);
    wrapper.find("button").trigger("click");
    await flushPromises();
    expect(wrapper.text()).toContain("any question?");
    expect(wrapper.text()).toContain("is it raining?");
  });

  it("When the ask button is clicked, the anwser from AI will be displayed", async () => {
    // Given
    const expected = "I'm ChatGPT";
    // setUp
    helper.apiMock
      .expectingPost("/api/v1/chat")
      .andReturnOnce({ answer: expected });

    // When
    const wrapper = await createWrapper();

    await wrapper.find("#ask-input").setValue("What's your name?");
    await wrapper.find("#ask-button").trigger("submit");
    await flushPromises();

    // Then
    wrapper.find(".ask-answer-container").isVisible();
    const actual = wrapper.find("#ask-answer").text();
    expect(actual).toBe(expected);
  });
});
