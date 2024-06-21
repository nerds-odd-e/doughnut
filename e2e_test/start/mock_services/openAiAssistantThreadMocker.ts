import ServiceMocker from "../../support/ServiceMocker"
import { MessageToMatch } from "./MessageToMatch"
import openAiAssistantCreatedRunMocker from "./openAiAssistantCreatedRunMocker"

const openAiAssistantThreadMocker = (
  serviceMocker: ServiceMocker,
  threadId: string,
  _mockedRunIds: string[],
) => {
  return {
    aRun(runId: string) {
      return openAiAssistantCreatedRunMocker(
        serviceMocker,
        threadId,
        runId,
      )
    },

    stubCreateMessage(message: MessageToMatch) {
      // for creating a message
      serviceMocker.mockPostMatchsAndNotMatches(
        `/threads/${threadId}/messages`,
        message,
        undefined,
        {
          id: "msg-abc123",
        },
      )
      return this
    },
  }
}

export default openAiAssistantThreadMocker
