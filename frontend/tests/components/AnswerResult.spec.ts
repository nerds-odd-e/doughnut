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
        answerDisplay: "answerDisplay",
        quizQuestion: {
          choices: [
            {
              reason: "The quick brown fox jumps over the lazy dog.",
            },
          ],
        },
      },
    })
    .mount();
  // beforeEach(async () => {
  //   const note = makeMe.aNoteRealm.please();
  //   const quizQuestion = makeMe.aQuizQuestion
  //     .withQuestionType("AI_QUESTION")
  //     .withQuestionStem("any question?")
  //     .withChoices(["option A", "option B", "option C"])
  //     .please();
  //   // helper.apiMock
  //   //   .expectingPost(`/api/ai/generate-question?note=${note.id}`)
  //   //   .andReturnOnce(quizQuestion);
  // });

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
    await flushPromises();
    expect(wrapper.find("#incorrectReason")).toBeDefined();
  });
  it("get reason from backend when my answer is wrong", async () => {
    await flushPromises();
    expect("The quick brown fox jumps over the lazy dog.").toEqual(
      wrapper.find("#incorrectReason").text(),
    );
  });
});
