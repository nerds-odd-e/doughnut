import { ConversationMessageController } from "@generated/donut-backend-api/sdk.gen"
import { mockSdkService, mockShowNote } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { beforeEach, expect, it, vi } from "vitest"
import {
  body,
  correctionBuilder,
  mountConversation,
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

const REASON = body.reason!
const SOLID_QUOTE =
  "WebIDs underpin Solid and are used as a primary identifier for Users in this specification."
const SOLID_URL = "https://solid.github.io/solid-oidc/"
const SOLID_LINK =
  "https://solid.github.io/solid-oidc/#:~:text=WebIDs%20underpin%20Solid%20and%20are%20used%20as%20a%20primary%20identifier%20for%20Users%20in%20this%20specification."

const aCorrection = () => correctionBuilder().id(body.id!)

const reviewOf = async (
  correction: ReturnType<ReturnType<typeof aCorrection>["please"]>
) => {
  mockSdkService(ConversationMessageController, "getConversationCorrections", [
    correction,
  ])
  const wrapper = mountConversation()
  await flushPromises()
  return wrapper.find('[data-testid="conversation-note-correction"]')
}

const expectSource = (
  review: Awaited<ReturnType<typeof reviewOf>>,
  quote: string,
  url: string,
  link: string
) => {
  const source = review.find('[data-testid="correction-source"]')
  expect(source.find("blockquote").text()).toBe(quote)
  expect(source.text()).not.toContain(REASON)
  expect(source.find("a").text()).toBe(url)
  expect(source.find("a").attributes()).toMatchObject({
    href: link,
    target: "_blank",
    rel: "noopener noreferrer",
  })
  expect(review.text()).not.toContain("No source was found.")
}

it.each([
  ["a web page", SOLID_QUOTE, SOLID_URL, SOLID_LINK],
  [
    "a Japanese dictionary entry",
    "〘 他動詞 ザ上一 〙",
    "https://kotobank.jp/word/%E6%A1%88%E3%81%98%E3%82%8B-429264",
    "https://kotobank.jp/word/%E6%A1%88%E3%81%98%E3%82%8B-429264#:~:text=%E3%80%98%20%E4%BB%96%E5%8B%95%E8%A9%9E%20%E3%82%B6%E4%B8%8A%E4%B8%80%20%E3%80%99",
  ],
  [
    "a PDF",
    "A common-cause example",
    "https://example.org/papers/causation.pdf",
    "https://example.org/papers/causation.pdf#:~:text=A%20common%2Dcause%20example",
  ],
  [
    "an address that already has a # part",
    "A common-cause example",
    "https://example.org/causation#examples",
    "https://example.org/causation#examples:~:text=A%20common%2Dcause%20example",
  ],
])(
  "shows an author the quote and a link to the quoted passage of %s, apart from the reason",
  async (_, quote, url, link) => {
    const review = await reviewOf(aCorrection().source(quote, url).please())
    expect(review.find("textarea").exists()).toBe(true)
    expectSource(review, quote, url, link)
  }
)

it("keeps the quote and link on a reopened accepted correction", async () => {
  const review = await reviewOf(
    aCorrection()
      .source(SOLID_QUOTE, SOLID_URL)
      .acceptedBy(user, "Reviewed", "b".repeat(40), "2026-10-14T01:00:00Z")
      .please()
  )
  expect(review.text()).toContain("Accepted")
  expectSource(review, SOLID_QUOTE, SOLID_URL, SOLID_LINK)
})

it("keeps the quote and link on a declined correction", async () => {
  const review = await reviewOf(
    aCorrection()
      .source(SOLID_QUOTE, SOLID_URL)
      .declinedBy(user, "2026-10-14T01:00:00Z")
      .please()
  )
  expect(review.text()).toContain("Declined")
  expectSource(review, SOLID_QUOTE, SOLID_URL, SOLID_LINK)
})

it("shows the quote and link to a reader who cannot accept", async () => {
  mockShowNote(makeMe.aNoteRealm.id(realm.id).readonly().please())
  const review = await reviewOf(
    aCorrection().source(SOLID_QUOTE, SOLID_URL).please()
  )
  expect(review.find("textarea").exists()).toBe(false)
  expectSource(review, SOLID_QUOTE, SOLID_URL, SOLID_LINK)
})

it("says no source was found for a content correction without one", async () => {
  const review = await reviewOf(aCorrection().please())
  expect(review.find('[data-testid="correction-source"]').text()).toBe(
    "No source was found."
  )
  expect(review.find('[data-testid="correction-source"] a').exists()).toBe(
    false
  )
})

it("shows no source line for a title correction", async () => {
  const review = await reviewOf(aCorrection().title("Common cause").please())
  expect(review.text()).toContain(REASON)
  expect(review.find('[data-testid="correction-source"]').exists()).toBe(false)
  expect(review.text()).not.toContain("No source was found.")
})
