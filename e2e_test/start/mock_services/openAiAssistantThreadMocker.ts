import ServiceMocker from "../../support/ServiceMocker"

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
      return await serviceMocker.stubPoster(
        `/threads/${threadId}/runs/run-abc123/submit_tool_outputs`,
        {
          id: "run-abc123",
          status: "queued",
        },
      )
    },
  }
}

export default openAiAssistantThreadMocker
