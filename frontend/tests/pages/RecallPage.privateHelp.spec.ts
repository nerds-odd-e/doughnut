import {
  ConversationMessageController,
  MemoryTrackerController,
  NoteController,
  RecallPromptController,
} from "@generated/donut-backend-api/sdk.gen"
import { messageCenter } from "@/store/messageCenter"
import { useRecallData } from "@/composables/useRecallData"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { expect, it, vi } from "vitest"
import { confirmTutorExclusionProposal } from "../components/conversation/aiResponseTestSupport"
import { privateHelpGeometryCases } from "./privateHelpGeometryCases"
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

privateHelpGeometryCases(ctx)

it("shows the answered attempt as excluded once the learner confirms the tutor's proposal in private help", async () => {
  const reason = "Two choices fit the stem equally well"
  const prompt = makeMe.aRecallPrompt
    .withQuestionStem("What does correlation establish?")
    .withChoices(["Association", "Causation"])
    .please()
  const answered = makeMe.anAnsweredQuestion
    .withId(prompt.id)
    .answerCorrect(false)
    .please()
  mockSdkService(MemoryTrackerController, "getRecallPrompt", prompt)
  mockSdkService(MemoryTrackerController, "getThresholdExceeded", {
    thresholdExceeded: false,
  })
  mockSdkService(RecallPromptController, "answer", answered)
  mockSdkService(
    ConversationMessageController,
    "startConversationAboutRecallPrompt",
    makeMe.aConversation.forAnsweredQuestion(answered).please()
  )
  mockSdkService(ConversationMessageController, "getConversationMessages", [])
  mockSdkService(ConversationMessageController, "markConversationAsRead", [])
  ctx.renderer.withCurrentUser(makeMe.aUser.please())
  givenRecallQueue(createMemoryTrackerLite(123), createMemoryTrackerLite(456))
  const wrapper = await ctx.mountPage({ attachTo: document.body })
  await wrapper.findAll("li.choice button")[1]!.trigger("click")
  await flushPromises()
  await wrapper
    .findAll("button")
    .find((button) => button.text() === "Get private help")!
    .trigger("click")
  await flushPromises()

  await confirmTutorExclusionProposal(
    makeMe.anAnsweredQuestion
      .withId(answered.id)
      .excludedAsAmbiguous(reason, "2026-10-13T08:00:00Z")
      .please()
  )

  expect(wrapper.find('[data-test="excluded-attempt"]').text()).toContain(
    reason
  )
  expect(document.querySelector("dialog")!.open).toBe(true)
})

it.each(["conversation close", "Escape", "nested refinement then Escape"])(
  "returns from private help through %s to the actual answered attempt and recall queue",
  async (closeMethod) => {
    const prompt = makeMe.aRecallPrompt
      .withId(73)
      .withQuestionStem("What does correlation establish?")
      .withChoices(["Association", "Causation"])
      .please()
    const noteRealm = makeMe.aNoteRealm
      .content("Correlation expresses association.")
      .please()
    mockSdkService(NoteController, "showNote", noteRealm)
    const answered = makeMe.anAnsweredQuestion
      .withNote(noteRealm.note)
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
    const reply = { id: 75, message: "Association alone does not prove cause." }
    const unrelated = { id: 76, message: "A reply in another discussion." }
    messageCenter.unreadMessages = [reply, unrelated]
    mockSdkService(ConversationMessageController, "getConversationMessages", [
      reply,
    ])
    const markRead = mockSdkService(
      ConversationMessageController,
      "markConversationAsRead",
      [unrelated]
    )
    ctx.renderer.withCurrentUser(makeMe.aUser.please())
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
    const queueBeforeHelp = [...useRecallData().toRepeat.value!]
    const progressBeforeHelp = wrapper.find(".progress-text").text()
    const entry = wrapper
      .findAll("button")
      .find((button) => button.text() === "Get private help")!
    await entry.trigger("click")
    await flushPromises()

    expect(start).toHaveBeenCalledWith(
      expect.objectContaining({ path: { recallPrompt: prompt.id } })
    )
    const dialog = document.querySelector<HTMLDialogElement>("dialog")!
    expect(dialog.textContent).toContain(reply.message)
    expect(markRead).toHaveBeenCalledWith({
      path: { conversationId: conversation.id },
    })
    expect(messageCenter.unreadMessages).toEqual([unrelated])
    expect(dialog.open).toBe(true)
    expect(dialog.textContent).toContain("Private recall help")
    expect(dialog.textContent).toContain(prompt.mcq!.questionStem)
    expect(dialog.querySelector(".is-selected")?.textContent).toContain(
      "Causation"
    )
    if (closeMethod === "nested refinement then Escape") {
      dialog
        .querySelector<HTMLButtonElement>(
          '[data-test="open-refine-note-modal"]'
        )!
        .click()
      await flushPromises()
      const refinement = document
        .querySelector('[data-test="refine-note-modal"]')!
        .closest("dialog")!
      expect(refinement.open).toBe(true)
      document.dispatchEvent(
        new KeyboardEvent("keydown", { key: "Escape", bubbles: true })
      )
      await flushPromises()
      expect(refinement.isConnected).toBe(false)
      expect(dialog.open).toBe(true)
      expect(dialog.textContent).toContain(reply.message)
      expect(dialog.querySelector(".is-selected")?.textContent).toContain(
        "Causation"
      )
    }
    if (
      closeMethod === "Escape" ||
      closeMethod === "nested refinement then Escape"
    ) {
      document.dispatchEvent(
        new KeyboardEvent("keydown", { key: "Escape", bubbles: true })
      )
    } else {
      dialog
        .querySelector<HTMLButtonElement>('[aria-label="Close dialog"]')!
        .click()
    }
    await flushPromises()

    expect(wrapper.find(".is-selected").text()).toContain("Causation")
    expect(wrapper.find(".progress-text").text()).toBe(progressBeforeHelp)
    expect(useRecallData().toRepeat.value).toEqual(queueBeforeHelp)
    expect(wrapper.vm.$router.currentRoute.value.name).toBe("recall")
    expect(document.activeElement).toBe(entry.element)
  }
)

beforeEach(() => {
  mockSdkService(
    ConversationMessageController,
    "getConversationCorrections",
    []
  )
})
