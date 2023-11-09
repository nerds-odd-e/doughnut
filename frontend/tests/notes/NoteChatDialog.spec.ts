import { VueWrapper, flushPromises } from "@vue/test-utils";
import { beforeEach, expect } from "vitest";
import NoteChatDialog from "@/components/notes/NoteChatDialog.vue";
import scrollToElement from "@/components/commons/scrollToElement";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

vitest.mock("@/components/commons/scrollToElement");

helper.resetWithApiMock(beforeEach, afterEach);

const note = makeMe.aNoteRealm.please();
const createWrapper = async () => {
  const wrapper = helper
    .component(NoteChatDialog)
    .withStorageProps({
      selectedNote: note.note,
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
      .expectingPost(`/api/quiz-questions/generate-question?note=${note.id}`)
      .andReturnOnce(quizQuestion);
    const wrapper = await createWrapper();
    wrapper.find("button").trigger("click");
    await flushPromises();
    expect(wrapper.text()).toContain("any question?");
    expect(wrapper.text()).toContain("option A");
    expect(wrapper.text()).toContain("option C");
  });

  it("scroll to bottom", async () => {
    helper.apiMock
      .expectingPost(`/api/quiz-questions/generate-question?note=${note.id}`)
      .andReturnOnce(quizQuestion);
    const wrapper = await createWrapper();
    wrapper.find("button").trigger("click");
    await flushPromises();
    expect(scrollToElement).toHaveBeenCalled();
  });

  describe("NoteChatDialog Conversation", () => {
    let wrapper: VueWrapper;

    const newQuestion = makeMe.aQuizQuestion
      .withQuestionType("AI_QUESTION")
      .withQuestionStem("is it raining?")
      .please();

    beforeEach(async () => {
      helper.apiMock
        .expectingPost(`/api/quiz-questions/generate-question?note=${note.id}`)
        .andReturnOnce(quizQuestion);
      wrapper = await createWrapper();
      wrapper.find("button").trigger("click");
      await flushPromises();
      helper.apiMock.expectingPost(
        `/api/quiz-questions/${quizQuestion.quizQuestionId}/contest`,
      );
      helper.apiMock
        .expectingPost(
          `/api/quiz-questions/${quizQuestion.quizQuestionId}/regenerate`,
        )
        .andReturnOnce(newQuestion);
      vitest.clearAllMocks();
    });

    it.skip("regenerate question when asked", async () => {
      wrapper.find("a#try-again").trigger("click");
      await flushPromises();
      expect(wrapper.text()).toContain("any question?");
      expect(wrapper.text()).toContain("is it raining?");
    });

    it("should scroll to the end", async () => {
      wrapper.find("a#try-again").trigger("click");
      await flushPromises();
      expect(scrollToElement).toHaveBeenCalled();
    });
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
