import * as fs from 'node:fs'
import * as http from 'node:http'
import type * as net from 'node:net'
import * as os from 'node:os'
import * as path from 'node:path'
import { afterAll, beforeAll, describe, expect, test } from 'vitest'
import type { DueMemoryTrackers } from 'doughnut-api'
import { recallStatus } from '../src/commands/recallStatus.js'

function tempConfigWithToken(): string {
  const configDir = fs.mkdtempSync(
    path.join(os.tmpdir(), 'doughnut-recall-status-')
  )
  fs.writeFileSync(
    path.join(configDir, 'access-tokens.json'),
    JSON.stringify({
      tokens: [{ label: 't', token: 'fake-bearer' }],
      defaultLabel: 't',
    })
  )
  return configDir
}

async function withRecallingResponse(
  trackers: DueMemoryTrackers,
  run: () => Promise<void>
): Promise<void> {
  const server = http.createServer((req, res) => {
    if (!req.url?.startsWith('/api/recalls/recalling')) {
      res.writeHead(404)
      res.end()
      return
    }
    res.writeHead(200, { 'Content-Type': 'application/json' })
    res.end(JSON.stringify(trackers))
  })
  await new Promise<void>((resolve, reject) => {
    server.listen(0, '127.0.0.1', () => resolve())
    server.on('error', reject)
  })
  const addr = server.address() as net.AddressInfo
  const configDir = tempConfigWithToken()
  const savedConfigDir = process.env.DOUGHNUT_CONFIG_DIR
  const savedApiBaseUrl = process.env.DOUGHNUT_API_BASE_URL
  process.env.DOUGHNUT_CONFIG_DIR = configDir
  process.env.DOUGHNUT_API_BASE_URL = `http://127.0.0.1:${addr.port}`
  try {
    await run()
  } finally {
    await new Promise<void>((resolve) => server.close(() => resolve()))
    fs.rmSync(configDir, { recursive: true, force: true })
    if (savedConfigDir === undefined) {
      delete process.env.DOUGHNUT_CONFIG_DIR
    } else {
      process.env.DOUGHNUT_CONFIG_DIR = savedConfigDir
    }
    if (savedApiBaseUrl === undefined) {
      delete process.env.DOUGHNUT_API_BASE_URL
    } else {
      process.env.DOUGHNUT_API_BASE_URL = savedApiBaseUrl
    }
  }
}

describe('recallStatus', () => {
  let savedConfigDir: string | undefined
  let savedApiBaseUrl: string | undefined

  beforeAll(() => {
    savedConfigDir = process.env.DOUGHNUT_CONFIG_DIR
    savedApiBaseUrl = process.env.DOUGHNUT_API_BASE_URL
  })

  afterAll(() => {
    if (savedConfigDir === undefined) {
      delete process.env.DOUGHNUT_CONFIG_DIR
    } else {
      process.env.DOUGHNUT_CONFIG_DIR = savedConfigDir
    }
    if (savedApiBaseUrl === undefined) {
      delete process.env.DOUGHNUT_API_BASE_URL
    } else {
      process.env.DOUGHNUT_API_BASE_URL = savedApiBaseUrl
    }
  })

  test('0 notes when toRepeat is absent', async () => {
    await withRecallingResponse({ totalAssimilatedCount: 0 }, async () => {
      await expect(recallStatus()).resolves.toBe('0 notes to recall today')
    })
  })

  test('0 notes when toRepeat is empty', async () => {
    await withRecallingResponse(
      { totalAssimilatedCount: 0, toRepeat: [] },
      async () => {
        await expect(recallStatus()).resolves.toBe('0 notes to recall today')
      }
    )
  })

  test('singular when exactly one due tracker', async () => {
    await withRecallingResponse(
      {
        totalAssimilatedCount: 0,
        toRepeat: [{ memoryTrackerId: 1 }],
      },
      async () => {
        await expect(recallStatus()).resolves.toBe('1 note to recall today')
      }
    )
  })

  test('plural when two due trackers', async () => {
    await withRecallingResponse(
      {
        totalAssimilatedCount: 0,
        toRepeat: [{ memoryTrackerId: 1 }, { memoryTrackerId: 2 }],
      },
      async () => {
        await expect(recallStatus()).resolves.toBe('2 notes to recall today')
      }
    )
  })

  test('plural for larger counts', async () => {
    const toRepeat = Array.from({ length: 10 }, (_, i) => ({
      memoryTrackerId: i + 1,
    }))
    await withRecallingResponse(
      { totalAssimilatedCount: 0, toRepeat },
      async () => {
        await expect(recallStatus()).resolves.toBe('10 notes to recall today')
      }
    )
  })
})
