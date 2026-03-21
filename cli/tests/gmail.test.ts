import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import * as fs from 'node:fs'
import * as os from 'node:os'
import * as path from 'node:path'
import {
  getLastEmailSubject,
  loadConfig,
  saveConfig,
  type GmailConfig,
} from '../src/gmail.js'

function createTempDir(): string {
  return fs.mkdtempSync(path.join(os.tmpdir(), 'doughnut-gmail-test-'))
}

describe('Gmail config file', () => {
  let configPath: string

  beforeEach(() => {
    const dir = createTempDir()
    configPath = path.join(dir, 'gmail.json')
  })

  test('loadConfig returns empty accounts when file does not exist', () => {
    const config = loadConfig(configPath)
    expect(config).toEqual({ accounts: [] })
  })

  test('saveConfig creates file and loadConfig reads it', () => {
    const config: GmailConfig = {
      clientId: 'client-123',
      clientSecret: 'secret-456',
      accounts: [
        {
          email: 'user@gmail.com',
          accessToken: 'tok',
          refreshToken: 'rtok',
          expiresAt: 999,
        },
      ],
    }
    saveConfig(config, configPath)
    const loaded = loadConfig(configPath)
    expect(loaded.clientId).toBe('client-123')
    expect(loaded.clientSecret).toBe('secret-456')
    expect(loaded.accounts).toHaveLength(1)
    expect(loaded.accounts[0].email).toBe('user@gmail.com')
  })

  test('saveConfig creates parent directory', () => {
    const dir = createTempDir()
    const nestedPath = path.join(dir, 'nested', 'gmail.json')
    saveConfig({ accounts: [] }, nestedPath)
    expect(fs.existsSync(nestedPath)).toBe(true)
  })
})

describe('last email subject (Gmail API)', () => {
  let configPath: string

  beforeEach(() => {
    const dir = createTempDir()
    configPath = path.join(dir, 'gmail.json')
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  test('throws when no accounts configured', async () => {
    saveConfig({ accounts: [] }, configPath)
    await expect(getLastEmailSubject(configPath)).rejects.toThrow(
      'No Gmail account configured'
    )
  })

  test('returns subject when messages exist', async () => {
    saveConfig(
      {
        clientId: 'c',
        clientSecret: 's',
        accounts: [
          {
            email: 'u@gmail.com',
            accessToken: 'at',
            refreshToken: 'rt',
            expiresAt: Date.now() + 3600_000,
          },
        ],
      },
      configPath
    )

    vi.stubGlobal(
      'fetch',
      vi
        .fn()
        .mockResolvedValueOnce({
          ok: true,
          json: () => Promise.resolve({ messages: [{ id: 'msg-1' }] }),
        })
        .mockResolvedValueOnce({
          ok: true,
          json: () =>
            Promise.resolve({
              payload: {
                headers: [{ name: 'Subject', value: 'Test Email Subject' }],
              },
            }),
        })
    )

    const subject = await getLastEmailSubject(configPath)
    expect(subject).toBe('Test Email Subject')
  })

  test('returns "(no messages)" when inbox is empty', async () => {
    saveConfig(
      {
        clientId: 'c',
        clientSecret: 's',
        accounts: [
          {
            email: 'u@gmail.com',
            accessToken: 'at',
            refreshToken: 'rt',
            expiresAt: Date.now() + 3600_000,
          },
        ],
      },
      configPath
    )

    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({ messages: [] }),
      })
    )

    const subject = await getLastEmailSubject(configPath)
    expect(subject).toBe('(no messages)')
  })

  test('returns "(no subject)" when message has no Subject header', async () => {
    saveConfig(
      {
        clientId: 'c',
        clientSecret: 's',
        accounts: [
          {
            email: 'u@gmail.com',
            accessToken: 'at',
            refreshToken: 'rt',
            expiresAt: Date.now() + 3600_000,
          },
        ],
      },
      configPath
    )

    vi.stubGlobal(
      'fetch',
      vi
        .fn()
        .mockResolvedValueOnce({
          ok: true,
          json: () => Promise.resolve({ messages: [{ id: 'msg-1' }] }),
        })
        .mockResolvedValueOnce({
          ok: true,
          json: () => Promise.resolve({ payload: { headers: [] } }),
        })
    )

    const subject = await getLastEmailSubject(configPath)
    expect(subject).toBe('(no subject)')
  })

  test('refreshes token when expired and fetches last email', async () => {
    const pastExpiry = Date.now() - 1000
    saveConfig(
      {
        clientId: 'c',
        clientSecret: 's',
        accounts: [
          {
            email: 'u@gmail.com',
            accessToken: 'old-at',
            refreshToken: 'rt',
            expiresAt: pastExpiry,
          },
        ],
      },
      configPath
    )

    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce({
        ok: true,
        json: () =>
          Promise.resolve({
            access_token: 'new-at',
            expires_in: 3600,
          }),
      })
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({ messages: [{ id: 'msg-1' }] }),
      })
      .mockResolvedValueOnce({
        ok: true,
        json: () =>
          Promise.resolve({
            payload: {
              headers: [{ name: 'Subject', value: 'Refreshed' }],
            },
          }),
      })
    vi.stubGlobal('fetch', fetchMock)

    const subject = await getLastEmailSubject(configPath)
    expect(subject).toBe('Refreshed')
    expect(fetchMock).toHaveBeenCalledTimes(3)
    const tokenCalls = fetchMock.mock.calls.filter((c) =>
      (c[0] as string).includes('/token')
    )
    expect(tokenCalls).toHaveLength(1)
  })

  test('throws when token refresh returns invalid_grant', async () => {
    saveConfig(
      {
        clientId: 'c',
        clientSecret: 's',
        accounts: [
          {
            email: 'u@gmail.com',
            accessToken: 'at',
            refreshToken: 'rt',
            expiresAt: Date.now() - 1000,
          },
        ],
      },
      configPath
    )

    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: false,
        json: () => Promise.resolve({ error: 'invalid_grant' }),
      })
    )

    await expect(getLastEmailSubject(configPath)).rejects.toThrow(
      'Session expired'
    )
  })
})
