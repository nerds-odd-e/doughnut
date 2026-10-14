import NoteConversation from "@/components/conversations/NoteConversation.vue"
import MessageCenterPage from "@/pages/MessageCenterPage.vue"
import { messageCenter } from "@/store/messageCenter"
import { ConversationMessageController } from "@generated/donut-backend-api/sdk.gen"
import helper, {
  mockSdkService,
  mockShowNote,
  wrapSdkResponse,
} from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { afterEach, beforeEach, expect, it, vi } from "vitest"

const user = makeMe.aUser.please()
const note = makeMe.aNote.please()
const conversation = makeMe.aConversation.withId(73).forANote(note).please()
const reply = { id: 75, message: "The stored answer is ready to read." }
const unrelated = { id: 76, message: "Another discussion's unread reply." }

beforeEach(() => {
  mockShowNote()
  window.HTMLElement.prototype.scrollIntoView = vi.fn()
  messageCenter.unreadMessages = [reply, unrelated]
  mockSdkService(ConversationMessageController, "getConversationsAboutNote", [
    conversation,
  ])
})
afterEach(() => {
  messageCenter.unreadMessages = []
})

it.each(["Message Center", "note conversation"])(
  "%s acknowledges its successfully displayed messages and retains unrelated unread replies",
  async (viewer) => {
    mockSdkService(ConversationMessageController, "getConversationMessages", [
      reply,
    ])
    const acknowledge = mockSdkService(
      ConversationMessageController,
      "markConversationAsRead",
      [unrelated]
    )
    mockSdkService(
      ConversationMessageController,
      "getConversationsOfCurrentUser",
      [makeMe.aConversationListItem.please()]
    )
    mockSdkService(
      ConversationMessageController,
      "getConversation",
      conversation
    )
    const wrapper =
      viewer === "Message Center"
        ? helper
            .component(MessageCenterPage)
            .withRouter()
            .withCleanStorage()
            .withCurrentUser(user)
            .withProps({ conversationId: conversation.id })
            .mount()
        : helper
            .component(NoteConversation)
            .withRouter()
            .withCleanStorage()
            .withCurrentUser(user)
            .withProps({ noteId: note.id })
            .mount()
    await flushPromises()

    expect(wrapper.text()).toContain(reply.message)
    expect(acknowledge).toHaveBeenCalledWith({
      path: { conversationId: conversation.id },
    })
    expect(messageCenter.unreadMessages).toEqual([unrelated])
    wrapper.unmount()
  }
)

it("retains unread replies when fetching a note conversation fails", async () => {
  const fetchMessages = mockSdkService(
    ConversationMessageController,
    "getConversationMessages",
    []
  )
  fetchMessages.mockResolvedValue({
    ...wrapSdkResponse(undefined),
    error: { message: "Conversation could not be loaded." },
  })
  const acknowledge = mockSdkService(
    ConversationMessageController,
    "markConversationAsRead",
    []
  )
  const wrapper = helper
    .component(NoteConversation)
    .withRouter()
    .withCleanStorage()
    .withCurrentUser(user)
    .withProps({ noteId: note.id })
    .mount()
  await flushPromises()

  expect(fetchMessages).toHaveBeenCalledWith({
    path: { conversationId: conversation.id },
  })
  expect(acknowledge).not.toHaveBeenCalled()
  expect(messageCenter.unreadMessages).toEqual([reply, unrelated])
  wrapper.unmount()
})

beforeEach(() => {
  mockSdkService(
    ConversationMessageController,
    "getConversationCorrections",
    []
  )
})
