import { expect, vi } from "vitest"
import ConversationInner from "@/components/conversations/ConversationInner.vue"
import helper from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"
import type { ConversationMessage } from "@/generated/backend"
import { flushPromises } from "@vue/test-utils"

const mockedPush = vi.fn()
vitest.mock("vue-router", () => ({
  useRouter: () => ({
    push: mockedPush,
  }),
}))

describe("ConversationInner", () => {
  let wrapper
  const note = makeMe.aNote.please()
  const conversation = makeMe.aConversation.note(note).please()
  const user = makeMe.aUser.please()

  beforeEach(() => {
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

  it.skip("handles new conversation creation when conversation.id is undefined", async () => {
    const newConversation = makeMe.aConversation.please()
    const conversationWithoutId = makeMe.aConversation
      .note(note)
      .withoutId()
      .please()

    wrapper = helper
      .component(ConversationInner)
      .withStorageProps({
        conversation: conversationWithoutId,
        user,
      })
      .mount()

    // Mock the API call
    const startConversationSpy = vi
      .spyOn(
        helper.managedApi.restConversationMessageController,
        "startConversationAboutNote"
      )
      .mockResolvedValue(newConversation)

    // Find the textarea and set its value
    const textarea = wrapper.find("textarea")
    await textarea.setValue("Test message")

    // Simulate pressing enter
    await wrapper.find("button[type='submit']").trigger("click")
    await flushPromises()

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
      .component(ConversationInner)
      .withStorageProps({
        conversation: conversationWithoutId,
        user,
      })
      .mount()

    const getMessagesSpy = vi.spyOn(
      helper.managedApi.restConversationMessageController,
      "getConversationMessages"
    )

    await flushPromises()

    expect(getMessagesSpy).not.toHaveBeenCalled()
  })
})
