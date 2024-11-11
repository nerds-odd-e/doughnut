import { expect, vi } from "vitest"
import ConversationInner from "@/components/conversations/ConversationInner.vue"
import helper from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"
import type {
  ConversationMessage,
  Message,
  MessageDelta,
  TopicTitleReplacement,
} from "@/generated/backend"
import { flushPromises } from "@vue/test-utils"

const simulateAiResponse = (content = "## I'm ChatGPT") => {
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
            value: content,
          },
        },
      ],
    },
  }

  helper.managedApi.eventSource.eventSourceRequest.onMessage(
    "thread.message.created",
    JSON.stringify(newMessage)
  )
  helper.managedApi.eventSource.eventSourceRequest.onMessage(
    "thread.message.delta",
    JSON.stringify(messageDelta)
  )
}

const createRunResponse = (functionName: string, args: object) => ({
  id: "run-123",
  thread_id: "thread-123",
  required_action: {
    submit_tool_outputs: {
      tool_calls: [
        {
          id: "call-456",
          function: {
            name: functionName,
            arguments: JSON.stringify(args),
          },
        },
      ],
    },
  },
})

const setupTestData = () => {
  const note = makeMe.aNote.details("").please()
  const conversation = makeMe.aConversation.note(note).please()
  const user = makeMe.aUser.please()
  return { note, conversation, user }
}

const mountComponent = (conversation, user) => {
  return helper
    .component(ConversationInner)
    .withStorageProps({ conversation, user })
    .mount()
}

const submitMessage = async (wrapper, message: string) => {
  const form = wrapper.find("form.chat-input-form")
  const textarea = wrapper.find("textarea")
  await textarea.setValue(message)
  await form.trigger("submit")
  await flushPromises()
}

const submitMessageAndSimulateRunResponse = async (
  wrapper,
  message,
  runResponse
) => {
  await submitMessage(wrapper, message)
  helper.managedApi.eventSource.eventSourceRequest.onMessage(
    "thread.run.requires_action",
    JSON.stringify(runResponse)
  )
  await flushPromises()
}

describe("ConversationInner", () => {
  let wrapper
  let note
  let conversation
  let user

  beforeEach(() => {
    window.HTMLElement.prototype.scrollIntoView = vi.fn()
    helper.managedApi.restConversationMessageController.replyToConversation =
      vi.fn()
    helper.managedApi.eventSource.restConversationMessageController.getAiReply =
      vi.fn()

    const testData = setupTestData()
    note = testData.note
    conversation = testData.conversation
    user = testData.user

    wrapper = mountComponent(conversation, user)
  })

  describe("ScrollTo behavior", () => {
    it("updates ScrollTo component trigger when messages change", async () => {
      const messages: ConversationMessage[] = [
        { id: 1, message: "Hello", sender: user },
        { id: 2, message: "Hi", sender: user },
      ]

      wrapper.vm.currentConversationMessages = messages
      await wrapper.vm.$nextTick()

      expect(
        wrapper.findComponent({ name: "ScrollTo" }).props("scrollTrigger")
      ).toBe(2)
    })
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
      expect(
        helper.managedApi.restConversationMessageController.replyToConversation
      ).not.toHaveBeenCalled()
    })

    it("allows form submission for non-empty messages", async () => {
      await submitMessage(wrapper, "Hello")
      expect(
        helper.managedApi.restConversationMessageController.replyToConversation
      ).toHaveBeenCalled()
    })
  })

  describe("AI Reply", () => {
    beforeEach(async () => {
      await submitMessage(wrapper, "Hello")
    })

    it("processes AI response and displays content", async () => {
      simulateAiResponse()
      await flushPromises()

      expect(wrapper.find(".ai-chat h2").text()).toEqual("I'm ChatGPT")
    })

    describe("Status Bar", () => {
      it("shows correct status messages during AI reply lifecycle", async () => {
        const statusBar = wrapper.find(".status-bar")
        const statusText = () => statusBar.find("small").text()

        // Initial state
        expect(statusBar.exists()).toBe(true)
        expect(statusText()).toBe("Starting AI reply...")

        // After message created
        helper.managedApi.eventSource.eventSourceRequest.onMessage(
          "thread.message.created",
          JSON.stringify({
            role: "assistant",
            thread_id: "test-thread-id",
            content: [],
          })
        )
        await wrapper.vm.$nextTick()
        expect(statusText()).toBe("Generating response...")

        // During writing
        helper.managedApi.eventSource.eventSourceRequest.onMessage(
          "thread.message.delta",
          JSON.stringify({
            delta: {
              content: [{ text: { value: "Test" } }],
            },
          })
        )
        await wrapper.vm.$nextTick()
        expect(statusText()).toBe("Writing response...")

        // After completion
        helper.managedApi.eventSource.eventSourceRequest.onMessage("done", "")
        await wrapper.vm.$nextTick()
        expect(wrapper.find(".status-bar").exists()).toBe(false)
      })

      it("hides status bar and shows error message on failure", async () => {
        const onError =
          helper.managedApi.eventSource.eventSourceRequest!.onError
        if (!onError) throw new Error("onError is not defined")

        onError(new Error("400 Bad Request"))
        await wrapper.vm.$nextTick()

        expect(wrapper.find(".status-bar").exists()).toBe(false)
        expect(wrapper.find(".last-error-message").text()).toBe("Bad Request")
      })
    })
  })

  describe("Message formatting", () => {
    it("renders user messages in pre tags", async () => {
      const messages: ConversationMessage[] = [
        { id: 1, message: "Hello\nWorld", sender: user },
      ]
      wrapper.vm.currentConversationMessages = messages
      await wrapper.vm.$nextTick()

      const userMessage = wrapper.find(".user-message")
      expect(userMessage.exists()).toBe(true)
      expect(userMessage.element.tagName).toBe("PRE")
      expect(userMessage.text()).toBe("Hello\nWorld")
    })

    it("renders AI messages as markdown HTML", async () => {
      const messages: ConversationMessage[] = [
        { id: 2, message: "## Hello\n**World**", sender: undefined },
      ]
      wrapper.vm.currentConversationMessages = messages
      await wrapper.vm.$nextTick()

      const aiMessage = wrapper.find(".ai-chat")
      expect(aiMessage.exists()).toBe(true)
      expect(aiMessage.find("h2").exists()).toBe(true)
      expect(aiMessage.find("strong").exists()).toBe(true)
    })
  })

  describe("Tool Call Handling", () => {
    const testCompletion = "**bold completion**"

    beforeEach(async () => {
      helper.managedApi.restAiController.submitToolCallResult = vi.fn()
      helper.managedApi.restAiController.cancelRun = vi.fn()
      helper.managedApi.restTextContentController.updateNoteDetails = vi.fn()

      await submitMessageAndSimulateRunResponse(
        wrapper,
        "Hello",
        createRunResponse("complete_note_details", {
          completion: testCompletion,
        })
      )
    })

    it("renders completion suggestion as markdown", async () => {
      const completionText = wrapper.find(".ai-chat .completion-text")
      expect(completionText.find("strong").exists()).toBe(true)
      expect(completionText.find("strong").text()).toBe("bold completion")
    })

    it("adds '...' prefix when note has existing details", async () => {
      // Recreate wrapper with note having existing details
      const noteWithDetails = makeMe.aNote.details("existing text").please()
      const conversationWithDetails = makeMe.aConversation
        .note(noteWithDetails)
        .please()

      wrapper = mountComponent(conversationWithDetails, user)

      await submitMessageAndSimulateRunResponse(
        wrapper,
        "Hello",
        createRunResponse("complete_note_details", {
          completion: testCompletion,
        })
      )

      const completionText = wrapper.find(".ai-chat .completion-text")
      expect(completionText.text()).toBe("...bold completion")
    })

    it("accepts the completion suggestion and updates the note", async () => {
      // Accept the suggestion
      await wrapper.find('button[class*="btn-primary"]').trigger("click")
      await flushPromises()

      expect(
        helper.managedApi.restTextContentController.updateNoteDetails
      ).toHaveBeenCalledWith(note.id, { details: testCompletion })

      expect(
        helper.managedApi.restAiController.submitToolCallResult
      ).toHaveBeenCalledWith("thread-123", "run-123", "call-456", {
        status: "accepted",
      })

      expect(wrapper.find(".completion-text").exists()).toBe(false)
    })

    it("rejects the completion suggestion without updating the note", async () => {
      // Reject the suggestion
      await wrapper.find('button[class*="btn-secondary"]').trigger("click")
      await flushPromises()

      expect(
        helper.managedApi.restTextContentController.updateNoteDetails
      ).not.toHaveBeenCalled()

      expect(helper.managedApi.restAiController.cancelRun).toHaveBeenCalledWith(
        "thread-123",
        "run-123"
      )

      expect(wrapper.find(".completion-text").exists()).toBe(false)
    })
  })

  describe("Topic Title Generation", () => {
    const testTitle = "Generated Title"

    beforeEach(async () => {
      helper.managedApi.restAiController.submitToolCallResult = vi.fn()
      helper.managedApi.restAiController.cancelRun = vi.fn()
      helper.managedApi.restTextContentController.updateNoteTopicConstructor =
        vi.fn()

      await submitMessageAndSimulateRunResponse(
        wrapper,
        "Hello",
        createRunResponse("suggest_note_topic_title", <TopicTitleReplacement>{
          newTopic: testTitle,
        })
      )
    })

    it("renders title suggestion", async () => {
      const titleSuggestion = wrapper.find(".title-suggestion")
      expect(titleSuggestion.exists()).toBe(true)
      expect(titleSuggestion.text()).toBe(testTitle)
      expect(
        await wrapper.findAll('button[class*="btn-primary"]')
      ).toHaveLength(1)
    })

    it("accepts the title suggestion and updates the note", async () => {
      await wrapper.find('button[class*="btn-primary"]').trigger("click")
      await flushPromises()

      expect(
        helper.managedApi.restTextContentController.updateNoteTopicConstructor
      ).toHaveBeenCalledWith(note.id, { topicConstructor: testTitle })

      expect(
        helper.managedApi.restAiController.submitToolCallResult
      ).toHaveBeenCalledWith("thread-123", "run-123", "call-456", {
        status: "accepted",
      })

      expect(wrapper.find(".title-suggestion").exists()).toBe(false)
    })

    it("rejects the title suggestion without updating the note", async () => {
      await wrapper.find('button[class*="btn-secondary"]').trigger("click")
      await flushPromises()

      expect(
        helper.managedApi.restTextContentController.updateNoteTopicConstructor
      ).not.toHaveBeenCalled()

      expect(helper.managedApi.restAiController.cancelRun).toHaveBeenCalledWith(
        "thread-123",
        "run-123"
      )

      expect(wrapper.find(".title-suggestion").exists()).toBe(false)
    })
  })
})
