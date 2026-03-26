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

  test('-c is rejected with exit 1', async () => {
    await run(['-c', 'hello'])
    await new Promise((r) => setImmediate(r))
    expect(errorSpy).toHaveBeenCalledWith(
      'doughnut: -c is not supported. Run `doughnut` in a terminal for the interactive shell.'
    )
    expect(exitSpy).toHaveBeenCalledWith(1)
  })

  test('-c= form is rejected with exit 1', async () => {
    await run(['-c=hello'])
    await new Promise((r) => setImmediate(r))
    expect(exitSpy).toHaveBeenCalledWith(1)
  })

  test('help subcommand is rejected with exit 1', async () => {
    await run(['help'])
    await new Promise((r) => setImmediate(r))
    expect(errorSpy).toHaveBeenCalledWith(
      'doughnut: there is no help subcommand. Run `doughnut` in a terminal, then type /help.'
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
      await run([])
      await new Promise((r) => setImmediate(r))
      expect(errorSpy).toHaveBeenCalledWith(
        'doughnut: interactive mode requires a terminal. For scripts, use `doughnut version` or `doughnut update`.'
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
