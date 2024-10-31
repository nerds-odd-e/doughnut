import { expect, vi } from "vitest"
import NoteConversation from "@/components/conversations/NoteConversation.vue"
import helper from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"
import { flushPromises } from "@vue/test-utils"
import ConversationInner from "@/components/conversations/ConversationInner.vue"
import ConversationTemplate from "@/components/conversations/ConversationTemplate.vue"

const mockedPush = vi.fn()
vitest.mock("vue-router", () => ({
  useRouter: () => ({
    push: mockedPush,
  }),
}))

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
    helper.managedApi.restConversationMessageController.startConversationAboutNote =
      vi.fn().mockResolvedValue(conversation)
  })

  it("calls api to start conversation and shows ConversationInner when successful", async () => {
    helper.managedApi.restConversationMessageController.getConversationsAboutNote =
      vi.fn().mockResolvedValue([])
    const wrapper = await mount()
    await wrapper.find("textarea").setValue("Hello")
    await wrapper.find("form.chat-input-form").trigger("submit")
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
})
