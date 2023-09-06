import { flushPromises } from "@vue/test-utils";
import { beforeEach, expect } from "vitest";
import NoteChatDialog from "@/components/notes/NoteChatDialog.vue";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

const note = makeMe.aNoteRealm.please();
const createWrapper = async () => {
  const wrapper = helper
    .component(NoteChatDialog)
    .withStorageProps({ selectedNote: note.note })
    .mount();
  await flushPromises();
  return wrapper;
};

describe("NoteChatDialog TestMe", () => {
  let expectation;
  beforeEach(() => {
    const quizQuestion = makeMe.aQuizQuestion
      .withQuestionType("AI_QUESTION")
      .withQuestionStem("any question?")
      .withChoices(["option A", "option B", "option C"])
      .please();

    expectation = helper.apiMock
      .expectingPost(`/api/ai/generate-question?note=${note.id}`)
      .andReturnOnce(quizQuestion);
  });

  it("render the question returned", async () => {
    const wrapper = await createWrapper();
    wrapper.find("button").trigger("click");
    await flushPromises();
    expect(wrapper.text()).toContain("any question?");
    expect(wrapper.text()).toContain("option A");
    expect(wrapper.text()).toContain("option C");
  });

  it("should allow user to enter a custom model in test environment", async () => {
    const wrapper = await createWrapper();
    await wrapper.find(".custom-model-input input").setValue("my-custom-model");
    await wrapper.find("button").trigger("click");
    await flushPromises();
    expect(expectation.actualRequestJsonBody()).toMatchObject({
      model: "my-custom-model",
    });
  });

  describe("when it is in production environment", () => {
    let original;

    beforeEach(() => {
      original = window.location;
      Object.defineProperty(window, "location", {
        value: {
          href: "www.odd-e.com",
        },
      });
    });

    afterEach(() => {
      Object.defineProperty(window, "location", original);
    });

    it("should hide custom model input in production environment until feature is fully developed", async () => {
      const wrapper = await createWrapper();
      expect(wrapper.find(".custom-model-input input").exists()).toBe(false);
      wrapper.find("button").trigger("click");
      await flushPromises();
    });
  });

  it("regenerate question when asked", async () => {
    const wrapper = await createWrapper();
    wrapper.find("button").trigger("click");
    await flushPromises();

    const quizQuestion = makeMe.aQuizQuestion
      .withQuestionType("AI_QUESTION")
      .withQuestionStem("is it raining?")
      .please();
    helper.apiMock
      .expectingPost(`/api/ai/generate-question?note=${note.id}`)
      .andReturnOnce(quizQuestion);
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
