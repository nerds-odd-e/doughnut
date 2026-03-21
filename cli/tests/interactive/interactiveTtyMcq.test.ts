/// <reference types="node" />
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
  buildCurrentPromptSeparator,
  getTerminalWidth,
  INTERACTIVE_INPUT_READY_OSC,
  stripAnsi,
} from '../../src/renderer.js'
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
import {
  countInputBoxTopOutlinesBeforeFirstBoxContent,
  liveRegionRepaintHasStaleCursorUpBeforeBoxTop,
} from '../ttyWriteSimulation.js'

const GREY_SGR = '\x1b[90m'
const SGR_RESET_LINE = '\x1b[0m\n'

function countGreyWholeLineWriteCurrentPromptsWhere(
  writeSpy: ReturnType<typeof vi.spyOn>,
  predicate: (plainOneLine: string) => boolean
): number {
  let count = 0
  for (const call of writeSpy.mock.calls) {
    const chunk = String(call[0] ?? '')
    if (chunk.includes('\x1b[2K')) continue
    if (!(chunk.startsWith(GREY_SGR) && chunk.endsWith(SGR_RESET_LINE)))
      continue
    const plain = stripAnsi(chunk).trimEnd()
    if (predicate(plain)) count++
  }
  return count
}

describe('TTY recall MCQ', () => {
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

  describe('choice selection', () => {
    test('down arrow moves highlight to second choice', async () => {
      writeSpy.mockClear()
      await submitTTYCommand(stdin, '/recall')

      const afterSubmit = ttyOutput(writeSpy)
      expect(afterSubmit).toContain(
        '↑↓ Enter or number to select; Esc to cancel'
      )
      expect(afterSubmit).toContain('  1. 4')
      expect(afterSubmit).toContain('  2. 3')

      writeSpy.mockClear()
      pressKey(stdin, 'down')
      await tick()

      const output = ttyOutput(writeSpy)
      expect(output).toContain('\x1b[7m')
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

      expect(mockAnswerQuiz).toHaveBeenCalledWith(100, 1, expect.any(Number))
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

  describe('live region: current prompt vs guidance (observable bytes)', () => {
    test('numbered choices are not grey whole-line writeCurrentPrompt rows', async () => {
      await submitTTYCommand(stdin, '/recall')
      expect(
        countGreyWholeLineWriteCurrentPromptsWhere(writeSpy, (plain) =>
          /^ {2}\d+\. /.test(plain)
        )
      ).toBe(0)
    })

    test('2K green separator is painted before MCQ stem text', async () => {
      await submitTTYCommand(stdin, '/recall')
      const raw = ttyOutput(writeSpy)
      const sep = buildCurrentPromptSeparator(getTerminalWidth())
      const stem = 'What is 2+2?'
      expect(raw).toContain(`\x1b[2K${sep}`)
      expect(raw).toContain(stem)
      expect(raw.lastIndexOf(`\x1b[2K${sep}`)).toBeLessThan(
        raw.lastIndexOf(stem)
      )
    })

    test('no stale extra cursor-up before input box top', async () => {
      await submitTTYCommand(stdin, '/recall')
      expect(
        liveRegionRepaintHasStaleCursorUpBeforeBoxTop(ttyOutput(writeSpy))
      ).toBe(false)
    })

    test('at most one input box top outline before first box content row', async () => {
      await submitTTYCommand(stdin, '/recall')
      expect(
        countInputBoxTopOutlinesBeforeFirstBoxContent(ttyOutput(writeSpy))
      ).toBeLessThanOrEqual(1)
    })

    test('emits interactive input-ready OSC after paint', async () => {
      await submitTTYCommand(stdin, '/recall')
      expect(ttyOutput(writeSpy)).toContain(INTERACTIVE_INPUT_READY_OSC)
    })
  })
})

describe('TTY recall MCQ wrapped choices (narrow terminal)', () => {
  let writeSpy: ReturnType<typeof vi.spyOn>
  let stdin: TTYStdin
  let savedColumns: number | undefined

  beforeEach(async () => {
    savedColumns = process.stdout.columns
    Object.defineProperty(process.stdout, 'columns', {
      value: 34,
      writable: true,
      configurable: true,
    })
    resetRecallStateForTesting()
    mockRecallNext.mockResolvedValue({
      type: 'mcq',
      recallPromptId: 200,
      stem: 'Pick:',
      choices: ['Long first option text that must wrap in narrow cols', 'B'],
    })
    mockAnswerQuiz.mockResolvedValue({ correct: true })
    ;({ stdin, writeSpy } = await startTTYSessionWithoutRecallReset())
  })

  afterEach(() => {
    endTTYSession(stdin)
    Object.defineProperty(process.stdout, 'columns', {
      value: savedColumns,
      writable: true,
      configurable: true,
    })
  })

  test('first choice wraps; down arrow highlights only the second choice', async () => {
    await submitTTYCommand(stdin, '/recall')
    writeSpy.mockClear()
    pressKey(stdin, 'down')
    await tick()

    const output = ttyOutput(writeSpy)
    const highlighted = output
      .split('\n')
      .filter((l: string) => l.includes('\x1b[7m'))
    expect(highlighted.length).toBe(1)
    expect(stripAnsi(highlighted[0]!)).toMatch(/ {2}2\. /)
  })
})
