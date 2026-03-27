import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import { mockRecallStatus } from './interactiveRecallMockAccess.js'
import { resetRecallStateForTesting } from '../../src/interactive.js'
import { stripAnsi } from '../../src/renderer.js'
import {
  endTTYSession,
  pushTTYCommandBytes,
  pushTTYCommandKey,
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
    const actual = await vi.importActual<
      typeof import('../../src/commands/recall.js')
    >('../../src/commands/recall.js')
    mockRecallStatus.mockImplementation((signal?: AbortSignal) =>
      actual.recallStatus(signal)
    )
  })

  test('after /recall-status completes, typed keys during grey wait must not leave a visible draft', async () => {
    await submitTTYCommand(stdin, '/recall-status')
    typeString(stdin, 'leaked-draft-xyz')
    await tick()

    await vi.waitFor(() =>
      expect(ttyOutput(writeSpy)).toContain('0 notes to recall today')
    )

    writeSpy.mockClear()
    pushTTYCommandBytes(stdin, 'x')
    await tick()
    pushTTYCommandKey(stdin, 'backspace')
    await tick()

    const visible = stripAnsi(ttyOutput(writeSpy))
    expect(visible).toContain('→ ')
  })
})
