import { describe, expect, test } from 'vitest'
import {
  MAX_INPUT_COMMAND_HISTORY,
  applyArrowDown,
  applyArrowUp,
  createInitialInputNavState,
  deleteBackward,
  insertAtCursor,
  moveCursorEnd,
  moveCursorHome,
  moveCursorLeft,
  moveCursorRight,
  pushSubmittedLine,
  resetLiveDraftFields,
  type InputNavState,
} from '../src/inputHistoryNav.js'

function state(partial: Partial<InputNavState>): InputNavState {
  return { ...createInitialInputNavState(), ...partial }
}

describe('inputHistoryNav', () => {
  test('pushSubmittedLine stores trimmed newest-first and caps length', () => {
    let lines = pushSubmittedLine([], '  a  ')
    expect(lines).toEqual(['a'])
    lines = pushSubmittedLine(lines, 'b')
    expect(lines).toEqual(['b', 'a'])
    lines = Array.from({ length: MAX_INPUT_COMMAND_HISTORY }, (_, i) => `h${i}`)
    lines = pushSubmittedLine(lines, 'new')
    expect(lines.length).toBe(MAX_INPUT_COMMAND_HISTORY)
    expect(lines[0]).toBe('new')
    expect(lines[MAX_INPUT_COMMAND_HISTORY - 1]).toBe(
      `h${MAX_INPUT_COMMAND_HISTORY - 2}`
    )
  })

  test('first ↑ moves cursor to start when not browsing', () => {
    const s0 = state({ buffer: 'ab', cursorOffset: 2, submittedLines: ['x'] })
    const s1 = applyArrowUp(s0)
    expect(s1.buffer).toBe('ab')
    expect(s1.cursorOffset).toBe(0)
    expect(s1.historyBrowseIndex).toBe(null)
  })

  test('↑ at column 0 with history enters newest line; cursor at start', () => {
    const s0 = state({
      buffer: 'draft',
      cursorOffset: 0,
      submittedLines: ['newest', 'older'],
    })
    const s1 = applyArrowUp(s0)
    expect(s1.historyDraftCache).toBe('draft')
    expect(s1.historyBrowseIndex).toBe(0)
    expect(s1.buffer).toBe('newest')
    expect(s1.cursorOffset).toBe(0)
  })

  test('↑ while browsing moves to older entry; cursor at start', () => {
    const s0 = state({
      buffer: 'newest',
      cursorOffset: 0,
      submittedLines: ['newest', 'older'],
      historyBrowseIndex: 0,
      historyDraftCache: 'draft',
    })
    const s1 = applyArrowUp(s0)
    expect(s1.historyBrowseIndex).toBe(1)
    expect(s1.buffer).toBe('older')
    expect(s1.cursorOffset).toBe(0)
  })

  test('↑ at oldest history is no-op', () => {
    const s0 = state({
      buffer: 'older',
      cursorOffset: 0,
      submittedLines: ['newest', 'older'],
      historyBrowseIndex: 1,
      historyDraftCache: 'd',
    })
    expect(applyArrowUp(s0)).toEqual(s0)
  })

  test('first ↓ moves cursor to end when not browsing', () => {
    const s0 = state({ buffer: 'ab', cursorOffset: 0 })
    const s1 = applyArrowDown(s0)
    expect(s1.buffer).toBe('ab')
    expect(s1.cursorOffset).toBe(2)
  })

  test('↓ within history moves to newer; cursor at end', () => {
    const s0 = state({
      buffer: 'older',
      cursorOffset: 0,
      submittedLines: ['newest', 'older'],
      historyBrowseIndex: 1,
      historyDraftCache: 'draft',
    })
    const s1 = applyArrowDown(s0)
    expect(s1.historyBrowseIndex).toBe(0)
    expect(s1.buffer).toBe('newest')
    expect(s1.cursorOffset).toBe('newest'.length)
  })

  test('↓ past newest restores pre-history draft; cursor at end', () => {
    const s0 = state({
      buffer: 'newest',
      cursorOffset: 3,
      submittedLines: ['newest', 'older'],
      historyBrowseIndex: 0,
      historyDraftCache: 'draft',
    })
    const s1 = applyArrowDown(s0)
    expect(s1.historyBrowseIndex).toBe(null)
    expect(s1.historyDraftCache).toBe(null)
    expect(s1.buffer).toBe('draft')
    expect(s1.cursorOffset).toBe('draft'.length)
  })

  test('↓ past newest with null cache restores empty buffer', () => {
    const s0 = state({
      buffer: 'only',
      cursorOffset: 4,
      submittedLines: ['only'],
      historyBrowseIndex: 0,
      historyDraftCache: null,
    })
    const s1 = applyArrowDown(s0)
    expect(s1.buffer).toBe('')
    expect(s1.cursorOffset).toBe(0)
  })

  test('insert clears browse state', () => {
    const s0 = state({
      buffer: 'hist',
      cursorOffset: 0,
      submittedLines: ['hist'],
      historyBrowseIndex: 0,
      historyDraftCache: 'x',
    })
    const s1 = insertAtCursor(s0, 'Z')
    expect(s1.historyBrowseIndex).toBe(null)
    expect(s1.historyDraftCache).toBe(null)
    expect(s1.buffer).toBe('Zhist')
    expect(s1.cursorOffset).toBe(1)
  })

  test('deleteBackward clears browse and deletes before cursor', () => {
    const s0 = state({ buffer: 'ab', cursorOffset: 1 })
    expect(deleteBackward(s0).buffer).toBe('b')
    expect(deleteBackward(s0).cursorOffset).toBe(0)
    const browsing = state({
      buffer: 'x',
      cursorOffset: 1,
      submittedLines: ['x'],
      historyBrowseIndex: 0,
      historyDraftCache: 'd',
    })
    const after = deleteBackward(browsing)
    expect(after.historyBrowseIndex).toBe(null)
    expect(after.buffer).toBe('')
  })

  test('left/right/home/end', () => {
    const s0 = state({ buffer: 'abcd', cursorOffset: 2 })
    expect(moveCursorLeft(s0).cursorOffset).toBe(1)
    expect(moveCursorRight(s0).cursorOffset).toBe(3)
    expect(moveCursorHome(s0).cursorOffset).toBe(0)
    expect(moveCursorEnd(s0).cursorOffset).toBe(4)
  })

  test('resetLiveDraftFields clears buffer and browse, keeps submittedLines', () => {
    const s0 = state({
      buffer: 'x',
      cursorOffset: 1,
      submittedLines: ['a'],
      historyBrowseIndex: 0,
      historyDraftCache: 'd',
    })
    const s1 = resetLiveDraftFields(s0)
    expect(s1.buffer).toBe('')
    expect(s1.cursorOffset).toBe(0)
    expect(s1.submittedLines).toEqual(['a'])
    expect(s1.historyBrowseIndex).toBe(null)
    expect(s1.historyDraftCache).toBe(null)
  })
})
