import { expect, vi } from "vitest"
import ConversationInner from "@/components/conversations/ConversationInner.vue"
import helper from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"
import type {
  ConversationMessage,
  Message,
  MessageDelta,
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

describe("ConversationInner", () => {
  let wrapper
  const note = makeMe.aNote.please()
  const conversation = makeMe.aConversation.note(note).please()
  const user = makeMe.aUser.please()

  // Helper functions
  const submitForm = async (message: string) => {
    const form = wrapper.find("form.chat-input-form")
    const textarea = wrapper.find("textarea")
    await textarea.setValue(message)
    await form.trigger("submit")
    await flushPromises()
  }

  beforeEach(() => {
    window.HTMLElement.prototype.scrollIntoView = vitest.fn()
    helper.managedApi.restConversationMessageController.replyToConversation =
      vi.fn()
    helper.managedApi.eventSource.restConversationMessageController.getAiReply =
      vi.fn()

    wrapper = helper
      .component(ConversationInner)
      .withStorageProps({
        conversation,
        user,
      })
      .mount()
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
      await submitForm("   ")
      expect(
        helper.managedApi.restConversationMessageController.replyToConversation
      ).not.toHaveBeenCalled()
    })

    it("allows form submission for non-empty messages", async () => {
      await submitForm("Hello")
      expect(
        helper.managedApi.restConversationMessageController.replyToConversation
      ).toHaveBeenCalled()
    })
  })

  describe("AI Reply", () => {
    it("processes AI response and displays content", async () => {
      await submitForm("Hello")
      simulateAiResponse()
      await flushPromises()

      expect(wrapper.find(".ai-chat h2").text()).toEqual("I'm ChatGPT")
    })

    describe("Status Bar", () => {
      beforeEach(async () => {
        await submitForm("Hello")
      })

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
        const statusBarAfter = wrapper.find(".status-bar")
        expect(statusBarAfter.exists()).toBe(false)
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
    beforeEach(async () => {
      await submitForm("Hello")
      helper.managedApi.restAiController.submitToolCallResult = vi.fn()
    })

    it("handles tool calls and submits results", async () => {
      const runResponse = {
        id: "run-123",
        thread_id: "thread-123",
        required_action: {
          submit_tool_outputs: {
            tool_calls: [
              {
                id: "call-456",
                function: {
                  arguments: JSON.stringify({
                    completion: "test completion",
                  }),
                },
              },
            ],
          },
        },
      }

      helper.managedApi.eventSource.eventSourceRequest.onMessage(
        "thread.run.requires_action",
        JSON.stringify(runResponse)
      )
      await flushPromises()

      expect(
        helper.managedApi.restAiController.submitToolCallResult
      ).toHaveBeenCalledWith("thread-123", "run-123", "call-456", {
        status: "accepted",
      })
    })
  })
})
