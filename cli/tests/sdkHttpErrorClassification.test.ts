import * as fs from 'node:fs'
import * as http from 'node:http'
import type * as net from 'node:net'
import { afterAll, beforeAll, describe, expect, test } from 'vitest'
import { recallStatus } from '../src/commands/recallStatus.js'
import { tempConfigWithToken } from './tempConfigTestHelpers.js'

async function withLocalApi(
  status: number,
  body: unknown,
  run: () => Promise<void>
): Promise<void> {
  const server = http.createServer((_, res) => {
    res.writeHead(status, { 'Content-Type': 'application/json' })
    res.end(JSON.stringify(body))
  })
  await new Promise<void>((resolve, reject) => {
    server.listen(0, '127.0.0.1', () => resolve())
    server.on('error', reject)
  })
  const addr = server.address() as net.AddressInfo
  const configDir = tempConfigWithToken()
  const prevConfigDir = process.env.DONUT_CONFIG_DIR
  const prevApiBaseUrl = process.env.DONUT_API_BASE_URL
  process.env.DONUT_CONFIG_DIR = configDir
  process.env.DONUT_API_BASE_URL = `http://127.0.0.1:${addr.port}`
  try {
    await run()
  } finally {
    await new Promise<void>((resolve) => server.close(() => resolve()))
    fs.rmSync(configDir, { recursive: true, force: true })
    if (prevConfigDir === undefined) delete process.env.DONUT_CONFIG_DIR
    else process.env.DONUT_CONFIG_DIR = prevConfigDir
    if (prevApiBaseUrl === undefined) delete process.env.DONUT_API_BASE_URL
    else process.env.DONUT_API_BASE_URL = prevApiBaseUrl
  }
}

describe('real SDK HTTP errors classify for user-visible messages', () => {
  let savedConfigDir: string | undefined
  let savedApiBaseUrl: string | undefined

  beforeAll(() => {
    savedConfigDir = process.env.DONUT_CONFIG_DIR
    savedApiBaseUrl = process.env.DONUT_API_BASE_URL
  })

  afterAll(() => {
    if (savedConfigDir === undefined) delete process.env.DONUT_CONFIG_DIR
    else process.env.DONUT_CONFIG_DIR = savedConfigDir
    if (savedApiBaseUrl === undefined) delete process.env.DONUT_API_BASE_URL
    else process.env.DONUT_API_BASE_URL = savedApiBaseUrl
  })

  test.each([
    [401, { error: 'nope' }, 'Access token is invalid or expired'],
    [403, { error: 'forbidden' }, 'Access token does not have permission'],
  ] as const)(
    '%i from API maps to token message',
    async (status, body, message) => {
      await withLocalApi(status, body, async () => {
        await expect(recallStatus()).rejects.toThrow(message)
      })
    }
  )

  test('503 ApiError OPENAI_NOT_AVAILABLE body maps to server message', async () => {
    await withLocalApi(
      503,
      {
        message: 'OpenAI is not available (no API key configured).',
        errorType: 'OPENAI_NOT_AVAILABLE',
        errors: {},
      },
      async () => {
        await expect(recallStatus()).rejects.toThrow(
          'OpenAI is not available (no API key configured).'
        )
      }
    )
  })

  test('502 ApiError OPENAI_SERVICE_ERROR without message uses upstream wording', async () => {
    await withLocalApi(
      502,
      {
        message: '',
        errorType: 'OPENAI_SERVICE_ERROR',
        errors: {},
      },
      async () => {
        await expect(recallStatus()).rejects.toThrow(
          'A dependency service failed (HTTP 502)'
        )
      }
    )
  })
})
