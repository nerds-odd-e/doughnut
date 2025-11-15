import { flushPromises } from "@vue/test-utils"
import AnsweredQuestionPage from "@/pages/AnsweredQuestionPage.vue"
import helper from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"

const mockedPush = vi.fn()
vitest.mock("vue-router", () => ({
  useRouter: () => ({
    push: mockedPush,
  }),
}))

describe("answered question page", () => {
  describe("answered question page for a link", () => {
    const REVIEW_QUESTION_ID = 1
    const link = makeMe.aLink.please()
    const mockedShowAnswerCall = vi.fn()
    const answeredQuestion = makeMe.anAnsweredQuestion
      .withNote(link)
      .withRecallPromptId(REVIEW_QUESTION_ID)
      .please()

    beforeEach(async () => {
      vitest.resetAllMocks()
      vi.spyOn(helper.managedApi.services, "showQuestion").mockResolvedValue(
        answeredQuestion as never
      )
      vi.spyOn(helper.managedApi.services, "show").mockResolvedValue(
        makeMe.aNoteRealm.please() as never
      )
      vi.spyOn(
        helper.managedApi.services,
        "startConversationAboutRecallPrompt"
      ).mockResolvedValue({ id: 123 } as never)
    })

    it("click on note when doing review", async () => {
      const wrapper = helper
        .component(AnsweredQuestionPage)
        .withStorageProps({ recallPromptId: REVIEW_QUESTION_ID })
        .currentRoute({ name: "recall" })
        .mount()
      await flushPromises()
      wrapper.find(".note-under-question").trigger("click")
      await flushPromises()
      expect(helper.managedApi.services.showQuestion).toHaveBeenCalledWith({
        recallPrompt: REVIEW_QUESTION_ID,
      })
    })

    describe("conversation button", () => {
      it("should start a conversation and redirect to message center", async () => {
        const wrapper = helper
          .component(AnsweredQuestionPage)
          .withStorageProps({ recallPromptId: REVIEW_QUESTION_ID })
          .mount()

        await flushPromises()

        const button = wrapper.find(".conversation-button")
        expect(button.attributes("title")).toBe(
          "Start a conversation about this question"
        )

        await button.trigger("click")
        await flushPromises()

        expect(
          helper.managedApi.services.startConversationAboutRecallPrompt
        ).toHaveBeenCalledWith({ recallPrompt: REVIEW_QUESTION_ID })
        expect(mockedPush).toHaveBeenCalledWith({
          name: "messageCenter",
          params: { conversationId: 123 },
        })
      })

      it("should not show conversation button when answer is not loaded", () => {
        mockedShowAnswerCall.mockResolvedValue(undefined)
        const wrapper = helper
          .component(AnsweredQuestionPage)
          .withStorageProps({ recallPromptId: REVIEW_QUESTION_ID })
          .mount()

        expect(wrapper.find(".conversation-button").exists()).toBe(false)
      })
    })
  })
})
