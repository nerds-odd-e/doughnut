import { describe, test, expect, type vi, beforeEach, afterEach } from 'vitest'
import {
  mockAnswerQuiz,
  mockRecallNext,
} from './interactiveRecallMockAccess.js'
import {
  isInRecallSubstate,
  resetRecallStateForTesting,
} from '../../src/interactive.js'
import {
  endTTYSession,
  expectTtyRecallYesNoReplyScrollback,
  pressEnter,
  pressKey,
  startTTYSessionWithoutRecallReset,
  submitTTYCommand,
  tick,
  ttyOutput,
  typeString,
  type TTYStdin,
} from './interactiveTestHelpers.js'

describe('TTY MCQ choice selection', () => {
  let writeSpy: ReturnType<typeof vi.spyOn>
  let stdin: TTYStdin

  beforeEach(async () => {
    resetRecallStateForTesting()
    mockRecallNext.mockResolvedValue({
      type: 'mcq',
      recallPromptId: 100,
      stem: 'What is 2+2?',
      choices: ['4', '3', '5'],
    })
    mockAnswerQuiz.mockResolvedValue({ correct: true })
    ;({ stdin, writeSpy } = await startTTYSessionWithoutRecallReset())
  })

  afterEach(() => {
    endTTYSession(stdin)
  })

  test('down arrow moves highlight to second choice', async () => {
    writeSpy.mockClear()
    await submitTTYCommand(stdin, '/recall')

    const afterSubmit = ttyOutput(writeSpy)
    expect(afterSubmit).toContain('↑↓ Enter or number to select; Esc to cancel')
    expect(afterSubmit).toContain('  1. 4')
    expect(afterSubmit).toContain('  2. 3')

    writeSpy.mockClear()
    pressKey(stdin, 'down')
    await tick()

    const output = ttyOutput(writeSpy)
    expect(output).toContain('\x1b[7m') // REVERSE on highlighted line
    expect(output).toContain('  2. 3')
  })

  test('Enter submits highlighted choice and calls answerQuiz', async () => {
    await submitTTYCommand(stdin, '/recall')

    pressEnter(stdin)
    await tick()
    await new Promise((r) => setTimeout(r, 50))

    expect(mockAnswerQuiz).toHaveBeenCalledWith(100, 0, expect.any(Number))
    expect(ttyOutput(writeSpy)).toContain('Recalled successfully')
  })

  test('typed number still works', async () => {
    await submitTTYCommand(stdin, '/recall')

    typeString(stdin, '2')
    await tick()
    pressEnter(stdin)
    await tick()

    expect(mockAnswerQuiz).toHaveBeenCalledWith(100, 1, expect.any(Number)) // "2" = choiceIndex 1
  })

  test('ESC shows stop confirmation, y exits recall mode', async () => {
    await submitTTYCommand(stdin, '/recall')
    mockAnswerQuiz.mockClear()
    writeSpy.mockClear()

    pressKey(stdin, 'escape')
    await tick()

    expect(ttyOutput(writeSpy)).toContain('Stop recall? (y/n)')
    expect(ttyOutput(writeSpy)).toContain('y or n; Esc to go back')

    typeString(stdin, 'y')
    await tick()
    pressEnter(stdin)
    await tick()

    const out = ttyOutput(writeSpy)
    expect(out).toContain('Stopped recall')
    expectTtyRecallYesNoReplyScrollback(writeSpy, 'y')
    expect(mockAnswerQuiz).not.toHaveBeenCalled()
    expect(isInRecallSubstate()).toBe(false)
  })

  test('ESC then n cancels confirmation and stays in MCQ', async () => {
    await submitTTYCommand(stdin, '/recall')
    mockAnswerQuiz.mockClear()
    writeSpy.mockClear()

    pressKey(stdin, 'escape')
    await tick()

    typeString(stdin, 'n')
    await tick()
    pressEnter(stdin)
    await tick()

    expect(isInRecallSubstate()).toBe(true)
    expect(mockAnswerQuiz).not.toHaveBeenCalled()

    const output = ttyOutput(writeSpy)
    expect(output).toContain('  1. 4')
    expect(output).toContain('  2. 3')
  })
})
