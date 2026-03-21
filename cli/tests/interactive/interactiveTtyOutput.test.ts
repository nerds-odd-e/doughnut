import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import { mockRecallStatus } from './interactiveRecallMockAccess.js'
import { resetRecallStateForTesting } from '../../src/interactive.js'
import { CLEAR_SCREEN } from '../../src/renderer.js'
import {
  createMockTTYStdin,
  endTTYSession,
  GREY_BG_PAST_INPUT,
  pressEnter,
  spyConsoleLogNoop,
  spyExitNoop,
  spyStdoutWriteTrue,
  startInteractiveOnStdin,
  startTTYSessionWithoutRecallReset,
  submitTTYCommand,
  tick,
  ttyOutput,
  typeString,
  type TTYStdin,
} from './interactiveTestHelpers.js'

describe('TTY: normal command output must not full-screen clear', () => {
  let writeSpy: ReturnType<typeof vi.spyOn>
  let stdin: TTYStdin

  beforeEach(async () => {
    resetRecallStateForTesting()
    mockRecallStatus.mockReset()
    mockRecallStatus.mockResolvedValue('0 notes to recall today')
    ;({ stdin, writeSpy } = await startTTYSessionWithoutRecallReset())
  })

  afterEach(async () => {
    endTTYSession(stdin)
    const actual = await vi.importActual<typeof import('../../src/recall.js')>(
      '../../src/recall.js'
    )
    mockRecallStatus.mockImplementation((signal?: AbortSignal) =>
      actual.recallStatus(signal)
    )
  })

  test.each([
    ['/help', 'List available commands'],
    ['/recall-status', '0 notes to recall today'],
  ] as const)('after %s, stdout writes must not include CLEAR_SCREEN', async (cmd, waitForText) => {
    writeSpy.mockClear()
    await submitTTYCommand(stdin, cmd)
    await vi.waitFor(() => expect(ttyOutput(writeSpy)).toContain(waitForText))

    expect(
      ttyOutput(writeSpy),
      'Command output should append to history without full-screen clear; only /clear and resize should emit CLEAR_SCREEN'
    ).not.toContain(CLEAR_SCREEN)
  })
})

describe('TTY exit: no full-screen redraw', () => {
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

  test('after Enter on exit, stdout must not clear and repaint the full UI (cursor must not land in input box)', async () => {
    typeString(stdin, 'exit')
    await tick()
    writeSpy.mockClear()
    pressEnter(stdin)
    await tick()
    await vi.waitFor(() => expect(exitSpy).toHaveBeenCalledWith(0))

    const outAfterSubmit = ttyOutput(writeSpy)
    expect(
      outAfterSubmit,
      'Exit should not run doFullRedraw (clear screen + live region); that leaves the cursor in the input row after exit'
    ).not.toContain(CLEAR_SCREEN)
  })

  test('after Enter on exit, committed line appears as grey history input (same as other submits)', async () => {
    typeString(stdin, 'exit')
    await tick()
    writeSpy.mockClear()
    pressEnter(stdin)
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
