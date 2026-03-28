import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  processInput,
  resetRecallStateForTesting,
} from '../../src/interactive.js'
import { makeTempConfigDir, withConfigDir } from './interactiveTestHelpers.js'

describe('processInput', () => {
  let logSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    resetRecallStateForTesting()
    logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
  })

  afterEach(() => {
    vi.useRealTimers()
    logSpy.mockRestore()
  })

  test('returns true for exit commands', async () => {
    expect(await processInput('exit')).toBe(true)
    expect(await processInput('  exit  ')).toBe(true)
    expect(await processInput('/exit')).toBe(true)
    expect(await processInput('  /exit  ')).toBe(true)
  })

  test('returns false and does not log for empty input', async () => {
    expect(await processInput('')).toBe(false)
    expect(await processInput('   ')).toBe(false)
    expect(logSpy).not.toHaveBeenCalled()
  })

  test('returns false and logs "Not supported" for any other input', async () => {
    expect(await processInput('hello')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith('Not supported')
  })

  test('returns false and shows usage for /add-access-token without token', async () => {
    expect(await processInput('/add-access-token')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith('Usage: /add-access-token <token>')
    logSpy.mockClear()
    expect(await processInput('/add-access-token ')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith('Usage: /add-access-token <token>')
  })

  test('returns false and removes token for /remove-access-token with label', async () => {
    const configDir = makeTempConfigDir([
      { label: 'Token A', token: 'a' },
      { label: 'Token B', token: 'b' },
    ])
    const restore = withConfigDir(configDir)
    expect(await processInput('/remove-access-token Token A')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith('Token "Token A" removed.')
    restore()
  })

  test('returns false and shows not found for /remove-access-token with unknown label', async () => {
    expect(await processInput('/remove-access-token Unknown')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith('Token "Unknown" not found.')
  })

  test.each([
    ['/remove-access-token ', 'Usage: /remove-access-token <label>'],
    [
      '/remove-access-token-completely ',
      'Usage: /remove-access-token-completely <label>',
    ],
    ['/create-access-token ', 'Usage: /create-access-token <label>'],
  ] as const)('returns false and shows usage for %s', async (input, expected) => {
    expect(await processInput(input)).toBe(false)
    expect(logSpy).toHaveBeenCalledWith(expected)
  })

  test('returns false and shows error for /create-access-token with no default token', async () => {
    const configDir = makeTempConfigDir([])
    const restore = withConfigDir(configDir)
    expect(await processInput('/create-access-token My New Token')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith(
      'No default access token. Add one first with /add-access-token.'
    )
    restore()
  })

  test('/last email is not a supported slash command', async () => {
    expect(await processInput('/last email')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith('Not supported')
  })

  test('/recall is not supported', async () => {
    expect(await processInput('/recall')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith('Not supported')
  })

  test('/stop is not supported', async () => {
    expect(await processInput('/stop')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith('Not supported')
  })

  test('/help is not supported', async () => {
    expect(await processInput('/help')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith('Not supported')
  })
})
