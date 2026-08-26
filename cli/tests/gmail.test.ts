import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import * as fs from 'node:fs'
import * as os from 'node:os'
import * as path from 'node:path'
import {
  getLastEmailSubject,
  loadConfig,
  saveConfig,
  type GmailConfig,
} from '../src/commands/gmail/gmail.js'

function createTempDir(): string {
  return fs.mkdtempSync(path.join(os.tmpdir(), 'donut-gmail-test-'))
}

function accountConfig(
  overrides: Partial<GmailConfig['accounts'][0]> & { expiresAt?: number } = {}
): GmailConfig {
  return {
    clientId: 'c',
    clientSecret: 's',
    accounts: [
      {
        email: 'u@gmail.com',
        accessToken: 'at',
        refreshToken: 'rt',
        expiresAt: Date.now() + 3600_000,
        ...overrides,
      },
    ],
  }
}

describe('Gmail config file', () => {
  let configPath: string

  beforeEach(() => {
    configPath = path.join(createTempDir(), 'gmail.json')
  })

  test('loadConfig returns empty accounts when file does not exist', () => {
    expect(loadConfig(configPath)).toEqual({ accounts: [] })
  })

  test('saveConfig round-trips client and account fields', () => {
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
    expect(loadConfig(configPath)).toEqual(config)
  })

  test('saveConfig creates parent directory', () => {
    const nestedPath = path.join(createTempDir(), 'nested', 'gmail.json')
    saveConfig({ accounts: [] }, nestedPath)
    expect(fs.existsSync(nestedPath)).toBe(true)
  })
})

describe('last email subject (Gmail API)', () => {
  let configPath: string

  beforeEach(() => {
    configPath = path.join(createTempDir(), 'gmail.json')
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
    saveConfig(accountConfig(), configPath)
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
    await expect(getLastEmailSubject(configPath)).resolves.toBe(
      'Test Email Subject'
    )
  })

  test('returns "(no messages)" when inbox is empty', async () => {
    saveConfig(accountConfig(), configPath)
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({ messages: [] }),
      })
    )
    await expect(getLastEmailSubject(configPath)).resolves.toBe('(no messages)')
  })

  test('returns "(no subject)" when message has no Subject header', async () => {
    saveConfig(accountConfig(), configPath)
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
    await expect(getLastEmailSubject(configPath)).resolves.toBe('(no subject)')
  })

  test('refreshes token when expired and fetches last email', async () => {
    saveConfig(accountConfig({ expiresAt: Date.now() - 1000 }), configPath)
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

    await expect(getLastEmailSubject(configPath)).resolves.toBe('Refreshed')
    expect(
      fetchMock.mock.calls.filter((c) => (c[0] as string).includes('/token'))
    ).toHaveLength(1)
  })

  test('throws when token refresh returns invalid_grant', async () => {
    saveConfig(accountConfig({ expiresAt: Date.now() - 1000 }), configPath)
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
