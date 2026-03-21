/// <reference types="node" />
import { describe, test, expect, type vi, beforeEach, afterEach } from 'vitest'
import makeMe from 'doughnut-test-fixtures/makeMe'
import { mockRecallNext } from './interactiveRecallMockAccess.js'
import { recallNextQuestion } from '../recallNextTestShapes.js'
import { resetRecallStateForTesting } from '../../src/interactive.js'
import { formatRecallNotebookCurrentPromptLine } from '../../src/recall.js'
import { stripAnsi } from '../../src/renderer.js'
import {
  endTTYSession,
  startTTYSessionWithoutRecallReset,
  submitTTYCommand,
  tick,
  ttyOutput,
  type TTYStdin,
} from './interactiveTestHelpers.js'

describe('TTY recall: notebook first in Current prompt (all question kinds)', () => {
  let writeSpy: ReturnType<typeof vi.spyOn>
  let stdin: TTYStdin

  beforeEach(async () => {
    resetRecallStateForTesting()
    ;({ stdin, writeSpy } = await startTTYSessionWithoutRecallReset())
  })

  afterEach(() => {
    endTTYSession(stdin)
  })

  test('MCQ: emoji notebook line precedes stem in live region', async () => {
    const chem = makeMe.aNotebook
    chem.notebuilder.title('Chem')
    mockRecallNext.mockResolvedValue(
      recallNextQuestion({
        id: 1,
        questionType: 'MCQ',
        notebook: chem.please(),
        multipleChoicesQuestion: { f0__stem: 'Q?', f1__choices: ['a', 'b'] },
      })
    )
    await submitTTYCommand(stdin, '/recall')
    await tick()
    const plain = stripAnsi(ttyOutput(writeSpy))
    const nb = formatRecallNotebookCurrentPromptLine('Chem')
    expect(plain).toContain(nb)
    expect(plain.indexOf(nb)).toBeLessThan(plain.indexOf('Q?'))
  })

  test('spelling: notebook line precedes Spell prompt in scrollback', async () => {
    const chem = makeMe.aNotebook
    chem.notebuilder.title('Chem')
    mockRecallNext.mockResolvedValue(
      recallNextQuestion({
        id: 2,
        questionType: 'SPELLING',
        notebook: chem.please(),
        spellingQuestion: { stem: 'word' },
      })
    )
    await submitTTYCommand(stdin, '/recall')
    await tick()
    const plain = stripAnsi(ttyOutput(writeSpy))
    const nb = formatRecallNotebookCurrentPromptLine('Chem')
    expect(plain).toContain(nb)
    expect(plain.indexOf(nb)).toBeLessThan(plain.indexOf('Spell:'))
  })

  test('just-review: notebook line precedes note title', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'just-review',
      memoryTrackerId: 3,
      notebookTitle: 'Chem',
      title: 'Note title here',
    })
    await submitTTYCommand(stdin, '/recall')
    await tick()
    const plain = stripAnsi(ttyOutput(writeSpy))
    const nb = formatRecallNotebookCurrentPromptLine('Chem')
    expect(plain).toContain(nb)
    expect(plain.indexOf(nb)).toBeLessThan(plain.indexOf('Note title here'))
  })
})
