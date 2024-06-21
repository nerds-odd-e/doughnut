import ServiceMocker from "../../support/ServiceMocker"
import { MessageToMatch } from "./MessageToMatch"
import openAiAssistantCreatedRunMocker from "./openAiAssistantCreatedRunMocker"

const openAiAssistantThreadMocker = (
  serviceMocker: ServiceMocker,
  threadId: string,
) => {
  return {
    aRun(runId: string) {
      return openAiAssistantCreatedRunMocker(
        serviceMocker,
        threadId,
        runId,
      )
    },
    async stubMultipleCreateRuns(runIds: string[]) {
      await serviceMocker.stubPosterWithMultipleResponses(`/threads/${threadId}/runs`,
        runIds.map((runId) => ({
        id: runId,
        status: "queued",
      })))
      return this
    },
    async stubCreateMessage(message: MessageToMatch) {
      // for creating a message
      await serviceMocker.mockPostMatchsAndNotMatches(
        `/threads/${threadId}/messages`,
        message,
        undefined,
        {
          id: "msg-abc123",
        },
      )
    },
  }
}

export default openAiAssistantThreadMocker
