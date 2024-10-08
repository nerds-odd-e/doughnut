import { flushPromises } from "@vue/test-utils"
import AnsweredQuestionPage from "@/pages/AnsweredQuestionPage.vue"
import helper from "../helpers"
import makeMe from "../fixtures/makeMe"
import type { AnsweredQuestion } from "@/generated/backend"

describe("repetition page", () => {
  describe("repetition page for a link", () => {
    const link = makeMe.aLink.please()
    const mockedShowAnswerCall = vitest.fn()
    const mockedNotePositionCall = vitest.fn()
    const answeredQuestion: AnsweredQuestion = {
      answer: {
        id: 1,
        correct: true,
      },
      answerDisplay: "",
      note: link,
      predefinedQuestion: makeMe.aPredefinedQuestion.please(),
      reviewQuestionInstanceId: 1,
    }
    beforeEach(async () => {
      vitest.resetAllMocks()
      helper.managedApi.restReviewQuestionController.showQuestion =
        mockedShowAnswerCall.mockResolvedValue(answeredQuestion)
      helper.managedApi.restNoteController.show1 =
        mockedNotePositionCall.mockResolvedValue(makeMe.aNoteRealm.please())
    })

    it("click on note when doing review", async () => {
      const wrapper = helper
        .component(AnsweredQuestionPage)
        .withStorageProps({ reviewQuestionInstanceId: 1 })
        .currentRoute({ name: "repeat" })
        .mount()
      await flushPromises()
      wrapper.find(".review-point-abbr").trigger("click")
      await flushPromises()
      expect(mockedShowAnswerCall).toHaveBeenCalledWith(1)
    })

    it("click on note when doing review and in a nested page", async () => {
      const wrapper = helper
        .component(AnsweredQuestionPage)
        .withStorageProps({ reviewQuestionInstanceId: 1 })
        .currentRoute({ name: "repeat-noteShow", params: { noteId: 123 } })
        .mount()
      await flushPromises()
      wrapper.find(".review-point-abbr").trigger("click")
    })
  })
})
