import AiReplyEventSource from "@/managedApi/AiReplyEventSource"
import { messageCenter } from "@/store/messageCenter"
import { useRecallData } from "@/composables/useRecallData"
import type { ConversationMessage } from "@generated/donut-backend-api"
import {
  ConversationMessageController,
  MemoryTrackerController,
  RecallPromptController,
} from "@generated/donut-backend-api/sdk.gen"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
} from "@tests/helpers"
import {
  getLastInstance,
  resetInstance,
} from "@tests/helpers/aiReplyEventSourceTracker"
import { flushPromises } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { afterEach, beforeEach, expect, it, vi } from "vitest"
import { simulateAiResponse } from "../components/conversation/aiResponseTestSupport"
import {
  createMemoryTrackerLite,
  givenRecallQueue,
  useRecallPageSpecContext,
} from "./recallPageTestSupport"

vi.mock("@/managedApi/AiReplyEventSource", async () => {
  const { aiReplyEventSourceTrackingMock } = await import(
    "../components/conversation/aiReplyEventSourceTrackingMock"
  )
  return aiReplyEventSourceTrackingMock()
})

const ctx = useRecallPageSpecContext()
beforeEach(() => {
  resetInstance()
  window.HTMLElement.prototype.scrollIntoView = vi.fn()
  vi.spyOn(AiReplyEventSource.prototype, "start").mockImplementation(vi.fn())
})
afterEach(resetInstance)

it("retains an optional practice exchange and returns to the original answered recall", async () => {
  const user = makeMe.aUser.please()
  const prompt = makeMe.aRecallPrompt
    .withId(73)
    .withQuestionStem("What does correlation establish?")
    .withChoices(["Association", "Causation"])
    .please()
  const answered = makeMe.anAnsweredQuestion
    .withId(prompt.id)
    .withMemoryTrackerId(123)
    .withMcq(
      makeMe.anMcq
        .withQuestionStem(prompt.mcq!.questionStem)
        .withChoices(prompt.mcq!.responseChoices)
        .please()
    )
    .withAnswer({ id: 74, choiceIndex: 1, correct: false })
    .please()
  const conversation = makeMe.aConversation
    .forAnsweredQuestion(answered)
    .please()
  const transcript: ConversationMessage[] = []
  const unrelated = { id: 76, message: "A reply in another discussion." }
  messageCenter.unreadMessages = [unrelated]
  const acknowledge = mockSdkServiceWithImplementation(
    ConversationMessageController,
    "markConversationAsRead",
    () => [unrelated]
  )
  mockSdkService(MemoryTrackerController, "getRecallPrompt", prompt)
  mockSdkService(MemoryTrackerController, "getThresholdExceeded", {
    thresholdExceeded: false,
  })
  const answer = mockSdkService(RecallPromptController, "answer", answered)
  const start = mockSdkService(
    ConversationMessageController,
    "startConversationAboutRecallPrompt",
    conversation
  )
  mockSdkServiceWithImplementation(
    ConversationMessageController,
    "getConversationMessages",
    () => [...transcript]
  )
  const send = mockSdkServiceWithImplementation(
    ConversationMessageController,
    "replyToConversation",
    (options) => {
      const message: ConversationMessage = {
        id: transcript.length + 1,
        message: options.body!,
        sender: user,
      }
      transcript.push(message)
      return message
    }
  )
  ctx.renderer.withCurrentUser(user)
  givenRecallQueue(createMemoryTrackerLite(123), createMemoryTrackerLite(456))
  const wrapper = await ctx.mountPage({ attachTo: document.body })
  await wrapper.findAll("li.choice button")[1]!.trigger("click")
  await flushPromises()
  expect(answer).toHaveBeenCalledWith(
    expect.objectContaining({
      path: { recallPrompt: prompt.id },
      body: expect.objectContaining({ choiceIndex: 1 }),
    })
  )
  const queue = [...useRecallData().toRepeat.value!]
  const progress = wrapper.find(".progress-text").text()
  const entry = wrapper
    .findAll("button")
    .find((button) => button.text() === "Get private help")!
  const open = async () => {
    await entry.trigger("click")
    await flushPromises()
    return document.querySelector<HTMLDialogElement>("dialog")!
  }
  let dialog = await open()
  const button = (text: string) =>
    [...dialog.querySelectorAll<HTMLButtonElement>("button")].find(
      (candidate) => candidate.textContent?.trim() === text
    )!
  button("Please explain the question and answer to me.").click()
  await flushPromises()

  const finishReply = async (message: string) => {
    simulateAiResponse(message)
    await flushPromises()
    const savedReply = { id: transcript.length + 1, message }
    transcript.push(savedReply)
    messageCenter.unreadMessages = [savedReply, unrelated]
    acknowledge.mockClear()
    getLastInstance()!.onMessageCallback("done", "")
    await flushPromises()
    expect(acknowledge).toHaveBeenCalledWith({
      path: { conversationId: conversation.id },
    })
    expect(messageCenter.unreadMessages).toEqual([unrelated])
    expect(
      dialog.querySelectorAll(".ai-assistant").item(transcript.length / 2 - 1)
        .textContent
    ).toContain(message)
  }
  const explanation =
    "Correlation establishes association; a common cause can explain co-movement."
  await finishReply(explanation)
  button("Check this distinction").click()
  await flushPromises()
  const practiceRequest =
    "Give me one different, related practice question. Wait for my answer before explaining it."
  expect(send).toHaveBeenLastCalledWith({
    path: { conversationId: conversation.id },
    body: practiceRequest,
  })
  const question =
    "Umbrella sales and traffic accidents rise together. Does this show umbrellas cause accidents, or could rain affect both?"
  await finishReply(question)
  const response =
    "Rain could affect both; co-movement alone does not establish causation."
  const textarea = dialog.querySelector<HTMLTextAreaElement>("textarea")!
  textarea.value = response
  textarea.dispatchEvent(new Event("input", { bubbles: true }))
  dialog
    .querySelector<HTMLFormElement>("form.message-input-form")!
    .dispatchEvent(new Event("submit", { bubbles: true, cancelable: true }))
  await flushPromises()
  expect(send).toHaveBeenLastCalledWith({
    path: { conversationId: conversation.id },
    body: response,
  })
  const feedback =
    "Yes. Rain is a plausible common cause, so the causal claim needs further evidence."
  await finishReply(feedback)
  dialog
    .querySelector<HTMLButtonElement>('[aria-label="Close dialog"]')!
    .click()
  await flushPromises()
  expect(wrapper.find(".is-selected").text()).toContain("Causation")
  expect(wrapper.find(".progress-text").text()).toBe(progress)
  expect(useRecallData().toRepeat.value).toEqual(queue)
  expect(answer).toHaveBeenCalledTimes(1)
  dialog = await open()
  expect(start).toHaveBeenLastCalledWith(
    expect.objectContaining({ path: { recallPrompt: prompt.id } })
  )
  expect(
    [...dialog.querySelectorAll(".user-message")].map(
      (message) => message.textContent
    )
  ).toEqual([
    "Please explain the question and answer to me.",
    practiceRequest,
    response,
  ])
  expect(
    [...dialog.querySelectorAll(".ai-assistant")].map(
      (message) => message.textContent
    )
  ).toEqual([explanation, question, feedback])
})
