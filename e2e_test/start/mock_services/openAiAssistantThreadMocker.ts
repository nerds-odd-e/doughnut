import ServiceMocker from "../../support/ServiceMocker"
import { MessageToMatch } from "./MessageToMatch"
import openAiAssistantCreatedRunMocker from "./openAiAssistantCreatedRunMocker"

const openAiAssistantThreadMocker = (
  serviceMocker: ServiceMocker,
  threadId: string,
) => {
  return {
    async stubCreateMessageAndCreateRun(
      message: MessageToMatch,
      runId: string,
    ) {
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
        id: runId,
        status: "queued",
      })
      return openAiAssistantCreatedRunMocker(serviceMocker, threadId, runId)
    },
  }
}

export default openAiAssistantThreadMocker
