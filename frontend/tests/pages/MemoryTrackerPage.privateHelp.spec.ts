import {
  ConversationMessageController,
  NoteController,
} from "@generated/donut-backend-api/sdk.gen"
import MemoryTrackerPage from "@/pages/MemoryTrackerPage.vue"
import helper, { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { resetInstance } from "@tests/helpers/aiReplyEventSourceTracker"
import { enableAutoUnmount, flushPromises } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { afterEach, expect, it, vi } from "vitest"
import { confirmTutorExclusionProposal } from "../components/conversation/aiResponseTestSupport"
import {
  memoryTrackerId,
  mockMemoryTrackerPageApis,
} from "./memoryTrackerPageTestSupport"
import { historyFromPrompts } from "./memoryTrackerPageViewTestSupport"

vi.mock("@/managedApi/AiReplyEventSource", async () => {
  const { aiReplyEventSourceTrackingMock } = await import(
    "../components/conversation/aiReplyEventSourceTrackingMock"
  )
  return aiReplyEventSourceTrackingMock()
})

enableAutoUnmount(afterEach)
afterEach(() => {
  resetInstance()
  document.body.innerHTML = ""
})

it("shows the history attempt as excluded and the corrected next recall time once the learner confirms in private help", async () => {
  const reason = "Two choices fit the stem equally well"
  const attempt = makeMe.anAnsweredQuestion.answerCorrect(false).please()
  const excludedAttempt = makeMe.anAnsweredQuestion
    .withId(attempt.id)
    .excludedAsAmbiguous(reason, "2026-10-13T08:00:00Z")
    .please()
  const historyOf = (answered: typeof attempt) =>
    historyFromPrompts([
      makeMe.aRecallPromptHistoryItem
        .withId(answered.id)
        .withMcq(answered.mcq!)
        .withAnswer(answered.answer!)
        .please(),
    ])
  const correctedNextRecallAt = "2026-11-20T09:00:00Z"
  const { getRecallHistorySpy, showMemoryTrackerSpy } =
    mockMemoryTrackerPageApis({
      recallHistory: historyOf(attempt),
      memoryTracker: makeMe.aMemoryTracker
        .nextRecallAt("2026-10-14T09:00:00Z")
        .please(),
    })
  mockSdkService(
    ConversationMessageController,
    "startConversationAboutRecallPrompt",
    makeMe.aConversation.forAnsweredQuestion(attempt).please()
  )
  mockSdkService(ConversationMessageController, "getConversationMessages", [])
  mockSdkService(ConversationMessageController, "markConversationAsRead", [])
  mockSdkService(NoteController, "showNote", makeMe.aNoteRealm.please())
  const wrapper = helper
    .component(MemoryTrackerPage)
    .withProps({ memoryTrackerId })
    .withRouter()
    .withCleanStorage()
    .withCurrentUser(makeMe.aUser.please())
    .mount({ attachTo: document.body })
  await flushPromises()
  await wrapper
    .findAll("button")
    .find((button) => button.text() === "Get private help")!
    .trigger("click")
  await flushPromises()
  getRecallHistorySpy.mockResolvedValue(
    wrapSdkResponse(historyOf(excludedAttempt))
  )
  showMemoryTrackerSpy.mockResolvedValue(
    wrapSdkResponse(
      makeMe.aMemoryTracker.nextRecallAt(correctedNextRecallAt).please()
    )
  )

  await confirmTutorExclusionProposal(excludedAttempt)

  expect(
    wrapper
      .find(
        '[data-testid="recall-history-item"] [data-test="excluded-attempt"]'
      )
      .text()
  ).toContain(reason)
  expect(wrapper.text()).toContain(
    new Date(correctedNextRecallAt).toLocaleString()
  )
  expect(document.querySelector("dialog")!.open).toBe(true)
})
