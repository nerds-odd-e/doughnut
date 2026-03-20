import * as fs from 'node:fs'
import * as http from 'node:http'
import type * as net from 'node:net'
import * as os from 'node:os'
import * as path from 'node:path'
import { afterAll, beforeAll, describe, expect, test } from 'vitest'
import { recallStatus } from '../src/recall.js'

function tempConfigWithToken(): string {
  const configDir = fs.mkdtempSync(
    path.join(os.tmpdir(), 'doughnut-sdk-http-classify-')
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

describe('real SDK HTTP errors classify for user-visible messages', () => {
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

  test('401 from API maps to invalid/expired token (not service unavailable)', async () => {
    const server = http.createServer((_, res) => {
      res.writeHead(401, { 'Content-Type': 'application/json' })
      res.end(JSON.stringify({ error: 'nope' }))
    })
    await new Promise<void>((resolve, reject) => {
      server.listen(0, '127.0.0.1', () => resolve())
      server.on('error', reject)
    })
    const addr = server.address() as net.AddressInfo
    const configDir = tempConfigWithToken()
    process.env.DOUGHNUT_CONFIG_DIR = configDir
    process.env.DOUGHNUT_API_BASE_URL = `http://127.0.0.1:${addr.port}`
    try {
      await expect(recallStatus()).rejects.toThrow(
        'Access token is invalid or expired'
      )
    } finally {
      await new Promise<void>((resolve) => server.close(() => resolve()))
      fs.rmSync(configDir, { recursive: true, force: true })
    }
  })

  test('403 from API maps to no-permission message', async () => {
    const server = http.createServer((_, res) => {
      res.writeHead(403, { 'Content-Type': 'application/json' })
      res.end(JSON.stringify({ error: 'forbidden' }))
    })
    await new Promise<void>((resolve, reject) => {
      server.listen(0, '127.0.0.1', () => resolve())
      server.on('error', reject)
    })
    const addr = server.address() as net.AddressInfo
    const configDir = tempConfigWithToken()
    process.env.DOUGHNUT_CONFIG_DIR = configDir
    process.env.DOUGHNUT_API_BASE_URL = `http://127.0.0.1:${addr.port}`
    try {
      await expect(recallStatus()).rejects.toThrow(
        'Access token does not have permission'
      )
    } finally {
      await new Promise<void>((resolve) => server.close(() => resolve()))
      fs.rmSync(configDir, { recursive: true, force: true })
    }
  })
})
