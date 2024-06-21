import scrollToElement from "@/components/commons/scrollToElement"
import NoteChatDialog from "@/components/notes/NoteChatDialog.vue"
import { AiAssistantResponse } from "@/generated/backend"
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
  it("When the chat button is clicked, the anwser from AI will be displayed", async () => {
    const expected = "I'm ChatGPT"
    const response: AiAssistantResponse = {
      messages: [
        {
          content: [
            {
              text: {
                value: expected,
              },
            },
          ],
        },
      ],
    }
    helper.managedApi.restAiController.chat = vi
      .fn()
      .mockResolvedValue(response)

    const wrapper = await createWrapper()

    await wrapper.find("#chat-input").setValue("What's your name?")
    await wrapper.find("#chat-button").trigger("submit")
    await flushPromises()

    wrapper.find(".chat-answer-container").isVisible()
    const actual = wrapper.find("#chat-answer").text()
    expect(actual).toBe(expected)
    expect(helper.managedApi.restAiController.chat).toHaveBeenCalledWith(
      note.id,
      expect.anything(),
    )
  })
})
