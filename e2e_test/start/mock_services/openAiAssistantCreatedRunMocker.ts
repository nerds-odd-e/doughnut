import type ServiceMocker from '../../support/ServiceMocker'
import type { MessageToMatch } from './MessageToMatch'
import { createRequiresActionRun } from './openAiMessageComposer'

const openAiAssistantCreatedRunMocker = (
  serviceMocker: ServiceMocker,
  threadId: string,
  runId: string
) => {
  return {
    stubRetrieveRunsThatCompleted() {
      const responses = [
        {
          id: runId,
          status: 'completed',
        },
      ]
      serviceMocker.stubGetterWithMutipleResponses(
        `/threads/${threadId}/runs/${runId}`,
        {},
        responses
      )
      return this
    },
    stubRetrieveRunsThatRequireAction(hashes: Record<string, string>[]) {
      const responses = hashes.map((hash) => {
        switch (hash.response) {
          case 'ask_single_answer_multiple_choice_question':
            return createRequiresActionRun(
              runId,
              threadId,
              'ask_single_answer_multiple_choice_question',
              JSON.parse(hash.arguments!)
            )
          case 'evaluate_question':
            return createRequiresActionRun(
              runId,
              threadId,
              'evaluate_question',
              JSON.parse(hash.arguments!)
            )
          case 'complete note details':
            return createRequiresActionRun(
              runId,
              threadId,
              'complete_note_details',
              {
                completion: hash.arguments?.match(/"(.*)"/)?.[1],
              }
            )
          case 'suggest note title':
            return createRequiresActionRun(
              runId,
              threadId,
              'suggest_note_title',
              {
                newTitle: hash.arguments?.match(/"(.*)"/)?.[1],
              }
            )
          default:
            throw new Error(`Unknown response: ${hash.response}`)
        }
      })

      serviceMocker.stubGetterWithMutipleResponses(
        `/threads/${threadId}/runs/${runId}`,
        {},
        responses
      )
      return this
    },

    async stubSubmitToolOutputs() {
      await serviceMocker.stubPoster(
        `/threads/${threadId}/runs/${runId}/submit_tool_outputs`,
        {
          id: runId,
          status: 'queued',
        }
      )

      return this
    },
    async stubListMessages(msgs: MessageToMatch[]) {
      return await serviceMocker.stubGetter(
        `/threads/${threadId}/messages`,
        {
          run_id: runId,
        },
        {
          object: 'list',
          data: msgs.map((msg) => ({
            object: 'thread.message',
            role: msg.role,
            content: [
              {
                type: 'text',
                text: {
                  value: msg.content,
                },
              },
            ],
          })),
        }
      )
    },
  }
}

export default openAiAssistantCreatedRunMocker
