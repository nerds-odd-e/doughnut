import AiResponse from "@/components/conversations/AiResponse.vue"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import {
  getLastInstance,
  resetInstance,
} from "@tests/helpers/aiReplyEventSourceTracker"
import { flushPromises } from "@vue/test-utils"
import { vi } from "vitest"
import AiReplyEventSource from "@/managedApi/AiReplyEventSource"
import {
  ConversationMessageController,
  RecallPromptController,
} from "@generated/donut-backend-api/sdk.gen"
import type {
  AmbiguousQuestionExclusionProposal,
  AnsweredQuestion,
  Conversation,
} from "@generated/donut-backend-api"

export const simulateAiResponse = (content = "## I'm ChatGPT") => {
  const instance = getLastInstance()
  if (!instance) {
    throw new Error("No AiReplyEventSource instance available")
  }
  const chunk = {
    choices: [
      {
        index: 0,
        message: {
          role: "assistant",
          content,
        },
        finish_reason: null,
      },
    ],
  }

  instance.onMessageCallback("chat.completion.chunk", JSON.stringify(chunk))
}

export const createToolCallChunk = (functionName: string, args: object) => {
  const argumentsString = JSON.stringify(args)
  return [
    {
      choices: [
        {
          index: 0,
          delta: {
            role: "assistant",
            tool_calls: [
              {
                index: 0,
                id: "call-456",
                type: "function",
                function: {
                  name: functionName,
                  arguments: argumentsString.substring(
                    0,
                    Math.min(20, argumentsString.length)
                  ),
                },
              },
            ],
          },
          finish_reason: null,
        },
      ],
    },
    ...(argumentsString.length > 20
      ? [
          {
            choices: [
              {
                index: 0,
                delta: {
                  tool_calls: [
                    {
                      index: 0,
                      function: {
                        arguments: argumentsString.substring(20),
                      },
                    },
                  ],
                },
                finish_reason: null,
              },
            ],
          },
        ]
      : []),
    {
      choices: [
        {
          index: 0,
          delta: {},
          finish_reason: "tool_calls",
        },
      ],
    },
  ]
}

export const mountAiResponse = (conversation: Conversation) =>
  helper
    .component(AiResponse)
    .withProps({ conversation, aiReplyTrigger: 0 })
    .mount()

export const submitAiReply = async (
  wrapper: ReturnType<typeof mountAiResponse>
) => {
  const current = Number(wrapper.props("aiReplyTrigger") ?? 0)
  await wrapper.setProps({ aiReplyTrigger: current + 1 })
  await flushPromises()
}

const simulateToolCall = async (chunks: object[]) => {
  const instance = getLastInstance()
  if (!instance) {
    throw new Error("No AiReplyEventSource instance available")
  }
  for (const chunk of chunks) {
    instance.onMessageCallback("chat.completion.chunk", JSON.stringify(chunk))
    await flushPromises()
  }
}

export const submitMessageAndSimulateToolCall = async (
  wrapper: ReturnType<typeof mountAiResponse>,
  toolCallChunks: ReturnType<typeof createToolCallChunk> | object
) => {
  await submitAiReply(wrapper)
  await simulateToolCall(
    Array.isArray(toolCallChunks) ? toolCallChunks : [toolCallChunks]
  )
}

/**
 * In the open recall help conversation attached to the document: the learner
 * asks, the tutor proposes the exclusion, and the learner confirms it. The
 * confirmation answers with `excludedAttempt`.
 */
export const confirmTutorExclusionProposal = async (
  excludedAttempt: AnsweredQuestion
) => {
  const clickButton = async (label: string) => {
    ;[...document.querySelectorAll("button")]
      .find((button) => button.textContent?.trim() === label)!
      .click()
    await flushPromises()
  }
  mockSdkService(
    ConversationMessageController,
    "replyToConversation",
    undefined
  )
  mockSdkService(RecallPromptController, "excludeAsAmbiguous", excludedAttempt)
  vi.spyOn(AiReplyEventSource.prototype, "start").mockImplementation(vi.fn())

  await clickButton("Why is my answer wrong?")
  await simulateToolCall(
    createToolCallChunk("AmbiguousQuestionExclusionProposal", <
      AmbiguousQuestionExclusionProposal
    >{ reason: excludedAttempt.answer!.exclusionReason })
  )
  await clickButton("Confirm")
}

export function useAiResponseMount() {
  let wrapper: ReturnType<typeof mountAiResponse>

  const beforeEachMount = () => {
    resetInstance()
    wrapper = mountAiResponse(makeMe.aConversation.please())
  }

  const afterEachReset = () => {
    resetInstance()
  }

  return {
    get wrapper() {
      return wrapper
    },
    beforeEachMount,
    afterEachReset,
  }
}
