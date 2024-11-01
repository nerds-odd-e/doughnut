import { expect, vi } from "vitest"
import ConversationComponent from "@/components/conversations/ConversationComponent.vue"
import helper from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"

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
    mockedPush.mockClear()
    helper.managedApi.restConversationMessageController.getConversationsAboutNote =
      vi.fn().mockResolvedValue([])
    wrapper = helper
      .component(ConversationComponent)
      .withStorageProps({
        conversation,
        user,
      })
      .mount()
  })

  it("routes to note show page when minimize button is clicked and subject is a note", async () => {
    const minimizeButton = wrapper.find("button.minimize-button")
    await minimizeButton.trigger("click")

    expect(mockedPush).toHaveBeenCalledWith({
      name: "noteShow",
      params: { noteId: note.id },
    })
  })
})
