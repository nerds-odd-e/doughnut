import { ConversationMessageController } from "@generated/donut-backend-api/sdk.gen"
import { mockSdkService, mockShowNote, wrapSdkError } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { beforeEach, expect, it, vi } from "vitest"
import { sidebarStructuralRefreshKey } from "@/components/notes/sidebarStructuralRefresh"
import {
  exchange,
  mountConversation,
  noteConversation,
  recallConversation,
  realm,
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
const proposal = () =>
  makeMe.aConversationNoteCorrection
    .forNote(realm.note)
    .fromMessage(exchange.id)
    .title("Proposed title")
    .please()

const acceptReviewedTitle = async (
  conversation: Parameters<typeof mountConversation>[0]
) => {
  const title = proposal()
  mockSdkService(ConversationMessageController, "getConversationCorrections", [
    title,
  ])
  const receipt = makeMe.aConversationNoteCorrection
    .id(title.id)
    .forNote(realm.note)
    .fromMessage(exchange.id)
    .title(title.proposedValue!)
    .acceptedBy(user, "Reviewed title", "b".repeat(40), "2026-10-14T03:00:00Z")
    .please()
  const accept = mockSdkService(
    ConversationMessageController,
    "acceptCorrection",
    receipt
  )
  const wrapper = mountConversation(conversation)
  await flushPromises()
  const review = wrapper.find('[data-testid="conversation-note-correction"]')
  await review.find("input").setValue("Reviewed title")
  const sidebarRefreshesBefore = sidebarStructuralRefreshKey.value
  await review
    .findAll("button")
    .find((button) => button.text() === "Accept")!
    .trigger("click")
  await flushPromises()
  return { title, receipt, accept, wrapper, review, sidebarRefreshesBefore }
}

it.each([noteConversation, recallConversation])(
  "accepts an explicitly edited title and retains the confirmed wording and revision",
  async (conversation) => {
    const { title, receipt, accept, wrapper, review } =
      await acceptReviewedTitle(conversation)
    expect(accept).toHaveBeenCalledWith({
      path: { correctionId: title.id },
      body: {
        reviewedValue: "Reviewed title",
        baseRevision: title.baseRevision,
      },
    })
    expect(review.text()).toContain("Reviewed title")
    expect(review.text()).toContain(
      `Accepted revision: ${receipt.acceptedRevision}`
    )
    wrapper.unmount()
  }
)

it.each([noteConversation, recallConversation])(
  "refreshes the sidebar listings after accepting a title correction",
  async (conversation) => {
    const { wrapper, sidebarRefreshesBefore } =
      await acceptReviewedTitle(conversation)
    expect(sidebarStructuralRefreshKey.value).toBe(sidebarRefreshesBefore + 1)
    wrapper.unmount()
  }
)

it.each(["UPDATE_VISIBLE_TEXT", "KEEP_VISIBLE_TEXT"])(
  "sends the author's reference-handling choice %s with explicit acceptance",
  async (choice) => {
    const title = proposal()
    const referenced = makeMe.aNoteRealm
      .id(realm.id)
      .title("Causation")
      .please()
    makeMe.aNoteRealm.under(referenced).please()
    mockShowNote(referenced)
    mockSdkService(
      ConversationMessageController,
      "getConversationCorrections",
      [title]
    )
    const accept = mockSdkService(
      ConversationMessageController,
      "acceptCorrection",
      title
    )
    const wrapper = mountConversation()
    await flushPromises()
    const review = wrapper.find('[data-testid="conversation-note-correction"]')
    const button = review
      .findAll("button")
      .find((button) => button.text() === "Accept")!
    expect(button.attributes("disabled")).toBeDefined()
    await review.find("input").setValue("Reviewed title")
    await review.find("select").setValue(choice)
    await button.trigger("click")
    await flushPromises()
    expect(accept).toHaveBeenCalledWith({
      path: { correctionId: title.id },
      body: {
        reviewedValue: "Reviewed title",
        baseRevision: title.baseRevision,
        referenceHandling: choice,
      },
    })
    wrapper.unmount()
  }
)

it("shows an ordinary title validation refusal while retaining the draft for correction", async () => {
  const title = proposal()
  mockSdkService(ConversationMessageController, "getConversationCorrections", [
    title,
  ])
  mockSdkService(
    ConversationMessageController,
    "acceptCorrection",
    title
  ).mockResolvedValue(
    wrapSdkError({
      errors: { newTitle: "Title cannot contain path separators" },
    })
  )
  const wrapper = mountConversation()
  await flushPromises()
  const review = wrapper.find('[data-testid="conversation-note-correction"]')
  await review.find("input").setValue("bad/title")
  await review
    .findAll("button")
    .find((button) => button.text() === "Accept")!
    .trigger("click")
  await flushPromises()
  expect(review.text()).toContain("Title cannot contain path separators")
  expect((review.find("input").element as HTMLInputElement).value).toBe(
    "bad/title"
  )
  wrapper.unmount()
})
