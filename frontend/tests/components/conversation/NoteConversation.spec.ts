import { ConversationMessageController } from "@generated/donut-backend-api/sdk.gen"
import NoteConversation from "@/components/conversations/NoteConversation.vue"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { expect, vi, describe, beforeEach, afterEach } from "vitest"
import AiReplyEventSource from "@/managedApi/AiReplyEventSource"

const mockedPush = vi.fn()
vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRouter: () => ({
      push: mockedPush,
    }),
  }
})

afterEach(() => {
  vi.clearAllMocks()
  vi.clearAllTimers()
  vi.useRealTimers()
})

describe("NoteConversation", () => {
  const note = makeMe.aNote.please()
  const conversation = makeMe.aConversation.withId(1).forANote(note).please()
  const user = makeMe.aUser.please()

  const mount = async () => {
    const wrapper = helper
      .component(NoteConversation)
      .withCurrentUser(user)
      .withCleanStorage()
      .withProps({
        noteId: note.id,
      })
      .mount()
    await flushPromises()
    return wrapper
  }

  let startConversationSpy: ReturnType<typeof mockSdkService>

  beforeEach(() => {
    window.HTMLElement.prototype.scrollIntoView = vi.fn()
    vi.useFakeTimers()
    startConversationSpy = mockSdkService(
      ConversationMessageController,
      "startConversationAboutNote",
      conversation
    )
    mockSdkService(ConversationMessageController, "getConversationMessages", [])
  })

  it("starts a conversation and shows the message input when none exist", async () => {
    mockSdkService(
      ConversationMessageController,
      "getConversationsAboutNote",
      []
    )
    const wrapper = await mount()

    await wrapper.find("textarea").setValue("Hello")
    await wrapper.find("button.send-button[type='button']").trigger("click")
    await flushPromises()

    expect(startConversationSpy).toHaveBeenCalledWith({
      path: { note: note.id },
      body: "Hello",
    })
    expect(wrapper.find("form.message-input-form").exists()).toBe(true)
  })

  it("shows the first conversation when conversations already exist", async () => {
    mockSdkService(ConversationMessageController, "getConversationsAboutNote", [
      conversation,
    ])
    const wrapper = await mount()

    expect(wrapper.find("form.message-input-form").exists()).toBe(true)
    expect(wrapper.find("select.conversation-select").exists()).toBe(false)
  })

  it("shows the start template when no conversation exists", async () => {
    mockSdkService(
      ConversationMessageController,
      "getConversationsAboutNote",
      []
    )
    const wrapper = await mount()

    expect(wrapper.text()).toContain("Start a conversation about this note")
  })

  it("allows switching between conversations", async () => {
    const conversations = [
      makeMe.aConversation.withId(1).forANote(note).please(),
      makeMe.aConversation.withId(2).forANote(note).please(),
    ]

    mockSdkService(
      ConversationMessageController,
      "getConversationsAboutNote",
      conversations
    )

    const wrapper = await mount()
    const select = wrapper.find("select.conversation-select")
    expect(select.exists()).toBe(true)
    expect((select.element as HTMLSelectElement).value).toBe("1")

    await select.setValue("2")
    await select.trigger("change")

    expect((select.element as HTMLSelectElement).value).toBe("2")
  })

  it("starts a new conversation from an existing conversation view", async () => {
    mockSdkService(ConversationMessageController, "getConversationsAboutNote", [
      conversation,
    ])

    const wrapper = await mount()

    await wrapper
      .find("button.daisy-btn-outline.daisy-btn-primary")
      .trigger("click")

    expect(wrapper.text()).toContain("Start a conversation about this note")

    await wrapper.find("textarea").setValue("New conversation message")
    await wrapper.find("button.send-button[type='button']").trigger("click")
    await flushPromises()

    expect(startConversationSpy).toHaveBeenCalledWith({
      path: { note: note.id },
      body: "New conversation message",
    })
    expect(wrapper.find("form.message-input-form").exists()).toBe(true)
  })

  it("starts AI reply when starting a conversation with AI invite", async () => {
    mockSdkService(
      ConversationMessageController,
      "getConversationsAboutNote",
      []
    )
    const mockStart = vi.fn()
    vi.spyOn(AiReplyEventSource.prototype, "start").mockImplementation(
      mockStart
    )

    const wrapper = await mount()

    await wrapper.find("textarea").setValue("Hello AI")
    await wrapper.find("form.message-input-form").trigger("submit")
    await flushPromises()

    expect(startConversationSpy).toHaveBeenCalledWith({
      path: { note: note.id },
      body: "Hello AI",
    })
    expect(mockStart).toHaveBeenCalled()
  })

  it("starts AI reply when sending with AI invite in an existing conversation", async () => {
    mockSdkService(ConversationMessageController, "getConversationsAboutNote", [
      conversation,
    ])
    const replySpy = mockSdkService(
      ConversationMessageController,
      "replyToConversation",
      undefined
    )
    const mockStart = vi.fn()
    vi.spyOn(AiReplyEventSource.prototype, "start").mockImplementation(
      mockStart
    )

    const wrapper = await mount()

    await wrapper.find("textarea").setValue("Hello AI")
    await wrapper.find("form.message-input-form").trigger("submit")
    await flushPromises()

    expect(replySpy).toHaveBeenCalledWith({
      path: { conversationId: conversation.id },
      body: "Hello AI",
    })
    expect(mockStart).toHaveBeenCalled()
  })
})
