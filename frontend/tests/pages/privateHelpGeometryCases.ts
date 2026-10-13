import {
  ConversationMessageController,
  MemoryTrackerController,
  RecallPromptController,
} from "@generated/donut-backend-api/sdk.gen"
import { useRecallData } from "@/composables/useRecallData"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { expect, it, onTestFinished } from "vitest"
import { page } from "vitest/browser"
import {
  confirmTutorExclusionProposal,
  simulateAiResponse,
} from "../components/conversation/aiResponseTestSupport"
import {
  createMemoryTrackerLite,
  givenRecallQueue,
  type useRecallPageSpecContext,
} from "./recallPageTestSupport"
import {
  expectPrivateHelpControlsInView,
  readAttemptContextEndsInView,
  readContentEndsInView,
  settleScrolling,
} from "./privateHelpGeometryTestSupport"

export function privateHelpGeometryCases(
  ctx: ReturnType<typeof useRecallPageSpecContext>
) {
  it.each([
    { width: 1728, height: 963 },
    { width: 390, height: 600 },
  ])(
    "keeps private help identifiable, dismissible, and fully readable at $width×$height",
    async ({ width, height }) => {
      const original = { width: window.innerWidth, height: window.innerHeight }
      onTestFinished(() => page.viewport(original.width, original.height))
      await page.viewport(width, height)
      const prompt = makeMe.aRecallPrompt
        .withId(73)
        .withQuestionStem("What does correlation establish?")
        .withChoices(["Association", "Causation"])
        .please()
      const answered = makeMe.anAnsweredQuestion
        .withId(prompt.id)
        .withMemoryTrackerId(123)
        .withMcq(prompt.mcq!)
        .withAnswer({ id: 74, choiceIndex: 1, correct: false })
        .please()
      const reason = "Two choices fit the stem equally well. ".repeat(150)
      const excluded = makeMe.anAnsweredQuestion
        .withId(answered.id)
        .withMemoryTrackerId(123)
        .withMcq(prompt.mcq!)
        .withAnswer(answered.answer!)
        .excludedAsAmbiguous(reason, "2026-10-13T08:00:00Z")
        .please()
      mockSdkService(MemoryTrackerController, "getRecallPrompt", prompt)
      mockSdkService(MemoryTrackerController, "getThresholdExceeded", {
        thresholdExceeded: false,
      })
      mockSdkService(RecallPromptController, "answer", answered)
      const conversation = makeMe.aConversation
        .forAnsweredQuestion(answered)
        .please()
      const start = mockSdkService(
        ConversationMessageController,
        "startConversationAboutRecallPrompt",
        conversation
      )
      const messages = mockSdkService(
        ConversationMessageController,
        "getConversationMessages",
        []
      )
      mockSdkService(
        ConversationMessageController,
        "markConversationAsRead",
        []
      )
      ctx.renderer.withCurrentUser(makeMe.aUser.please())
      givenRecallQueue(
        createMemoryTrackerLite(123),
        createMemoryTrackerLite(456)
      )
      const wrapper = await ctx.mountPage({ attachTo: document.body })
      await wrapper.findAll("li.choice button")[1]!.trigger("click")
      await flushPromises()
      const progress = wrapper.find(".progress-text").text()
      const queue = [...useRecallData().toRepeat.value!]
      const entry = wrapper
        .findAll("button")
        .find((button) => button.text() === "Get private help")!
      await entry.trigger("click")
      await flushPromises()
      let dialog = document.querySelector<HTMLDialogElement>("dialog")!
      await settleScrolling(dialog)
      expectPrivateHelpControlsInView(dialog)

      await confirmTutorExclusionProposal(excluded)
      await settleScrolling(dialog)
      expectPrivateHelpControlsInView(dialog)
      simulateAiResponse("A long explanation. ".repeat(100))
      await settleScrolling(dialog)
      expectPrivateHelpControlsInView(dialog)
      await readContentEndsInView(
        dialog,
        dialog.querySelector<HTMLElement>(".ai-assistant p")!
      )
      await readAttemptContextEndsInView(dialog)
      expectPrivateHelpControlsInView(dialog).click()
      await flushPromises()
      expect(document.activeElement).toBe(entry.element)
      expect(wrapper.find(".progress-text").text()).toBe(progress)
      expect(useRecallData().toRepeat.value).toEqual(queue)
      expect(wrapper.find(".is-selected").text()).toContain("Causation")

      const exchanges = [
        "First exchange. ",
        "Second exchange. ",
        "Last exchange. ",
      ].map((text, index) => ({ id: 80 + index, message: text.repeat(60) }))
      messages.mockResolvedValue(wrapSdkResponse(exchanges))
      start.mockResolvedValue(
        wrapSdkResponse(
          makeMe.aConversation
            .withId(conversation.id)
            .forAnsweredQuestion(excluded)
            .please()
        )
      )
      await entry.trigger("click")
      await flushPromises()
      dialog = document.querySelector<HTMLDialogElement>("dialog")!
      await settleScrolling(dialog)
      expectPrivateHelpControlsInView(dialog)
      const replies = [
        ...dialog.querySelectorAll<HTMLElement>(".ai-assistant p"),
      ]
      expect(replies).toHaveLength(3)
      await readContentEndsInView(dialog, replies.at(-1)!)
      await readContentEndsInView(dialog, replies[0]!)
      await readAttemptContextEndsInView(dialog)
      expectPrivateHelpControlsInView(dialog).click()
      await flushPromises()
      expect(document.activeElement).toBe(entry.element)
    }
  )
}
