import ServiceMocker from "../../support/ServiceMocker"

const openAiAssistantCreatedRunMocker = (
  serviceMocker: ServiceMocker,
  threadId: string,
  runId: string,
) => {
  return {
    async stubSubmitToolOutputs() {
      await serviceMocker.stubPoster(`/threads/${threadId}/runs/${runId}/submit_tool_outputs`, {
        id: runId,
        status: "queued",
      })

      return this
    },
  }
}

export default openAiAssistantCreatedRunMocker
