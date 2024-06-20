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
    async stubListMessages(msg: string) {
      return await serviceMocker.stubGetter(
        `/threads/${threadId}/messages`,
        {},
        {
          object: "list",
          data: [
            {
              object: "thread.message",
              content: [
                {
                  type: "text",
                  text: {
                    value: msg,
                  },
                },
              ],
            },
          ],
        },
      )
    },
  }
}

export default openAiAssistantCreatedRunMocker
