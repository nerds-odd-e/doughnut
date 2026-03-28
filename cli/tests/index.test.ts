import { Readable } from 'node:stream'
import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import { runInteractive } from '../src/interactive.js'
import { run } from '../src/run.js'
import { formatVersionOutput } from '../src/commands/version.js'

class ProcessExitForTest extends Error {
  readonly code: number | undefined
  constructor(code?: number) {
    super(`process.exit(${code})`)
    this.name = 'ProcessExitForTest'
    this.code = code
  }
}

describe('run entry routing', () => {
  let logSpy: ReturnType<typeof vi.spyOn>
  let errorSpy: ReturnType<typeof vi.spyOn>
  let exitSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
    errorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined)
    exitSpy = vi.spyOn(process, 'exit').mockImplementation(((code?: number) => {
      throw new ProcessExitForTest(code)
    }) as typeof process.exit)
  })

  afterEach(() => {
    logSpy.mockRestore()
    errorSpy.mockRestore()
    exitSpy.mockRestore()
  })

  test('invalid option and exit 1 for disallowed argv', async () => {
    for (const argv of [['-c', 'hello'], ['-c=hello']] as const) {
      errorSpy.mockClear()
      exitSpy.mockClear()
      await expect(run([...argv])).rejects.toThrow(ProcessExitForTest)
      await new Promise((r) => setImmediate(r))
      expect(errorSpy).toHaveBeenCalledWith('doughnut: invalid option')
      expect(exitSpy).toHaveBeenCalledWith(1)
    }
  })

  test('help subcommand is rejected with exit 1', async () => {
    await expect(run(['help'])).rejects.toThrow(ProcessExitForTest)
    await new Promise((r) => setImmediate(r))
    expect(errorSpy).toHaveBeenCalledWith(
      'doughnut: not a terminal (use version or update)'
    )
    expect(exitSpy).toHaveBeenCalledWith(1)
  })

  test('interactive path without TTY exits 1', async () => {
    const orig = process.stdin.isTTY
    Object.defineProperty(process.stdin, 'isTTY', {
      value: false,
      configurable: true,
      writable: true,
    })
    try {
      await expect(run([])).rejects.toThrow(ProcessExitForTest)
      await new Promise((r) => setImmediate(r))
      expect(errorSpy).toHaveBeenCalledWith(
        'doughnut: not a terminal (use version or update)'
      )
      expect(exitSpy).toHaveBeenCalledWith(1)
    } finally {
      Object.defineProperty(process.stdin, 'isTTY', {
        value: orig,
        configurable: true,
        writable: true,
      })
    }
  })

  test('version without TTY still prints version', async () => {
    const orig = process.stdin.isTTY
    Object.defineProperty(process.stdin, 'isTTY', {
      value: false,
      configurable: true,
      writable: true,
    })
    try {
      await run(['version'])
      await new Promise((r) => setImmediate(r))
      expect(logSpy).toHaveBeenCalledWith(formatVersionOutput())
      expect(exitSpy).not.toHaveBeenCalled()
    } finally {
      Object.defineProperty(process.stdin, 'isTTY', {
        value: orig,
        configurable: true,
        writable: true,
      })
    }
  })

  test('TTY interactive prints version then waits for stdin to end', async () => {
    const stdin = new Readable({
      read() {
        // no-op: test drives stdin via push()
      },
    }) as Readable & { isTTY: boolean }
    stdin.isTTY = true

    const session = runInteractive(stdin)
    await new Promise((r) => setImmediate(r))
    expect(logSpy).toHaveBeenCalledWith(formatVersionOutput())
    expect(exitSpy).not.toHaveBeenCalled()

    stdin.push(null)
    await session
    expect(exitSpy).not.toHaveBeenCalled()
  })
})
