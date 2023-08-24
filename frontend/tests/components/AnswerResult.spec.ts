import { flushPromises } from "@vue/test-utils";
import AnswerResult from "@/components/review/AnswerResult.vue";
import helper from "../helpers";
// import makeMe from "../fixtures/makeMe";

helper.resetWithApiMock(beforeEach, afterEach);

describe("AnswerResult", () => {
  const wrapper = helper
    .component(AnswerResult)
    .withProps({
      answeredQuestion: {
        answerId: 1,
        correct: false,
        choiceIndex: 1,
        answerDisplay: "answerDisplay",
        quizQuestion: {
          choices: [
            {
              reason: "The quick brown fox jumps over the lazy dog.",
            },
            {
              reason: "The quick brown fox jumps over the lazy dog (2).",
            },
            {
              reason: "The quick brown fox jumps over the lazy dog (3).",
            },
            {
              reason: "The quick brown fox jumps over the lazy dog (4).",
            },
          ],
        },
      },
    })
    .mount();

  it("reason exists when my answer is wrong", async () => {
    await flushPromises();
    expect(wrapper.find("#incorrectReason")).toBeDefined();
  });

  it("get reason from backend when my selected answer is wrong", async () => {
    await flushPromises();
    expect(wrapper.find("#incorrectReason").text()).toEqual(
      "The quick brown fox jumps over the lazy dog (2).",
    );
  });
});
