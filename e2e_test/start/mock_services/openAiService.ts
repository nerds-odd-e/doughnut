import ServiceMocker from "../../support/ServiceMocker"
import testability from "../testability"
import { MessageToMatch } from "./MessageToMatch"
import createOpenAiChatCompletionMock from "./createOpenAiChatCompletionMock"
import openAiAssistantThreadMocker from "./openAiAssistantThreadMocker"

type RunStreamData = {
  runId: string
  fullMessage: string
}

const openAiService = () => {
  const serviceMocker = new ServiceMocker("openAi", 5001)
  return {
    mock() {
      testability().mockService(serviceMocker)
    },
    restore() {
      testability().restoreMockedService(serviceMocker)
    },

    restartImposter() {
      return serviceMocker.install()
    },

    chatCompletion() {
      return createOpenAiChatCompletionMock(serviceMocker)
    },

    stubCreateImage() {
      return serviceMocker.stubPoster(`/images/generations`, {
        created: 1589478378,
        data: [
          {
            url: "https://moon",
            b64_json:
              "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=",
          },
        ],
      })
    },

    stubOpenAiCompletionWithErrorResponse() {
      return serviceMocker.stubGetterWithError500Response(`/*`, {})
    },

    async alwaysResponseAsUnauthorized() {
      await serviceMocker.install()
      await serviceMocker.stubPosterUnauthorized(`/*`, {
        status: "BAD_REQUEST",
        message: "nah nah nah, you need a valid token",
        error: {
          "OpenAi Error": "BAD_REQUEST",
        },
      })
    },

    stubOpenAiUploadResponse(shouldSuccess: boolean) {
      if (shouldSuccess) {
        return serviceMocker.stubPoster(`/files`, {
          id: "file-abc123",
          object: "file",
          bytes: 175,
          created_at: 1613677385,
          filename: "Question-%s.jsonl",
          purpose: "fine-tune",
        })
      } else {
        return serviceMocker.stubPosterWithError500Response("/v1/files", {})
      }
    },

    async stubCreateAssistant(
      newId: string,
      _nameOfAssistant: string,
      modelName: string,
    ) {
      return await serviceMocker.mockPostMatchsAndNotMatches(
        `/assistants`,
        {
          name: _nameOfAssistant,
          model: modelName,
        },
        undefined,
        [{
          id: newId,
        }],
      )
    },

    stubAIChat(messages: Record<string, string>[], assistantId?: string) {
    const thread = this.stubCreateThread(
      "thread-abc123",
    ).stubCreateRunStreams("thread-abc123", assistantId, messages.map((row) => ({
      runId: row["run id"]!,
      fullMessage: row["assistant reply"]!,
  })))
    messages.forEach((row) => {
      const userMessage: MessageToMatch = {
        role: "user",
        content: row["user message"]!,
      }
      thread.stubCreateMessage(userMessage)
    })

    },

    stubCreateThread(threadId: string) {
      serviceMocker.stubPoster(`/threads`, {
        id: threadId,
      })
      return this
    },

    stubCreateRuns(threadId: string, runIds: string[]) {
      serviceMocker.stubPosterWithMultipleResponses(`/threads/${threadId}/runs`,
        runIds.map((runId) => ({
        id: runId,
        status: "queued",
      })))
      return openAiAssistantThreadMocker(serviceMocker, threadId, runIds)
    },

    stubCreateRunStreams(threadId: string, _assistantId: string | undefined, runStreamData: RunStreamData[]) {
      serviceMocker.mockPostMatchsAndNotMatches(`/threads/${threadId}/runs`,
        {},
        undefined,
        runStreamData.map(({runId, fullMessage}) =>
`event: thread.message.created
data: {"thread_id": "${threadId}", "run_id": "${runId}", "role": "assistant", "content": []}

event: thread.message.delta
data: {"delta": {"content": [{"index": 0, "type": "text", "text": {"value": "${fullMessage}"}}]}}

event: thread.run.step.completed
data: {"run_id": "${runId}", "status": "completed"}

`),
    { "Content-Type": "text/event-stream" })
      return openAiAssistantThreadMocker(serviceMocker, threadId, [])
    },

    async stubFineTuningStatus(successful: boolean) {
      return await serviceMocker.stubPoster(`/fine_tuning/jobs`, {
        object: "fine_tuning.job",
        id: "ftjob-abc123",
        model: "gpt-3.5-turbo-0613",
        created_at: 1614807352,
        fine_tuned_model: null,
        organization_id: "org-123",
        result_files: [],
        status: successful ? "queued" : "failed",
        validation_file: null,
        training_file: "file-abc123",
      })
    },

    async stubGetModels(modelNames: string) {
      return await serviceMocker.stubGetter(`/models`, undefined, {
        object: "list",
        data: modelNames.split(",").map((modelName) => {
          return {
            id: modelName.trim(),
            object: "model",
            created: 1614807352,
            owned_by: "openai",
          }
        }),
      })
    },
  }
}

export default openAiService
