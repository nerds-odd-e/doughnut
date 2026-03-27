/// <reference types="node" />
import { describe, test, expect, type vi, beforeEach, afterEach } from 'vitest'
import makeMe from 'doughnut-test-fixtures/makeMe'
import { mockRecallNext } from './interactiveRecallMockAccess.js'
import { recallNextQuestion } from '../recallNextTestShapes.js'
import { mcqRecallPromptWithNotebook } from '../recallPromptFixtures.js'
import { resetRecallStateForTesting } from '../../src/interactive.js'
import { formatRecallNotebookCurrentPromptLine } from '../../src/commands/recall.js'
import { stripAnsi } from '../../src/renderer.js'
import {
  endTTYSession,
  startTTYSessionWithoutRecallReset,
  submitTTYCommand,
  tick,
  ttyOutput,
  type TTYStdin,
} from './interactiveTestHelpers.js'

describe('TTY recall: notebook first in Current prompt (MCQ and just-review)', () => {
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
      recallNextQuestion(
        mcqRecallPromptWithNotebook(1, 'Q?', ['a', 'b'], chem.please())
      )
    )
    await submitTTYCommand(stdin, '/recall')
    await tick()
    const plain = stripAnsi(ttyOutput(writeSpy))
    const nb = formatRecallNotebookCurrentPromptLine('Chem')
    expect(plain).toContain(nb)
    expect(plain.indexOf(nb)).toBeLessThan(plain.indexOf('Q?'))
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
