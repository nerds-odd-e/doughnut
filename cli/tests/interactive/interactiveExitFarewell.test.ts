import './interactiveTestMocks.js'
import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import { resetRecallStateForTesting } from '../../src/interactive.js'
import {
  createMockTTYStdin,
  endTTYSession,
  pushTTYCommandBytes,
  pushTTYCommandEnter,
  spyConsoleLogNoop,
  spyExitNoop,
  spyStdoutWriteTrue,
  startInteractiveOnStdin,
  tick,
  ttyOutput,
  type TTYStdin,
} from './interactiveTestHelpers.js'

describe('interactive exit: Bye. in user-visible output', () => {
  describe('TTY (PTY past transcript)', () => {
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

    afterEach(async () => {
      await endTTYSession(stdin)
    })

    test('after Enter on exit, past messages include Bye.', async () => {
      pushTTYCommandBytes(stdin, 'exit')
      await tick()
      writeSpy.mockClear()
      pushTTYCommandEnter(stdin)
      await tick()
      await vi.waitFor(() => expect(exitSpy).toHaveBeenCalledWith(0))
      expect(ttyOutput(writeSpy)).toContain('Bye.')
    })
  })
})
