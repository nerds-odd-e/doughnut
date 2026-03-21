/**
 * Observable contract: after the TTY paints recall MCQ with wrapped choice lines, the final
 * cursor must sit on the live input row (the bordered line with the → prompt). Cursor-up math
 * must use the full live-region height from buildLiveRegionLines, not a height that ignores
 * extra guidance rows from wrapping.
 */
import process from 'node:process'
import { afterEach, describe, expect, test, type vi } from 'vitest'
import './interactive/interactiveTestMocks.js'
import { resetRecallStateForTesting } from '../src/interactive.js'
import { recallMcqNumberedChoiceLines } from '../src/recallMcqDisplay.js'
import { PROMPT } from '../src/renderer.js'
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

const CURSOR_ROW_MESSAGE =
  'Recall MCQ uses one extra final CUU in ttyAdapter so the real TTY lands on the → row. ' +
  'Naive replay leaves simulated `row` one index below that line in the sparse buffer (row + 1 === promptRow). ' +
  'If the real cursor still sits below the prompt, increase extraCursorUpAfterLiveRegionPaint for recallMcq.'

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
    expect(row + 1, CURSOR_ROW_MESSAGE).toBe(promptRow)
  }

  test('wide terminal (no choice wrap): cursor ends on the → input row', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'mcq',
      recallPromptId: 1,
      stem: 'Q?',
      choices: ['A', 'B'],
    })
    await sessionWithColumns(100)
    await submitTTYCommand(stdin, '/recall')
    await tick()
    assertCursorOnInputPromptRow(ttyOutput(writeSpy))
  })

  test('narrow terminal (wrapped choices), after ↓ repaint: cursor on the → input row', async () => {
    const choices = [
      'First option with enough text to wrap across multiple rows at narrow width',
      'B',
    ] as const
    mockRecallNext.mockResolvedValue({
      type: 'mcq',
      recallPromptId: 2,
      stem: 'Pick:',
      choices: [...choices],
    })
    await sessionWithColumns(36)
    expect(
      recallMcqNumberedChoiceLines([...choices], 36).length,
      'Sanity: this fixture must wrap to more than two physical choice lines at width 36 or the regression does not apply.'
    ).toBeGreaterThanOrEqual(3)
    await submitTTYCommand(stdin, '/recall')
    await tick()
    pressKey(stdin, 'down')
    await tick()
    assertCursorOnInputPromptRow(ttyOutput(writeSpy))
  })

  test('narrow then resize (guidance shrinks): cursor still on the → input row', async () => {
    const choices = [
      'First option with enough text to wrap across multiple rows at narrow width',
      'B',
    ] as const
    mockRecallNext.mockResolvedValue({
      type: 'mcq',
      recallPromptId: 3,
      stem: 'Pick:',
      choices: [...choices],
    })
    await sessionWithColumns(36)
    expect(
      recallMcqNumberedChoiceLines([...choices], 36).length
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
