import { FlexiPredicate, Operator } from "@anev/ts-mountebank"
import ServiceMocker from "../../support/ServiceMocker"
import { HttpMethod } from "@anev/ts-mountebank"
import { MessageToMatch } from "./MessageToMatch"

type FunctionCall = {
  role: "function"
  function_call: {
    name: string
    arguments: string
    content: string // this is temporary, until chat-gpt 3.5 fine tuning support function_call
  }
}

type TextBasedMessage = {
  role: "user" | "assistant" | "system"
  content: string
}

type ChatMessageInResponse = TextBasedMessage | FunctionCall

function mockChatCompletion(
  serviceMocker: ServiceMocker,
  messagesToMatch: MessageToMatch[],
  message: ChatMessageInResponse,
  finishReason: "length" | "stop" | "function_call",
): Promise<void> {
  const body = { messages: messagesToMatch }
  const predicate = new FlexiPredicate()
    .withOperator(Operator.matches)
    .withPath(`/v1/chat/completions`)
    .withMethod(HttpMethod.POST)
    .withBody(body)
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

function mockChatCompletionForMessageContaining(
  serviceMocker: ServiceMocker,
  messagesToMatch: MessageToMatch[],
  reply: string,
  finishReason: "length" | "stop",
) {
  return mockChatCompletion(
    serviceMocker,
    messagesToMatch,
    { role: "assistant", content: reply },
    finishReason,
  )
}

const openAiService = () => {
  const serviceMocker = new ServiceMocker("openAi", 5001)
  return {
    mock() {
      cy.wrap(serviceMocker).mock()
    },
    restore() {
      cy.wrap(serviceMocker).restore()
    },

    restartImposter() {
      return serviceMocker.install()
    },

    mockChatCompletionWithIncompleteAssistantMessage(
      incomplete: string,
      reply: string,
      finishReason: "stop" | "length",
    ) {
      const messages = [{ content: "^" + Cypress._.escapeRegExp(incomplete) + "$" }]
      return mockChatCompletionForMessageContaining(serviceMocker, messages, reply, finishReason)
    },

    mockChatCompletionWithContext(reply: string, context: string) {
      const messageToMatch: MessageToMatch = { role: "system", content: context }
      const messages = [messageToMatch]
      return mockChatCompletionForMessageContaining(serviceMocker, messages, reply, "stop")
    },

    mockChatCompletionWithMessages(reply: string, messages: MessageToMatch[]) {
      return mockChatCompletionForMessageContaining(serviceMocker, messages, reply, "stop")
    },

    stubChatCompletion(reply: string, finishReason: "length" | "stop") {
      return mockChatCompletionForMessageContaining(serviceMocker, [], reply, finishReason)
    },

    stubAnyChatCompletionFunctionCall(functionName: string, argumentsString: string) {
      return mockChatCompletion(
        serviceMocker,
        [],
        {
          role: "function",
          function_call: {
            name: functionName,
            arguments: argumentsString,
          },
          content: argumentsString,
        },
        "function_call",
      )
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
        errors: {
          "OpenAi Error": "BAD_REQUEST",
        },
      })
    },
  }
}

export default openAiService
