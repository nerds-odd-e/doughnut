import { FlexiPredicate, HttpMethod, Operator } from "@anev/ts-mountebank"
import ServiceMocker from "../../support/ServiceMocker"
import { MessageToMatch } from "./MessageToMatch"
import testability from "../testability"

type FunctionCall = {
  role: "function"
  function_call: {
    name: string
    arguments: string
  }
}

type TextBasedMessage = {
  role: "user" | "assistant" | "system"
  content: string
}

type BodyToMatch = {
  messages?: MessageToMatch[]
  model?: string
}

type ChatMessageInResponse = TextBasedMessage | FunctionCall

function mockChatCompletion(
  serviceMocker: ServiceMocker,
  bodyToMatch: BodyToMatch,
  message: ChatMessageInResponse,
  finishReason: "length" | "stop" | "function_call",
): Promise<void> {
  const predicate = new FlexiPredicate()
    .withOperator(Operator.matches)
    .withPath(`/v1/chat/completions`)
    .withMethod(HttpMethod.POST)
    .withBody(bodyToMatch)
  return serviceMocker.mockWithPredicate(predicate, {
    object: "chat.completion",
    choices: [
      {
        message,
        index: 0,
        finish_reason: finishReason,
      },
    ],
  })
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
      return {
        requestMessageMatches(message: MessageToMatch) {
          return this.requestMessagesMatch([message])
        },
        requestMessagesMatch(messages: MessageToMatch[]) {
          return this.requestMatches({ messages })
        },
        requestMatches(bodyToMatch: BodyToMatch) {
          const stubFunctionCall = (functionName: string, argumentsString: string) => {
            return mockChatCompletion(
              serviceMocker,
              bodyToMatch,
              {
                role: "function",
                function_call: {
                  name: functionName,
                  arguments: argumentsString,
                },
              },
              "function_call",
            )
          }

          return {
            stubNonfunctionCallResponse(reply: string, finishReason: "length" | "stop" = "stop") {
              return mockChatCompletion(
                serviceMocker,
                bodyToMatch,
                { role: "assistant", content: reply },
                finishReason,
              )
            },
            stubNoteDetailsCompletion(reply: string) {
              return stubFunctionCall("note_details_completion", reply)
            },
            stubQuestionGeneration(reply: string) {
              return stubFunctionCall("ask_single_answer_multiple_choice_question", reply)
            },
            stubQuestionEvaluation(reply: string) {
              return stubFunctionCall("evaluate_question", reply)
            },
          }
        },
      }
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
    async stubFineTuningStatus(successful: boolean) {
      const predicate = new FlexiPredicate()
        .withOperator(Operator.matches)
        .withPath(`/v1/fine_tuning/jobs`)
        .withMethod(HttpMethod.POST)

      return await serviceMocker.mockWithPredicate(predicate, {
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
      const predicate = new FlexiPredicate()
        .withOperator(Operator.matches)
        .withPath(`/v1/models`)
        .withMethod(HttpMethod.GET)

      return await serviceMocker.mockWithPredicate(predicate, {
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
