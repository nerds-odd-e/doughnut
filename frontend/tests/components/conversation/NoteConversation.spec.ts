import { expect, vi } from "vitest"
import NoteConversation from "@/components/conversations/NoteConversation.vue"
import helper from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"
import { flushPromises, type VueWrapper } from "@vue/test-utils"

const mockedPush = vi.fn()
vitest.mock("vue-router", () => ({
  useRouter: () => ({
    push: mockedPush,
  }),
}))

describe("NoteConversation", () => {
  let wrapper: VueWrapper
  const note = makeMe.aNote.please()

  beforeEach(() => {
    wrapper = helper
      .component(NoteConversation)
      .withProps({
        noteId: note.id,
      })
      .mount()
    helper.managedApi.restConversationMessageController.startConversationAboutNote =
      vi.fn()
  })

  it("call api to start conversation", async () => {
    await wrapper.find("textarea").setValue("Hello")
    await wrapper.find("form.chat-input-form").trigger("submit")
    await flushPromises()

    expect(
      helper.managedApi.restConversationMessageController
        .startConversationAboutNote
    ).toHaveBeenCalledWith(note.id, "Hello")
  })
})
