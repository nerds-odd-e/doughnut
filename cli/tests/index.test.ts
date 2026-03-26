import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import { run } from '../src/run.js'
import { formatVersionOutput } from '../src/version.js'

describe('CLI', () => {
  test('version command outputs doughnut prefix with version', () => {
    const output = formatVersionOutput()
    expect(output).toMatch(/^doughnut \d+\.\d+\.\d+$/)
  })
})

describe('run entry routing', () => {
  let logSpy: ReturnType<typeof vi.spyOn>
  let errorSpy: ReturnType<typeof vi.spyOn>
  let exitSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
    errorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined)
    exitSpy = vi
      .spyOn(process, 'exit')
      .mockImplementation((() => undefined) as unknown as typeof process.exit)
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
      await run([...argv])
      await new Promise((r) => setImmediate(r))
      expect(errorSpy).toHaveBeenCalledWith('doughnut: invalid option')
      expect(exitSpy).toHaveBeenCalledWith(1)
    }
  })

  test('help subcommand is rejected with exit 1', async () => {
    await run(['help'])
    await new Promise((r) => setImmediate(r))
    expect(errorSpy).toHaveBeenCalledWith('doughnut: use /help in the shell')
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
      await run([])
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
})
