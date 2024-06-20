import ServiceMocker from "../../support/ServiceMocker"
import openAiAssistantCreatedRunMocker from "./openAiAssistantCreatedRunMocker"

const openAiAssistantThreadMocker = (serviceMocker: ServiceMocker, threadId: string) => {
  return {
    async stubCreateMessageAndCreateRunAndSubmit() {
      // for creating a message
      await serviceMocker.stubPoster(`/threads/${threadId}/messages`, {
        id: "msg-abc123",
      })
      await serviceMocker.stubPoster(`/threads/${threadId}/runs`, {
        id: "run-abc123",
        status: "queued",
      })
      return openAiAssistantCreatedRunMocker(serviceMocker, threadId, "run-abc123")
    },
  }
}

export default openAiAssistantThreadMocker
