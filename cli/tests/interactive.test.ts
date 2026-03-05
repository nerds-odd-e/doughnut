import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import { Readable } from 'node:stream'
import { processInput, runInteractive } from '../src/interactive.js'

function createMockStdin(input: string): NodeJS.ReadableStream {
  const stream = new Readable({
    read() {
      // no-op: data is pushed manually
    },
  })
  stream.push(input)
  stream.push(null)
  return Object.assign(stream, { isTTY: false })
}

describe('processInput', () => {
  test('returns true for exit', () => {
    expect(processInput('exit')).toBe(true)
    expect(processInput('  exit  ')).toBe(true)
  })

  test('returns false and does not log for empty input', () => {
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
    expect(processInput('')).toBe(false)
    expect(processInput('   ')).toBe(false)
    expect(logSpy).not.toHaveBeenCalled()
    logSpy.mockRestore()
  })

  test('returns false and logs "Not supported" for any other input', () => {
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
    expect(processInput('hello')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith('Not supported')
    logSpy.mockRestore()
  })
})

describe('interactive CLI (e2e style)', () => {
  let logSpy: ReturnType<typeof vi.spyOn>
  let exitSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
    exitSpy = vi
      .spyOn(process, 'exit')
      .mockImplementation((() => undefined) as unknown as typeof process.exit)
  })

  afterEach(() => {
    logSpy.mockRestore()
    exitSpy.mockRestore()
  })

  test('responds "Not supported" to any input', async () => {
    const stdin = createMockStdin('hello\nexit\n')
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))
    expect(logSpy).toHaveBeenCalledWith('Not supported')
    expect(exitSpy).toHaveBeenCalledWith(0)
  })

  test('exit command exits the CLI', async () => {
    const stdin = createMockStdin('exit\n')
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))
    expect(exitSpy).toHaveBeenCalledWith(0)
  })

  test('each line triggers separate response (shift-enter adds newline in TTY)', async () => {
    const stdin = createMockStdin('line1\nline2\nexit\n')
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))
    const notSupportedCalls = logSpy.mock.calls.filter(
      (c) => c[0] === 'Not supported'
    )
    expect(notSupportedCalls).toHaveLength(2)
  })

  test('shows Cursor-like UI with version, box, and placeholder', async () => {
    const stdin = createMockStdin('exit\n')
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))
    const output = logSpy.mock.calls.flat().join('\n')
    expect(output).toContain('doughnut')
    expect(output).toContain('→')
    expect(output).toContain('Plan, search, build anything')
    expect(output).toContain('exit to quit')
  })
})
