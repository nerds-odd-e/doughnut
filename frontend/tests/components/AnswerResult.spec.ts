import { flushPromises } from "@vue/test-utils";
import AnswerResult from "@/components/review/AnswerResult.vue";
import { AnsweredQuestion } from "@/generated/backend";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

helper.resetWithApiMock(beforeEach, afterEach);

describe("AnswerResult", () => {
  const answeredQuestion: AnsweredQuestion = {
    answerId: 1,
    correct: false,
    choiceIndex: 1,
    answerDisplay: "answerDisplay",
    quizQuestion: makeMe.aQuizQuestion.please(),
  };
  const wrapper = helper
    .component(AnswerResult)
    .withProps({ answeredQuestion })
    .mount();

  it("reason exists when my answer is wrong", async () => {
    await flushPromises();
    expect(wrapper.find("#incorrectReason")).toBeDefined();
  });
});
