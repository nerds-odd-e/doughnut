import ConversationInner from "@/components/conversations/ConversationInner.vue"
import AiReplyEventSource from "@/managedApi/AiReplyEventSource"
import type { ConversationMessage } from "@generated/donut-backend-api"
import { ConversationMessageController } from "@generated/donut-backend-api/sdk.gen"
import helper, {
  mockSdkService,
  mockSdkServiceWithImplementation,
} from "@tests/helpers"
import {
  getLastInstance,
  resetInstance,
} from "@tests/helpers/aiReplyEventSourceTracker"
import { flushPromises } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { simulateAiResponse } from "./aiResponseTestSupport"

vi.mock("@/managedApi/AiReplyEventSource", async () => {
  const { aiReplyEventSourceTrackingMock } = await import(
    "./aiReplyEventSourceTrackingMock"
  )
  return aiReplyEventSourceTrackingMock()
})

describe("Recall conversation reasoning", () => {
  beforeEach(() => {
    mockSdkService(ConversationMessageController, "markConversationAsRead", [])
    resetInstance()
    window.HTMLElement.prototype.scrollIntoView = vi.fn()
    vi.spyOn(AiReplyEventSource.prototype, "start").mockImplementation(vi.fn())
  })

  afterEach(resetInstance)

  it("explains immediately, accepts reasoning and disagreement, and reopens the exchange", async () => {
    const user = makeMe.aUser.please()
    const conversation = makeMe.aConversation
      .forAnsweredQuestion(makeMe.anAnsweredQuestion.please())
      .please()
    const storedMessages: ConversationMessage[] = []
    const read = mockSdkServiceWithImplementation(
      ConversationMessageController,
      "getConversationMessages",
      () => [...storedMessages]
    )
    const send = mockSdkServiceWithImplementation(
      ConversationMessageController,
      "replyToConversation",
      (options) => {
        const message: ConversationMessage = {
          id: storedMessages.length + 1,
          message: options.body!,
          sender: user,
        }
        storedMessages.push(message)
        return message
      }
    )
    const mountConversation = () =>
      helper
        .component(ConversationInner)
        .withCleanStorage()
        .withProps({ conversation, user })
        .mount()
    let wrapper = mountConversation()
    await flushPromises()

    const explanation = "Please explain the question and answer to me."
    expect(wrapper.find(".default-message-button").text()).toBe(explanation)
    await wrapper.find(".default-message-button").trigger("click")
    await flushPromises()
    expect(send).toHaveBeenLastCalledWith({
      path: { conversationId: conversation.id },
      body: explanation,
    })

    const finishReply = async (message: string) => {
      simulateAiResponse(message)
      await flushPromises()
      expect(wrapper.findAll(".ai-assistant").at(-1)!.text()).toContain(message)
      storedMessages.push({ id: storedMessages.length + 1, message })
      const source = getLastInstance()
      expect(source).not.toBeNull()
      source!.onMessageCallback("done", "")
      await flushPromises()
      expect(wrapper.findAll(".ai-assistant").at(-1)!.text()).toBe(message)
    }
    const explainReply =
      "Co-movement establishes correlation; causal claims need further evidence."
    await finishReply(explainReply)

    const submit = async (message: string) => {
      await wrapper.find("textarea").setValue(message)
      await wrapper.find("form.message-input-form").trigger("submit")
      await flushPromises()
      expect(send).toHaveBeenLastCalledWith({
        path: { conversationId: conversation.id },
        body: message,
      })
      expect(wrapper.findAll(".user-message").at(-1)!.text()).toBe(message)
    }
    const reasoning = "I chose causation because both quantities rose together."
    await submit(reasoning)
    const reasoningReply =
      "That co-movement is why the causal choice seems plausible; it alone does not distinguish causation from a common cause."
    await finishReply(reasoningReply)

    const disagreement =
      "I did not forget the distinction. Both choices satisfy the question's wording."
    await submit(disagreement)
    const correctionReply =
      "Thank you for clarifying. The wording admits both choices, so the marked result does not settle that objection."
    await finishReply(correctionReply)

    wrapper.unmount()
    wrapper = mountConversation()
    await flushPromises()
    expect(read).toHaveBeenLastCalledWith({
      path: { conversationId: conversation.id },
    })
    expect(
      wrapper.findAll(".user-message").map((message) => message.text())
    ).toEqual([explanation, reasoning, disagreement])
    expect(
      wrapper.findAll(".ai-assistant").map((message) => message.text())
    ).toEqual([explainReply, reasoningReply, correctionReply])
    wrapper.unmount()
  })
})

beforeEach(() => {
  mockSdkService(
    ConversationMessageController,
    "getConversationCorrections",
    []
  )
})
