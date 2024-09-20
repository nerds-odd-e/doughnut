import AnswerResult from "@/components/review/AnswerResult.vue"
import type { AnsweredQuestion } from "@/generated/backend"
import { flushPromises } from "@vue/test-utils"
import makeMe from "../fixtures/makeMe"
import helper from "../helpers"

describe("AnswerResult", () => {
  const answeredQuestion: AnsweredQuestion = {
    answer: {
      id: 1,
      correct: false,
      choiceIndex: 1,
    },
    answerDisplay: "answerDisplay",
    predefinedQuestion: makeMe.aPredefinedQuestion.please(),
    reviewQuestionInstanceId: 1,
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
