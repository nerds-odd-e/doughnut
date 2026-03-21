/**
 * Observable contract: after the TTY paints recall MCQ, the final cursor must sit on the live
 * input row (the bordered line with the → prompt). Wrong `liveLineCount` / `inputLineRowInLiveBlock`
 * (e.g. wrapped stem or choices counted with JS `.length` instead of terminal columns) breaks
 * CUU after `drawBox` — duplicate separators, drift, or a cursor off the → row.
 */
import process from 'node:process'
import { afterEach, describe, expect, test, type vi } from 'vitest'
import './interactive/interactiveTestMocks.js'
import { resetRecallStateForTesting } from '../src/interactive.js'
import { formatMcqChoiceLines, PROMPT } from '../src/renderer.js'
import {
  mockAnswerQuiz,
  mockRecallNext,
} from './interactive/interactiveRecallMockAccess.js'
import {
  endTTYSession,
  pressKey,
  startTTYSessionWithoutRecallReset,
  submitTTYCommand,
  tick,
  ttyOutput,
  type TTYStdin,
} from './interactive/interactiveTestHelpers.js'
import {
  cursorPositionAfterTtyWrites,
  lastRowIndexContainingPlain,
} from './ttyWriteSimulation.js'
import { recallNextQuestion } from './recallNextTestShapes.js'

const CURSOR_ROW_MESSAGE =
  'After painting the live region, the final CUU must land exactly on the → input row. ' +
  'If the cursor row does not equal the prompt row, the CUU count in drawBox/doFullRedraw is wrong.'

describe('recall MCQ on TTY: cursor row after paint', () => {
  let stdin: TTYStdin
  let writeSpy: ReturnType<typeof vi.spyOn>
  let savedColumns: number | undefined

  afterEach(() => {
    endTTYSession(stdin)
    Object.defineProperty(process.stdout, 'columns', {
      value: savedColumns,
      writable: true,
      configurable: true,
    })
  })

  async function sessionWithColumns(width: number) {
    savedColumns = process.stdout.columns
    Object.defineProperty(process.stdout, 'columns', {
      value: width,
      writable: true,
      configurable: true,
    })
    resetRecallStateForTesting()
    mockAnswerQuiz.mockResolvedValue({ correct: true })
    ;({ stdin, writeSpy } = await startTTYSessionWithoutRecallReset())
  }

  function assertCursorOnInputPromptRow(rawWrites: string) {
    const { row, lines } = cursorPositionAfterTtyWrites(rawWrites)
    const promptRow = lastRowIndexContainingPlain(lines, PROMPT)
    expect(
      promptRow,
      'Sanity: live session output should contain the → prompt inside the input box.'
    ).toBeGreaterThanOrEqual(0)
    expect(row, CURSOR_ROW_MESSAGE).toBe(promptRow)
  }

  test('wide terminal (no choice wrap): cursor ends on the → input row', async () => {
    mockRecallNext.mockResolvedValue(
      recallNextQuestion({
        id: 1,
        questionType: 'MCQ',
        multipleChoicesQuestion: {
          f0__stem: 'Q?',
          f1__choices: ['A', 'B'],
        },
      })
    )
    await sessionWithColumns(100)
    await submitTTYCommand(stdin, '/recall')
    await tick()
    assertCursorOnInputPromptRow(ttyOutput(writeSpy))
  })

  test('wrapped MCQ stem (narrow terminal), after ↓: cursor on the → input row', async () => {
    mockRecallNext.mockResolvedValue(
      recallNextQuestion({
        id: 300,
        questionType: 'MCQ',
        multipleChoicesQuestion: {
          f0__stem: 'A question long enough to wrap at thirty columns wide',
          f1__choices: ['A', 'B'],
        },
      })
    )
    await sessionWithColumns(30)
    await submitTTYCommand(stdin, '/recall')
    await tick()
    pressKey(stdin, 'down')
    await tick()
    assertCursorOnInputPromptRow(ttyOutput(writeSpy))
  })

  test('wrapped choices: after ↓↑↓, cursor on → row and live region does not drift into history', async () => {
    const choices = [
      'First option with enough text to wrap across multiple rows at narrow width',
      'Second option also long enough to wrap at this narrow terminal width here',
    ] as const
    mockRecallNext.mockResolvedValue(
      recallNextQuestion({
        id: 4,
        questionType: 'MCQ',
        multipleChoicesQuestion: {
          f0__stem: 'Pick:',
          f1__choices: [...choices],
        },
      })
    )
    await sessionWithColumns(36)
    expect(
      formatMcqChoiceLines([...choices], 36).length,
      'Sanity: both choices must produce multiple physical lines at width 36.'
    ).toBeGreaterThanOrEqual(5)
    await submitTTYCommand(stdin, '/recall')
    await tick()
    pressKey(stdin, 'down')
    await tick()
    pressKey(stdin, 'up')
    await tick()
    pressKey(stdin, 'down')
    await tick()
    const raw = ttyOutput(writeSpy)
    assertCursorOnInputPromptRow(raw)
    const { lines } = cursorPositionAfterTtyWrites(raw)
    const recallRow = lastRowIndexContainingPlain(lines, '/recall')
    const separatorRow = lastRowIndexContainingPlain(lines, '─'.repeat(10))
    expect(recallRow).toBeGreaterThanOrEqual(0)
    expect(separatorRow).toBeGreaterThanOrEqual(0)
    expect(separatorRow).toBeGreaterThan(recallRow)
  })

  test('narrow then resize (guidance shrinks): cursor still on the → input row', async () => {
    const choices = [
      'First option with enough text to wrap across multiple rows at narrow width',
      'B',
    ] as const
    mockRecallNext.mockResolvedValue(
      recallNextQuestion({
        id: 3,
        questionType: 'MCQ',
        multipleChoicesQuestion: {
          f0__stem: 'Pick:',
          f1__choices: [...choices],
        },
      })
    )
    await sessionWithColumns(36)
    expect(
      formatMcqChoiceLines([...choices], 36).length
    ).toBeGreaterThanOrEqual(3)
    await submitTTYCommand(stdin, '/recall')
    await tick()
    Object.defineProperty(process.stdout, 'columns', {
      value: 100,
      writable: true,
      configurable: true,
    })
    process.stdout.emit('resize')
    await tick()
    assertCursorOnInputPromptRow(ttyOutput(writeSpy))
  })
})
