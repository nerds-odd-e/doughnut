import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import * as fs from 'node:fs'
import * as os from 'node:os'
import * as path from 'node:path'
import { UserController, type UserToken } from 'doughnut-api'

vi.mock('doughnut-api', () => ({
  getApiConfig: () => ({ apiBaseUrl: 'http://localhost:9081' }),
  configureClient: vi.fn(),
  UserController: {
    getTokenInfo: vi.fn(),
    revokeToken: vi.fn(),
    generateToken: vi.fn(),
  },
}))

import {
  addAccessToken,
  createAccessToken,
  formatTokenLines,
  getDefaultTokenLabel,
  listAccessTokens,
  removeAccessToken,
  removeAccessTokenCompletely,
  setDefaultTokenLabel,
} from '../src/commands/accessToken.js'
import { buildTokenListLines, visibleLength } from '../src/renderer.js'

function createTempDir(): string {
  return fs.mkdtempSync(path.join(os.tmpdir(), 'doughnut-access-token-test-'))
}

function mockGetTokenInfo(label: UserToken['label'], id = 1) {
  return vi
    .mocked(UserController.getTokenInfo)
    .mockResolvedValueOnce({ data: { id, label } } as never)
}

describe('access tokens (persisted file + API)', () => {
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

  describe('adding a token', () => {
    test('validates token via API and saves with returned label', async () => {
      mockGetTokenInfo('My Token')

      await addAccessToken('my-secret-token')

      const tokens = listAccessTokens()
      expect(tokens).toHaveLength(1)
      expect(tokens[0]!.label).toBe('My Token')
      expect(tokens[0]!.token).toBe('my-secret-token')
    })

    test('appends multiple tokens', async () => {
      mockGetTokenInfo('First', 1)
      mockGetTokenInfo('Second', 2)

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

    test('throws when token validation fails (API error)', async () => {
      vi.mocked(UserController.getTokenInfo).mockRejectedValue({
        status: 401,
      } as never)

      await expect(addAccessToken('bad-token')).rejects.toThrow(
        'Access token is invalid or expired'
      )
      expect(listAccessTokens()).toEqual([])
    })

    test('throws ApiError message when server returns OPENAI_NOT_AVAILABLE body', async () => {
      vi.mocked(UserController.getTokenInfo).mockRejectedValue({
        errorType: 'OPENAI_NOT_AVAILABLE',
        message: 'OpenAI is not available (no API key configured).',
        errors: {},
      } as never)

      await expect(addAccessToken('any-token')).rejects.toThrow(
        'OpenAI is not available (no API key configured).'
      )
      expect(listAccessTokens()).toEqual([])
    })

    test('throws HTTP 502 wording when status present without ApiError body', async () => {
      vi.mocked(UserController.getTokenInfo).mockRejectedValue({
        status: 502,
      } as never)

      await expect(addAccessToken('any-token')).rejects.toThrow(
        'A dependency service failed (HTTP 502)'
      )
      expect(listAccessTokens()).toEqual([])
    })

    test('throws a no-permission message when token validation gets 403', async () => {
      vi.mocked(UserController.getTokenInfo).mockRejectedValue({
        status: 403,
      } as never)

      await expect(addAccessToken('any-token')).rejects.toThrow(
        'Access token does not have permission'
      )
      expect(listAccessTokens()).toEqual([])
    })

    test('throws when adding duplicate token', async () => {
      mockGetTokenInfo('My Token')
      mockGetTokenInfo('My Token') // second call still validates via API

      await addAccessToken('my-secret-token')

      await expect(addAccessToken('my-secret-token')).rejects.toThrow(
        'Token already added.'
      )
      expect(listAccessTokens()).toHaveLength(1)
    })
  })

  describe('stored token list', () => {
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

  describe('which token is default', () => {
    test('returns undefined when no tokens exist', () => {
      expect(getDefaultTokenLabel()).toBeUndefined()
    })

    test('returns first token label by default', async () => {
      mockGetTokenInfo('First', 1)
      mockGetTokenInfo('Second', 2)

      await addAccessToken('token-1')
      await addAccessToken('token-2')

      expect(getDefaultTokenLabel()).toBe('First')
    })

    test('returns explicitly set default', async () => {
      mockGetTokenInfo('First', 1)
      mockGetTokenInfo('Second', 2)

      await addAccessToken('token-1')
      await addAccessToken('token-2')
      setDefaultTokenLabel('Second')

      expect(getDefaultTokenLabel()).toBe('Second')
    })

    test('falls back to first token if saved default no longer exists', async () => {
      mockGetTokenInfo('Only')

      await addAccessToken('token-1')
      setDefaultTokenLabel('Deleted')

      expect(getDefaultTokenLabel()).toBe('Only')
    })
  })

  describe('removing a token locally', () => {
    test('removes token by label and returns true', async () => {
      mockGetTokenInfo('First', 1)
      mockGetTokenInfo('Second', 2)

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
      mockGetTokenInfo('First', 1)
      mockGetTokenInfo('Second', 2)

      await addAccessToken('token-1')
      await addAccessToken('token-2')
      setDefaultTokenLabel('First')
      expect(getDefaultTokenLabel()).toBe('First')

      removeAccessToken('First')
      expect(getDefaultTokenLabel()).toBe('Second')
    })
  })

  describe('revoke on server and remove locally', () => {
    test('revokes token on server and removes locally', async () => {
      mockGetTokenInfo('MyToken')
      vi.mocked(UserController.revokeToken).mockResolvedValueOnce({
        data: undefined,
      } as never)

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

    test('rethrows AbortError from revokeToken', async () => {
      mockGetTokenInfo('MyToken')
      await addAccessToken('secret-token')
      vi.mocked(UserController.revokeToken).mockRejectedValueOnce(
        new DOMException('Aborted', 'AbortError')
      )
      await expect(
        removeAccessTokenCompletely('MyToken')
      ).rejects.toMatchObject({ name: 'AbortError' })
    })

    test('throws when server is not available', async () => {
      mockGetTokenInfo('MyToken')
      vi.mocked(UserController.revokeToken).mockRejectedValueOnce(
        new TypeError('fetch failed')
      )

      await addAccessToken('secret-token')
      await expect(removeAccessTokenCompletely('MyToken')).rejects.toThrow(
        'Doughnut service is not available'
      )
      expect(listAccessTokens()).toHaveLength(1)
    })
  })

  describe('creating a new token via API', () => {
    test('creates token on server using default token and saves locally', async () => {
      mockGetTokenInfo('Default Token')
      await addAccessToken('default-secret')

      vi.mocked(UserController.generateToken).mockResolvedValueOnce({
        data: { id: 2, token: 'new-secret', label: 'New Token' },
      } as never)

      await createAccessToken('New Token')

      const tokens = listAccessTokens()
      expect(tokens).toHaveLength(2)
      expect(tokens[1]!.label).toBe('New Token')
      expect(tokens[1]!.token).toBe('new-secret')
    })

    test('throws when no default token exists', async () => {
      await expect(createAccessToken('Some Token')).rejects.toThrow(
        'No default access token. Add one first with /add-access-token.'
      )
    })

    test('throws when service is not available', async () => {
      mockGetTokenInfo('Default Token')
      await addAccessToken('default-secret')

      vi.mocked(UserController.generateToken).mockRejectedValueOnce(
        new TypeError('fetch failed')
      )

      await expect(createAccessToken('New Token')).rejects.toThrow(
        'Doughnut service is not available'
      )
    })

    test('throws when generateToken fails', async () => {
      mockGetTokenInfo('Default Token')
      await addAccessToken('default-secret')

      vi.mocked(UserController.generateToken).mockRejectedValueOnce({
        status: 401,
      } as never)

      await expect(createAccessToken('New Token')).rejects.toThrow(
        'Access token is invalid or expired'
      )
    })
  })
})

describe('token list lines for display', () => {
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

describe('token list layout at terminal width', () => {
  test('with narrow width, each line ≤ width or ends with "..."', () => {
    const tokens = [
      { label: 'Short', token: 'a' },
      {
        label: 'A very long token label that exceeds terminal width',
        token: 'b',
      },
    ]
    const lines = buildTokenListLines(tokens, 'Short', 20, 0)
    expect(lines.length).toBeGreaterThan(0)
    for (const line of lines) {
      expect(visibleLength(line)).toBeLessThanOrEqual(20)
      if (visibleLength(line) === 20) {
        expect(line).toContain('...')
      }
    }
  })

  test('long label truncated with "..." at narrow width', () => {
    const tokens = [
      { label: 'ExtremelyLongTokenLabelThatWillBeTruncated', token: 'x' },
    ]
    const lines = buildTokenListLines(tokens, undefined, 25, 0)
    expect(lines).toHaveLength(1)
    expect(lines[0]).toContain('...')
    expect(visibleLength(lines[0])).toBeLessThanOrEqual(25)
  })

  test('with wide width, no truncation', () => {
    const tokens = [
      { label: 'Token A', token: 'a' },
      { label: 'Token B', token: 'b' },
    ]
    const lines = buildTokenListLines(tokens, 'Token A', 120, 0)
    expect(lines.length).toBeGreaterThan(0)
    expect(lines.some((l) => l.endsWith('...'))).toBe(false)
  })
})
