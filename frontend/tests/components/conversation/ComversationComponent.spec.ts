import { expect } from "vitest"
import ConversationComponent from "@/components/conversations/ConversationComponent.vue"
import NoteShow from "@/components/notes/NoteShow.vue"
import helper from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"
import type { ConversationMessage } from "@/generated/backend"

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
})
