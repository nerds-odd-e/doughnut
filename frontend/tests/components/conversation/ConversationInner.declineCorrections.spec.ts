import { ConversationMessageController } from "@generated/donut-backend-api/sdk.gen"
import {
  mockSdkService,
  mockShowNote,
  wrapSdkError,
  wrapSdkResponse,
} from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { beforeEach, expect, it, vi } from "vitest"
import {
  body,
  declinedCorrection,
  mountConversation,
  noteConversation,
  realm,
  recallConversation,
  setupCorrections,
} from "./conversationCorrectionTestSupport"

vi.mock("@/managedApi/AiReplyEventSource", async () => {
  const { aiReplyEventSourceTrackingMock } = await import(
    "./aiReplyEventSourceTrackingMock"
  )
  return aiReplyEventSourceTrackingMock()
})

beforeEach(setupCorrections)

it.each([noteConversation, recallConversation])(
  "declines only after the server confirms and reopens the retained proposal",
  async (conversation) => {
    const declined = declinedCorrection()
    let confirm: (
      result: ReturnType<typeof wrapSdkResponse<typeof declined>>
    ) => void
    const decline = mockSdkService(
      ConversationMessageController,
      "declineCorrection",
      declined
    )
    decline.mockImplementation(
      () =>
        new Promise((resolve) => {
          confirm = resolve
        })
    )
    const wrapper = mountConversation(conversation)
    await flushPromises()
    const review = wrapper.find('[data-testid="conversation-note-correction"]')
    await review
      .findAll("button")
      .find((button) => button.text() === "Decline")!
      .trigger("click")
    expect(review.text()).toContain("Pending review")
    expect(decline).toHaveBeenCalledWith({ path: { correctionId: body.id } })
    confirm!(wrapSdkResponse(declined))
    await flushPromises()
    expect(review.text()).toContain("Declined")
    wrapper.unmount()
    mockSdkService(
      ConversationMessageController,
      "getConversationCorrections",
      [declined]
    )
    const reopened = mountConversation(conversation)
    await flushPromises()
    const retained = reopened.find(
      '[data-testid="conversation-note-correction"]'
    )
    expect(retained.text()).toContain("Declined")
    expect(retained.text()).toContain(body.reason)
    expect(retained.findAll("strong").at(-1)?.text()).toBe(
      "Proposed distinction"
    )
    reopened.unmount()
  }
)

it("preserves edited input and offers retry when decline fails", async () => {
  const declined = declinedCorrection()
  const decline = mockSdkService(
    ConversationMessageController,
    "declineCorrection",
    declined
  )
  decline.mockResolvedValueOnce(wrapSdkError({ message: "Request failed" }))
  const wrapper = mountConversation()
  await flushPromises()
  const review = wrapper.find('[data-testid="conversation-note-correction"]')
  await review.find("textarea").setValue("My review stays here")
  await review
    .findAll("button")
    .find((button) => button.text() === "Decline")!
    .trigger("click")
  await flushPromises()
  expect(review.text()).toContain("Could not decline. Try again.")
  expect(review.text()).toContain("Pending review")
  expect((review.find("textarea").element as HTMLTextAreaElement).value).toBe(
    "My review stays here"
  )
  await review
    .findAll("button")
    .find((button) => button.text() === "Decline")!
    .trigger("click")
  await flushPromises()
  expect(review.text()).toContain("Declined")
  wrapper.unmount()
})

it("lets a conversation participant decline under readonly note access", async () => {
  mockShowNote(makeMe.aNoteRealm.id(realm.id).readonly().please())
  mockSdkService(
    ConversationMessageController,
    "declineCorrection",
    declinedCorrection()
  )
  const wrapper = mountConversation(recallConversation)
  await flushPromises()
  const review = wrapper.find('[data-testid="conversation-note-correction"]')
  await review
    .findAll("button")
    .find((button) => button.text() === "Decline")!
    .trigger("click")
  await flushPromises()
  expect(review.text()).toContain("Declined")
  wrapper.unmount()
})
