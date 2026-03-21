import './interactiveTestMocks.js'
import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import { resetRecallStateForTesting } from '../../src/interactive.js'
import {
  GREY_BG_PAST_INPUT,
  runPipedInteractive,
} from './interactiveTestHelpers.js'

describe('interactive CLI (e2e style)', () => {
  let logSpy: ReturnType<typeof vi.spyOn>
  let exitSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    resetRecallStateForTesting()
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
    await runPipedInteractive('hello\nexit\n')
    expect(logSpy).toHaveBeenCalledWith('Not supported')
    expect(exitSpy).toHaveBeenCalledWith(0)
  })

  test('shows past input in grey background box', async () => {
    await runPipedInteractive('hello\nexit\n')
    const pastInputCall = logSpy.mock.calls.find(
      (c) => typeof c[0] === 'string' && c[0].includes(GREY_BG_PAST_INPUT)
    )
    expect(pastInputCall).toBeDefined()
    expect(pastInputCall![0]).toContain('hello')
    expect(pastInputCall![0]).not.toContain('→')
  })

  test.each([
    ['exit\n', 'exit'],
    ['/exit\n', '/exit'],
  ])('%s line exits the CLI', async (input) => {
    await runPipedInteractive(input)
    expect(exitSpy).toHaveBeenCalledWith(0)
    expect(logSpy.mock.calls.some((c) => c[0] === 'Bye.')).toBe(true)
  })

  test('each line triggers separate response', async () => {
    await runPipedInteractive('line1\nline2\nexit\n')
    const notSupportedCalls = logSpy.mock.calls.filter(
      (c) => c[0] === 'Not supported'
    )
    expect(notSupportedCalls).toHaveLength(2)
  })

  test('box uses full terminal width in piped mode', async () => {
    Object.defineProperty(process.stdout, 'columns', {
      value: 100,
      writable: true,
      configurable: true,
    })
    await runPipedInteractive('exit\n')
    const boxCall = logSpy.mock.calls.find(
      (c) => typeof c[0] === 'string' && c[0].includes('┌')
    )
    expect(boxCall).toBeDefined()
    const topBorder = boxCall![0].split('\n')[0]
    expect(topBorder.length).toBe(100)
  })

  test('shows version, box with placeholder and prompt', async () => {
    await runPipedInteractive('exit\n')
    const output = logSpy.mock.calls.flat().join('\n')
    expect(output).toContain('doughnut')
    expect(output).toContain('→')
    expect(output).toContain('`exit` to quit.')
    expect(output).toContain('┌')
    expect(output).toContain('┘')
  })

  test('shows "  / commands" in the Current guidance when user has not typed /', async () => {
    await runPipedInteractive('exit\n')
    const output = logSpy.mock.calls.flat().join('\n')
    expect(output).toContain('  / commands')
    expect(output).toContain('\x1b[90m')
  })
})
