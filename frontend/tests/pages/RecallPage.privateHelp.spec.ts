import {
  ConversationMessageController,
  MemoryTrackerController,
  RecallPromptController,
} from "@generated/donut-backend-api/sdk.gen"
import { useRecallData } from "@/composables/useRecallData"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { expect, it } from "vitest"
import {
  createMemoryTrackerLite,
  givenRecallQueue,
  useRecallPageSpecContext,
} from "./recallPageTestSupport"

const ctx = useRecallPageSpecContext()

it.each(["conversation close", "Escape"])(
  "returns from private help through %s to the actual answered attempt and recall queue",
  async (closeMethod) => {
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
    mockSdkService(ConversationMessageController, "getConversationMessages", [])
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
    expect(dialog.open).toBe(true)
    expect(dialog.textContent).toContain("Private recall help")
    expect(dialog.textContent).toContain(prompt.mcq!.questionStem)
    expect(dialog.querySelector(".is-selected")?.textContent).toContain(
      "Causation"
    )
    if (closeMethod === "Escape") {
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
