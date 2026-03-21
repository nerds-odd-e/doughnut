import { describe, expect, test } from 'vitest'
import {
  MAX_COMMITTED_COMMANDS,
  afterBareSlashEscape,
  appendCommittedCommand,
  applyLastLineEdit,
  caretOneLeft,
  caretToDraftEnd,
  caretToDraftStart,
  clearLiveCommandLine,
  deleteBeforeCaret,
  emptyInteractiveCommandInput,
  insertIntoDraft,
  lineDraftAfterEscapingBareSlash,
  onArrowDown,
  onArrowUp,
  replaceLastLogicalLine,
  type InteractiveCommandInput,
} from '../src/interactiveCommandInput.js'

function partial(p: Partial<InteractiveCommandInput>): InteractiveCommandInput {
  return { ...emptyInteractiveCommandInput(), ...p }
}

describe('interactiveCommandInput', () => {
  test('appendCommittedCommand trims, newest-first, caps', () => {
    let c = appendCommittedCommand([], '  a  ')
    expect(c).toEqual(['a'])
    c = appendCommittedCommand(c, 'b')
    expect(c).toEqual(['b', 'a'])
    const full = Array.from(
      { length: MAX_COMMITTED_COMMANDS },
      (_, i) => `h${i}`
    )
    c = appendCommittedCommand(full, 'new')
    expect(c.length).toBe(MAX_COMMITTED_COMMANDS)
    expect(c[0]).toBe('new')
    expect(c[MAX_COMMITTED_COMMANDS - 1]).toBe(`h${MAX_COMMITTED_COMMANDS - 2}`)
  })

  test('replaceLastLogicalLine', () => {
    expect(replaceLastLogicalLine('a', 'b')).toBe('b')
    expect(replaceLastLogicalLine('x\ny', 'z')).toBe('x\nz')
  })

  test('lineDraftAfterEscapingBareSlash', () => {
    expect(lineDraftAfterEscapingBareSlash('/')).toBe('')
    expect(lineDraftAfterEscapingBareSlash('a\n/')).toBe('a')
    expect(lineDraftAfterEscapingBareSlash('/ex')).toBe('/ex')
  })

  test('afterBareSlashEscape ends history walk and places caret at end', () => {
    const s = partial({
      lineDraft: '/',
      historyWalkIndex: 0,
      lineDraftBeforeHistoryWalk: 'x',
    })
    const out = afterBareSlashEscape(s)
    expect(out.lineDraft).toBe('')
    expect(out.caretOffset).toBe(0)
    expect(out.historyWalkIndex).toBe(null)
    expect(out.lineDraftBeforeHistoryWalk).toBe(null)
  })

  test('applyLastLineEdit', () => {
    const s = partial({
      lineDraft: 'a\nb',
      historyWalkIndex: 0,
      lineDraftBeforeHistoryWalk: 'z',
    })
    const out = applyLastLineEdit(s, '/help ')
    expect(out.lineDraft).toBe('a\n/help ')
    expect(out.caretOffset).toBe(out.lineDraft.length)
    expect(out.historyWalkIndex).toBe(null)
  })

  test('onArrowUp: caret to draft start, then into history', () => {
    const s0 = partial({
      lineDraft: 'ab',
      caretOffset: 2,
      committedCommands: ['x'],
    })
    const s1 = onArrowUp(s0)
    expect(s1.lineDraft).toBe('ab')
    expect(s1.caretOffset).toBe(0)
    expect(s1.historyWalkIndex).toBe(null)

    const s2 = onArrowUp(s1)
    expect(s2.lineDraftBeforeHistoryWalk).toBe('ab')
    expect(s2.historyWalkIndex).toBe(0)
    expect(s2.lineDraft).toBe('x')
    expect(s2.caretOffset).toBe(0)
  })

  test('onArrowUp at oldest committed line is a no-op', () => {
    const s0 = partial({
      lineDraft: 'older',
      committedCommands: ['newest', 'older'],
      historyWalkIndex: 1,
      lineDraftBeforeHistoryWalk: 'd',
    })
    expect(onArrowUp(s0)).toEqual(s0)
  })

  test('onArrowDown within history moves to newer with caret at end', () => {
    const s0 = partial({
      lineDraft: 'older',
      caretOffset: 0,
      committedCommands: ['newest', 'older'],
      historyWalkIndex: 1,
      lineDraftBeforeHistoryWalk: 'draft',
    })
    const s1 = onArrowDown(s0)
    expect(s1.historyWalkIndex).toBe(0)
    expect(s1.lineDraft).toBe('newest')
    expect(s1.caretOffset).toBe('newest'.length)
  })

  test('onArrowDown restores suspended draft past newest', () => {
    const s0 = partial({
      lineDraft: 'newest',
      caretOffset: 6,
      committedCommands: ['newest', 'older'],
      historyWalkIndex: 0,
      lineDraftBeforeHistoryWalk: 'draft',
    })
    const s1 = onArrowDown(s0)
    expect(s1.lineDraft).toBe('draft')
    expect(s1.caretOffset).toBe(5)
    expect(s1.historyWalkIndex).toBe(null)
  })

  test('onArrowDown past newest with no suspended draft clears line', () => {
    const s0 = partial({
      lineDraft: 'only',
      caretOffset: 4,
      committedCommands: ['only'],
      historyWalkIndex: 0,
      lineDraftBeforeHistoryWalk: null,
    })
    const s1 = onArrowDown(s0)
    expect(s1.lineDraft).toBe('')
    expect(s1.caretOffset).toBe(0)
  })

  test('insertIntoDraft clears history walk', () => {
    const s0 = partial({
      lineDraft: 'h',
      caretOffset: 0,
      committedCommands: ['h'],
      historyWalkIndex: 0,
      lineDraftBeforeHistoryWalk: 'x',
    })
    const s1 = insertIntoDraft(s0, 'Z')
    expect(s1.historyWalkIndex).toBe(null)
    expect(s1.lineDraftBeforeHistoryWalk).toBe(null)
    expect(s1.lineDraft).toBe('Zh')
    expect(s1.caretOffset).toBe(1)
  })

  test('deleteBeforeCaret and caret motion', () => {
    const s0 = partial({ lineDraft: 'ab', caretOffset: 1 })
    expect(deleteBeforeCaret(s0).lineDraft).toBe('b')
    expect(caretOneLeft(s0).caretOffset).toBe(0)
    expect(caretToDraftStart(s0).caretOffset).toBe(0)
    expect(caretToDraftEnd(s0).caretOffset).toBe(2)
  })

  test('clearLiveCommandLine keeps committedCommands', () => {
    const s0 = partial({
      lineDraft: 'x',
      caretOffset: 1,
      committedCommands: ['a'],
      historyWalkIndex: 0,
      lineDraftBeforeHistoryWalk: 'd',
    })
    const s1 = clearLiveCommandLine(s0)
    expect(s1.lineDraft).toBe('')
    expect(s1.caretOffset).toBe(0)
    expect(s1.committedCommands).toEqual(['a'])
    expect(s1.historyWalkIndex).toBe(null)
  })
})
