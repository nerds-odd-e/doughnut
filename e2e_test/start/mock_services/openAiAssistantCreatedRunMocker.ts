import ServiceMocker from "../../support/ServiceMocker"

const openAiAssistantCreatedRunMocker = (
  serviceMocker: ServiceMocker,
  threadId: string,
  runId: string,
) => {
  return {
    async stubSubmitToolOutputs() {
      return await serviceMocker.stubPoster(
        `/threads/${threadId}/runs/run-abc123/submit_tool_outputs`,
        {
          id: runId,
          status: "queued",
        },
      )
    },
  }
}

export default openAiAssistantCreatedRunMocker
