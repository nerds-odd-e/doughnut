import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import { mockRecallStatus } from './interactiveRecallMockAccess.js'
import { resetRecallStateForTesting } from '../../src/interactive.js'
import { INTERACTIVE_INPUT_READY_OSC } from '../../src/renderer.js'
import {
  endTTYSession,
  pressKey,
  startTTYSessionWithoutRecallReset,
  submitTTYCommand,
  tick,
  ttyOutput,
  typeString,
  type TTYStdin,
} from './interactiveTestHelpers.js'

describe('TTY: line draft must not survive interactive fetch wait', () => {
  let writeSpy: ReturnType<typeof vi.spyOn>
  let stdin: TTYStdin

  beforeEach(async () => {
    resetRecallStateForTesting()
    mockRecallStatus.mockReset()
    mockRecallStatus.mockImplementation(
      () =>
        new Promise((resolve) =>
          setTimeout(() => resolve('0 notes to recall today'), 120)
        )
    )
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

  test('after /recall-status completes, typed keys during grey wait must not leave a draft (ready OSC after harmless edit)', async () => {
    await submitTTYCommand(stdin, '/recall-status')
    typeString(stdin, 'leaked-draft-xyz')
    await tick()

    await vi.waitFor(() =>
      expect(ttyOutput(writeSpy)).toContain('0 notes to recall today')
    )

    writeSpy.mockClear()
    typeString(stdin, 'x')
    await tick()
    pressKey(stdin, 'backspace')
    await tick()

    expect(ttyOutput(writeSpy)).toContain(INTERACTIVE_INPUT_READY_OSC)
  })
})
