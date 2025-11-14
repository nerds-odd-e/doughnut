import { FlexiPredicate, HttpMethod, Operator } from '@anev/ts-mountebank'
import type { MessageToMatch } from './MessageToMatch'
import ServiceMocker from '../../support/ServiceMocker'
import testability from '../testability'
import createOpenAiChatCompletionMock from './createOpenAiChatCompletionMock'
import openAiAssistantThreadMocker from './openAiAssistantThreadMocker'
import {
  buildRunStreamEvent,
  buildChatCompletionStreamEvent,
  type RunStreamData,
} from './openAiMessageComposer'

interface BodyMatch {
  assistant_id?: string
}

const openAiService = () => {
  const serviceMocker = new ServiceMocker('openAi', 5001)
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

    stubCreateImage() {
      return serviceMocker.stubPoster(`/images/generations`, {
        created: 1589478378,
        data: [
          {
            url: 'https://moon',
            b64_json:
              'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=',
          },
        ],
      })
    },

    stubOpenAiCompletionWithErrorResponse() {
      return serviceMocker.stubGetterWithError500Response(`/*`, {})
    },

    stubToolCallSubmission(threadId: string, runId: string) {
      serviceMocker.stubPoster(
        `/threads/${threadId}/runs/${runId}/submit_tool_outputs`,
        {}
      )
      return this
    },

    stubRunCancellation(threadId: string, runId: string) {
      serviceMocker.stubPoster(`/threads/${threadId}/runs/${runId}/cancel`, {})
      return this
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

    stubOpenAiUploadResponse(shouldSuccess: boolean) {
      if (shouldSuccess) {
        return serviceMocker.stubPoster(`/files`, {
          id: 'file-abc123',
          object: 'file',
          bytes: 175,
          created_at: 1613677385,
          filename: 'Question-%s.jsonl',
          purpose: 'fine-tune',
        })
      } else {
        return serviceMocker.stubPosterWithError500Response('/v1/files', {})
      }
    },

    stubOpenAiVectorFileUpload() {
      const vectorStoreId = 'vector-store-abc123'
      serviceMocker.stubPoster(`/vector_stores`, {
        id: vectorStoreId,
      })
      serviceMocker.stubPoster(`/vector_stores/${vectorStoreId}/files`, {
        id: 'vector-file-1',
      })
    },

    async stubCreateAssistant(
      newId: string,
      nameOfAssistant: string,
      modelName?: string
    ) {
      return await serviceMocker.mockPostMatchsAndNotMatches(
        `/assistants`,
        {
          name: nameOfAssistant,
          model: modelName,
        },
        undefined,
        [
          {
            id: newId,
            name: nameOfAssistant,
          },
        ]
      )
    },

    createThreadWithRunStreamAndStubMessages(
      threadId: string,
      messages: Record<string, string>[],
      assistantId?: string
    ) {
      const thread = this.stubCreateRunStreams(
        threadId,
        assistantId,
        messages.map((row) => ({
          runId: row['run id']!,
          thread_id: threadId,
          threadId: threadId,
          responseType: row['response type'] as 'requires action',
          fullMessage: row['assistant reply']!,
        }))
      )

      messages.forEach((row) => {
        const userMessage: MessageToMatch = {
          role: 'user',
          content: row['user message']!,
        }
        thread.stubCreateMessage(userMessage)
      })
      return this
    },

    stubCreateThread(threadId: string) {
      serviceMocker.stubPoster(`/threads`, {
        id: threadId,
      })
      return this
    },

    stubChatCompletionStream(messages: Record<string, string>[]) {
      // Create separate responses for each message
      const responses = messages.map((row) =>
        buildChatCompletionStreamEvent(row['assistant reply']!)
      )

      // Use stubPosterWithMultipleResponses to send each response in sequence
      serviceMocker.stubPosterWithMultipleResponses(
        `/chat/completions`,
        responses,
        { 'Content-Type': 'text/event-stream' }
      )
      return this
    },

    stubCreateRuns(threadId: string, runIds: string[]) {
      serviceMocker.stubPosterWithMultipleResponses(
        `/threads/${threadId}/runs`,
        runIds.map((runId) => ({
          id: runId,
          thread_id: threadId,
          status: 'queued',
        }))
      )
      return openAiAssistantThreadMocker(serviceMocker, threadId, runIds)
    },

    stubCreateRunStreams(
      threadId: string,
      assistantId: string | undefined,
      runStreamData: RunStreamData[]
    ) {
      const bodyToMatch: BodyMatch = {}
      if (assistantId) {
        bodyToMatch.assistant_id = assistantId
      }
      serviceMocker.mockPostMatchsAndNotMatches(
        `/threads/${threadId}/runs$`,
        bodyToMatch,
        undefined,
        runStreamData.map((event) => buildRunStreamEvent(threadId, event)),
        { 'Content-Type': 'text/event-stream' }
      )
      return openAiAssistantThreadMocker(serviceMocker, threadId, [])
    },

    async stubFineTuningStatus(successful: boolean) {
      return await serviceMocker.stubPoster(`/fine_tuning/jobs`, {
        object: 'fine_tuning.job',
        id: 'ftjob-abc123',
        model: 'gpt-3.5-turbo-0613',
        created_at: 1614807352,
        fine_tuned_model: null,
        organization_id: 'org-123',
        result_files: [],
        status: successful ? 'queued' : 'failed',
        validation_file: null,
        training_file: 'file-abc123',
      })
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
