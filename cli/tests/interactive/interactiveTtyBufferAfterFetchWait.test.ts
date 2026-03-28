import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import { mockRecallNext } from './interactiveRecallMockAccess.js'
import { resetRecallStateForTesting } from '../../src/interactive.js'
import { stripAnsi } from '../../src/renderer.js'
import {
  endTTYSession,
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
    mockRecallNext.mockReset()
    mockRecallNext.mockImplementation(
      () =>
        new Promise((resolve) =>
          setTimeout(
            () =>
              resolve({
                type: 'none',
                message: '0 notes to recall today',
              } as const),
            120
          )
        )
    )
    ;({ stdin, writeSpy } = await startTTYSessionWithoutRecallReset())
  })

  afterEach(async () => {
    await endTTYSession(stdin)
    const actual = await vi.importActual<
      typeof import('../../src/commands/recall.js')
    >('../../src/commands/recall.js')
    mockRecallNext.mockImplementation((due, signal) =>
      actual.recallNext(due, signal)
    )
  })

  test('after /recall completes, typed keys during grey wait must not leave a visible draft', async () => {
    await submitTTYCommand(stdin, '/recall')
    typeString(stdin, 'leaked-draft-xyz')
    await tick()

    await vi.waitFor(() =>
      expect(ttyOutput(writeSpy)).toContain('Load more from next 3 days')
    )

    expect(stripAnsi(ttyOutput(writeSpy))).not.toContain('leaked-draft')
  })
})
