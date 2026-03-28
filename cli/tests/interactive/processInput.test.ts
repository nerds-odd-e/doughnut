import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  processInput,
  resetRecallStateForTesting,
} from '../../src/interactive.js'

describe('processInput', () => {
  let logSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    resetRecallStateForTesting()
    logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
  })

  afterEach(() => {
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

  test('returns false and logs "Not supported" for non-exit input', async () => {
    expect(await processInput('hello')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith('Not supported')
    logSpy.mockClear()
    expect(await processInput('/list-access-token')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith('Not supported')
  })
})
