import ServiceMocker from "../../support/ServiceMocker"
import { MessageToMatch } from "./MessageToMatch"
import openAiAssistantCreatedRunMocker from "./openAiAssistantCreatedRunMocker"

const openAiAssistantThreadMocker = (serviceMocker: ServiceMocker, threadId: string) => {
  return {
    async stubCreateMessageAndCreateRun(message: MessageToMatch) {
      // for creating a message
      await serviceMocker.mockPostMatchsAndNotMatches(
        `/threads/${threadId}/messages`,
        message,
        undefined,
        {
          id: "msg-abc123",
        },
      )
      await serviceMocker.stubPoster(`/threads/${threadId}/runs`, {
        id: "run-abc123",
        status: "queued",
      })
      return this
    },

    async stubRetrieveRunsThatCompleted() {
      const responses = [
        {
          id: "run-abc123",
          status: "completed",
        },
      ]
      await serviceMocker.stubGetterWithMutipleResponses(
        `/threads/${threadId}/runs/run-abc123`,
        {},
        responses,
      )
      return openAiAssistantCreatedRunMocker(serviceMocker, threadId, "run-abc123")
    },

    async stubRetrieveRunsThatRequireAction(hashes: Record<string, string>[]) {
      const createRequiresActionRun = (functionName: string, argumentsObj: unknown) => {
        return {
          id: "run-abc123",
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
        `/threads/${threadId}/runs/run-abc123`,
        {},
        responses,
      )
      return openAiAssistantCreatedRunMocker(serviceMocker, threadId, "run-abc123")
    },
  }
}

export default openAiAssistantThreadMocker
