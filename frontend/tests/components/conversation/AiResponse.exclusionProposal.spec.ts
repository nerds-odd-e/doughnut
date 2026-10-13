import { RecallPromptController } from "@generated/donut-backend-api/sdk.gen"
import type { AmbiguousQuestionExclusionProposal } from "@generated/donut-backend-api"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import { resetInstance } from "@tests/helpers/aiReplyEventSourceTracker"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import {
  createToolCallChunk,
  mountAiResponse,
  submitMessageAndSimulateToolCall,
} from "./aiResponseTestSupport"

vi.mock("@/managedApi/AiReplyEventSource", async () => {
  const { aiReplyEventSourceTrackingMock } = await import(
    "./aiReplyEventSourceTrackingMock"
  )
  return aiReplyEventSourceTrackingMock()
})

describe("AiResponse ambiguous question exclusion proposal", () => {
  const reason = "Two choices fit the stem equally well"
  const answeredQuestion = makeMe.anAnsweredQuestion
    .answerCorrect(false)
    .please()
  let wrapper: ReturnType<typeof mountAiResponse>
  let excludeAsAmbiguousSpy: ReturnType<typeof mockSdkService>

  const buttonLabelled = (label: string) =>
    wrapper.findAll("button").find((button) => button.text() === label)!

  beforeEach(async () => {
    resetInstance()
    excludeAsAmbiguousSpy = mockSdkService(
      RecallPromptController,
      "excludeAsAmbiguous",
      answeredQuestion
    )
    wrapper = mountAiResponse(
      makeMe.aConversation.forAnsweredQuestion(answeredQuestion).please()
    )
    await submitMessageAndSimulateToolCall(
      wrapper,
      createToolCallChunk("AmbiguousQuestionExclusionProposal", <
        AmbiguousQuestionExclusionProposal
      >{ reason })
    )
  })

  afterEach(() => {
    resetInstance()
  })

  it("shows the proposed correction with its reason", () => {
    expect(wrapper.find(".ai-assistant").text()).toContain(
      "Proposed correction: exclude this attempt as an ambiguous question"
    )
    expect(wrapper.find(".exclusion-reason").text()).toBe(reason)
  })

  it("excludes the attempt when the learner confirms", async () => {
    await buttonLabelled("Confirm").trigger("click")
    await flushPromises()

    expect(excludeAsAmbiguousSpy).toHaveBeenCalledWith({
      path: { recallPrompt: answeredQuestion.id },
      body: { reason },
    })
    expect(wrapper.find(".exclusion-reason").exists()).toBe(false)
  })

  it("leaves the attempt as it was when the learner declines", async () => {
    await buttonLabelled("Decline").trigger("click")
    await flushPromises()

    expect(excludeAsAmbiguousSpy).not.toHaveBeenCalled()
    expect(wrapper.find(".exclusion-reason").exists()).toBe(false)
  })
})
