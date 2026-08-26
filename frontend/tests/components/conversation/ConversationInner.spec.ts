import { ConversationMessageController } from "@generated/doughnut-backend-api/sdk.gen"
import ConversationInner from "@/components/conversations/ConversationInner.vue"
import { type ConversationMessage } from "@generated/doughnut-backend-api"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService, mockShowNote } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { expect, vi } from "vitest"
import AiReplyEventSource from "@/managedApi/AiReplyEventSource"
import { resetInstance } from "@tests/helpers/aiReplyEventSourceTracker"
import { simulateAiResponse } from "./aiResponseTestSupport"

vi.mock("@/managedApi/AiReplyEventSource", async () => {
  const { aiReplyEventSourceTrackingMock } = await import(
    "./aiReplyEventSourceTrackingMock"
  )
  return aiReplyEventSourceTrackingMock()
})

const setupTestData = () => {
  const note = makeMe.aNote.content("").please()
  const conversation = makeMe.aConversation.forANote(note).please()
  const user = makeMe.aUser.please()
  return { note, conversation, user }
}

const mountComponent = (conversation, user) =>
  helper
    .component(ConversationInner)
    .withCleanStorage()
    .withProps({ conversation, user })
    .mount()

const submitMessage = async (wrapper, message: string) => {
  await wrapper.find("textarea").setValue(message)
  await wrapper.find("form.message-input-form").trigger("submit")
  await flushPromises()
}

describe("ConversationInner", () => {
  let wrapper
  let conversation
  let user
  let replyToConversationSpy: ReturnType<typeof mockSdkService>

  beforeEach(() => {
    window.HTMLElement.prototype.scrollIntoView = vi.fn()
    mockShowNote()
    replyToConversationSpy = mockSdkService(
      ConversationMessageController,
      "replyToConversation",
      undefined
    )
    mockSdkService(ConversationMessageController, "getConversationMessages", [])
    resetInstance()
    vi.spyOn(AiReplyEventSource.prototype, "start").mockImplementation(vi.fn())

    const testData = setupTestData()
    conversation = testData.conversation
    user = testData.user

    wrapper = mountComponent(conversation, user)
  })

  afterEach(() => {
    resetInstance()
  })

  it("updates ScrollTo trigger when messages change", async () => {
    wrapper.vm.currentConversationMessages = [
      { id: 1, message: "Hello", sender: user },
      { id: 2, message: "Hi", sender: user },
    ] satisfies ConversationMessage[]
    await wrapper.vm.$nextTick()

    expect(
      wrapper.findComponent({ name: "ScrollTo" }).props("scrollTrigger")
    ).toBe(2)
  })

  describe("Form submission", () => {
    it("disables submit button for empty or whitespace-only messages", async () => {
      const button = wrapper.find('button[type="submit"]')
      const textarea = wrapper.find("textarea")

      expect(button.attributes("disabled")).toBe("")

      await textarea.setValue("   ")
      expect(button.attributes("disabled")).toBe("")

      await textarea.setValue("Hello")
      expect(button.attributes("disabled")).toBeUndefined()
    })

    it("prevents form submission for empty messages", async () => {
      await submitMessage(wrapper, "   ")
      expect(replyToConversationSpy).not.toHaveBeenCalled()
    })

    it("allows form submission for non-empty messages", async () => {
      await submitMessage(wrapper, "Hello")
      expect(replyToConversationSpy).toHaveBeenCalled()
    })
  })

  describe("AI Reply", () => {
    beforeEach(async () => {
      await submitMessage(wrapper, "Hello")
    })

    it("processes AI response and displays content", async () => {
      simulateAiResponse()
      await flushPromises()

      expect(wrapper.find(".ai-assistant h2").text()).toEqual("I'm ChatGPT")
    })
  })

  describe("Message formatting", () => {
    it("renders user messages in pre tags", async () => {
      wrapper.vm.currentConversationMessages = [
        { id: 1, message: "Hello\nWorld", sender: user },
      ] satisfies ConversationMessage[]
      await wrapper.vm.$nextTick()

      const userMessage = wrapper.find(".user-message")
      expect(userMessage.element.tagName).toBe("PRE")
      expect(userMessage.text()).toBe("Hello\nWorld")
    })

    it("renders AI messages as markdown HTML", async () => {
      wrapper.vm.currentConversationMessages = [
        { id: 2, message: "## Hello\n**World**", sender: undefined },
      ] satisfies ConversationMessage[]
      await wrapper.vm.$nextTick()

      const aiMessage = wrapper.find(".ai-assistant")
      expect(aiMessage.find("h2").exists()).toBe(true)
      expect(aiMessage.find("strong").exists()).toBe(true)
    })
  })

  describe("Default questions", () => {
    const mountRecallConversation = () => {
      const recallConversation = makeMe.aConversation
        .forAnsweredQuestion(makeMe.anAnsweredQuestion.please())
        .please()
      return mountComponent(recallConversation, user)
    }

    it("shows default messages for recall conversations with no messages", async () => {
      wrapper = mountRecallConversation()
      wrapper.vm.currentConversationMessages = []
      await wrapper.vm.$nextTick()

      const defaultButtons = wrapper.findAll(".default-message-button")
      expect(defaultButtons).toHaveLength(4)
      expect(defaultButtons[0].text()).toBe("Why is my answer wrong?")
    })

    it("doesn't show default messages for non-recall conversations", async () => {
      wrapper.vm.currentConversationMessages = []
      await wrapper.vm.$nextTick()

      expect(wrapper.findAll(".default-message-button")).toHaveLength(0)
    })

    it("sends message when default question is clicked", async () => {
      wrapper = mountRecallConversation()
      wrapper.vm.currentConversationMessages = []
      await wrapper.vm.$nextTick()

      await wrapper.find(".default-message-button").trigger("click")

      expect(replyToConversationSpy).toHaveBeenCalledWith({
        path: { conversationId: wrapper.props("conversation").id },
        body: "Why is my answer wrong?",
      })
    })
  })
})
