import { ConversationMessageController } from "@generated/donut-backend-api/sdk.gen"
import { mockSdkService, wrapSdkError, wrapSdkResponse } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { beforeEach, expect, it, vi } from "vitest"
import {
  body,
  exchange,
  mountConversation,
  realm,
  setupCorrections,
} from "./conversationCorrectionTestSupport"

vi.mock("@/managedApi/AiReplyEventSource", async () => {
  const { aiReplyEventSourceTrackingMock } = await import(
    "./aiReplyEventSourceTrackingMock"
  )
  return aiReplyEventSourceTrackingMock()
})
beforeEach(setupCorrections)

const staleResponse = () => ({
  ...wrapSdkError({ message: "This review is stale." }),
  response: new Response(null, { status: 409 }),
})

it.each(["CONTENT", "TITLE"] as const)(
  "keeps edited %s through an explicit current review and sends its new base only on another acceptance",
  async (field) => {
    const proposal = { ...body, field }
    mockSdkService(
      ConversationMessageController,
      "getConversationCorrections",
      [proposal]
    )
    const accept = mockSdkService(
      ConversationMessageController,
      "acceptCorrection",
      proposal
    )
    accept.mockResolvedValue(staleResponse())
    const current = {
      noteRealm: makeMe.aNoteRealm
        .id(realm.id)
        .title("Current title")
        .content("Current accepted wording")
        .please(),
      baseRevision: "c".repeat(40),
    }
    let resolveRefresh: (
      result: ReturnType<typeof wrapSdkResponse<typeof current>>
    ) => void
    const refresh = mockSdkService(
      ConversationMessageController,
      "loadCurrentReview",
      current
    )
    refresh.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveRefresh = resolve
        })
    )
    const wrapper = mountConversation()
    await flushPromises()
    const review = wrapper.find('[data-testid="conversation-note-correction"]')
    const editor = review.find(field === "TITLE" ? "input" : "textarea")
    const click = (label: string) =>
      review
        .findAll("button")
        .find((button) => button.text() === label)!
        .trigger("click")
    await editor.setValue("My retained wording")
    await click("Accept")
    await flushPromises()
    expect(review.text()).toContain("This review is stale")
    await click("Load current content")
    await flushPromises()
    expect(refresh).toHaveBeenCalledWith({ path: { correctionId: body.id } })
    resolveRefresh!(wrapSdkResponse(current))
    await flushPromises()
    expect(review.text()).toContain(
      field === "TITLE" ? "Current title" : "Current accepted wording"
    )
    expect(review.text()).toContain("Review your wording, then choose Accept")
    expect((editor.element as HTMLInputElement).value).toBe(
      "My retained wording"
    )
    expect(accept).toHaveBeenCalledTimes(1)
    expect(review.find("details").text()).toContain("Proposed distinction")
    expect(wrapper.text()).toContain(exchange.message)
    await click("Accept")
    await flushPromises()
    expect(accept).toHaveBeenLastCalledWith({
      path: { correctionId: body.id },
      body: {
        reviewedValue: "My retained wording",
        baseRevision: current.baseRevision,
      },
    })
    wrapper.unmount()
  }
)

it("keeps the input and previous base when the explicit refresh fails", async () => {
  const accept = mockSdkService(
    ConversationMessageController,
    "acceptCorrection",
    body
  )
  accept.mockResolvedValue(staleResponse())
  const refresh = mockSdkService(
    ConversationMessageController,
    "loadCurrentReview",
    { noteRealm: realm, baseRevision: "c".repeat(40) }
  )
  refresh.mockResolvedValue(wrapSdkError({ message: "Read failed" }))
  const wrapper = mountConversation()
  await flushPromises()
  const review = wrapper.find('[data-testid="conversation-note-correction"]')
  const click = (label: string) =>
    review
      .findAll("button")
      .find((button) => button.text() === label)!
      .trigger("click")
  await review.find("textarea").setValue("My retained wording")
  await click("Accept")
  await flushPromises()
  await click("Load current content")
  await flushPromises()
  expect(review.text()).toContain(
    "Could not load the current note. Your review is kept"
  )
  expect((review.find("textarea").element as HTMLTextAreaElement).value).toBe(
    "My retained wording"
  )
  await click("Accept")
  await flushPromises()
  expect(accept).toHaveBeenLastCalledWith({
    path: { correctionId: body.id },
    body: {
      reviewedValue: "My retained wording",
      baseRevision: body.baseRevision,
    },
  })
  wrapper.unmount()
})
