import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import { resetRecallStateForTesting } from '../../src/interactive.js'
import {
  createMockTTYStdin,
  endTTYSession,
  GREY_BG_PAST_INPUT,
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

describe('TTY exit', () => {
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

  test('after Enter on exit, committed line appears as grey past user message (same as other submits)', async () => {
    pushTTYCommandBytes(stdin, 'exit')
    await tick()
    writeSpy.mockClear()
    pushTTYCommandEnter(stdin)
    await tick()
    await vi.waitFor(() => expect(exitSpy).toHaveBeenCalledWith(0))

    const outAfterSubmit = ttyOutput(writeSpy)
    expect(outAfterSubmit).toContain(GREY_BG_PAST_INPUT)
    expect(outAfterSubmit).toContain('exit')
    expect(
      outAfterSubmit,
      'must not repaint the live input box after quit'
    ).not.toContain('┌')
  })
})
