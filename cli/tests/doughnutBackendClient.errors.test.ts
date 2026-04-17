import { describe, expect, test } from 'vitest'
import { withBackendClient } from '../src/backendApi/doughnutBackendClient.js'

describe('withBackendClient error messages', () => {
  test('uses API message for HTTP 413 attach failures', async () => {
    await expect(
      withBackendClient('t', async () => {
        throw {
          status: 413,
          message:
            'The uploaded file exceeds the maximum upload size (100 MB).',
          errorType: 'MULTIPART_SIZE_EXCEEDED',
        }
      })
    ).rejects.toThrow(
      'The uploaded file exceeds the maximum upload size (100 MB).'
    )
  })

  test('falls back for HTTP 413 without message', async () => {
    await expect(
      withBackendClient('t', async () => {
        throw { status: 413 }
      })
    ).rejects.toThrow(/maximum upload size/)
  })
})
