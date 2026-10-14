import {
  ConversationMessageController,
  NoteController,
} from "@generated/donut-backend-api/sdk.gen"
import ConversationComponent from "@/components/conversations/ConversationComponent.vue"
import helper, { mockSdkService } from "@tests/helpers"
import { resetInstance } from "@tests/helpers/aiReplyEventSourceTracker"
import { enableAutoUnmount, flushPromises } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { afterEach, expect, it, vi } from "vitest"
import { confirmTutorExclusionProposal } from "./aiResponseTestSupport"

vi.mock("@/managedApi/AiReplyEventSource", async () => {
  const { aiReplyEventSourceTrackingMock } = await import(
    "./aiReplyEventSourceTrackingMock"
  )
  return aiReplyEventSourceTrackingMock()
})

enableAutoUnmount(afterEach)
afterEach(() => {
  resetInstance()
  document.body.innerHTML = ""
})

it("shows the subject attempt as excluded once the learner confirms the tutor's proposal", async () => {
  const attempt = makeMe.anAnsweredQuestion.answerCorrect(false).please()
  const reason = "Two choices fit the stem equally well"
  mockSdkService(ConversationMessageController, "getConversationMessages", [])
  mockSdkService(ConversationMessageController, "markConversationAsRead", [])
  mockSdkService(NoteController, "showNote", makeMe.aNoteRealm.please())
  const wrapper = helper
    .component(ConversationComponent)
    .withCleanStorage()
    .withRouter()
    .withCurrentUser(makeMe.aUser.please())
    .withProps({
      conversation: makeMe.aConversation.forAnsweredQuestion(attempt).please(),
      user: makeMe.aUser.please(),
    })
    .mount({ attachTo: document.body })
  await flushPromises()
  const subject = wrapper.find(".subject-container")
  expect(subject.find('[data-test="excluded-attempt"]').exists()).toBe(false)

  await confirmTutorExclusionProposal(
    makeMe.anAnsweredQuestion
      .withId(attempt.id)
      .excludedAsAmbiguous(reason, "2026-10-13T08:00:00Z")
      .please()
  )

  const excluded = subject.find('[data-test="excluded-attempt"]')
  expect(excluded.text()).toContain("Excluded — ambiguous question")
  expect(excluded.text()).toContain(reason)
})

beforeEach(() => {
  mockSdkService(
    ConversationMessageController,
    "getConversationCorrections",
    []
  )
})
