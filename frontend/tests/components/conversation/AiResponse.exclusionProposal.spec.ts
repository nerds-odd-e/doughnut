import { RecallPromptController } from "@generated/donut-backend-api/sdk.gen"
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

  const showProposal = (proposalReason: string | undefined) =>
    submitMessageAndSimulateToolCall(
      wrapper,
      createToolCallChunk("AmbiguousQuestionExclusionProposal", {
        reason: proposalReason,
      })
    )

  beforeEach(() => {
    resetInstance()
    excludeAsAmbiguousSpy = mockSdkService(
      RecallPromptController,
      "excludeAsAmbiguous",
      answeredQuestion
    )
    wrapper = mountAiResponse(
      makeMe.aConversation.forAnsweredQuestion(answeredQuestion).please()
    )
  })

  afterEach(() => {
    resetInstance()
  })

  it("shows the proposed correction with its reason", async () => {
    await showProposal(reason)
    expect(wrapper.find(".ai-assistant").text()).toContain(
      "Proposed correction: exclude this attempt as an ambiguous question"
    )
    expect(wrapper.find(".exclusion-reason").text()).toBe(reason)
  })

  it("excludes the attempt when the learner confirms", async () => {
    await showProposal(reason)
    await buttonLabelled("Confirm").trigger("click")
    await flushPromises()

    expect(excludeAsAmbiguousSpy).toHaveBeenCalledWith({
      path: { recallPrompt: answeredQuestion.id },
      body: { reason },
    })
    expect(wrapper.find(".exclusion-reason").exists()).toBe(false)
  })

  it("leaves the attempt as it was when the learner declines", async () => {
    await showProposal(reason)
    await buttonLabelled("Decline").trigger("click")
    await flushPromises()

    expect(excludeAsAmbiguousSpy).not.toHaveBeenCalled()
    expect(wrapper.find(".exclusion-reason").exists()).toBe(false)
  })

  it.each([undefined, "", "   ", "\t\n"])(
    "offers only decline for a proposal with reason %j",
    async (proposalReason) => {
      await showProposal(proposalReason)

      expect(
        wrapper.findAll("button").some((button) => button.text() === "Confirm")
      ).toBe(false)
      await buttonLabelled("Decline").trigger("click")
      await flushPromises()

      expect(wrapper.find(".exclusion-reason").exists()).toBe(false)
      expect(excludeAsAmbiguousSpy).not.toHaveBeenCalled()
    }
  )
})
