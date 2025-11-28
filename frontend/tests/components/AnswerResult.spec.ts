import AnswerResult from "@/components/review/AnswerResult.vue"
import { flushPromises } from "@vue/test-utils"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"

describe("AnswerResult", () => {
  const answeredQuestion = makeMe.anAnsweredQuestion
    .withQuestionId(1)
    .answerCorrect(false)
    .withChoiceIndex(1)
    .please()

  const wrapper = helper
    .component(AnswerResult)
    .withProps({ answeredQuestion })
    .mount()

  it("reason exists when my answer is wrong", async () => {
    await flushPromises()
    expect(wrapper.find("#incorrectReason")).toBeDefined()
  })
})
