import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  mockMarkAsRecalled,
  mockRecallNext,
} from './interactiveRecallMockAccess.js'
import { isInRecallSubstate } from '../../src/interactive.js'
import { stripAnsi } from '../../src/renderer.js'
import {
  endTTYSession,
  expectTtyRecallYesNoReplyScrollback,
  pushTTYCommandBytes,
  pushTTYCommandEnter,
  pushTTYCommandEscape,
  pushTTYCommandKey,
  submitTTYCommand,
  tick,
  ttyOutput,
  ttySessionWithSpies,
  type TTYStdin,
} from './interactiveTestHelpers.js'
import { recallNextQuestion } from '../recallNextTestShapes.js'
import { mcqRecallPrompt } from '../recallPromptFixtures.js'

describe('TTY recall substates ESC (MCQ, y/n, load-more)', () => {
  let stdin: TTYStdin
  let writeSpy: ReturnType<typeof vi.spyOn>

  beforeEach(async () => {
    ;({ stdin, writeSpy } = await ttySessionWithSpies())
  })

  afterEach(() => {
    endTTYSession(stdin)
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
    await pushTTYCommandEscape(stdin)
    await vi.waitFor(() =>
      expect(stripAnsi(ttyOutput(writeSpy))).toContain('Stop recall? (y/n)')
    )

    const escRepaint = stripAnsi(ttyOutput(writeSpy))
    expect(escRepaint).toContain('Stop recall? (y/n)')
    expect(escRepaint).toContain('y or n; Esc to go back')
    expect(mockMarkAsRecalled).not.toHaveBeenCalled()
    expect(isInRecallSubstate()).toBe(true)

    pushTTYCommandBytes(stdin, 'y')
    await vi.waitFor(() =>
      expect(ttyOutput(writeSpy)).toContain('Stopped recall')
    )

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

    await pushTTYCommandEscape(stdin)
    await vi.waitFor(() =>
      expect(stripAnsi(ttyOutput(writeSpy))).toContain('Stop recall? (y/n)')
    )

    pushTTYCommandBytes(stdin, 'n')
    await vi.waitFor(() =>
      expect(ttyOutput(writeSpy)).toContain('y or n; /stop to exit recall')
    )

    expect(isInRecallSubstate()).toBe(true)
    expect(mockMarkAsRecalled).not.toHaveBeenCalled()
    expect(ttyOutput(writeSpy)).toContain('y or n; /stop to exit recall')
  })

  test('ESC in Load more shows stop confirmation, y exits recall mode', async () => {
    mockRecallNext.mockResolvedValue({ type: 'none', message: '0 notes' })
    await submitTTYCommand(stdin, '/recall')

    expect(ttyOutput(writeSpy)).toContain('y or n; /stop to exit recall')

    writeSpy.mockClear()
    await pushTTYCommandEscape(stdin)
    await vi.waitFor(() =>
      expect(stripAnsi(ttyOutput(writeSpy))).toContain('Stop recall? (y/n)')
    )

    const escRepaint = stripAnsi(ttyOutput(writeSpy))
    expect(escRepaint).toContain('Stop recall? (y/n)')
    expect(escRepaint).toContain('y or n; Esc to go back')
    expect(isInRecallSubstate()).toBe(true)

    pushTTYCommandBytes(stdin, 'y')
    await vi.waitFor(() =>
      expect(ttyOutput(writeSpy)).toContain('Stopped recall')
    )

    expect(ttyOutput(writeSpy)).toContain('Stopped recall')
    expectTtyRecallYesNoReplyScrollback(writeSpy, 'y')
    expect(isInRecallSubstate()).toBe(false)
  })

  test('real backspace (str=\\x7f) in stop confirmation on empty input is a no-op, not DEL insertion', async () => {
    mockRecallNext.mockResolvedValue(
      recallNextQuestion(mcqRecallPrompt(100, 'test', ['a', 'b', 'c']))
    )
    await submitTTYCommand(stdin, '/recall')
    await pushTTYCommandEscape(stdin)
    writeSpy.mockClear()

    // Real TTY: DEL as stdin byte; Ink maps to `delete` — confirm panel ignores it on empty.
    pushTTYCommandKey(stdin, 'backspace')
    await tick()
    pushTTYCommandEnter(stdin)
    await tick()

    expect(
      stripAnsi(ttyOutput(writeSpy)),
      'Backspace on empty stop-confirmation input must be a no-op.'
    ).not.toContain('Please press y or n')
    expect(isInRecallSubstate()).toBe(true)
  })

  test('ESC then n in Load more cancels confirmation and stays in recall', async () => {
    mockRecallNext.mockResolvedValue({ type: 'none', message: '0 notes' })
    await submitTTYCommand(stdin, '/recall')

    await pushTTYCommandEscape(stdin)
    await vi.waitFor(() =>
      expect(stripAnsi(ttyOutput(writeSpy))).toContain('Stop recall? (y/n)')
    )

    pushTTYCommandBytes(stdin, 'n')
    await vi.waitFor(() =>
      expect(ttyOutput(writeSpy)).toContain('Load more from next 3 days? (y/n)')
    )

    expect(isInRecallSubstate()).toBe(true)
    expect(ttyOutput(writeSpy)).toContain('Load more from next 3 days? (y/n)')
  })
})
