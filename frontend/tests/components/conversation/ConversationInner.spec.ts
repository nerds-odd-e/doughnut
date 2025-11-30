import { expect, vi } from "vitest"
import ConversationInner from "@/components/conversations/ConversationInner.vue"
import helper, { mockShowNote, mockSdkService } from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"
import { type ConversationMessage } from "@generated/backend"
import { flushPromises } from "@vue/test-utils"
import { simulateAiResponse } from "./AiResponse.spec"
import AiReplyEventSource from "@/managedApi/AiReplyEventSource"
import { resetInstance } from "@tests/helpers/aiReplyEventSourceTracker"

class MockIntersectionObserver {
  readonly root: Element | null = null
  readonly rootMargin: string = "0px"
  readonly thresholds: ReadonlyArray<number> = [0]

  observe = vi.fn()
  unobserve = vi.fn()
  disconnect = vi.fn()
  takeRecords = vi.fn().mockReturnValue([])
}

beforeAll(() => {
  // Mock IntersectionObserver
  global.IntersectionObserver = MockIntersectionObserver

  // Mock window.performance
  Object.defineProperty(window, "performance", {
    value: {
      now: () => Date.now(),
    },
    configurable: true,
  })
})

// Mock AiReplyEventSource to track instances (shared with AiResponse.spec.ts)
vi.mock("@/managedApi/AiReplyEventSource", async () => {
  const actual = await vi.importActual<
    typeof import("@/managedApi/AiReplyEventSource")
  >("@/managedApi/AiReplyEventSource")
  const { setLastInstance } = await import(
    "@tests/helpers/aiReplyEventSourceTracker"
  )
  return {
    default: class extends actual.default {
      constructor(conversationId: number) {
        super(conversationId)
        setLastInstance(this)
      }
    },
  }
})

const setupTestData = () => {
  const note = makeMe.aNote.details("").please()
  const conversation = makeMe.aConversation.forANote(note).please()
  const user = makeMe.aUser.please()
  return { note, conversation, user }
}

const mountComponent = (conversation, user) => {
  return helper
    .component(ConversationInner)
    .withCleanStorage()
    .withProps({ conversation, user })
    .mount()
}

const submitMessage = async (wrapper, message: string) => {
  const form = wrapper.find("form.chat-input-form")
  const textarea = wrapper.find("textarea")
  await textarea.setValue(message)
  await form.trigger("submit")
  await flushPromises()
}

describe("ConversationInner", () => {
  let wrapper
  let conversation
  let user
  let replyToConversationSpy: ReturnType<
    typeof mockSdkService<"replyToConversation">
  >

  beforeEach(() => {
    window.HTMLElement.prototype.scrollIntoView = vi.fn()
    mockShowNote()
    replyToConversationSpy = mockSdkService("replyToConversation", undefined)
    mockSdkService("getConversationMessages", [])
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

      expect(wrapper.find(".ai-chat h2").text()).toEqual("I'm ChatGPT")
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

  describe("Default questions", () => {
    it("shows default messages for review conversations with no messages", async () => {
      const reviewConversation = {
        ...conversation,
        subject: {
          answeredQuestion: makeMe.anAnsweredQuestion.please(),
        },
      }
      wrapper = mountComponent(reviewConversation, user)

      // Simulate empty conversation messages
      wrapper.vm.currentConversationMessages = []
      await wrapper.vm.$nextTick()

      const defaultButtons = wrapper.findAll(".default-message-button")
      expect(defaultButtons.length).toBe(4)
      expect(defaultButtons[0].text()).toBe("Why is my answer wrong?")
    })

    it("doesn't show default messages for non-review conversations", async () => {
      const nonReviewConversation = {
        ...conversation,
        subject: {
          answeredQuestion: undefined,
        },
      }
      wrapper = mountComponent(nonReviewConversation, user)

      wrapper.vm.currentConversationMessages = []
      await wrapper.vm.$nextTick()

      const defaultButtons = wrapper.findAll(".default-message-button")
      expect(defaultButtons.length).toBe(0)
    })

    it("sends message when default question is clicked", async () => {
      const reviewConversation = {
        ...conversation,
        subject: {
          answeredQuestion: makeMe.anAnsweredQuestion.please(),
        },
      }
      wrapper = mountComponent(reviewConversation, user)

      wrapper.vm.currentConversationMessages = []
      await wrapper.vm.$nextTick()

      const firstButton = wrapper.find(".default-message-button")
      await firstButton.trigger("click")

      expect(replyToConversationSpy).toHaveBeenCalledWith({
        path: { conversationId: reviewConversation.id },
        body: "Why is my answer wrong?",
      })
    })
  })
})
