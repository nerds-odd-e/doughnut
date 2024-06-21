import ServiceMocker from "../../support/ServiceMocker"
import { MessageToMatch } from "./MessageToMatch"
import openAiAssistantCreatedRunMocker from "./openAiAssistantCreatedRunMocker"

const openAiAssistantThreadMocker = (
  serviceMocker: ServiceMocker,
  threadId: string,
) => {
  return {
    async stubMultipleCreateRuns(runIds: string[]) {
      await serviceMocker.stubPosterWithMultipleResponses(`/threads/${threadId}/runs`,
        runIds.map((runId) => ({
        id: runId,
        status: "queued",
      })))
      return this
    },
    async stubCreateMessage(message: MessageToMatch, runId: string) {
      // for creating a message
      await serviceMocker.mockPostMatchsAndNotMatches(
        `/threads/${threadId}/messages`,
        message,
        undefined,
        {
          id: "msg-abc123",
        },
      )
      return openAiAssistantCreatedRunMocker(
        serviceMocker,
        threadId,
        runId,
      )
    },

 }
}

export default openAiAssistantThreadMocker
