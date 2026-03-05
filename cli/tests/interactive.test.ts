import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import { Readable } from 'node:stream'
import {
  buildBoxLines,
  processInput,
  renderBox,
  renderPastInput,
  runInteractive,
  visibleLength,
} from '../src/interactive.js'

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

describe('visibleLength', () => {
  test('returns length without ANSI codes', () => {
    expect(visibleLength('hello')).toBe(5)
    expect(visibleLength('\x1b[90mhello\x1b[0m')).toBe(5)
    expect(visibleLength('→ \x1b[90m`exit` to quit.\x1b[0m')).toBe(17)
  })
})

describe('renderBox', () => {
  test('renders a single-line box', () => {
    const result = renderBox(['hello'], 20)
    const lines = result.split('\n')
    expect(lines).toHaveLength(3)
    expect(lines[0]).toBe('┌──────────────────┐')
    expect(lines[1]).toContain('hello')
    expect(lines[1]).toMatch(/^│.*│$/)
    expect(lines[2]).toBe('└──────────────────┘')
  })

  test('renders a multi-line box (box expands with newlines)', () => {
    const result = renderBox(['line 1', 'line 2', 'line 3'], 20)
    const lines = result.split('\n')
    expect(lines).toHaveLength(5)
    expect(lines[0]).toBe('┌──────────────────┐')
    expect(lines[1]).toContain('line 1')
    expect(lines[2]).toContain('line 2')
    expect(lines[3]).toContain('line 3')
    expect(lines[4]).toBe('└──────────────────┘')
  })

  test('pads short lines to fill the box width', () => {
    const result = renderBox(['hi'], 20)
    const lines = result.split('\n')
    expect(lines[1]).toBe('│ hi               │')
    expect(lines[1].length).toBe(20)
  })

  test('pads correctly when line contains ANSI codes', () => {
    const grey = '\x1b[90m'
    const reset = '\x1b[0m'
    const result = renderBox([`${grey}hi${reset}`], 20)
    const lines = result.split('\n')
    expect(visibleLength(lines[1])).toBe(20)
    expect(lines[1]).toContain('hi')
  })
})

describe('buildBoxLines', () => {
  test('empty buffer shows placeholder with prompt', () => {
    const lines = buildBoxLines('', 40)
    expect(lines).toHaveLength(1)
    expect(lines[0]).toContain('→')
    expect(lines[0]).toContain('`exit` to quit.')
  })

  test('single-line buffer shows prompt + text', () => {
    const lines = buildBoxLines('hello', 40)
    expect(lines).toHaveLength(1)
    expect(lines[0]).toBe('→ hello')
  })

  test('multi-line buffer produces one line per newline', () => {
    const lines = buildBoxLines('line1\nline2\nline3', 40)
    expect(lines).toHaveLength(3)
    expect(lines[0]).toBe('→ line1')
    expect(lines[1]).toBe('  line2')
    expect(lines[2]).toBe('  line3')
  })
})

describe('renderPastInput', () => {
  test('renders text in grey background with no border', () => {
    const result = renderPastInput('hello', 30)
    expect(result).not.toContain('┌')
    expect(result).not.toContain('│')
    expect(result).toContain('hello')
    expect(result).toContain('\x1b[48;5;236m')
  })

  test('has empty-line vertical padding inside the box', () => {
    const result = renderPastInput('hello', 30)
    const lines = result.split('\n')
    expect(visibleLength(lines[1])).toBe(28)
    expect(lines[1].replace(/\x1b\[[0-9;]*m/g, '').trim()).toBe('')
    const lastBgLine = lines[lines.length - 2]
    expect(lastBgLine.replace(/\x1b\[[0-9;]*m/g, '').trim()).toBe('')
  })

  test('has empty-line vertical margin outside the box', () => {
    const result = renderPastInput('hello', 30)
    const lines = result.split('\n')
    expect(lines[0]).toBe('')
    expect(lines[lines.length - 1]).toBe('')
  })

  test('does not include prompt arrow', () => {
    const result = renderPastInput('hello', 30)
    expect(result).not.toContain('→')
  })

  test('handles multi-line input', () => {
    const result = renderPastInput('line1\nline2', 30)
    expect(result).toContain('line1')
    expect(result).toContain('line2')
    const lines = result.split('\n')
    const bgLines = lines.filter((l) => l.includes('\x1b[48;5;236m'))
    expect(bgLines).toHaveLength(4)
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

  test('shows past input in grey background box', async () => {
    const stdin = createMockStdin('hello\nexit\n')
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))
    const pastInputCall = logSpy.mock.calls.find(
      (c) => typeof c[0] === 'string' && c[0].includes('\x1b[48;5;236m')
    )
    expect(pastInputCall).toBeDefined()
    expect(pastInputCall![0]).toContain('hello')
    expect(pastInputCall![0]).not.toContain('→')
  })

  test('exit command exits the CLI', async () => {
    const stdin = createMockStdin('exit\n')
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))
    expect(exitSpy).toHaveBeenCalledWith(0)
  })

  test('each line triggers separate response', async () => {
    const stdin = createMockStdin('line1\nline2\nexit\n')
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))
    const notSupportedCalls = logSpy.mock.calls.filter(
      (c) => c[0] === 'Not supported'
    )
    expect(notSupportedCalls).toHaveLength(2)
  })

  test('shows version, box with placeholder and prompt', async () => {
    const stdin = createMockStdin('exit\n')
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))
    const output = logSpy.mock.calls.flat().join('\n')
    expect(output).toContain('doughnut')
    expect(output).toContain('→')
    expect(output).toContain('`exit` to quit.')
    expect(output).toContain('┌')
    expect(output).toContain('┘')
  })
})
