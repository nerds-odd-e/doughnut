import assert from 'node:assert/strict'
import { describe, test } from 'node:test'
import {
  recordedResponsesPostsMatchMarkers,
  type RecordedImposterRequest,
} from './openAiImposterRecordedRequests'

const responsesPost = (body: string | object): RecordedImposterRequest => ({
  method: 'POST',
  path: '/responses',
  body,
})

describe('recorded OpenAI response requests', () => {
  test('accepts an owner marker without a foreign marker', () => {
    assert.equal(
      recordedResponsesPostsMatchMarkers(
        [responsesPost({ input: 'OWNER_MARKER' })],
        'OWNER_MARKER',
        'FOREIGN_MARKER'
      ),
      true
    )
  })

  test('rejects a foreign marker even when the owner request was recorded last', () => {
    assert.equal(
      recordedResponsesPostsMatchMarkers(
        [
          responsesPost('{"input":"FOREIGN_MARKER"}'),
          responsesPost({ input: 'OWNER_MARKER' }),
        ],
        'OWNER_MARKER',
        'FOREIGN_MARKER'
      ),
      false
    )
  })

  test('rejects recordings without the owner marker', () => {
    assert.equal(
      recordedResponsesPostsMatchMarkers(
        [responsesPost({ input: 'SOME_OTHER_REQUEST' })],
        'OWNER_MARKER',
        'FOREIGN_MARKER'
      ),
      false
    )
  })

  test('retains the presence-only condition when no foreign marker is supplied', () => {
    assert.equal(
      recordedResponsesPostsMatchMarkers(
        [
          responsesPost({ input: 'AN_UNRELATED_MARKER' }),
          responsesPost('{"input":"OWNER_MARKER"}'),
        ],
        'OWNER_MARKER'
      ),
      true
    )
  })
})
