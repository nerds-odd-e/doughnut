import { expect, vi } from "vitest"
import ConversationInner from "@/components/conversations/ConversationInner.vue"
import helper from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"
import type { ConversationMessage } from "@/generated/backend"

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
})
