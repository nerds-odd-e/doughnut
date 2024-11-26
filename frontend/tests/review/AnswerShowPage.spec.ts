import { flushPromises } from "@vue/test-utils"
import AnsweredQuestionPage from "@/pages/AnsweredQuestionPage.vue"
import helper from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"
import type { AnsweredQuestion } from "@/generated/backend"

const mockedPush = vi.fn()
vitest.mock("vue-router", () => ({
  useRouter: () => ({
    push: mockedPush,
  }),
}))

describe("repetition page", () => {
  describe("repetition page for a link", () => {
    const link = makeMe.aLink.please()
    const mockedShowAnswerCall = vi.fn()
    const mockedNotePositionCall = vi.fn()
    const mockedStartConversationCall = vi.fn()
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
      helper.managedApi.restConversationMessageController.startConversationAboutReviewQuestion =
        mockedStartConversationCall.mockResolvedValue({ id: 123 })
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

    describe("conversation button", () => {
      it("should start a conversation and redirect to message center", async () => {
        const wrapper = helper
          .component(AnsweredQuestionPage)
          .withStorageProps({ reviewQuestionInstanceId: 1 })
          .mount()

        await flushPromises()

        const button = wrapper.find(".conversation-button")
        expect(button.attributes("title")).toBe(
          "Start a conversation about this question"
        )

        await button.trigger("click")
        await flushPromises()

        expect(mockedStartConversationCall).toHaveBeenCalledWith(1)
        expect(mockedPush).toHaveBeenCalledWith({
          name: "messageCenter",
          params: { conversationId: 123 },
        })
      })

      it("should not show conversation button when answer is not loaded", () => {
        mockedShowAnswerCall.mockResolvedValue(undefined)
        const wrapper = helper
          .component(AnsweredQuestionPage)
          .withStorageProps({ reviewQuestionInstanceId: 1 })
          .mount()

        expect(wrapper.find(".conversation-button").exists()).toBe(false)
      })
    })
  })
})
