import { expect, vi } from "vitest"
import ConversationComponent from "@/components/conversations/ConversationComponent.vue"
import NoteShow from "@/components/notes/NoteShow.vue"
import helper from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"
import type { ConversationMessage } from "@/generated/backend"

const mockedPush = vi.fn()
vitest.mock("vue-router", () => ({
  useRouter: () => ({
    push: mockedPush,
  }),
}))

describe("ConversationComponent", () => {
  let wrapper
  const note = makeMe.aNote.please()
  const conversation = makeMe.aConversation.note(note).please()
  const user = makeMe.aUser.please()

  beforeEach(() => {
    wrapper = helper
      .component(ConversationComponent)
      .withStorageProps({
        conversation,
        user,
      })
      .mount()
  })

  it("renders NoteShow component when subject is a note", () => {
    expect(wrapper.findComponent(NoteShow).exists()).toBe(true)
    expect(wrapper.findComponent(NoteShow).props("noteId")).toBe(note.id)
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

  it("handles new conversation creation when conversation.id is undefined", async () => {
    const newConversation = makeMe.aConversation.please()
    const conversationWithoutId = makeMe.aConversation
      .note(note)
      .withoutId()
      .please()

    wrapper = helper
      .component(ConversationComponent)
      .withStorageProps({
        conversation: conversationWithoutId,
        user,
      })
      .mount()

    // Mock the API call
    const startConversationSpy = vi
      .spyOn(
        wrapper.vm.managedApi.restConversationMessageController,
        "startConversationAboutNote"
      )
      .mockResolvedValue(newConversation)

    // Submit message
    wrapper.vm.message = "Test message"
    await wrapper.vm.handleSendMessage()

    expect(startConversationSpy).toHaveBeenCalledWith(note.id, "Test message")
    expect(mockedPush).toHaveBeenCalledWith({
      name: "messageCenter",
      params: { conversationId: newConversation.id },
    })
  })

  it("does not fetch messages when conversation.id is undefined", async () => {
    const conversationWithoutId = makeMe.aConversation
      .note(note)
      .withoutId()
      .please()

    wrapper = helper
      .component(ConversationComponent)
      .withStorageProps({
        conversation: conversationWithoutId,
        user,
      })
      .mount()

    const getMessagesSpy = vi.spyOn(
      wrapper.vm.managedApi.restConversationMessageController,
      "getConversationMessages"
    )

    await wrapper.vm.fetchConversationMessages()

    expect(getMessagesSpy).not.toHaveBeenCalled()
  })
})
