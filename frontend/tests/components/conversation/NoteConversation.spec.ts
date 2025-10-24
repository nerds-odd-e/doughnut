import ConversationInner from "@/components/conversations/ConversationInner.vue"
import ConversationTemplate from "@/components/conversations/ConversationTemplate.vue"
import NoteConversation from "@/components/conversations/NoteConversation.vue"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { expect, vi } from "vitest"

const mockedPush = vi.fn()
vitest.mock("vue-router", () => ({
  useRouter: () => ({
    push: mockedPush,
  }),
}))

// Add window.performance mock
const mockPerformance = {
  now: vi.fn(() => Date.now()),
}
vi.stubGlobal("performance", mockPerformance)

afterEach(() => {
  vi.clearAllMocks()
  vi.clearAllTimers()
  vi.useRealTimers() // Restore real timers
})

describe("NoteConversation", () => {
  const note = makeMe.aNote.please()
  const conversation = { id: 1, title: "Test Conversation" }
  const user = makeMe.aUser.please()

  const mount = async () => {
    const wrapper = helper
      .component(NoteConversation)
      .withCurrentUser(user)
      .withStorageProps({
        noteId: note.id,
      })
      .mount()
    await flushPromises()
    return wrapper
  }

  beforeEach(() => {
    window.HTMLElement.prototype.scrollIntoView = vi.fn()
    vi.useFakeTimers() // Use fake timers to control timing
  })

  beforeEach(() => {
    helper.managedApi.restConversationMessageController.startConversationAboutNote =
      vi.fn().mockResolvedValue(conversation)
  })

  it("calls api to start conversation and shows ConversationInner when successful", async () => {
    helper.managedApi.restConversationMessageController.getConversationsAboutNote =
      vi.fn().mockResolvedValue([])
    const wrapper = await mount()
    await wrapper.find("textarea").setValue("Hello")
    await wrapper.find("button.send-button[type='button']").trigger("click")
    await flushPromises()

    expect(
      helper.managedApi.restConversationMessageController
        .startConversationAboutNote
    ).toHaveBeenCalledWith(note.id, "Hello")

    // Verify ConversationInner is rendered with correct props
    const conversationInner = wrapper.findComponent(ConversationInner)
    expect(conversationInner.exists()).toBe(true)
    expect(conversationInner.props("conversation")).toEqual(conversation)
  })

  it("shows the first conversation if conversation exists", async () => {
    helper.managedApi.restConversationMessageController.getConversationsAboutNote =
      vi.fn().mockResolvedValue([conversation])
    const wrapper = await mount()
    const conversationInner = wrapper.findComponent(ConversationInner)

    expect(conversationInner.exists()).toBe(true)
    expect(conversationInner.props("conversation")).toEqual(conversation)
  })

  it("shows ConversationTemplate when no conversation exists", async () => {
    helper.managedApi.restConversationMessageController.getConversationsAboutNote =
      vi.fn().mockResolvedValue([])
    const wrapper = await mount()
    const conversationTemplate = wrapper.findComponent(ConversationTemplate)
    const conversationInner = wrapper.findComponent(ConversationInner)

    expect(conversationTemplate.exists()).toBe(true)
    expect(conversationInner.exists()).toBe(false)
  })

  it("allows switching between conversations", async () => {
    const conversations = [
      { id: 1, title: "First Conversation" },
      { id: 2, title: "Second Conversation" },
    ]

    helper.managedApi.restConversationMessageController.getConversationsAboutNote =
      vi.fn().mockResolvedValue(conversations)

    const wrapper = await mount()

    // Verify initial conversation is selected
    expect(
      wrapper.findComponent(ConversationInner).props("conversation")
    ).toEqual(conversations[0])

    // Change conversation
    await wrapper.find("select.conversation-select").setValue("2")
    await wrapper.find("select.conversation-select").trigger("change")

    // Verify conversation changed
    expect(
      wrapper.findComponent(ConversationInner).props("conversation")
    ).toEqual(conversations[1])
  })

  it("shows conversation selector only when multiple conversations exist", async () => {
    helper.managedApi.restConversationMessageController.getConversationsAboutNote =
      vi.fn().mockResolvedValue([conversation])

    const wrapper = await mount()

    // Verify selector is not shown with single conversation
    expect(wrapper.find("select.conversation-select").exists()).toBe(false)
  })

  it("allows starting a new conversation when in conversation view", async () => {
    // Setup with existing conversation
    const existingConversation = { id: 1, title: "Test Conversation" }
    helper.managedApi.restConversationMessageController.getConversationsAboutNote =
      vi.fn().mockResolvedValue([existingConversation])

    const wrapper = await mount()

    // Verify we're showing the ConversationInner initially
    expect(wrapper.findComponent(ConversationInner).exists()).toBe(true)

    // Click new conversation button
    await wrapper.find("button.daisy-btn-outline-primary").trigger("click")

    // Verify we're now showing the ConversationTemplate
    expect(wrapper.findComponent(ConversationTemplate).exists()).toBe(true)
    expect(wrapper.findComponent(ConversationInner).exists()).toBe(false)

    // Start new conversation
    await wrapper.find("textarea").setValue("New conversation message")
    await wrapper.find("button.send-button[type='button']").trigger("click")
    await flushPromises()

    // Verify API was called
    expect(
      helper.managedApi.restConversationMessageController
        .startConversationAboutNote
    ).toHaveBeenCalledWith(note.id, "New conversation message")

    // Verify we're back to showing ConversationInner
    expect(wrapper.findComponent(ConversationInner).exists()).toBe(true)
  })

  it("handles AI reply when starting new conversation with AI invite", async () => {
    helper.managedApi.restConversationMessageController.getConversationsAboutNote =
      vi.fn().mockResolvedValue([])
    helper.managedApi.eventSource.restConversationMessageController.getAiReply =
      vi.fn()

    const wrapper = await mount()

    // Trigger send message with AI invite
    await wrapper.find("textarea").setValue("Hello AI")
    await wrapper.find("form.chat-input-form").trigger("submit")
    await flushPromises()

    // Verify conversation was started
    expect(
      helper.managedApi.restConversationMessageController
        .startConversationAboutNote
    ).toHaveBeenCalledWith(note.id, "Hello AI")

    // Verify AI reply was requested
    expect(
      helper.managedApi.eventSource.restConversationMessageController.getAiReply
    ).toHaveBeenCalledWith(conversation.id)

    // Verify ConversationInner is rendered with correct props
    const conversationInner = wrapper.findComponent(ConversationInner)
    expect(conversationInner.exists()).toBe(true)
    expect(conversationInner.props("conversation")).toEqual(conversation)
    expect(conversationInner.props("initialAiReply")).toBe(true)
  })

  it("handles AI reply when sending message with AI invite in existing conversation", async () => {
    helper.managedApi.restConversationMessageController.getConversationsAboutNote =
      vi.fn().mockResolvedValue([conversation])
    helper.managedApi.restConversationMessageController.replyToConversation =
      vi.fn()
    helper.managedApi.eventSource.restConversationMessageController.getAiReply =
      vi.fn()

    const wrapper = await mount()

    await wrapper.find("textarea").setValue("Hello AI")
    await wrapper.find("form.chat-input-form").trigger("submit")
    await flushPromises()

    // Verify message was sent
    expect(
      helper.managedApi.restConversationMessageController.replyToConversation
    ).toHaveBeenCalledWith(conversation.id, "Hello AI")

    // Verify AI reply was requested
    expect(
      helper.managedApi.eventSource.restConversationMessageController.getAiReply
    ).toHaveBeenCalledWith(conversation.id)
  })
})
