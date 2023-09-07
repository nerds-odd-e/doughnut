import { flushPromises } from "@vue/test-utils";
import { beforeEach, expect } from "vitest";
import NoteChatDialog from "@/components/notes/NoteChatDialog.vue";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

const note = makeMe.aNoteRealm.please();
const developer = makeMe.aUser().setIsDeveloper(true).please();
const learner = makeMe.aUser().setIsDeveloper(false).please();
const createWrapper = async (isDeveloper = false) => {
  const wrapper = helper
    .component(NoteChatDialog)
    .withStorageProps({
      selectedNote: note.note,
      user: isDeveloper ? developer : learner,
    })
    .mount();
  await flushPromises();
  return wrapper;
};

describe("NoteChatDialog TestMe", () => {
  const quizQuestion = makeMe.aQuizQuestion
    .withQuestionType("AI_QUESTION")
    .withQuestionStem("any question?")
    .withChoices(["option A", "option B", "option C"])
    .please();

  it("render the question returned", async () => {
    helper.apiMock
      .expectingPost(`/api/ai/generate-question?note=${note.id}`)
      .andReturnOnce(quizQuestion);
    const wrapper = await createWrapper();
    wrapper.find("button").trigger("click");
    await flushPromises();
    expect(wrapper.text()).toContain("any question?");
    expect(wrapper.text()).toContain("option A");
    expect(wrapper.text()).toContain("option C");
  });

  it("should allow developer to enter a custom model", async () => {
    const customModel = "my-custom-model";
    helper.apiMock
      .expectingPost(
        `/api/ai/generate-question-with-custom-model?note=${note.id}&model=${customModel}`,
      )
      .andReturnOnce(quizQuestion);
    const wrapper = await createWrapper(true);
    await wrapper.find(".custom-model-input input").setValue(customModel);
    await wrapper.find("button").trigger("click");
    await flushPromises();
  });

  it("should not show custom model input to learner", async () => {
    const wrapper = await createWrapper();
    expect(wrapper.find(".custom-model-input input").exists()).toBe(false);
  });

  it("regenerate question when asked", async () => {
    helper.apiMock
      .expectingPost(`/api/ai/generate-question?note=${note.id}`)
      .andReturnOnce(quizQuestion);
    const wrapper = await createWrapper();
    wrapper.find("button").trigger("click");
    await flushPromises();

    const newQuestion = makeMe.aQuizQuestion
      .withQuestionType("AI_QUESTION")
      .withQuestionStem("is it raining?")
      .please();
    helper.apiMock
      .expectingPost(`/api/ai/generate-question?note=${note.id}`)
      .andReturnOnce(newQuestion);
    wrapper.find("button#try-again").trigger("click");
    await flushPromises();
    expect(wrapper.text()).toContain("any question?");
    expect(wrapper.text()).toContain("is it raining?");
  });
});

describe("NoteChatDialog Conversation", () => {
  it("When the chat button is clicked, the anwser from AI will be displayed", async () => {
    // Given
    const expected = "I'm ChatGPT";
    const response: Generated.ChatResponse = { assistantMessage: expected };
    // setUp
    helper.apiMock
      .expectingPost(`/api/ai/chat?note=${note.id}`)
      .andReturnOnce(response);

    // When
    const wrapper = await createWrapper();

    await wrapper.find("#chat-input").setValue("What's your name?");
    await wrapper.find("#chat-button").trigger("submit");
    await flushPromises();

    // Then
    wrapper.find(".chat-answer-container").isVisible();
    const actual = wrapper.find("#chat-answer").text();
    expect(actual).toBe(expected);
  });
});
