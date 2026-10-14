import { ConversationMessageController } from "@generated/donut-backend-api/sdk.gen"
import MessageCenterPage from "@/pages/MessageCenterPage.vue"
import helper, { mockSdkService, mockShowNote } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { beforeEach, expect, it, vi } from "vitest"
import { getLastInstance } from "@tests/helpers/aiReplyEventSourceTracker"
import { createToolCallChunk } from "./aiResponseTestSupport"

import {
  body,
  exchange,
  mountConversation,
  noteConversation,
  realm,
  recallConversation,
  setupCorrections,
  user,
} from "./conversationCorrectionTestSupport"

vi.mock("@/managedApi/AiReplyEventSource", async () => {
  const { aiReplyEventSourceTrackingMock } = await import(
    "./aiReplyEventSourceTrackingMock"
  )
  return aiReplyEventSourceTrackingMock()
})

beforeEach(setupCorrections)

it.each([noteConversation, recallConversation])(
  "reopens the stored pending body review and discards unsent editing",
  async (conversation) => {
    const wrapper = mountConversation(conversation)
    await flushPromises()
    const review = wrapper.find('[data-testid="conversation-note-correction"]')
    expect(review.text()).toContain("Causation")
    expect(review.text()).toContain("Pending review")
    expect(review.text()).toContain(body.reason)
    expect(review.find("strong").text()).toBe("Current distinction")
    expect(review.element.previousElementSibling?.textContent).toContain(
      exchange.message
    )
    await review.find("textarea").setValue("My unsent review")
    await wrapper.find(".minimize-button").trigger("click")
    expect(wrapper.emitted("close-dialog")).toHaveLength(1)
    wrapper.unmount()
    const reopened = mountConversation(conversation)
    await flushPromises()
    expect(
      (
        reopened.find('[data-testid="conversation-note-correction"] textarea')
          .element as HTMLTextAreaElement
      ).value
    ).toBe(body.proposedValue)
    reopened.unmount()
  }
)

it("shows and edits the stored title in Messages", async () => {
  const title = makeMe.aConversationNoteCorrection
    .forNote(realm.note)
    .fromMessage(exchange.id)
    .title("Common causes")
    .please()
  mockSdkService(ConversationMessageController, "getConversationCorrections", [
    title,
  ])
  mockSdkService(ConversationMessageController, "getConversationsAboutNote", [
    noteConversation,
  ])
  mockSdkService(
    ConversationMessageController,
    "getConversationsOfCurrentUser",
    [makeMe.aConversationListItem.please()]
  )
  mockSdkService(
    ConversationMessageController,
    "getConversation",
    noteConversation
  )
  const mountMessages = () =>
    helper
      .component(MessageCenterPage)
      .withCleanStorage()
      .withRouter()
      .withCurrentUser(user)
      .withProps({ conversationId: noteConversation.id })
      .mount()
  const wrapper = mountMessages()
  await flushPromises()
  const review = wrapper.find('[data-testid="conversation-note-correction"]')
  expect(review.text()).toContain("Current title")
  await review.find("input").setValue("Reviewed title")
  expect((review.find("input").element as HTMLInputElement).value).toBe(
    "Reviewed title"
  )
  wrapper.unmount()
  const reopened = mountMessages()
  await flushPromises()
  expect(
    (
      reopened.find('[data-testid="conversation-note-correction"] input')
        .element as HTMLInputElement
    ).value
  ).toBe(title.proposedValue)
  reopened.unmount()
})

it("renders proposed Markdown for readers under ordinary note edit permission", async () => {
  mockShowNote(makeMe.aNoteRealm.id(realm.id).readonly().please())
  const wrapper = mountConversation(recallConversation)
  await flushPromises()
  const review = wrapper.find('[data-testid="conversation-note-correction"]')
  expect(review.findAll("strong").at(-1)?.text()).toBe("Proposed distinction")
  expect(review.find("textarea").exists()).toBe(false)
  wrapper.unmount()
})

it("refreshes stored proposals after the assistant completes and retains unsent input during follow-up", async () => {
  const readCorrections = mockSdkService(
    ConversationMessageController,
    "getConversationCorrections",
    []
  )
  mockSdkService(
    ConversationMessageController,
    "replyToConversation",
    undefined
  )
  const wrapper = mountConversation()
  await flushPromises()
  await wrapper.find("textarea").setValue("Please clarify")
  await wrapper.find("form.message-input-form").trigger("submit")
  await flushPromises()
  const stream = getLastInstance()!
  for (const chunk of createToolCallChunk("NoteContentCompletion", {
    content: body.proposedValue,
    reason: body.reason,
  })) {
    await stream.onMessageCallback(
      "chat.completion.chunk",
      JSON.stringify(chunk)
    )
  }
  mockSdkService(ConversationMessageController, "getConversationCorrections", [
    body,
  ])
  await stream.onMessageCallback("done", "")
  await flushPromises()
  expect(
    wrapper.findAll('[data-testid="conversation-note-correction"]')
  ).toHaveLength(1)
  await wrapper
    .find('[data-testid="conversation-note-correction"] textarea')
    .setValue("Unsent wording")
  await wrapper.find("form.message-input-form textarea").setValue("Continue")
  await wrapper.find("form.message-input-form").trigger("submit")
  await flushPromises()
  expect(
    (
      wrapper.find('[data-testid="conversation-note-correction"] textarea')
        .element as HTMLTextAreaElement
    ).value
  ).toBe("Unsent wording")
  expect(readCorrections).toHaveBeenCalled()
  wrapper.unmount()
})
