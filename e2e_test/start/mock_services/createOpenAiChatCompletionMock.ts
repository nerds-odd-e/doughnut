import { FlexiPredicate, HttpMethod, Operator } from "@anev/ts-mountebank"
import ServiceMocker from "../../support/ServiceMocker"
import { MessageToMatch } from "./MessageToMatch"

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

const openAiChatCompletionStubber = (serviceMocker: ServiceMocker, bodyToMatch: BodyToMatch) => {
  const stubChatCompletion = (
    message: ChatMessageInResponse,
    finishReason: "length" | "stop" | "function_call",
  ): Promise<void> => {
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

  const stubFunctionCall = (functionName: string, argumentsString: string) => {
    return stubChatCompletion(
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
      return stubChatCompletion({ role: "assistant", content: reply }, finishReason)
    },
    stubNoteDetailsCompletion(argumentsString: string) {
      return stubFunctionCall("note_details_completion", argumentsString)
    },
    stubQuestionGeneration(argumentsString: string) {
      return stubFunctionCall("ask_single_answer_multiple_choice_question", argumentsString)
    },
    stubQuestionEvaluation(argumentsString: string) {
      return stubFunctionCall("evaluate_question", argumentsString)
    },
  }
}

const createOpenAiChatCompletionMock = (serviceMocker: ServiceMocker) => {
  return {
    requestMessageMatches(message: MessageToMatch) {
      return this.requestMessagesMatch([message])
    },
    requestMessagesMatch(messages: MessageToMatch[]) {
      return this.requestMatches({ messages })
    },
    requestMatches(bodyToMatch: BodyToMatch) {
      return openAiChatCompletionStubber(serviceMocker, bodyToMatch)
    },
  }
}

export default createOpenAiChatCompletionMock
