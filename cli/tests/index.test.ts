import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import { formatVersionOutput } from '../src/version.js'
import { run } from '../src/index.js'

describe('CLI', () => {
  test('version command outputs doughnut prefix with version', () => {
    const output = formatVersionOutput()
    expect(output).toMatch(/^doughnut \d+\.\d+\.\d+$/)
  })

  test('help subcommand prints help and does not start interactive mode', async () => {
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
    await run(['help'])
    expect(logSpy).toHaveBeenCalled()
    const output = logSpy.mock.calls.flat().join('\n')
    expect(output).toContain('Subcommands')
    expect(output).toContain('version')
    expect(output).toContain('update')
    expect(output).toContain('/add gmail')
    expect(output).toContain('/last email')
    logSpy.mockRestore()
  })
})

describe('run with -c option', () => {
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

  test('-c "hello" prints version and "Not supported", then exits 0', async () => {
    run(['-c', 'hello'])
    await new Promise((r) => setImmediate(r))
    const output = logSpy.mock.calls.flat().join('\n')
    expect(output).toMatch(/doughnut \d+\.\d+\.\d+/)
    expect(output).toContain('Not supported')
    expect(exitSpy).toHaveBeenCalledWith(0)
  })

  test('-c "exit" prints version and exits 0', async () => {
    run(['-c', 'exit'])
    await new Promise((r) => setImmediate(r))
    const output = logSpy.mock.calls.flat().join('\n')
    expect(output).toMatch(/doughnut \d+\.\d+\.\d+/)
    expect(exitSpy).toHaveBeenCalledWith(0)
  })

  test('-c "/exit" prints version and exits 0', async () => {
    run(['-c', '/exit'])
    await new Promise((r) => setImmediate(r))
    const output = logSpy.mock.calls.flat().join('\n')
    expect(output).toMatch(/doughnut \d+\.\d+\.\d+/)
    expect(exitSpy).toHaveBeenCalledWith(0)
  })

  test('-c "/recall" rejects with message and exits 1 (interactive-only command)', async () => {
    run(['-c', '/recall'])
    await new Promise((r) => setImmediate(r))
    expect(errorSpy).toHaveBeenCalledWith(
      'This command requires interactive mode. Run `doughnut` without -c.'
    )
    expect(exitSpy).toHaveBeenCalledWith(1)
  })

  test('-c with no value prints error and exits 1', async () => {
    run(['-c'])
    await new Promise((r) => setImmediate(r))
    expect(errorSpy).toHaveBeenCalledWith('doughnut: -c requires an argument')
    expect(exitSpy).toHaveBeenCalledWith(1)
  })

  test('-c=value form works', async () => {
    run(['-c=hello'])
    await new Promise((r) => setImmediate(r))
    const output = logSpy.mock.calls.flat().join('\n')
    expect(output).toMatch(/doughnut \d+\.\d+\.\d+/)
    expect(output).toContain('Not supported')
    expect(exitSpy).toHaveBeenCalledWith(0)
  })
})
