import ServiceMocker from "../../support/ServiceMocker"
import testability from "../testability"
import createOpenAiChatCompletionMock from "./createOpenAiChatCompletionMock"

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
      return serviceMocker.stubPoster(`/v1/images/generations`, {
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
        return serviceMocker.stubPoster(`/v1/files`, {
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

    async stubCreateAssistant(newId: string, _nameOfAssistant: string, modelName: string) {
      return await serviceMocker.mockMatchsAndNotMatches(
        `/v1/assistants`,
        {
          name: _nameOfAssistant,
          model: modelName,
        },
        undefined,
        {
          id: newId,
        },
      )
    },

    thread(threadId: string) {
      return {
        async stubCreateThreadAndRun() {
          await serviceMocker.stubPoster(`/v1/threads`, {
            id: threadId,
          })
          await serviceMocker.stubPoster(`/v1/threads/${threadId}/messages`, {
            id: "msg-abc123",
          })
          return await serviceMocker.stubPoster(`/v1/threads/${threadId}/runs`, {
            id: "run-abc123",
          })
        },
        singletonStubRetrieveRun() {
          const singletonIndex = undefined
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          const stubRetrieveRun = async (status: string, partial: any) => {
            const resp = await serviceMocker.stubGetter(
              `/v1/threads/${threadId}/runs/run-abc123`,
              {},
              {
                id: "run-abc123",
                status,
                ...partial,
              },
              singletonIndex,
            )
            return resp
          }
          return {
            async completed() {
              return stubRetrieveRun("completed", {})
            },
          }
        },

        async stubRetrieveRuns(hashes: Record<string, string>[]) {
          const responses = hashes.map((hash) => {
            switch (hash["response"]) {
              case "ask":
                return {
                  id: "run-abc123",
                  status: "requires_action",
                  required_action: {
                    type: "submit_tool_outputs",
                    submit_tool_outputs: {
                      tool_calls: [
                        {
                          type: "function",
                          function: {
                            name: "ask_clarification_question",
                            arguments: JSON.stringify({
                              question: hash["arguments"],
                            }),
                          },
                        },
                      ],
                    },
                  },
                }
              case "complete":
                return {
                  id: "run-abc123",
                  status: "completed",
                }
              default:
                throw new Error(`Unknown response: ${hash["response"]}`)
            }
          })

          return await serviceMocker.stubGetterWithMutipleResponses(
            `/v1/threads/${threadId}/runs/run-abc123`,
            {},
            responses,
          )
        },
      }
    },

    async stubFineTuningStatus(successful: boolean) {
      return await serviceMocker.stubPoster(`/v1/fine_tuning/jobs`, {
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
      return await serviceMocker.stubGetter(`/v1/models`, undefined, {
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
