import {
  ConversationMessageController,
  NoteController,
} from "@generated/donut-backend-api/sdk.gen"
import type { AnsweredQuestion } from "@generated/donut-backend-api"
import MemoryTrackerPageView from "@/pages/MemoryTrackerPageView.vue"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { enableAutoUnmount, flushPromises } from "@vue/test-utils"
import { afterEach, expect, it } from "vitest"
import { historyFromPrompts } from "./memoryTrackerPageViewTestSupport"

enableAutoUnmount(afterEach)
afterEach(() => {
  document.body.innerHTML = ""
})

it("opens each history attempt in its own contextual private help and restores its entry focus", async () => {
  const attempts = ["Association question", "Evidence question"].map(
    (stem, index) =>
      makeMe.anAnsweredQuestion
        .withId(73 + index)
        .withMcq(
          makeMe.anMcq
            .withQuestionStem(stem)
            .withChoices(["Association", "Causation"])
            .please()
        )
        .withAnswer({ id: 80 + index, choiceIndex: 1, correct: false })
        .please()
  )
  const history = attempts.map((attempt) =>
    makeMe.aRecallPromptHistoryItem
      .withId(attempt.id)
      .withMcq(attempt.mcq!)
      .withAnswer(attempt.answer!)
      .please()
  )
  const conversationFor = (attempt: AnsweredQuestion) =>
    makeMe.aConversation.forAnsweredQuestion(attempt).please()
  const start = mockSdkService(
    ConversationMessageController,
    "startConversationAboutRecallPrompt",
    conversationFor(attempts[0]!)
  )
  mockSdkService(ConversationMessageController, "getConversationMessages", [])
  mockSdkService(NoteController, "showNote", makeMe.aNoteRealm.please())
  const wrapper = helper
    .component(MemoryTrackerPageView)
    .withRouter()
    .withCleanStorage()
    .withCurrentUser(makeMe.aUser.please())
    .withProps({
      memoryTrackerId: 1,
      memoryTracker: makeMe.aMemoryTracker.please(),
      recallHistory: historyFromPrompts(history),
    })
    .mount({ attachTo: document.body })
  await flushPromises()
  const cards = wrapper.findAll('[data-testid="recall-history-item"]')
  expect(cards).toHaveLength(2)

  for (const [index, attempt] of attempts.entries()) {
    start.mockResolvedValueOnce(wrapSdkResponse(conversationFor(attempt)))
    const entry = cards[index]!.findAll("button").find(
      (button) => button.text() === "Get private help"
    )!
    await entry.trigger("click")
    await flushPromises()
    expect(start).toHaveBeenLastCalledWith(
      expect.objectContaining({ path: { recallPrompt: attempt.id } })
    )
    const dialog = document.querySelector<HTMLDialogElement>("dialog")!
    expect(dialog.textContent).toContain(attempt.mcq!.questionStem)
    expect(dialog.querySelector(".is-selected")?.textContent).toContain(
      attempt.mcq!.responseChoices[1]
    )
    dialog
      .querySelector<HTMLButtonElement>('[aria-label="Close dialog"]')!
      .click()
    await flushPromises()
    expect(document.activeElement).toBe(entry.element)
    expect(cards[index]!.element.isConnected).toBe(true)
    expect(cards[index]!.text()).toContain(attempt.mcq!.questionStem)
  }
})
