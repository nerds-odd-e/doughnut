import AnswerResult from "@/components/review/AnswerResult.vue"
import { AnsweredQuestion } from "@/generated/backend"
import { flushPromises } from "@vue/test-utils"
import makeMe from "../fixtures/makeMe"
import helper from "../helpers"

describe("AnswerResult", () => {
  const answeredQuestion: AnsweredQuestion = {
    answerId: 1,
    correct: false,
    choiceIndex: 1,
    answerDisplay: "answerDisplay",
    quizQuestionAndAnswer: makeMe.aQuizQuestionAndAnswer.please(),
  }
  const wrapper = helper
    .component(AnswerResult)
    .withProps({ answeredQuestion })
    .mount()

  it("reason exists when my answer is wrong", async () => {
    await flushPromises()
    expect(wrapper.find("#incorrectReason")).toBeDefined()
  })
})
