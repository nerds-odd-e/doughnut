import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  mockAnswerSpelling,
  mockMarkAsRecalled,
  mockRecallNext,
} from './interactiveRecallMockAccess.js'
import { isInRecallSubstate } from '../../src/interactive.js'
import { stripAnsi } from '../../src/renderer.js'
import {
  endTTYSession,
  expectTtyRecallYesNoReplyScrollback,
  pressEnter,
  pressKey,
  pressRealBackspace,
  pushTTYCommandEscape,
  submitTTYCommand,
  tick,
  ttyOutput,
  ttySessionWithSpies,
  typeString,
  type TTYStdin,
} from './interactiveTestHelpers.js'
import { recallNextQuestion } from '../recallNextTestShapes.js'
import { spellingRecallPrompt } from '../recallPromptFixtures.js'

describe('TTY recall substates ESC (spelling, y/n, load-more)', () => {
  let stdin: TTYStdin
  let writeSpy: ReturnType<typeof vi.spyOn>

  beforeEach(async () => {
    ;({ stdin, writeSpy } = await ttySessionWithSpies())
  })

  afterEach(() => {
    endTTYSession(stdin)
  })

  test('ESC in spelling shows stop confirmation, y exits recall mode', async () => {
    mockRecallNext.mockResolvedValue(
      recallNextQuestion(spellingRecallPrompt(100, 'test'))
    )
    mockAnswerSpelling.mockResolvedValue({ correct: true })
    await submitTTYCommand(stdin, '/recall')

    expect(ttyOutput(writeSpy)).toContain(
      'type your answer; /stop to exit recall'
    )

    mockAnswerSpelling.mockClear()
    writeSpy.mockClear()
    await pushTTYCommandEscape(stdin)
    await vi.waitFor(() =>
      expect(stripAnsi(ttyOutput(writeSpy))).toContain('Stop recall? (y/n)')
    )

    const escRepaint = stripAnsi(ttyOutput(writeSpy))
    expect(escRepaint).toContain('Stop recall? (y/n)')
    expect(escRepaint).toContain('y or n; Esc to go back')
    expect(mockAnswerSpelling).not.toHaveBeenCalled()
    expect(isInRecallSubstate()).toBe(true)

    typeString(stdin, 'y')
    await vi.waitFor(() => {
      const p = stripAnsi(ttyOutput(writeSpy))
      expect(p).toMatch(/Stop recall\?\s*\(y\/n\)[\s\S]*\n\s*y(?:\s|$)/m)
    })
    pressEnter(stdin)
    await tick()

    expect(ttyOutput(writeSpy)).toContain('Stopped recall')
    expectTtyRecallYesNoReplyScrollback(writeSpy, 'y')
    expect(mockAnswerSpelling).not.toHaveBeenCalled()
    expect(isInRecallSubstate()).toBe(false)
  })

  test('ESC then n in spelling cancels confirmation and stays in recall', async () => {
    mockRecallNext.mockResolvedValue(
      recallNextQuestion(spellingRecallPrompt(100, 'test'))
    )
    mockAnswerSpelling.mockResolvedValue({ correct: true })
    await submitTTYCommand(stdin, '/recall')
    mockAnswerSpelling.mockClear()
    writeSpy.mockClear()

    await pushTTYCommandEscape(stdin)

    typeString(stdin, 'n')
    await tick()
    pressEnter(stdin)
    await tick()

    expect(isInRecallSubstate()).toBe(true)
    expect(mockAnswerSpelling).not.toHaveBeenCalled()
    expect(ttyOutput(writeSpy)).toContain(
      'type your answer; /stop to exit recall'
    )
  })

  test('ESC in Yes I remember shows stop confirmation, y exits recall mode', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'just-review',
      memoryTrackerId: 42,
      notebookTitle: 'Notebook',
      title: 'Test note',
    })
    await submitTTYCommand(stdin, '/recall')

    expect(ttyOutput(writeSpy)).toContain('y or n; /stop to exit recall')

    mockMarkAsRecalled.mockClear()
    writeSpy.mockClear()
    pressKey(stdin, 'escape')
    await tick()

    const escRepaint = stripAnsi(ttyOutput(writeSpy))
    expect(escRepaint).toContain('Stop recall? (y/n)')
    expect(escRepaint).toContain('y or n; Esc to go back')
    expect(mockMarkAsRecalled).not.toHaveBeenCalled()
    expect(isInRecallSubstate()).toBe(true)

    typeString(stdin, 'y')
    await tick()
    pressEnter(stdin)
    await tick()

    expect(ttyOutput(writeSpy)).toContain('Stopped recall')
    expectTtyRecallYesNoReplyScrollback(writeSpy, 'y')
    expect(mockMarkAsRecalled).not.toHaveBeenCalled()
    expect(isInRecallSubstate()).toBe(false)
  })

  test('ESC then n in just-review cancels confirmation and stays in recall', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'just-review',
      memoryTrackerId: 42,
      notebookTitle: 'Notebook',
      title: 'Test note',
    })
    await submitTTYCommand(stdin, '/recall')
    mockMarkAsRecalled.mockClear()
    writeSpy.mockClear()

    pressKey(stdin, 'escape')
    await tick()

    typeString(stdin, 'n')
    await tick()
    pressEnter(stdin)
    await tick()

    expect(isInRecallSubstate()).toBe(true)
    expect(mockMarkAsRecalled).not.toHaveBeenCalled()
    expect(ttyOutput(writeSpy)).toContain('y or n; /stop to exit recall')
  })

  test('ESC in Load more shows stop confirmation, y exits recall mode', async () => {
    mockRecallNext.mockResolvedValue({ type: 'none', message: '0 notes' })
    await submitTTYCommand(stdin, '/recall')

    expect(ttyOutput(writeSpy)).toContain('y or n; /stop to exit recall')

    writeSpy.mockClear()
    pressKey(stdin, 'escape')
    await tick()

    const escRepaint = stripAnsi(ttyOutput(writeSpy))
    expect(escRepaint).toContain('Stop recall? (y/n)')
    expect(escRepaint).toContain('y or n; Esc to go back')
    expect(isInRecallSubstate()).toBe(true)

    typeString(stdin, 'y')
    await tick()
    pressEnter(stdin)
    await tick()

    expect(ttyOutput(writeSpy)).toContain('Stopped recall')
    expectTtyRecallYesNoReplyScrollback(writeSpy, 'y')
    expect(isInRecallSubstate()).toBe(false)
  })

  test('real backspace (str=\\x7f) in stop confirmation on empty input is a no-op, not DEL insertion', async () => {
    mockRecallNext.mockResolvedValue(
      recallNextQuestion(spellingRecallPrompt(100, 'test'))
    )
    await submitTTYCommand(stdin, '/recall')
    await pushTTYCommandEscape(stdin)
    writeSpy.mockClear()

    // Real TTY backspace: str='\x7f' (DEL char, truthy). If the handler checks
    // `str && !ctrl && !meta` before `key.name === 'backspace'`, it inserts '\x7f'
    // into the draft instead of deleting. On enter, '\x7f'.trim() is non-empty
    // and not 'y'/'n', triggering "Please answer y or n".
    pressRealBackspace(stdin)
    await tick()
    pressEnter(stdin)
    await tick()

    expect(
      stripAnsi(ttyOutput(writeSpy)),
      'Backspace on empty stop-confirmation input must be a no-op. ' +
        'If this fails, the `str && !ctrl && !meta` branch fires before `key.name === "backspace"` ' +
        'in the isPendingStopConfirmation handler, inserting the DEL character \\x7f. ' +
        'Fix: move the backspace check above the str-insertion check in that handler.'
    ).not.toContain('Please answer y or n')
    expect(isInRecallSubstate()).toBe(true)
  })

  test('ESC then n in Load more cancels confirmation and stays in recall', async () => {
    mockRecallNext.mockResolvedValue({ type: 'none', message: '0 notes' })
    await submitTTYCommand(stdin, '/recall')

    pressKey(stdin, 'escape')
    await tick()

    typeString(stdin, 'n')
    await tick()
    pressEnter(stdin)
    await tick()

    expect(isInRecallSubstate()).toBe(true)
    expect(ttyOutput(writeSpy)).toContain('Load more from next 3 days? (y/n)')
  })
})
