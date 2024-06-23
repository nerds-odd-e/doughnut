import scrollToElement from "@/components/commons/scrollToElement"
import NoteChatDialog from "@/components/notes/NoteChatDialog.vue"
import { Message, MessageDelta } from "@/generated/backend"
import { VueWrapper, flushPromises } from "@vue/test-utils"
import { beforeEach, expect } from "vitest"
import makeMe from "../fixtures/makeMe"
import helper from "../helpers"

vitest.mock("@/components/commons/scrollToElement")

const note = makeMe.aNoteRealm.please()
const createWrapper = async () => {
  const wrapper = helper
    .component(NoteChatDialog)
    .withStorageProps({
      selectedNote: note.note,
    })
    .mount()
  await flushPromises()
  return wrapper
}

describe("NoteChatDialog TestMe", () => {
  const mockedGenerateQuestion = vitest.fn()

  beforeEach(() => {
    helper.managedApi.restQuizQuestionController.generateQuestion =
      mockedGenerateQuestion
  })

  const quizQuestionInNotebook = makeMe.aQuizQuestionInNotebook
    .withQuestionStem("any question?")
    .withChoices(["option A", "option B", "option C"])
    .please()

  it("render the question returned", async () => {
    mockedGenerateQuestion.mockResolvedValue(quizQuestionInNotebook)
    const wrapper = await createWrapper()
    wrapper.find("button").trigger("click")
    await flushPromises()
    expect(wrapper.text()).toContain("any question?")
    expect(wrapper.text()).toContain("option A")
    expect(wrapper.text()).toContain("option C")
    expect(mockedGenerateQuestion).toHaveBeenCalledWith(note.id)
  })

  it("scroll to bottom", async () => {
    mockedGenerateQuestion.mockResolvedValue(quizQuestionInNotebook)
    const wrapper = await createWrapper()
    wrapper.find("button").trigger("click")
    await flushPromises()
    expect(scrollToElement).toHaveBeenCalled()
  })

  describe("NoteChatDialog Conversation", () => {
    let wrapper: VueWrapper
    const mockedContest = vitest.fn()
    const mockedRegenerate = vitest.fn()

    const newQuestion = makeMe.aQuizQuestion
      .withQuestionStem("is it raining?")
      .please()

    beforeEach(async () => {
      mockedGenerateQuestion.mockResolvedValueOnce(quizQuestionInNotebook)
      helper.managedApi.restQuizQuestionController.contest =
        mockedContest.mockResolvedValue({})
      helper.managedApi.restQuizQuestionController.regenerate =
        mockedRegenerate.mockResolvedValue(newQuestion)
      wrapper = await createWrapper()
      wrapper.find("button").trigger("click")
      await flushPromises()
    })

    it("calls the api", async () => {
      wrapper.find("a#try-again").trigger("click")
      await flushPromises()
      expect(mockedContest).toHaveBeenCalledWith(
        quizQuestionInNotebook.quizQuestion.id,
      )
      expect(mockedRegenerate).toHaveBeenCalledWith(
        quizQuestionInNotebook.quizQuestion.id,
      )
    })

    it("regenerate question when asked", async () => {
      wrapper.find("a#try-again").trigger("click")
      await flushPromises()
      expect(wrapper.text()).toContain("any question?")
      expect(wrapper.text()).toContain("is it raining?")
    })

    it("should scroll to the end", async () => {
      wrapper.find("a#try-again").trigger("click")
      await flushPromises()
      expect(scrollToElement).toHaveBeenCalled()
    })
  })
})

describe("NoteChatDialog Conversation", () => {

  beforeEach(() => {
    helper.managedApi.eventSource.restAiController.chat = vi
      .fn()
  })

  const askAndReplied = async () => {
    const newMessage: Message = {
          role: "assistant",
          thread_id: "test-thread-id",
          content: [],
        }
  const messageDelta: MessageDelta = {
    delta: {
        content: [
          {
            text: {
              value: "## I'm ChatGPT"
            },
          },
        ],
      }
    }


   const wrapper = await createWrapper()
    await wrapper.find("textarea").setValue("What's your name?")
    await wrapper.find("#chat-button").trigger("submit")
    helper.managedApi.eventSource.eventSourceRequest.onMessage("thread.message.created", JSON.stringify(newMessage))
    helper.managedApi.eventSource.eventSourceRequest.onMessage("thread.message.delta", JSON.stringify(messageDelta))
    await flushPromises()
    return wrapper
  }

  it("called the api", async () => {
   await askAndReplied()
    expect(helper.managedApi.eventSource.restAiController.chat).toHaveBeenCalledWith(
      note.id,
      expect.anything(),
    )
  })

  it("shows the user's message as well", async () => {
   const wrapper = await askAndReplied()
    const actual = wrapper.find(".chat-answer-container.user").text()
    expect(actual).toBe("What's your name?")
  })

  it("When the chat button is clicked, the anwser from AI will be displayed", async () => {
    const wrapper = await askAndReplied()
    const actual = wrapper.find(".chat-answer-container.assistant").text()
    expect(actual).toBe("I'm ChatGPT")
  })

  it("has one assistant icon", async () => {
    const wrapper = await askAndReplied()
    expect(wrapper.findAll(".assistant-icon")).toHaveLength(1)
  })

  it("called the api again but with the threadId", async () => {
    const wrapper = await askAndReplied()
    await wrapper.find("textarea").setValue("What's your name?")
    await wrapper.find("#chat-button").trigger("submit")
    await flushPromises()
    expect(helper.managedApi.eventSource.restAiController.chat).toHaveBeenCalledWith(
      note.id,
      expect.objectContaining({
        threadId: "test-thread-id",
      }),
    )
  })

})
