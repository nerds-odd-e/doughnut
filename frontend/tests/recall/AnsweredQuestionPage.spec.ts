import { flushPromises } from "@vue/test-utils"
import AnsweredQuestionPage from "@/pages/AnsweredQuestionPage.vue"
import helper, { mockSdkService } from "@tests/helpers"
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
    const answeredQuestion = makeMe.anAnsweredQuestion
      .withNote(link)
      .withRecallPromptId(REVIEW_QUESTION_ID)
      .please()
    let showQuestionSpy: ReturnType<typeof mockSdkService<"showQuestion">>
    let startConversationSpy: ReturnType<
      typeof mockSdkService<"startConversationAboutRecallPrompt">
    >

    beforeEach(async () => {
      vitest.resetAllMocks()
      showQuestionSpy = mockSdkService("showQuestion", answeredQuestion)
      mockSdkService("showNote", makeMe.aNoteRealm.please())
      startConversationSpy = mockSdkService(
        "startConversationAboutRecallPrompt",
        makeMe.aConversation.withId(123).please()
      )
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
      expect(showQuestionSpy).toHaveBeenCalledWith({
        path: { recallPrompt: REVIEW_QUESTION_ID },
        client: expect.anything(),
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

        expect(startConversationSpy).toHaveBeenCalledWith({
          path: { recallPrompt: REVIEW_QUESTION_ID },
        })
        expect(mockedPush).toHaveBeenCalledWith({
          name: "messageCenter",
          params: { conversationId: 123 },
        })
      })

      it("should not show conversation button when answer is not loaded", () => {
        const wrapper = helper
          .component(AnsweredQuestionPage)
          .withStorageProps({ recallPromptId: REVIEW_QUESTION_ID })
          .mount()

        expect(wrapper.find(".conversation-button").exists()).toBe(false)
      })
    })
  })
})
