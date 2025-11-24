import { expect, vi } from "vitest"
import ConversationComponent from "@/components/conversations/ConversationComponent.vue"
import helper from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"
import { ConversationMessageController, NoteController } from "@generated/backend/sdk.gen"

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
    vi.spyOn(ConversationMessageController, "getConversationsAboutNote").mockResolvedValue({
      data: [],
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })
    vi.spyOn(ConversationMessageController, "getConversationMessages").mockResolvedValue({
      data: [],
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })
    // Mock showNote to prevent real API calls from StoredApiCollection
    vi.spyOn(NoteController, "showNote").mockResolvedValue({
      data: makeMe.aNoteRealm.please(),
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })
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

      vi.spyOn(ConversationMessageController, "getConversationMessages").mockResolvedValue({
        data: [],
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })
      const wrapper = helper
        .component(ConversationComponent)
        .withStorageProps({
          conversation,
          user,
        })
        .mount()

      // Click maximize button
      await wrapper.find('[aria-label="Toggle maximize"]').trigger("click")

      expect(wrapper.find(".subject-container").exists()).toBe(false)

      // Click restore button
      await wrapper.find('[aria-label="Toggle maximize"]').trigger("click")

      expect(wrapper.find(".subject-container").exists()).toBe(true)
    })
  })
})
