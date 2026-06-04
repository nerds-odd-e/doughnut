import { FlexiPredicate, HttpMethod, Operator } from '@anev/ts-mountebank'
import ServiceMocker from '../../support/ServiceMocker'
import testability from '../testability'
import createOpenAiResponsesMock from './createOpenAiResponsesMock'
import { pollUntilFocusContextRetrievalPromptShapesMatch } from './openAiFocusContextRecallAssertions'
import {
  cyFetchOpenAiImposterRequests,
  OPEN_AI_IMPOSTER_PORT,
  responsesPostBodies,
} from './openAiImposterRecordedRequests'
import { buildResponsesStreamEvent } from './openAiMessageComposer'

const openAiService = () => {
  const serviceMocker = new ServiceMocker('openAi', OPEN_AI_IMPOSTER_PORT)
  const MOCK_TOKEN = 'mock-token-for-e2e-testing'
  return {
    mock() {
      testability().setOpenAiTokenOverride(MOCK_TOKEN)
      testability().mockService(serviceMocker)
    },
    restore() {
      testability().restoreMockedService(serviceMocker)
      testability().setOpenAiTokenOverride(null)
    },

    disable() {
      testability().setOpenAiTokenOverride(null)
    },

    restartImposter() {
      return serviceMocker.install()
    },

    responses() {
      return createOpenAiResponsesMock(serviceMocker)
    },

    // Smarter stub for POST /embeddings used by semantic search tests.
    // - If the request body contains "something else" in its input, return a very different vector.
    // - Otherwise, return 10 identical embeddings (more than requested) with a base vector.
    stubCreateEmbeddings() {
      const buildEmbeddingResponse = (vector: number[]) => ({
        object: 'list',
        data: Array.from({ length: 10 }).map((_, index) => ({
          object: 'embedding',
          index,
          embedding: vector,
        })),
        model: 'text-embedding-3-small',
        usage: { prompt_tokens: 0, total_tokens: 0 },
      })

      const buildSizedEmbeddingResponse = (
        count: number,
        vector: number[]
      ) => ({
        object: 'list',
        data: Array.from({ length: count }).map((_, index) => ({
          object: 'embedding',
          index,
          embedding: vector,
        })),
        model: 'text-embedding-3-small',
        usage: { prompt_tokens: 0, total_tokens: 0 },
      })

      const baseVector = [0.1, 0.1, 0.1, 0.1, 0.1]
      const differentVector = [0.9, 0.9, 0.9, 0.9, 0.9]

      // Match when the input contains "something else"
      serviceMocker.mockPostMatchsAndNotMatches(
        `/embeddings`,
        { input: '.*something else.*' },
        undefined,
        [buildEmbeddingResponse(differentVector)]
      )

      // Ensure indexing calls for the Physics note (single input) get exactly one embedding
      serviceMocker.mockPostMatchsAndNotMatches(
        `/embeddings`,
        { input: ['.*The study of nature.*'] },
        undefined,
        [buildSizedEmbeddingResponse(1, baseVector)]
      )

      // Default for all other inputs
      return serviceMocker.mockPostMatchsAndNotMatches(
        `/embeddings`,
        {},
        { input: '.*something else.*' },
        [buildEmbeddingResponse(baseVector)]
      )
    },

    stubOpenAiWithErrorResponse() {
      return serviceMocker.stubGetterWithError500Response(`/*`, {})
    },

    async alwaysResponseAsUnauthorized() {
      await serviceMocker.install()
      await serviceMocker.stubPosterUnauthorized(`/*`, {
        status: 'BAD_REQUEST',
        message: 'nah nah nah, you need a valid token',
        error: {
          'OpenAi Error': 'BAD_REQUEST',
        },
      })
    },

    expectLastResponsesPostBodyContains(marker: string) {
      cyFetchOpenAiImposterRequests().then((requests) => {
        const postBodies = responsesPostBodies(requests)
        expect(
          postBodies.length,
          'OpenAI POST /responses requests recorded'
        ).to.be.greaterThan(0)
        expect(postBodies.join('\n')).to.include(marker)
      })
    },

    expectResponsesPostBodiesIncludeFocusContextRetrievalPromptShapes() {
      pollUntilFocusContextRetrievalPromptShapesMatch()
    },

    stubConversationAiReplyStream(messages: Record<string, string>[]) {
      const responses = messages.map((row) =>
        buildResponsesStreamEvent(
          row['assistant reply']!,
          row['response type'] as 'requires action' | 'message delta & complete'
        )
      )

      serviceMocker.stubPosterWithMultipleResponses(`/responses`, responses, {
        'Content-Type': 'text/event-stream',
      })
      return this
    },

    async stubGetModels(modelNames: string) {
      return await serviceMocker.stubGetter(`/models`, undefined, {
        object: 'list',
        data: modelNames.split(',').map((modelName) => {
          return {
            id: modelName.trim(),
            object: 'model',
            created: 1614807352,
            owned_by: 'openai',
          }
        }),
      })
    },

    stubTranscription(transcript: string) {
      const predicate = new FlexiPredicate()
        .withOperator(Operator.matches)
        .withPath(`/audio/transcriptions`)
        .withMethod(HttpMethod.POST)
        .withHeader('Content-Type', 'multipart/form-data')

      return serviceMocker.mockWithPredicates(
        [predicate],
        [
          {
            text: transcript,
          },
        ]
      )
    },
  }
}

export default openAiService
