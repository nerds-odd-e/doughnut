/// <reference types="node" />
import { afterEach, beforeEach, describe, expect, test, type vi } from 'vitest'
import makeMe from 'doughnut-test-fixtures/makeMe'
import {
  mockAnswerQuiz,
  mockRecallNext,
} from './interactiveRecallMockAccess.js'
import { resetRecallStateForTesting } from '../../src/interactive.js'
import { formatRecallNotebookCurrentPromptLine } from '../../src/recall.js'
import { stripAnsi } from '../../src/renderer.js'
import {
  endTTYSession,
  pressKey,
  startTTYSessionWithoutRecallReset,
  submitTTYCommand,
  tick,
  ttyOutput,
  type TTYStdin,
} from './interactiveTestHelpers.js'
import { recallNextQuestion } from '../recallNextTestShapes.js'
import { mcqRecallPromptWithNotebook } from '../recallPromptFixtures.js'

/**
 * Regression: MCQ + Esc must repaint only via drawBox() so the live region does not show a second
 * stage band and does not keep the MCQ notebook/stem lines under "Recalling".
 */
describe('TTY MCQ: Esc → stop recall confirmation (live region layout)', () => {
  const uniqueNotebookTitle = 'McqEscStopConfirmNb_k9Qz'
  let writeSpy: ReturnType<typeof vi.spyOn>
  let stdin: TTYStdin

  beforeEach(async () => {
    resetRecallStateForTesting()
    const nb = makeMe.aNotebook
    nb.notebuilder.title(uniqueNotebookTitle)
    mockRecallNext.mockResolvedValue(
      recallNextQuestion(
        mcqRecallPromptWithNotebook(
          100,
          'Stem for esc-stop layout test?',
          ['A', 'B'],
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

  test('one Recalling stage paint, notebook line absent, stop question visible', async () => {
    await submitTTYCommand(stdin, '/recall')
    writeSpy.mockClear()

    pressKey(stdin, 'escape')
    await tick()

    const plain = stripAnsi(ttyOutput(writeSpy))
    const notebookLine =
      formatRecallNotebookCurrentPromptLine(uniqueNotebookTitle)

    expect(
      plain,
      'After Esc on an MCQ recall, stdout should include the stop-recall y/n prompt from the live region.'
    ).toContain('Stop recall? (y/n)')

    const stopPromptOccurrences = (plain.match(/Stop recall\? \(y\/n\)/g) ?? [])
      .length
    expect(
      stopPromptOccurrences,
      'The stop-recall question must be emitted once for this Esc repaint. Two copies usually mean writeCurrentPrompt wrote the line and drawBox painted it again in Current guidance — bypassing the live-region erase and duplicating the stage band area.'
    ).toBe(1)

    expect(
      plain,
      'While confirming stop recall, the MCQ notebook line must not stay on screen; it belongs to the hidden question. If this fails, Current prompt still includes the notebook under the stage band.'
    ).not.toContain(notebookLine)

    const recallingCount = (plain.match(/Recalling/g) ?? []).length
    expect(
      recallingCount,
      'Bytes written for this repaint should contain the "Recalling" label only once. Duplicate counts usually mean beginCurrentPrompt/writeCurrentPrompt wrote outside drawBox before the live region repaint, duplicating the stage band and separator.'
    ).toBe(1)
  })
})
