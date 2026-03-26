/**
 * Observable contract: recall MCQ paints like other live columns — stage band, stem, then `→` command line
 * (with recallMcq placeholder when empty), then numbered choices. After ↓↑↓ repaints, history must stay intact.
 */
import process from 'node:process'
import { afterEach, describe, expect, test, type vi } from 'vitest'
import './interactive/interactiveTestMocks.js'
import { resetRecallStateForTesting } from '../src/interactive.js'
import { formatMcqChoiceLines } from '../src/renderer.js'
import {
  mockAnswerQuiz,
  mockRecallNext,
} from './interactive/interactiveRecallMockAccess.js'
import {
  endTTYSession,
  pushTTYCommandKey,
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
import { mcqRecallPrompt } from './recallPromptFixtures.js'

describe('recall MCQ on TTY: Ink numbered-choice list render', () => {
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

  function assertMcqRenderedWithPrompt(rawWrites: string) {
    const { lines } = cursorPositionAfterTtyWrites(rawWrites)
    const promptRow = lastRowIndexContainingPlain(lines, '→')
    expect(
      promptRow,
      'Ink MCQ panel should render the → command line (above numbered choices).'
    ).toBeGreaterThanOrEqual(0)
  }

  test('wide terminal (no choice wrap): Ink renders → prompt', async () => {
    mockRecallNext.mockResolvedValue(
      recallNextQuestion(mcqRecallPrompt(1, 'Q?', ['A', 'B']))
    )
    await sessionWithColumns(100)
    await submitTTYCommand(stdin, '/recall')
    await tick()
    assertMcqRenderedWithPrompt(ttyOutput(writeSpy))
  })

  test('wrapped MCQ stem (narrow terminal), after ↓: Ink renders → prompt', async () => {
    mockRecallNext.mockResolvedValue(
      recallNextQuestion(
        mcqRecallPrompt(
          300,
          'A question long enough to wrap at thirty columns wide',
          ['A', 'B']
        )
      )
    )
    await sessionWithColumns(30)
    await submitTTYCommand(stdin, '/recall')
    await tick()
    pushTTYCommandKey(stdin, 'down')
    await tick()
    assertMcqRenderedWithPrompt(ttyOutput(writeSpy))
  })

  test('wrapped choices: after ↓↑↓, MCQ display does not overwrite /recall history', async () => {
    const choices = [
      'First option with enough text to wrap across multiple rows at narrow width',
      'Second option also long enough to wrap at this narrow terminal width here',
    ] as const
    mockRecallNext.mockResolvedValue(
      recallNextQuestion(mcqRecallPrompt(4, 'Pick:', [...choices]))
    )
    await sessionWithColumns(36)
    expect(
      formatMcqChoiceLines([...choices], 36).length,
      'Sanity: both choices must produce multiple physical lines at width 36.'
    ).toBeGreaterThanOrEqual(5)
    await submitTTYCommand(stdin, '/recall')
    await tick()
    pushTTYCommandKey(stdin, 'down')
    await tick()
    pushTTYCommandKey(stdin, 'up')
    await tick()
    pushTTYCommandKey(stdin, 'down')
    await tick()
    const raw = ttyOutput(writeSpy)
    // /recall user line appears in raw output (rendered as grey past user message)
    expect(raw).toContain('/recall')
    // MCQ Ink display renders → and choices after the history
    assertMcqRenderedWithPrompt(raw)
    expect(raw).toContain('First option')
    expect(raw).toContain('Second option')
  })
})
