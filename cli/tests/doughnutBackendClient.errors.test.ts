import { afterEach, describe, expect, test, vi } from 'vitest'
import {
  downloadNotebookExportZip,
  withBackendClient,
} from '../src/backendApi/doughnutBackendClient.js'
import { tempConfigWithToken } from './tempConfigTestHelpers.js'

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

describe('downloadNotebookExportZip', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  test('throws a readable error when the response does not name a file', async () => {
    process.env.DOUGHNUT_CONFIG_DIR = tempConfigWithToken()
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        headers: { get: () => null },
        arrayBuffer: () => Promise.resolve(new ArrayBuffer(0)),
      })
    )

    await expect(downloadNotebookExportZip(1)).rejects.toThrow(
      'The export response did not name a file.'
    )
  })

  test('reports a 404 as the notebook being gone or unreadable, not as the endpoint being unavailable', async () => {
    process.env.DOUGHNUT_CONFIG_DIR = tempConfigWithToken()
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: false,
        status: 404,
        text: () => Promise.resolve(''),
      })
    )

    await expect(downloadNotebookExportZip(1)).rejects.toThrow(
      'The notebook no longer exists in Doughnut, or you no longer have read access to it.'
    )
  })
})
