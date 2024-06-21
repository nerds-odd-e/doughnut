import ServiceMocker from "../../support/ServiceMocker"
import { MessageToMatch } from "./MessageToMatch"

const openAiAssistantCreatedRunMocker = (
  serviceMocker: ServiceMocker,
  threadId: string,
  runId: string,
) => {
  return {
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
