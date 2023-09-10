import { flushPromises } from "@vue/test-utils";
import { beforeEach, expect } from "vitest";
import NoteChatDialog from "@/components/notes/NoteChatDialog.vue";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

const note = makeMe.aNoteRealm.please();
const admin = makeMe.aUser().admin(true).please();
const learner = makeMe.aUser().admin(false).please();
const createWrapper = async (isAdmin = false) => {
  const wrapper = helper
    .component(NoteChatDialog)
    .withStorageProps({
      selectedNote: note.note,
      user: isAdmin ? admin : learner,
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

  it("should allow admin to enter a custom model without specifying temperature", async () => {
    const customModel = "gpt-4";
    helper.apiMock
      .expectingPost(
        `/api/ai/generate-question-with-custom-config?note=${note.id}&model=${customModel}&temperature=1`,
      )
      .andReturnOnce(quizQuestion);
    const wrapper = await createWrapper(true);
    await wrapper.find(".custom-model-input input").setValue(customModel);
    await wrapper.find("button").trigger("click");
    await flushPromises();
  });

  it("should allow admin to change temperature without specifying model", async () => {
    const temperature = 0.9;
    helper.apiMock
      .expectingPost(
        `/api/ai/generate-question-with-custom-config?note=${note.id}&model=&temperature=${temperature}`,
      )
      .andReturnOnce(quizQuestion);
    const wrapper = await createWrapper(true);
    await wrapper.find("input[type=range]").setValue(temperature);
    await wrapper.find("button").trigger("click");
    await flushPromises();
  });

  it("should allow admin to change temperature and model", async () => {
    const temperature = 0.9;
    const customModel = "my-custom-model";
    helper.apiMock
      .expectingPost(
        `/api/ai/generate-question-with-custom-config?note=${note.id}&model=${customModel}&temperature=${temperature}`,
      )
      .andReturnOnce(quizQuestion);
    const wrapper = await createWrapper(true);
    await wrapper.find(".custom-model-input input").setValue(customModel);
    await wrapper.find("input[type=range]").setValue(temperature);
    await wrapper.find("button").trigger("click");
    await flushPromises();
  });

  it("should show error to admin when invalid custom model is used", async () => {
    const customModel = "my-custom-model";
    helper.apiMock
      .expectingPost(
        `/api/ai/generate-question-with-custom-config?note=${note.id}&model=${customModel}&temperature=1`,
      )
      .andRespondOnce({ status: 500 });
    const wrapper = await createWrapper(true);
    await wrapper.find(".custom-model-input input").setValue(customModel);
    await wrapper.find("button").trigger("click");
    await flushPromises();
    expect(wrapper.find(".custom-model-error").exists()).toBe(true);
    expect(wrapper.text()).toContain("Invalid custom model input");
  });

  it("should not show error to admin when valid custom model is used", async () => {
    const customModel = "gpt-4";
    helper.apiMock
      .expectingPost(
        `/api/ai/generate-question-with-custom-config?note=${note.id}&model=${customModel}&temperature=1`,
      )
      .andReturnOnce(quizQuestion);
    const wrapper = await createWrapper(true);
    await wrapper.find(".custom-model-input input").setValue(customModel);
    await wrapper.find("button").trigger("click");
    await flushPromises();
    expect(wrapper.find(".custom-model-error").exists()).toBe(false);
    expect(wrapper.text()).not.toContain("Invalid custom model input");
  });

  it("should not show error to admin after retrying with valid custom model", async () => {
    const customModelInvalid = "my-custom-model";
    helper.apiMock
      .expectingPost(
        `/api/ai/generate-question-with-custom-config?note=${note.id}&model=${customModelInvalid}&temperature=1`,
      )
      .andRespondOnce({ status: 500 });
    const wrapper = await createWrapper(true);
    await wrapper
      .find(".custom-model-input input")
      .setValue(customModelInvalid);
    await wrapper.find("button").trigger("click");
    await flushPromises();
    expect(wrapper.find(".custom-model-error").exists()).toBe(true);
    expect(wrapper.text()).toContain("Invalid custom model input");
    const customModelValid = "gpt-4";
    helper.apiMock
      .expectingPost(
        `/api/ai/generate-question-with-custom-config?note=${note.id}&model=${customModelValid}&temperature=1`,
      )
      .andReturnOnce(quizQuestion);
    await wrapper.find(".custom-model-input input").setValue(customModelValid);
    await wrapper.find("button").trigger("click");
    await flushPromises();
    expect(wrapper.find(".custom-model-error").exists()).toBe(false);
    expect(wrapper.text()).not.toContain("Invalid custom model input");
  });

  it("should not show custom model input to learner", async () => {
    const wrapper = await createWrapper();
    expect(wrapper.find(".custom-model-input input").exists()).toBe(false);
  });

  it("should not show temperature range input to learner", async () => {
    const wrapper = await createWrapper();
    expect(wrapper.find("input[type=range]").exists()).toBe(false);
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
