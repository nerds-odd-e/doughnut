import { ConversationMessageController } from "@generated/donut-backend-api/sdk.gen"
import { mockSdkService, mockShowNote } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { beforeEach, expect, it, vi } from "vitest"
import {
  body,
  changedLineTexts,
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

beforeEach(setupCorrections)

const mountChange = async (current: string, proposed: string) => {
  mockShowNote(makeMe.aNoteRealm.id(realm.id).content(current).please())
  mockSdkService(ConversationMessageController, "getConversationCorrections", [
    { ...body, proposedValue: proposed },
  ])
  const wrapper = mountConversation()
  await flushPromises()
  return wrapper.find('[data-testid="note-correction-change"]')
}

it("marks only the replaced and added words of a one-line paragraph", async () => {
  const opening =
    "The prisoner's dilemma is a game between two players. Each player chooses to cooperate or to defect. "
  const ending = ". The game is studied in economics and biology."
  const added = " Both would do better by cooperating."
  const change = await mountChange(
    `${opening}Defecting always pays more${ending}`,
    `${opening}Betrayal wins each round${ending}${added}`
  )
  expect(markedTexts(change, "del")).toEqual(["Defecting always pays more"])
  expect(markedTexts(change, "ins")).toEqual([
    "Betrayal wins each round",
    added,
  ])
  expect(changedLineTexts(change)).toEqual([
    `${opening}Defecting always pays moreBetrayal wins each round${ending}${added}`,
  ])
})

it("marks only the characters added in the middle of a Japanese line", async () => {
  const change = await mountChange(
    "日本語の文は、質問になります。\n次の行",
    "日本語の文は、最後に「か」がつくと質問になります。\n次の行"
  )
  expect(markedTexts(change, "ins")).toEqual(["最後に「か」がつくと"])
  expect(markedTexts(change, "del")).toEqual([])
  expect(changedLineTexts(change)).toEqual([
    "日本語の文は、最後に「か」がつくと質問になります。",
    "次の行",
  ])
})
