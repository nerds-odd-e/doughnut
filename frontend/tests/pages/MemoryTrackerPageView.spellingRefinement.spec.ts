import type { ConversationMessage } from "@generated/donut-backend-api"
import {
  AiController,
  ConversationMessageController,
  NoteController,
} from "@generated/donut-backend-api/sdk.gen"
import MemoryTrackerPageView from "@/pages/MemoryTrackerPageView.vue"
import { teardownGlobalClientForTesting } from "@/managedApi/clientSetup"
import { useNoteStore } from "@/store/noteStore"
import { fireEvent, screen } from "@testing-library/vue"
import { enableAutoUnmount, flushPromises } from "@vue/test-utils"
import helper, {
  mockSdkService,
  mockSdkServiceWithImplementation,
} from "@tests/helpers"
import makeMe from "donut-test-fixtures/makeMe"
import { afterEach, expect, it } from "vitest"
import { historyFromPrompts } from "./memoryTrackerPageViewTestSupport"

enableAutoUnmount(afterEach)
afterEach(() => {
  teardownGlobalClientForTesting()
  document.body.innerHTML = ""
})

it("continues spelling history help into ordinary authorized note refinement", async () => {
  const learner = makeMe.aUser.please()
  const noteRealm = makeMe.aNoteRealm
    .title("Sedition")
    .content("Conduct or speech inciting rebellion against authority.")
    .please()
  const spellingAnswer = {
    id: 83,
    spellingAnswer: "Seddition",
    correct: false,
  }
  const historyPrompt = makeMe.aRecallPromptHistoryItem
    .withId(82)
    .spelling()
    .withAnswer(spellingAnswer)
    .withAnswerTime("2026-10-12T00:00:00Z")
    .please()
  const answered = makeMe.anAnsweredQuestion
    .withId(82)
    .withNote(noteRealm.note)
    .spelling()
    .withAnswer(spellingAnswer)
    .please()
  const conversation = makeMe.aConversation
    .forAnsweredQuestion(answered)
    .please()
  const history: ConversationMessage[] = []
  mockSdkService(NoteController, "showNote", noteRealm)
  const start = mockSdkService(
    ConversationMessageController,
    "startConversationAboutRecallPrompt",
    conversation
  )
  mockSdkServiceWithImplementation(
    ConversationMessageController,
    "getConversationMessages",
    () => [...history]
  )
  mockSdkService(ConversationMessageController, "markConversationAsRead", [])
  const send = mockSdkServiceWithImplementation(
    ConversationMessageController,
    "replyToConversation",
    (options) => {
      const message: ConversationMessage = {
        id: 84,
        sender: learner,
        message: options.body!,
      }
      history.push(message)
      return message
    }
  )
  const generate = mockSdkService(
    AiController,
    "generateRefinementSuggestions",
    {
      items: [],
    }
  )
  const renderer = helper
    .component(MemoryTrackerPageView)
    .withCleanStorage()
    .withCurrentUser(learner)
    .withRouter()
    .withProps({
      memoryTracker: makeMe.aMemoryTracker.please(),
      memoryTrackerId: 81,
      recallHistory: historyFromPrompts([historyPrompt]),
    })
  useNoteStore().refreshNoteRealm(noteRealm)
  renderer.render()
  await flushPromises()
  expect(screen.getByText("Seddition")).toBeDefined()
  await fireEvent.click(screen.getByText("Get private help"))
  await flushPromises()
  expect(start).toHaveBeenCalledWith({ path: { recallPrompt: answered.id } })

  const help = document.querySelector<HTMLDialogElement>("dialog.modal-mask")!
  const message = "Why was my spelling marked incorrect?"
  await fireEvent.update(
    help.querySelector<HTMLTextAreaElement>("textarea")!,
    message
  )
  await fireEvent.submit(
    help.querySelector<HTMLFormElement>("form.message-input-form")!
  )
  await flushPromises()
  expect(send).toHaveBeenCalledWith({
    path: { conversationId: conversation.id },
    body: message,
  })
  expect(help.querySelector(".user-message")?.textContent).toBe(message)

  await fireEvent.click(screen.getByText("Refine note"))
  await flushPromises()
  expect(generate).toHaveBeenCalledWith({
    path: { note: noteRealm.note.id },
    signal: expect.any(AbortSignal),
  })
  expect(screen.getByText("Refinement layout:")).toBeDefined()
})
