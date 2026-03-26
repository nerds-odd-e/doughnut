/// <reference types="node" />
import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import makeMe from 'doughnut-test-fixtures/makeMe'
import {
  mockAnswerQuiz,
  mockRecallNext,
} from './interactiveRecallMockAccess.js'
import {
  isInRecallSubstate,
  resetRecallStateForTesting,
} from '../../src/interactive.js'
import { formatRecallNotebookCurrentPromptLine } from '../../src/recall.js'
import { INTERACTIVE_INPUT_READY_OSC, stripAnsi } from '../../src/renderer.js'
import {
  endTTYSession,
  expectTtyRecallYesNoReplyScrollback,
  pushTTYCommandBytes,
  pushTTYCommandEnter,
  pushTTYCommandEscape,
  pushTTYCommandKey,
  startTTYSessionWithoutRecallReset,
  submitTTYCommand,
  tick,
  ttyOutput,
  type TTYStdin,
} from './interactiveTestHelpers.js'
import { recallNextQuestion } from '../recallNextTestShapes.js'
import {
  mcqRecallPrompt,
  mcqRecallPromptWithNotebook,
} from '../recallPromptFixtures.js'

/** Distinct title so Esc → stop-confirm repaint cannot match unrelated output. */
const MCQ_NOTEBOOK_TITLE = 'McqEscStopConfirmNb_k9Qz'

const GREY_SGR = '\x1b[90m'

function greyWholeLineWriteChunk(chunk: string): boolean {
  if (!chunk.startsWith(GREY_SGR)) return false
  return (
    chunk.endsWith('\x1b[0m\n') ||
    chunk.endsWith('\x1b[39m\n') ||
    chunk.endsWith('\x1b[0m') ||
    chunk.endsWith('\x1b[39m')
  )
}

function countGreyWholeLineWriteCurrentPromptsWhere(
  writeSpy: ReturnType<typeof vi.spyOn>,
  predicate: (plainOneLine: string) => boolean
): number {
  let count = 0
  for (const call of writeSpy.mock.calls) {
    const chunk = String(call[0] ?? '')
    if (chunk.includes('\x1b[2K')) continue
    if (!greyWholeLineWriteChunk(chunk)) continue
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
    const nb = makeMe.aNotebook
    nb.notebuilder.title(MCQ_NOTEBOOK_TITLE)
    mockRecallNext.mockResolvedValue(
      recallNextQuestion(
        mcqRecallPromptWithNotebook(
          100,
          'What is 2+2?',
          ['4', '3', '5'],
          nb.please()
        )
      )
    )
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
      pushTTYCommandKey(stdin, 'down')
      await tick()

      const output = ttyOutput(writeSpy)
      expect(output).toContain('\x1b[7m')
      expect(output).toContain('  2. 3')
    })

    test('Enter submits highlighted choice and calls answerQuiz', async () => {
      await submitTTYCommand(stdin, '/recall')

      pushTTYCommandEnter(stdin)
      await tick()
      await new Promise((r) => setTimeout(r, 50))

      expect(mockAnswerQuiz).toHaveBeenCalledWith(100, 0, expect.any(Number))
      expect(ttyOutput(writeSpy)).toContain('Recalled successfully')
    })

    test('typed number still works', async () => {
      await submitTTYCommand(stdin, '/recall')

      pushTTYCommandBytes(stdin, '2')
      await tick()
      pushTTYCommandEnter(stdin)
      await tick()

      expect(mockAnswerQuiz).toHaveBeenCalledWith(100, 1, expect.any(Number))
    })

    test('real backspace (str=\\x7f) in MCQ deletes typed input, not inserts DEL char', async () => {
      await submitTTYCommand(stdin, '/recall')
      mockAnswerQuiz.mockClear()

      // Type '2' to select choice 2, then delete it — should revert to default choice (1)
      pushTTYCommandBytes(stdin, '2')
      await tick()
      pushTTYCommandKey(stdin, 'backspace')
      await tick()
      pushTTYCommandEnter(stdin)
      await tick()
      await new Promise((r) => setTimeout(r, 50))

      expect(
        mockAnswerQuiz.mock.calls[0],
        'After typing "2" and backspace, the draft should be empty so the highlighted choice (index 0) is submitted.'
      ).toEqual([100, 0, expect.any(Number)])
    })

    test('ESC shows stop confirmation, y exits recall mode', async () => {
      await submitTTYCommand(stdin, '/recall')
      mockAnswerQuiz.mockClear()
      writeSpy.mockClear()

      await pushTTYCommandEscape(stdin)

      const escRepaint = stripAnsi(ttyOutput(writeSpy))
      expect(
        (escRepaint.match(/Stop recall\? \(y\/n\)/g) ?? []).length,
        'The stop-recall question must be emitted once for this Esc repaint. Two copies usually mean writeCurrentPrompt wrote the line and drawBox painted it again in Current guidance — bypassing the live-region erase and duplicating the stage band area.'
      ).toBe(1)
      expect(
        escRepaint,
        'While confirming stop recall, the MCQ notebook line must not stay on screen; it belongs to the hidden question. If this fails, Current prompt still includes the notebook under the stage band.'
      ).not.toContain(formatRecallNotebookCurrentPromptLine(MCQ_NOTEBOOK_TITLE))
      expect(
        (escRepaint.match(/Recalling/g) ?? []).length,
        'Bytes written for this repaint should contain the "Recalling" label only once. Duplicate counts usually mean beginCurrentPrompt/writeCurrentPrompt wrote outside drawBox before the live region repaint, duplicating the stage band and separator.'
      ).toBe(1)
      expect(escRepaint).toContain('y or n; Esc to go back')

      pushTTYCommandBytes(stdin, 'y')
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

      await pushTTYCommandEscape(stdin)

      pushTTYCommandBytes(stdin, 'n')
      await tick()

      expect(isInRecallSubstate()).toBe(true)
      expect(mockAnswerQuiz).not.toHaveBeenCalled()

      const output = ttyOutput(writeSpy)
      expect(output).toContain('  1. 4')
      expect(output).toContain('  2. 3')
    })

    test('ESC on stop sheet returns to MCQ without stop sheet repainting after the question', async () => {
      const mcqStem = 'What is 2+2?'
      await submitTTYCommand(stdin, '/recall')
      mockAnswerQuiz.mockClear()

      await pushTTYCommandEscape(stdin)
      await vi.waitFor(() =>
        expect(stripAnsi(ttyOutput(writeSpy))).toContain('Stop recall? (y/n)')
      )

      writeSpy.mockClear()
      await pushTTYCommandEscape(stdin)
      await tick()
      await tick()
      await new Promise((r) => setTimeout(r, 120))

      const out = stripAnsi(ttyOutput(writeSpy))
      const stemIdx = out.lastIndexOf(mcqStem)
      const stopIdx = out.lastIndexOf('Stop recall? (y/n)')

      expect(
        stemIdx,
        'Second Esc should dismiss the stop sheet and show the MCQ stem again.'
      ).toBeGreaterThan(-1)
      expect(
        stopIdx === -1 || stemIdx > stopIdx,
        'Stop sheet must stay dismissed: if the last “Stop recall?” is after the last MCQ stem, stop confirmation was re-opened after Esc “go back” (duplicate Esc handling).'
      ).toBe(true)
    })
  })

  describe('live region: current prompt vs guidance (observable bytes)', () => {
    test('choices not via writeCurrentPrompt; input-ready OSC emitted', async () => {
      writeSpy.mockClear()
      await submitTTYCommand(stdin, '/recall')
      // Numbered choices must not appear as grey writeCurrentPrompt lines
      expect(
        countGreyWholeLineWriteCurrentPromptsWhere(writeSpy, (plain) =>
          /^ {2}\d+\. /.test(plain)
        )
      ).toBe(0)
      // INTERACTIVE_INPUT_READY_OSC is emitted when the numbered-choice list panel is shown
      const raw = ttyOutput(writeSpy)
      expect(raw).toContain(INTERACTIVE_INPUT_READY_OSC)
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
    mockRecallNext.mockResolvedValue(
      recallNextQuestion(
        mcqRecallPrompt(200, 'Pick:', [
          'Long first option text that must wrap in narrow cols',
          'B',
        ])
      )
    )
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
    pushTTYCommandKey(stdin, 'down')
    await tick()

    const output = ttyOutput(writeSpy)
    const highlighted = output
      .split('\n')
      .filter((l: string) => l.includes('\x1b[7m'))
    expect(highlighted.length).toBe(1)
    expect(stripAnsi(highlighted[0]!)).toMatch(/ {2}2\. /)
  })
})
