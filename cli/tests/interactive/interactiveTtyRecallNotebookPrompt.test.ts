/// <reference types="node" />
import { describe, test, expect, type vi, beforeEach, afterEach } from 'vitest'
import { mockRecallNext } from './interactiveRecallMockAccess.js'
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
    mockRecallNext.mockResolvedValue({
      type: 'mcq',
      recallPromptId: 1,
      notebookTitle: 'Chem',
      stem: 'Q?',
      choices: ['a', 'b'],
    })
    await submitTTYCommand(stdin, '/recall')
    await tick()
    const plain = stripAnsi(ttyOutput(writeSpy))
    const nb = formatRecallNotebookCurrentPromptLine('Chem')
    expect(plain).toContain(nb)
    expect(plain.indexOf(nb)).toBeLessThan(plain.indexOf('Q?'))
  })

  test('spelling: notebook line precedes Spell prompt in scrollback', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'spelling',
      recallPromptId: 2,
      notebookTitle: 'Chem',
      stem: 'word',
    })
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
