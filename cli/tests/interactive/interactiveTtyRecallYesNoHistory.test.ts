import { describe, test, expect, type vi, beforeEach, afterEach } from 'vitest'
import {
  mockMarkAsRecalled,
  mockRecallNext,
} from './interactiveRecallMockAccess.js'
import { resetRecallStateForTesting } from '../../src/interactive.js'
import {
  endTTYSession,
  expectTtyRecallYesNoReplyScrollback,
  pressEnter,
  startTTYSessionWithoutRecallReset,
  submitTTYCommand,
  tick,
  ttyOutput,
  typeString,
  type TTYStdin,
} from './interactiveTestHelpers.js'

describe('TTY: recall-session y/n answers (scrollback)', () => {
  let writeSpy: ReturnType<typeof vi.spyOn>
  let stdin: TTYStdin

  beforeEach(async () => {
    resetRecallStateForTesting()
    ;({ stdin, writeSpy } = await startTTYSessionWithoutRecallReset())
  })

  afterEach(() => {
    endTTYSession(stdin)
  })

  test('load more: n omits history input; session summary still logged', async () => {
    mockRecallNext.mockResolvedValue({ type: 'none', message: '0 notes' })
    await submitTTYCommand(stdin, '/recall')

    writeSpy.mockClear()
    typeString(stdin, 'n')
    await tick()
    pressEnter(stdin)
    await tick()

    const out = ttyOutput(writeSpy)
    expect(out).toContain('0 notes to recall today')
    expectTtyRecallYesNoReplyScrollback(writeSpy, 'n')
  })

  test('just-review: y omits history input; outcome still logged', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'just-review',
      memoryTrackerId: 42,
      notebookTitle: 'Notebook',
      title: 'Test note',
    })
    mockMarkAsRecalled.mockResolvedValue(undefined)
    await submitTTYCommand(stdin, '/recall')

    writeSpy.mockClear()
    typeString(stdin, 'y')
    await tick()
    pressEnter(stdin)
    await tick()

    const out = ttyOutput(writeSpy)
    expect(out).toContain('Recalled successfully')
    expect(mockMarkAsRecalled).toHaveBeenCalledWith(42, true)
    expectTtyRecallYesNoReplyScrollback(writeSpy, 'y')
  })

  test('just-review: n omits history input', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'just-review',
      memoryTrackerId: 7,
      notebookTitle: 'Notebook',
      title: 'Other',
    })
    mockMarkAsRecalled.mockResolvedValue(undefined)
    await submitTTYCommand(stdin, '/recall')

    writeSpy.mockClear()
    typeString(stdin, 'n')
    await tick()
    pressEnter(stdin)
    await tick()

    const out = ttyOutput(writeSpy)
    expect(out).toContain('Marked as not recalled')
    expect(mockMarkAsRecalled).toHaveBeenCalledWith(7, false)
    expectTtyRecallYesNoReplyScrollback(writeSpy, 'n')
  })
})
