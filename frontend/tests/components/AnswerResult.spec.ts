import { flushPromises } from "@vue/test-utils";
import AnswerResult from "@/components/review/AnswerResult.vue";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

describe("AnswerResult", () => {
  // it("xxx", async () => {
  //   // Doing now...
  //   const wrapper = helper
  //     .component(AnswerResult)
  //     .withProps({
  //       answeredQuestion: {
  //         answerId: 1,
  //         correct: false,
  //         answerDisplay: "answerDisplay",
  //         quizQuestion: {},
  //       },
  //     })
  //     .mount();

  //   await flushPromises();
  //   expect(wrapper.emitted()["update:modelValue"]).toBeUndefined();
  // });
  it("reason exists when my answer is wrong", async () => {
    const wrapper = helper
      .component(AnswerResult)
      .withProps({
        answeredQuestion: {
          answerId: 1,
          correct: false,
          answerDisplay: "answerDisplay",
          quizQuestion: {},
        },
      })
      .mount();

    await flushPromises();
    expect(wrapper.find("#incorrectReason")).toBeDefined();
  });
});
