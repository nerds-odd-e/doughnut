import ServiceMocker from "../../support/ServiceMocker"
import { MessageToMatch } from "./MessageToMatch"
import openAiAssistantCreatedRunMocker from "./openAiAssistantCreatedRunMocker"

const openAiAssistantThreadMocker = (
  serviceMocker: ServiceMocker,
  threadId: string,
  mockedRunIds: string[]
) => {
  return {
    aRun(runId: string) {
      if (!mockedRunIds.includes(runId)) {
        throw new Error(
          `Run ID ${runId} not found in mockedRunIds, please mock the run first`
        )
      }
      return openAiAssistantCreatedRunMocker(serviceMocker, threadId, runId)
    },

    stubCreateMessage(message: MessageToMatch) {
      // for creating a message
      serviceMocker.mockPostMatchsAndNotMatches(
        `/threads/${threadId}/messages`,
        message,
        undefined,
        [
          {
            id: "msg-abc123",
          },
        ]
      )
      return this
    },
  }
}

export default openAiAssistantThreadMocker
