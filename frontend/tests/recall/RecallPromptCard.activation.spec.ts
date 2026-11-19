import { RecallPromptController } from "@generated/donut-backend-api/sdk.gen"
import { mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { describe, expect, it } from "vitest"
import {
  contentLoaderVisible,
  contestableQuestionVisible,
  createDeferredGate,
  createMemoryTrackerLite,
  getRecallPrompt,
  getRecallPromptSpy,
  mountRecallPromptCardReady,
  setupRecallPromptCardTests,
  wrapSdkResponse,
} from "./recallPromptCardTestSupport"

describe("a prefetched multiple-choice card", () => {
  setupRecallPromptCardTests()

  const openingTimePrompt = (id: number, time: string) =>
    makeMe.aRecallPrompt
      .withId(id)
      .withQuestionStem(`Which door opens at ${time}?`)
      .withChoices(["North entrance", `Entrance marked ${time}`])
      .please()

  async function startPrefetch() {
    const warm = createDeferredGate()
    const select = createDeferredGate()
    const server = { available: openingTimePrompt(42, "nine") }
    getRecallPromptSpy
      .mockImplementation(async () => wrapSdkResponse(server.available))
      .mockResolvedValueOnce(wrapSdkResponse(getRecallPrompt()))
      .mockImplementationOnce(async () => {
        const prepared = server.available
        await warm.gate
        return wrapSdkResponse(prepared)
      })
      .mockImplementationOnce(async () => {
        await select.gate
        return wrapSdkResponse(server.available)
      })

    const card = await mountRecallPromptCardReady([1, 2, 3], 2)
    expect(getRecallPromptSpy).toHaveBeenCalledWith({
      path: { memoryTracker: 2 },
    })

    async function reachCorrectedCard() {
      server.available = openingTimePrompt(43, "ten")
      await card.setProps({ currentIndex: 1 })
      expect(contentLoaderVisible(card)).toBe(true)
      expect(contestableQuestionVisible(card)).toBe(false)
      expect(getRecallPromptSpy).toHaveBeenLastCalledWith({
        path: { memoryTracker: 2 },
      })
      select.resolve()
      await flushPromises()
      expect(card.text()).toContain("Which door opens at ten?")
    }

    return { card, server, warm, reachCorrectedCard }
  }

  it("selects the current question after prefetch completes and continues lookahead", async () => {
    const { warm, reachCorrectedCard } = await startPrefetch()
    warm.resolve()
    await flushPromises()
    await reachCorrectedCard()
    expect(getRecallPromptSpy).toHaveBeenCalledWith({
      path: { memoryTracker: 3 },
    })
  })

  it("answers the displayed prompt after a later edit and a late background response", async () => {
    const answer = mockSdkService(
      RecallPromptController,
      "answer",
      makeMe.anAnsweredQuestion.please()
    )
    const { card, server, warm, reachCorrectedCard } = await startPrefetch()
    await reachCorrectedCard()
    server.available = openingTimePrompt(44, "eleven")
    await card.setProps({
      memoryTrackers: [1, 2, 3].map((id) => createMemoryTrackerLite(id)),
    })
    warm.resolve()
    await flushPromises()

    const choices = card.findAll("li.choice button")
    expect(choices[1]!.text()).toContain("Entrance marked ten")
    await choices[1]!.trigger("click")
    await flushPromises()
    expect(answer).toHaveBeenCalledWith({
      path: { recallPrompt: 43 },
      body: expect.objectContaining({ choiceIndex: 1 }),
    })
  })
})
