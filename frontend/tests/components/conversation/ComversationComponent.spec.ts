import { expect, vi } from "vitest"
import ConversationInner from "@/components/conversations/ConversationInner.vue"
import helper from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"
import type { ConversationMessage } from "@/generated/backend"

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
})
