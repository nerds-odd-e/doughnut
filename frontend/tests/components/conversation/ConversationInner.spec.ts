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

const simulateAiResponse = () => {
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
            value: "## I'm ChatGPT",
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

  beforeEach(() => {
    helper.managedApi.restConversationMessageController.replyToConversation =
      vi.fn()
    wrapper = helper
      .component(ConversationInner)
      .withStorageProps({
        conversation,
        user,
      })
      .mount()
  })

  it("passes message length to ScrollTo component when messages update", async () => {
    const messages: ConversationMessage[] = [
      {
        id: 1,
        message: "Hello",
        sender: user,
      },
      {
        id: 2,
        message: "Hi",
        sender: user,
      },
    ]

    // Mock the API call response
    wrapper.vm.currentConversationMessages = messages
    await wrapper.vm.$nextTick()

    expect(
      wrapper.findComponent({ name: "ScrollTo" }).props("scrollTrigger")
    ).toBe(2)
  })

  it("disables the button when message is empty", async () => {
    const button = wrapper.find('button[type="submit"]')
    const textarea = wrapper.find("textarea")

    expect(button.attributes("disabled")).toBe("")

    await textarea.setValue("   ")
    expect(button.attributes("disabled")).toBe("")

    await textarea.setValue("Hello")
    expect(button.attributes("disabled")).toBeUndefined()
  })

  it("disables form when message is empty", async () => {
    const form = wrapper.find("form.chat-input-form")
    const textarea = wrapper.find("textarea")

    await textarea.setValue("   ")
    await form.trigger("submit")
    expect(
      helper.managedApi.restConversationMessageController.replyToConversation
    ).not.toHaveBeenCalled()

    await textarea.setValue("Hello")
    await form.trigger("submit")
    expect(
      helper.managedApi.restConversationMessageController.replyToConversation
    ).toHaveBeenCalled()
  })

  it("disables AI reply", async () => {
    helper.managedApi.eventSource.restConversationMessageController.getAiReply =
      vi.fn()
    const form = wrapper.find("form.chat-input-form")
    const textarea = wrapper.find("textarea")
    await textarea.setValue("Hello")
    await form.trigger("submit")
    await flushPromises()
    expect(
      helper.managedApi.eventSource.restConversationMessageController.getAiReply
    ).toHaveBeenCalled()

    simulateAiResponse()

    expect(wrapper.vm.currentAiReply).toEqual("## I'm ChatGPT")
  })

  it("shows and updates status bar during AI reply", async () => {
    helper.managedApi.eventSource.restConversationMessageController.getAiReply =
      vi.fn()
    const form = wrapper.find("form.chat-input-form")
    const textarea = wrapper.find("textarea")
    await textarea.setValue("Hello")
    await form.trigger("submit")
    await flushPromises()

    // Check initial status
    expect(wrapper.find(".status-bar").exists()).toBe(true)
    expect(wrapper.find(".status-bar small").text()).toBe(
      "Starting AI reply..."
    )

    // Simulate message created
    helper.managedApi.eventSource.eventSourceRequest.onMessage(
      "thread.message.created",
      JSON.stringify({
        role: "assistant",
        thread_id: "test-thread-id",
        content: [],
      })
    )
    await wrapper.vm.$nextTick()
    expect(wrapper.find(".status-bar small").text()).toBe(
      "Generating response..."
    )

    // Simulate message delta
    helper.managedApi.eventSource.eventSourceRequest.onMessage(
      "thread.message.delta",
      JSON.stringify({
        delta: {
          content: [{ text: { value: "Test" } }],
        },
      })
    )
    await wrapper.vm.$nextTick()
    expect(wrapper.find(".status-bar small").text()).toBe("Writing response...")

    // Simulate done event
    helper.managedApi.eventSource.eventSourceRequest.onMessage("done", "")
    await wrapper.vm.$nextTick()
    expect(wrapper.find(".status-bar").exists()).toBe(false)
  })

  it("hides status bar on error", async () => {
    helper.managedApi.eventSource.restConversationMessageController.getAiReply =
      vi.fn()
    const form = wrapper.find("form.chat-input-form")
    const textarea = wrapper.find("textarea")
    await textarea.setValue("Hello")
    await form.trigger("submit")
    await flushPromises()

    expect(wrapper.find(".status-bar").exists()).toBe(true)
    // Simulate error
    const onError = helper.managedApi.eventSource.eventSourceRequest!.onError
    if (!onError) {
      throw new Error("onError is not defined")
    }

    onError(new Error("400 Bad Request"))
    await wrapper.vm.$nextTick()

    expect(wrapper.find(".status-bar").exists()).toBe(false)
    expect(wrapper.find(".last-error-message").text()).toBe("Bad Request")
  })
})
