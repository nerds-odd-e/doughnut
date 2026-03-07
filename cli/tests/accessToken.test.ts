import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import * as fs from 'node:fs'
import * as os from 'node:os'
import * as path from 'node:path'
import { UserController } from '@generated/backend/sdk.gen'

vi.mock('@generated/backend/sdk.gen', () => ({
  UserController: {
    getTokenInfo: vi.fn(),
    revokeToken: vi.fn(),
  },
}))

vi.mock('doughnut-api', () => ({
  getApiConfig: () => ({ apiBaseUrl: 'http://localhost:9081' }),
  configureClient: vi.fn(),
}))

import {
  addAccessToken,
  formatTokenLines,
  getDefaultTokenLabel,
  listAccessTokens,
  removeAccessToken,
  removeAccessTokenCompletely,
  setDefaultTokenLabel,
} from '../src/accessToken.js'

function createTempDir(): string {
  return fs.mkdtempSync(path.join(os.tmpdir(), 'doughnut-access-token-test-'))
}

describe('addAccessToken', () => {
  let originalConfigDir: string | undefined

  beforeEach(() => {
    originalConfigDir = process.env.DOUGHNUT_CONFIG_DIR
    process.env.DOUGHNUT_CONFIG_DIR = createTempDir()
    vi.clearAllMocks()
  })

  afterEach(() => {
    if (originalConfigDir === undefined) {
      delete process.env.DOUGHNUT_CONFIG_DIR
    } else {
      process.env.DOUGHNUT_CONFIG_DIR = originalConfigDir
    }
  })

  test('validates token via API and saves with returned label', async () => {
    vi.mocked(UserController.getTokenInfo).mockResolvedValue({
      data: { id: 1, label: 'My Token' },
    } as never)
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)

    await addAccessToken('my-secret-token')

    expect(logSpy).toHaveBeenCalledWith('Token added')
    const tokens = listAccessTokens()
    expect(tokens).toHaveLength(1)
    expect(tokens[0]!.label).toBe('My Token')
    expect(tokens[0]!.token).toBe('my-secret-token')
  })

  test('appends multiple tokens', async () => {
    vi.mocked(UserController.getTokenInfo)
      .mockResolvedValueOnce({
        data: { id: 1, label: 'First' },
      } as never)
      .mockResolvedValueOnce({
        data: { id: 2, label: 'Second' },
      } as never)
    vi.spyOn(console, 'log').mockImplementation(() => undefined)

    await addAccessToken('token-1')
    await addAccessToken('token-2')

    const tokens = listAccessTokens()
    expect(tokens).toHaveLength(2)
    expect(tokens[0]!.label).toBe('First')
    expect(tokens[1]!.label).toBe('Second')
  })

  test('throws when service is not available', async () => {
    vi.mocked(UserController.getTokenInfo).mockRejectedValue(
      new TypeError('fetch failed')
    )

    await expect(addAccessToken('any-token')).rejects.toThrow(
      'Doughnut service is not available'
    )
    expect(listAccessTokens()).toEqual([])
  })

  test('throws when token is invalid', async () => {
    vi.mocked(UserController.getTokenInfo).mockResolvedValue({
      data: undefined,
      error: { status: 401 },
    } as never)

    await expect(addAccessToken('bad-token')).rejects.toThrow(
      'Token is invalid or expired.'
    )
    expect(listAccessTokens()).toEqual([])
  })
})

describe('listAccessTokens', () => {
  let originalConfigDir: string | undefined

  beforeEach(() => {
    originalConfigDir = process.env.DOUGHNUT_CONFIG_DIR
    process.env.DOUGHNUT_CONFIG_DIR = createTempDir()
  })

  afterEach(() => {
    if (originalConfigDir === undefined) {
      delete process.env.DOUGHNUT_CONFIG_DIR
    } else {
      process.env.DOUGHNUT_CONFIG_DIR = originalConfigDir
    }
  })

  test('returns empty array when no config file exists', () => {
    expect(listAccessTokens()).toEqual([])
  })

  test('returns tokens from config file', () => {
    const configPath = path.join(
      process.env.DOUGHNUT_CONFIG_DIR!,
      'access-tokens.json'
    )
    fs.writeFileSync(
      configPath,
      JSON.stringify({
        tokens: [{ label: 'My Token', token: 'abc-123' }],
      })
    )
    const tokens = listAccessTokens()
    expect(tokens).toHaveLength(1)
    expect(tokens[0]!.label).toBe('My Token')
    expect(tokens[0]!.token).toBe('abc-123')
  })
})

describe('getDefaultTokenLabel', () => {
  let originalConfigDir: string | undefined

  beforeEach(() => {
    originalConfigDir = process.env.DOUGHNUT_CONFIG_DIR
    process.env.DOUGHNUT_CONFIG_DIR = createTempDir()
  })

  afterEach(() => {
    if (originalConfigDir === undefined) {
      delete process.env.DOUGHNUT_CONFIG_DIR
    } else {
      process.env.DOUGHNUT_CONFIG_DIR = originalConfigDir
    }
  })

  test('returns undefined when no tokens exist', () => {
    expect(getDefaultTokenLabel()).toBeUndefined()
  })

  test('returns first token label by default', async () => {
    vi.mocked(UserController.getTokenInfo)
      .mockResolvedValueOnce({ data: { id: 1, label: 'First' } } as never)
      .mockResolvedValueOnce({ data: { id: 2, label: 'Second' } } as never)
    vi.spyOn(console, 'log').mockImplementation(() => undefined)

    await addAccessToken('token-1')
    await addAccessToken('token-2')

    expect(getDefaultTokenLabel()).toBe('First')
  })

  test('returns explicitly set default', async () => {
    vi.mocked(UserController.getTokenInfo)
      .mockResolvedValueOnce({ data: { id: 1, label: 'First' } } as never)
      .mockResolvedValueOnce({ data: { id: 2, label: 'Second' } } as never)
    vi.spyOn(console, 'log').mockImplementation(() => undefined)

    await addAccessToken('token-1')
    await addAccessToken('token-2')
    setDefaultTokenLabel('Second')

    expect(getDefaultTokenLabel()).toBe('Second')
  })

  test('falls back to first token if saved default no longer exists', async () => {
    vi.mocked(UserController.getTokenInfo).mockResolvedValueOnce({
      data: { id: 1, label: 'Only' },
    } as never)
    vi.spyOn(console, 'log').mockImplementation(() => undefined)

    await addAccessToken('token-1')
    setDefaultTokenLabel('Deleted')

    expect(getDefaultTokenLabel()).toBe('Only')
  })
})

describe('removeAccessToken', () => {
  let originalConfigDir: string | undefined

  beforeEach(() => {
    originalConfigDir = process.env.DOUGHNUT_CONFIG_DIR
    process.env.DOUGHNUT_CONFIG_DIR = createTempDir()
    vi.clearAllMocks()
  })

  afterEach(() => {
    if (originalConfigDir === undefined) {
      delete process.env.DOUGHNUT_CONFIG_DIR
    } else {
      process.env.DOUGHNUT_CONFIG_DIR = originalConfigDir
    }
  })

  test('removes token by label and returns true', async () => {
    vi.mocked(UserController.getTokenInfo)
      .mockResolvedValueOnce({ data: { id: 1, label: 'First' } } as never)
      .mockResolvedValueOnce({ data: { id: 2, label: 'Second' } } as never)
    vi.spyOn(console, 'log').mockImplementation(() => undefined)

    await addAccessToken('token-1')
    await addAccessToken('token-2')

    expect(removeAccessToken('First')).toBe(true)
    const tokens = listAccessTokens()
    expect(tokens).toHaveLength(1)
    expect(tokens[0]!.label).toBe('Second')
  })

  test('returns false when label not found', () => {
    expect(removeAccessToken('Nonexistent')).toBe(false)
  })

  test('clears defaultLabel when removing the default token', async () => {
    vi.mocked(UserController.getTokenInfo)
      .mockResolvedValueOnce({ data: { id: 1, label: 'First' } } as never)
      .mockResolvedValueOnce({ data: { id: 2, label: 'Second' } } as never)
    vi.spyOn(console, 'log').mockImplementation(() => undefined)

    await addAccessToken('token-1')
    await addAccessToken('token-2')
    setDefaultTokenLabel('First')
    expect(getDefaultTokenLabel()).toBe('First')

    removeAccessToken('First')
    expect(getDefaultTokenLabel()).toBe('Second')
  })
})

describe('removeAccessTokenCompletely', () => {
  let originalConfigDir: string | undefined

  beforeEach(() => {
    originalConfigDir = process.env.DOUGHNUT_CONFIG_DIR
    process.env.DOUGHNUT_CONFIG_DIR = createTempDir()
    vi.clearAllMocks()
  })

  afterEach(() => {
    if (originalConfigDir === undefined) {
      delete process.env.DOUGHNUT_CONFIG_DIR
    } else {
      process.env.DOUGHNUT_CONFIG_DIR = originalConfigDir
    }
  })

  test('revokes token on server and removes locally', async () => {
    vi.mocked(UserController.getTokenInfo).mockResolvedValueOnce({
      data: { id: 1, label: 'MyToken' },
    } as never)
    vi.mocked(UserController.revokeToken).mockResolvedValueOnce({
      data: undefined,
    } as never)
    vi.spyOn(console, 'log').mockImplementation(() => undefined)

    await addAccessToken('secret-token')
    await removeAccessTokenCompletely('MyToken')

    expect(UserController.revokeToken).toHaveBeenCalled()
    expect(listAccessTokens()).toEqual([])
  })

  test('throws when label not found locally', async () => {
    await expect(removeAccessTokenCompletely('Missing')).rejects.toThrow(
      'Token "Missing" not found.'
    )
  })

  test('throws when server is not available', async () => {
    vi.mocked(UserController.getTokenInfo).mockResolvedValueOnce({
      data: { id: 1, label: 'MyToken' },
    } as never)
    vi.mocked(UserController.revokeToken).mockRejectedValueOnce(
      new TypeError('fetch failed')
    )
    vi.spyOn(console, 'log').mockImplementation(() => undefined)

    await addAccessToken('secret-token')
    await expect(removeAccessTokenCompletely('MyToken')).rejects.toThrow(
      'Doughnut service is not available'
    )
    expect(listAccessTokens()).toHaveLength(1)
  })
})

describe('formatTokenLines', () => {
  test('marks default token with ★', () => {
    const tokens = [
      { label: 'Token A', token: 'a' },
      { label: 'Token B', token: 'b' },
    ]
    const lines = formatTokenLines(tokens, 'Token A')
    expect(lines[0]).toBe('★ Token A')
    expect(lines[1]).toBe('  Token B')
  })

  test('marks no token when default is undefined', () => {
    const tokens = [{ label: 'Token A', token: 'a' }]
    const lines = formatTokenLines(tokens, undefined)
    expect(lines[0]).toBe('  Token A')
  })
})
