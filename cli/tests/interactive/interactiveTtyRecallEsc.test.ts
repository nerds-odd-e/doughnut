import { describe, test, expect, type vi, beforeEach, afterEach } from 'vitest'
import {
  mockAnswerSpelling,
  mockMarkAsRecalled,
  mockRecallNext,
} from './interactiveRecallMockAccess.js'
import { isInRecallSubstate } from '../../src/interactive.js'
import {
  endTTYSession,
  pressKey,
  submitTTYCommand,
  tick,
  ttyOutput,
  ttySessionWithSpies,
  type TTYStdin,
} from './interactiveTestHelpers.js'

describe('TTY recall substates ESC (spelling, y/n, load-more)', () => {
  let stdin: TTYStdin
  let writeSpy: ReturnType<typeof vi.spyOn>

  beforeEach(async () => {
    ;({ stdin, writeSpy } = await ttySessionWithSpies())
  })

  afterEach(() => {
    endTTYSession(stdin)
  })

  test('ESC in spelling prompt exits recall mode', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'spelling',
      recallPromptId: 100,
      stem: 'test',
    })
    mockAnswerSpelling.mockResolvedValue({ correct: true })
    await submitTTYCommand(stdin, '/recall')

    expect(ttyOutput(writeSpy)).toContain(
      'type your answer; /stop to exit recall'
    )

    pressKey(stdin, 'escape')
    await tick()

    expect(isInRecallSubstate()).toBe(false)
    expect(mockAnswerSpelling).not.toHaveBeenCalled()
  })

  test('ESC in Yes I remember y/n prompt exits recall mode', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'just-review',
      memoryTrackerId: 42,
      title: 'Test note',
    })
    await submitTTYCommand(stdin, '/recall')

    expect(ttyOutput(writeSpy)).toContain('y or n; /stop to exit recall')

    pressKey(stdin, 'escape')
    await tick()

    expect(isInRecallSubstate()).toBe(false)
    expect(mockMarkAsRecalled).not.toHaveBeenCalled()
  })

  test('ESC in Load more y/n prompt exits recall mode', async () => {
    mockRecallNext.mockResolvedValue({ type: 'none', message: '0 notes' })
    await submitTTYCommand(stdin, '/recall')

    expect(ttyOutput(writeSpy)).toContain('y or n; /stop to exit recall')

    pressKey(stdin, 'escape')
    await tick()

    expect(isInRecallSubstate()).toBe(false)
  })
})
