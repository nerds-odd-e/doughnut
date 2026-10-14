import { ConversationMessageController } from "@generated/donut-backend-api/sdk.gen"
import {
  mockSdkService,
  mockShowNote,
  productionRouterAt,
  wrapSdkError,
  wrapSdkResponse,
} from "@tests/helpers"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { flushPromises } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { beforeEach, expect, it, vi } from "vitest"
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

const accepted = () =>
  makeMe.aConversationNoteCorrection
    .id(body.id)
    .forNote(realm.note)
    .fromMessage(exchange.id)
    .content(body.proposedValue!)
    .acceptedBy(
      user,
      "**Reviewed distinction**",
      "b".repeat(40),
      "2026-10-14T02:00:00Z"
    )
    .please()

it.each([noteConversation, recallConversation])(
  "accepts edited content only after confirmation and reopens its receipt after a later note edit",
  async (conversation) => {
    const receipt = accepted()
    let confirm: (
      result: ReturnType<typeof wrapSdkResponse<typeof receipt>>
    ) => void
    const accept = mockSdkService(
      ConversationMessageController,
      "acceptCorrection",
      receipt
    )
    accept.mockImplementation(
      () =>
        new Promise((resolve) => {
          confirm = resolve
        })
    )
    const router = await productionRouterAt(noteShowLocation(realm.id))
    const wrapper = mountConversation(conversation, router)
    await flushPromises()
    const review = wrapper.find('[data-testid="conversation-note-correction"]')
    await review.find("textarea").setValue(" **Reviewed distinction** ")
    await review
      .findAll("button")
      .find((button) => button.text() === "Accept")!
      .trigger("click")
    expect(review.text()).toContain("Pending review")
    expect(accept).toHaveBeenCalledWith({
      path: { correctionId: body.id },
      body: {
        reviewedValue: " **Reviewed distinction** ",
        baseRevision: body.baseRevision,
      },
    })
    mockShowNote(
      makeMe.aNoteRealm.id(realm.id).content(receipt.reviewedValue!).please()
    )
    confirm!(wrapSdkResponse(receipt))
    await flushPromises()
    expect(review.text()).toContain(
      `Accepted revision: ${receipt.acceptedRevision}`
    )
    expect(
      review.find('[data-testid="accepted-reviewed-content"]').text()
    ).toBe("Reviewed distinction")
    expect(review.find("strong").text()).toBe("Reviewed distinction")
    await review
      .findAll("button")
      .find((button) => button.text() === "Open note")!
      .trigger("click")
    await flushPromises()
    expect(wrapper.emitted("close-dialog")).toHaveLength(1)
    expect(router.currentRoute.value).toMatchObject(noteShowLocation(realm.id))
    wrapper.unmount()
    mockShowNote(
      makeMe.aNoteRealm.id(realm.id).content("**Later ordinary edit**").please()
    )
    mockSdkService(
      ConversationMessageController,
      "getConversationCorrections",
      [receipt]
    )
    const reopened = mountConversation(conversation)
    await flushPromises()
    const retained = reopened.find(
      '[data-testid="conversation-note-correction"]'
    )
    expect(retained.text()).toContain("Later ordinary edit")
    expect(
      retained.find('[data-testid="accepted-reviewed-content"]').text()
    ).toBe("Reviewed distinction")
    expect(retained.text()).toContain(receipt.acceptedRevision)
    reopened.unmount()
  }
)

it("keeps input after a lost response and retries the same proposal and base to recover its original receipt", async () => {
  const receipt = accepted()
  const accept = mockSdkService(
    ConversationMessageController,
    "acceptCorrection",
    receipt
  )
  accept.mockResolvedValueOnce(wrapSdkError({ message: "Response lost" }))
  const wrapper = mountConversation(recallConversation)
  await flushPromises()
  const review = wrapper.find('[data-testid="conversation-note-correction"]')
  await review.find("textarea").setValue(receipt.reviewedValue!)
  const clickAccept = () =>
    review
      .findAll("button")
      .find((button) => button.text() === "Accept")!
      .trigger("click")
  await clickAccept()
  await flushPromises()
  expect(review.text()).toContain("Could not confirm acceptance")
  expect((review.find("textarea").element as HTMLTextAreaElement).value).toBe(
    receipt.reviewedValue
  )
  expect(review.text()).toContain("Pending review")
  await clickAccept()
  await flushPromises()
  expect(accept.mock.calls.map(([options]) => options)).toEqual([
    {
      path: { correctionId: body.id },
      body: {
        reviewedValue: receipt.reviewedValue,
        baseRevision: body.baseRevision,
      },
    },
    {
      path: { correctionId: body.id },
      body: {
        reviewedValue: receipt.reviewedValue,
        baseRevision: body.baseRevision,
      },
    },
  ])
  expect(review.text()).toContain(receipt.acceptedRevision)
  wrapper.unmount()
})

it("retains confirmed acceptance when refreshing the note fails", async () => {
  const receipt = accepted()
  mockSdkService(ConversationMessageController, "acceptCorrection", receipt)
  const wrapper = mountConversation()
  await flushPromises()
  mockShowNote(realm).mockResolvedValue(
    wrapSdkError({ message: "Read failed" })
  )
  const review = wrapper.find('[data-testid="conversation-note-correction"]')
  await review
    .findAll("button")
    .find((button) => button.text() === "Accept")!
    .trigger("click")
  await flushPromises()
  expect(review.text()).toContain(
    "The correction is accepted. Could not reload the note."
  )
  expect(review.text()).toContain(receipt.acceptedRevision)
  wrapper.unmount()
})

it("offers only participant decline for a readonly MCQ note", async () => {
  mockShowNote(makeMe.aNoteRealm.id(realm.id).readonly().please())
  const wrapper = mountConversation(recallConversation)
  await flushPromises()
  const review = wrapper.find('[data-testid="conversation-note-correction"]')
  expect(review.findAll("button").map((button) => button.text())).toEqual([
    "Decline",
  ])
  wrapper.unmount()
})
