import { expect, vi } from "vitest"
import ConversationComponent from "@/components/conversations/ConversationComponent.vue"
import helper, { mockSdkService } from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"

const mockedPush = vi.fn()
vitest.mock("vue-router", () => ({
  useRouter: () => ({
    push: mockedPush,
  }),
}))

beforeEach(() => {
  vi.useFakeTimers()
})

afterEach(() => {
  vi.clearAllTimers()
})

describe("ConversationComponent", () => {
  let wrapper
  const note = makeMe.aNote.please()
  const conversation = makeMe.aConversation.forANote(note).please()
  const user = makeMe.aUser.please()

  beforeEach(() => {
    mockedPush.mockClear()
    mockSdkService("getConversationsAboutNote", [])
    mockSdkService("getConversationMessages", [])
    // Mock showNote to prevent real API calls from StoredApiCollection
    mockSdkService("showNote", makeMe.aNoteRealm.please())
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

  describe("maximize/restore functionality", () => {
    it("should toggle maximize state when maximize button is clicked", async () => {
      const note = makeMe.aNote.please()
      const conversation = makeMe.aConversation.forANote(note).please()
      const user = makeMe.aUser.please()

      mockSdkService("getConversationMessages", [])
      const wrapper = helper
        .component(ConversationComponent)
        .withStorageProps({
          conversation,
          user,
        })
        .mount()

      await wrapper.find('[aria-label="Toggle maximize"]').trigger("click")
      expect(wrapper.find(".subject-container").exists()).toBe(false)

      await wrapper.find('[aria-label="Toggle maximize"]').trigger("click")
      expect(wrapper.find(".subject-container").exists()).toBe(true)
    })
  })
})
