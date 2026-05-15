import type ServiceMocker from '../../support/ServiceMocker'
import type { MessageToMatch } from './MessageToMatch'

type BodyToMatch = {
  input?: string
  instructions?: string
}

const responseBody = (outputText: string) => ({
  id: 'resp-mock',
  object: 'response',
  created_at: Math.floor(Date.now() / 1000),
  model: 'gpt-4.1-mini',
  output: [
    {
      id: 'msg-mock',
      type: 'message',
      role: 'assistant',
      status: 'completed',
      content: [
        {
          type: 'output_text',
          text: outputText,
          annotations: [],
        },
      ],
    },
  ],
  parallel_tool_calls: false,
  tool_choice: 'none',
  tools: [],
  metadata: {},
  error: null,
  incomplete_details: null,
  instructions: null,
  temperature: null,
  top_p: null,
})

const bodyToMatchForMessage = (message: MessageToMatch): BodyToMatch => {
  if ('name' in message) {
    return {}
  }
  if (message.role === 'developer' || message.role === 'system') {
    return { instructions: message.content }
  }
  return { input: message.content }
}

const openAiResponsesStubber = (
  serviceMocker: ServiceMocker,
  bodyToMatch: BodyToMatch,
  bodyNotToMatch?: BodyToMatch
) => {
  const stubOutput = (outputText: string) =>
    serviceMocker.mockPostMatchsAndNotMatches(
      `/responses`,
      bodyToMatch,
      bodyNotToMatch,
      [responseBody(outputText)]
    )

  return {
    stubOutputText: stubOutput,
    requestDoesNotMessageMatch(message: MessageToMatch) {
      return openAiResponsesStubber(
        serviceMocker,
        bodyToMatch,
        bodyToMatchForMessage(message)
      )
    },
  }
}

const createOpenAiResponsesMock = (serviceMocker: ServiceMocker) => ({
  requestMessageMatches(message: MessageToMatch) {
    return openAiResponsesStubber(serviceMocker, bodyToMatchForMessage(message))
  },
})

export default createOpenAiResponsesMock
