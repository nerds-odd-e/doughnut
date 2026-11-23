import { RecallPromptController } from "@generated/donut-backend-api/sdk.gen"
import { mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { describe, expect, it } from "vitest"
import { recallPromptRequestsFor } from "@tests/helpers/recallDataTestSupport"
import {
  contentLoaderVisible,
  createDeferredGate,
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
    const prefetch = createDeferredGate()
    const server = { available: openingTimePrompt(42, "nine") }
    getRecallPromptSpy
      .mockImplementation(async () => wrapSdkResponse(server.available))
      .mockResolvedValueOnce(wrapSdkResponse(openingTimePrompt(41, "eight")))
      .mockImplementationOnce(async () => {
        const prefetched = server.available
        await prefetch.gate
        return wrapSdkResponse(prefetched)
      })
    const card = await mountRecallPromptCardReady([1, 2, 3], 2)
    expect(recallPromptRequestsFor(getRecallPromptSpy, 2)).toHaveLength(1)
    return { card, server, prefetch }
  }

  it("shows a completed prefetch immediately and continues lookahead", async () => {
    const { card, prefetch } = await startPrefetch()
    prefetch.resolve()
    await flushPromises()
    await card.setProps({ currentIndex: 1 })
    expect(contentLoaderVisible(card)).toBe(false)
    expect(card.text()).toContain("Which door opens at nine?")
    expect(recallPromptRequestsFor(getRecallPromptSpy, 2)).toHaveLength(1)
    await flushPromises()
    expect(recallPromptRequestsFor(getRecallPromptSpy, 3)).toHaveLength(1)
  })

  it("shows the loader for an in-flight prefetch, then that request's result", async () => {
    const { card, prefetch } = await startPrefetch()
    await card.setProps({ currentIndex: 1 })
    expect(contentLoaderVisible(card)).toBe(true)
    prefetch.resolve()
    await flushPromises()
    expect(card.text()).toContain("Which door opens at nine?")
    expect(recallPromptRequestsFor(getRecallPromptSpy, 2)).toHaveLength(1)
  })

  it("answers the prefetched prompt after the server prompt changes", async () => {
    const answer = mockSdkService(
      RecallPromptController,
      "answer",
      makeMe.anAnsweredQuestion.please()
    )
    const { card, server, prefetch } = await startPrefetch()
    prefetch.resolve()
    await flushPromises()
    server.available = openingTimePrompt(43, "ten")
    await card.setProps({ currentIndex: 1 })
    await flushPromises()

    const choices = card.findAll("li.choice button")
    expect(choices[1]!.text()).toContain("Entrance marked nine")
    await choices[1]!.trigger("click")
    await flushPromises()
    expect(answer).toHaveBeenCalledWith({
      path: { recallPrompt: 42 },
      body: expect.objectContaining({ choiceIndex: 1 }),
    })
  })
})
