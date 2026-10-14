import AiReplyEventSource from "@/managedApi/AiReplyEventSource"
import ConversationInner from "@/components/conversations/ConversationInner.vue"
import { ConversationMessageController } from "@generated/donut-backend-api/sdk.gen"
import helper, { mockSdkService, mockShowNote } from "@tests/helpers"
import { resetInstance } from "@tests/helpers/aiReplyEventSourceTracker"
import makeMe from "donut-test-fixtures/makeMe"
import { vi } from "vitest"

export const user = makeMe.aUser.please()
export const realm = makeMe.aNoteRealm
  .title("Causation")
  .content("**Current distinction**")
  .please()
export const exchange = {
  id: 73,
  message: "An example explains this distinction.",
}
export const noteConversation = makeMe.aConversation
  .forANote(realm.note)
  .please()
export const recallConversation = makeMe.aConversation
  .forAnsweredQuestion(makeMe.anAnsweredQuestion.withNote(realm.note).please())
  .please()
const correctionBuilder = () =>
  makeMe.aConversationNoteCorrection
    .forNote(realm.note)
    .fromMessage(exchange.id)
    .content("**Proposed distinction**")
    .reason("Add a common-cause example.")
export const body = correctionBuilder().please()
export const declinedCorrection = () =>
  correctionBuilder()
    .id(body.id!)
    .declinedBy(user, "2026-10-14T01:00:00Z")
    .please()

export const setupCorrections = () => {
  resetInstance()
  vi.spyOn(AiReplyEventSource.prototype, "start").mockImplementation(vi.fn())
  window.HTMLElement.prototype.scrollIntoView = vi.fn()
  mockShowNote(realm)
  mockSdkService(ConversationMessageController, "getConversationMessages", [
    exchange,
  ])
  mockSdkService(ConversationMessageController, "getConversationCorrections", [
    body,
  ])
  mockSdkService(ConversationMessageController, "markConversationAsRead", [])
}

export const mountConversation = (conversation = noteConversation) =>
  helper
    .component(ConversationInner)
    .withCleanStorage()
    .withCurrentUser(user)
    .withProps({ conversation, user })
    .mount()
