import './interactiveTestMocks.js'
import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import { resetRecallStateForTesting } from '../../src/interactive.js'
import {
  createMockTTYStdin,
  endTTYSession,
  pushTTYCommandBytes,
  pushTTYCommandEnter,
  runPipedInteractive,
  spyConsoleLogNoop,
  spyExitNoop,
  spyStdoutWriteTrue,
  startInteractiveOnStdin,
  tick,
  ttyOutput,
  type TTYStdin,
} from './interactiveTestHelpers.js'

describe('interactive exit: Bye. in user-visible output', () => {
  describe('TTY (PTY scrollback)', () => {
    let writeSpy: ReturnType<typeof vi.spyOn>
    let exitSpy: ReturnType<typeof vi.spyOn>
    let stdin: TTYStdin

    beforeEach(async () => {
      resetRecallStateForTesting()
      spyConsoleLogNoop()
      writeSpy = spyStdoutWriteTrue()
      exitSpy = spyExitNoop()
      stdin = createMockTTYStdin()
      await startInteractiveOnStdin(stdin)
    })

    afterEach(() => {
      endTTYSession(stdin)
    })

    test('after Enter on exit, scrollback includes Bye.', async () => {
      pushTTYCommandBytes(stdin, 'exit')
      await tick()
      writeSpy.mockClear()
      pushTTYCommandEnter(stdin)
      await tick()
      await vi.waitFor(() => expect(exitSpy).toHaveBeenCalledWith(0))
      expect(ttyOutput(writeSpy)).toContain('Bye.')
    })
  })

  describe('piped stdin (non-TTY interactive)', () => {
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

    test('exit line prints Bye. before process exit', async () => {
      await runPipedInteractive('exit\n')
      await vi.waitFor(() => expect(exitSpy).toHaveBeenCalledWith(0))
      expect(logSpy.mock.calls.some((c) => c[0] === 'Bye.')).toBe(true)
    })
  })
})
