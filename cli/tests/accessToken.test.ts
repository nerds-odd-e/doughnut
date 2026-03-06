import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import * as fs from 'node:fs'
import * as os from 'node:os'
import * as path from 'node:path'
import { addAccessToken, listAccessTokens } from '../src/accessToken.js'

function createTempDir(): string {
  return fs.mkdtempSync(path.join(os.tmpdir(), 'doughnut-access-token-test-'))
}

describe('addAccessToken', () => {
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

  test('saves token with hardcoded label and prints confirmation', async () => {
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
    await addAccessToken('my-secret-token')
    expect(logSpy).toHaveBeenCalledWith('Token added')
    logSpy.mockRestore()

    const tokens = listAccessTokens()
    expect(tokens).toHaveLength(1)
    expect(tokens[0].label).toBe('test')
    expect(tokens[0].token).toBe('my-secret-token')
  })

  test('appends multiple tokens', async () => {
    vi.spyOn(console, 'log').mockImplementation(() => undefined)
    await addAccessToken('token-1')
    await addAccessToken('token-2')
    vi.restoreAllMocks()

    const tokens = listAccessTokens()
    expect(tokens).toHaveLength(2)
    expect(tokens[0].token).toBe('token-1')
    expect(tokens[1].token).toBe('token-2')
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
    expect(tokens[0].label).toBe('My Token')
    expect(tokens[0].token).toBe('abc-123')
  })
})
