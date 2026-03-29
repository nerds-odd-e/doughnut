import * as fs from 'node:fs'
import * as http from 'node:http'
import type * as net from 'node:net'
import { afterAll, beforeAll, describe, expect, test } from 'vitest'
import { recallStatus } from '../src/commands/recallStatus.js'
import { tempConfigWithToken } from './tempConfigTestHelpers.js'

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

  test('503 ApiError OPENAI_NOT_AVAILABLE body maps to server message (not generic connectivity)', async () => {
    const body = {
      message: 'OpenAI is not available (no API key configured).',
      errorType: 'OPENAI_NOT_AVAILABLE',
      errors: {},
    }
    const server = http.createServer((_, res) => {
      res.writeHead(503, { 'Content-Type': 'application/json' })
      res.end(JSON.stringify(body))
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
        'OpenAI is not available (no API key configured).'
      )
    } finally {
      await new Promise<void>((resolve) => server.close(() => resolve()))
      fs.rmSync(configDir, { recursive: true, force: true })
    }
  })

  test('502 ApiError OPENAI_SERVICE_ERROR without message uses upstream wording', async () => {
    const body = {
      message: '',
      errorType: 'OPENAI_SERVICE_ERROR',
      errors: {},
    }
    const server = http.createServer((_, res) => {
      res.writeHead(502, { 'Content-Type': 'application/json' })
      res.end(JSON.stringify(body))
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
        'A dependency service failed (HTTP 502)'
      )
    } finally {
      await new Promise<void>((resolve) => server.close(() => resolve()))
      fs.rmSync(configDir, { recursive: true, force: true })
    }
  })
})
