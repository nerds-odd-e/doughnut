import ServiceMocker from "../../support/ServiceMocker"
import { MessageToMatch } from "./MessageToMatch"

const openAiAssistantCreatedRunMocker = (
  serviceMocker: ServiceMocker,
  threadId: string,
  runId: string,
) => {
  return {
    async stubRetrieveRunsThatCompleted() {
      const responses = [
        {
          id: runId,
          status: "completed",
        },
      ]
      await serviceMocker.stubGetterWithMutipleResponses(
        `/threads/${threadId}/runs/${runId}`,
        {},
        responses,
      )
      return this
    },
    async stubRetrieveRunsThatRequireAction(hashes: Record<string, string>[]) {
      const createRequiresActionRun = (
        functionName: string,
        argumentsObj: unknown,
      ) => {
        return {
          id: runId,
          status: "requires_action",
          required_action: {
            type: "submit_tool_outputs",
            submit_tool_outputs: {
              tool_calls: [
                {
                  type: "function",
                  function: {
                    name: functionName,
                    arguments: JSON.stringify(argumentsObj),
                  },
                },
              ],
            },
          },
        }
      }

      const responses = hashes.map((hash) => {
        switch (hash["response"]) {
          case "ask clarification question":
            return createRequiresActionRun("ask_clarification_question", {
              question: hash["arguments"],
            })
          case "complete note details":
            return createRequiresActionRun("complete_note_details", {
              completion: hash["arguments"]?.match(/"(.*)"/)?.[1],
            })
          default:
            throw new Error(`Unknown response: ${hash["response"]}`)
        }
      })

      await serviceMocker.stubGetterWithMutipleResponses(
        `/threads/${threadId}/runs/${runId}`,
        {},
        responses,
      )
      return openAiAssistantCreatedRunMocker(serviceMocker, threadId, runId)
    },

    async stubSubmitToolOutputs() {
      await serviceMocker.stubPoster(
        `/threads/${threadId}/runs/${runId}/submit_tool_outputs`,
        {
          id: runId,
          status: "queued",
        },
      )

      return this
    },
    async stubListMessages(msgs: MessageToMatch[]) {
      return await serviceMocker.stubGetter(
        `/threads/${threadId}/messages`,
        {},
        {
          object: "list",
          data: msgs.map((msg) => ({
            object: "thread.message",
            role: msg.role,
            content: [
              {
                type: "text",
                text: {
                  value: msg.content,
                },
              },
            ],
          })),
        },
      )
    },
  }
}

export default openAiAssistantCreatedRunMocker
