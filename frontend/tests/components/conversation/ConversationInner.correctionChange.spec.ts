import { ConversationMessageController } from "@generated/donut-backend-api/sdk.gen"
import { mockSdkService, mockShowNote, wrapSdkError } from "@tests/helpers"
import { flushPromises, type DOMWrapper } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { beforeEach, expect, it, vi } from "vitest"
import {
  body,
  changedLineTexts,
  declinedCorrection,
  markedTexts,
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

const solidOidc = (
  webId: string,
  openIdConnect: string,
  sentence: string
) => `---
${webId}: WebID
type: Note
url: https://solidproject.org/TR/oidc
${openIdConnect}: OpenID Connect
a part of: Solid
---

${sentence}`
const oldSentence = "Solid OIDC lets an app prove who the user is."
const newSentence = "Solid OIDC lets a pod verify who the user is."
const removedWords = "n app prove"
const addedWords = " pod verify"
const currentContent = solidOidc(
  "a specialization of",
  "an instance of",
  oldSentence
)
const proposedContent = solidOidc("uses", "builds on", newSentence)
const proposal = { ...body, proposedValue: proposedContent }

const mockCorrection = (correction: typeof body) =>
  mockSdkService(ConversationMessageController, "getConversationCorrections", [
    correction,
  ])

beforeEach(() => {
  setupCorrections()
  mockShowNote(makeMe.aNoteRealm.id(realm.id).content(currentContent).please())
  mockCorrection(proposal)
})

const mountReview = async () => {
  const wrapper = mountConversation()
  await flushPromises()
  return wrapper.find('[data-testid="conversation-note-correction"]')
}
const click = (review: DOMWrapper<Element>, label: string) =>
  review
    .findAll("button")
    .find((button) => button.text() === label)!
    .trigger("click")

it("marks the removed and added text of a pending content correction", async () => {
  const review = await mountReview()
  expect(markedTexts(review, "del")).toEqual([
    "a specialization of",
    "an instance of",
    removedWords,
  ])
  expect(markedTexts(review, "ins")).toEqual(["uses", "builds on", addedWords])
  const change = review.find('[data-testid="note-correction-change"]')
  expect(change.text()).toContain("type: Note")
  expect(change.text()).toContain("url: https://solidproject.org/TR/oidc")
  expect(change.text()).toContain("a part of: Solid")
})

it("shows the change before the current content, the original proposal and the editor", async () => {
  const review = await mountReview()
  const html = review.html()
  const changeAt = html.indexOf("note-correction-change")
  const rest = ["Current content</h4>", "Original proposal", "<textarea"]
  for (const part of rest) {
    expect(html.indexOf(part)).toBeGreaterThan(changeAt)
  }
  expect((review.find("textarea").element as HTMLTextAreaElement).value).toBe(
    proposedContent
  )
  expect(review.find("details").text()).toContain(newSentence)
})

it("removes the mark of a line typed back and accepts the wording without that change", async () => {
  const accept = mockSdkService(
    ConversationMessageController,
    "acceptCorrection",
    proposal
  )
  const review = await mountReview()
  const edited = solidOidc("uses", "an instance of", newSentence)
  await review.find("textarea").setValue(edited)
  expect(markedTexts(review, "del")).toEqual([
    "a specialization of",
    removedWords,
  ])
  expect(markedTexts(review, "ins")).toEqual(["uses", addedWords])
  await click(review, "Accept")
  expect(accept).toHaveBeenCalledWith({
    path: { correctionId: body.id },
    body: { reviewedValue: edited, baseRevision: body.baseRevision },
  })
})

it("says there is no change when the wording equals the current content", async () => {
  const review = await mountReview()
  await review.find("textarea").setValue(currentContent)
  expect(
    review.find('[data-testid="note-correction-change"]').text()
  ).toContain("No change")
  expect(review.findAll("del, ins")).toHaveLength(0)
})

const mountReviewAddingABlankLine = async () => {
  mockShowNote(
    makeMe.aNoteRealm.id(realm.id).content("first\nsecond\nthird").please()
  )
  const review = await mountReview()
  await review.find("textarea").setValue("first\nchanged\n\nthird")
  return review
}

it("shows each line with no added characters", async () => {
  const review = await mountReviewAddingABlankLine()
  const change = review.find('[data-testid="note-correction-change"]')
  expect(changedLineTexts(change)).toEqual([
    "first",
    "second",
    "changed",
    "",
    "third",
  ])
  expect(change.find("div").element.textContent).toBe("firstsecondchangedthird")
})

it("marks whole a removed line and the added lines that share no wording with it", async () => {
  const review = await mountReviewAddingABlankLine()
  expect(markedTexts(review, "del")).toEqual(["second"])
  expect(markedTexts(review, "ins")).toEqual(["changed", ""])
})

it("visibly marks a blank added line", async () => {
  const review = await mountReviewAddingABlankLine()
  const blank = review.findAll("ins").find((mark) => mark.text() === "")!
  document.body.append(review.element)
  const { height } = blank.element.getBoundingClientRect()
  review.element.remove()
  expect(height).toBeGreaterThan(0)
})

it("marks the change against the content loaded after a stale acceptance", async () => {
  mockSdkService(
    ConversationMessageController,
    "acceptCorrection",
    proposal
  ).mockResolvedValue({
    ...wrapSdkError({ message: "This review is stale." }),
    response: new Response(null, { status: 409 }),
  } as never)
  const refreshedContent = solidOidc("uses", "an instance of", oldSentence)
  mockSdkService(ConversationMessageController, "loadCurrentReview", {
    noteRealm: makeMe.aNoteRealm
      .id(realm.id)
      .content(refreshedContent)
      .please(),
    baseRevision: "c".repeat(40),
  })
  const review = await mountReview()
  await click(review, "Accept")
  await flushPromises()
  await click(review, "Load current content")
  await flushPromises()
  expect(markedTexts(review, "del")).toEqual(["an instance of", removedWords])
  expect(markedTexts(review, "ins")).toEqual(["builds on", addedWords])
})

it.each([
  ["a title correction", () => mockCorrection({ ...proposal, field: "TITLE" })],
  ["a declined correction", () => mockCorrection(declinedCorrection())],
  [
    "a reader who cannot accept",
    () =>
      mockShowNote(
        makeMe.aNoteRealm
          .id(realm.id)
          .content(currentContent)
          .readonly()
          .please()
      ),
  ],
])("shows no change display for %s", async (_name, arrange) => {
  arrange()
  const review = await mountReview()
  expect(review.text()).toContain("Current")
  expect(review.find('[data-testid="note-correction-change"]').exists()).toBe(
    false
  )
  expect(review.findAll("del, ins")).toHaveLength(0)
})
