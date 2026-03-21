import { describe, test, expect } from 'vitest'
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
  ttyArrowKeyUsesSlashSuggestionCycle,
  type InteractiveCommandInput,
} from '../src/interactiveCommandInput.js'

function commandInputWith(
  partial: Partial<InteractiveCommandInput>
): InteractiveCommandInput {
  return { ...emptyInteractiveCommandInput(), ...partial }
}

describe('appendCommittedCommand', () => {
  test('ignores whitespace-only lines', () => {
    expect(appendCommittedCommand([], '   ')).toEqual([])
  })

  test('stores trimmed text', () => {
    expect(appendCommittedCommand([], '  a  ')).toEqual(['a'])
  })

  test('prepends so the newest commit is first', () => {
    const once = appendCommittedCommand([], 'a')
    expect(appendCommittedCommand(once, 'b')).toEqual(['b', 'a'])
  })

  test('drops the oldest entry when over the max length', () => {
    const full = Array.from(
      { length: MAX_COMMITTED_COMMANDS },
      (_, i) => `h${i}`
    )
    const next = appendCommittedCommand(full, 'new')
    expect(next.length).toBe(MAX_COMMITTED_COMMANDS)
    expect(next[0]).toBe('new')
    expect(next[MAX_COMMITTED_COMMANDS - 1]).toBe(
      `h${MAX_COMMITTED_COMMANDS - 2}`
    )
  })
})

describe('replaceLastLogicalLine', () => {
  test('replaces the whole draft when it is a single line', () => {
    expect(replaceLastLogicalLine('a', 'b')).toBe('b')
  })

  test('replaces only the last line when the draft is multiline', () => {
    expect(replaceLastLogicalLine('x\ny', 'z')).toBe('x\nz')
  })
})

describe('lineDraftAfterEscapingBareSlash', () => {
  test('returns empty string when the draft is only `/`', () => {
    expect(lineDraftAfterEscapingBareSlash('/')).toBe('')
  })

  test('drops the last line when it is exactly `/`', () => {
    expect(lineDraftAfterEscapingBareSlash('a\n/')).toBe('a')
  })

  test('returns the draft unchanged when the last line is not bare `/`', () => {
    expect(lineDraftAfterEscapingBareSlash('/ex')).toBe('/ex')
  })
})

describe('afterBareSlashEscape', () => {
  test('clears a one-line `/` draft, ends history walk, and leaves caret at end', () => {
    const out = afterBareSlashEscape(
      commandInputWith({
        lineDraft: '/',
        historyWalkIndex: 0,
        lineDraftBeforeHistoryWalk: 'x',
      })
    )
    expect(out.lineDraft).toBe('')
    expect(out.caretOffset).toBe(0)
    expect(out.historyWalkIndex).toBe(null)
    expect(out.lineDraftBeforeHistoryWalk).toBe(null)
  })
})

describe('applyLastLineEdit', () => {
  test('replaces the last line, ends history walk, and puts the caret at end', () => {
    const out = applyLastLineEdit(
      commandInputWith({
        lineDraft: 'a\nb',
        historyWalkIndex: 0,
        lineDraftBeforeHistoryWalk: 'z',
      }),
      '/help '
    )
    expect(out.lineDraft).toBe('a\n/help ')
    expect(out.caretOffset).toBe(out.lineDraft.length)
    expect(out.historyWalkIndex).toBe(null)
    expect(out.lineDraftBeforeHistoryWalk).toBe(null)
  })
})

describe('TTY slash-suggestion vs ↑ precedence (phase A1)', () => {
  test('first ↑ moves caret to start when not in history and caret is not at 0, even if slash suggestions apply', () => {
    const lineDraft = '/help_PHASE_A1_UNIQUE'
    const state = commandInputWith({
      lineDraft,
      caretOffset: 2,
      historyWalkIndex: null,
      committedCommands: ['/version'],
    })
    expect(
      ttyArrowKeyUsesSlashSuggestionCycle('up', state, false, true),
      'first ↑ must not use slash suggestion cycling while the caret is still away from column 0'
    ).toBe(false)
    const after = onArrowUp(state)
    expect(
      after.lineDraft,
      'first ↑ must leave lineDraft unchanged (no history recall, no suggestion side effects)'
    ).toBe(lineDraft)
    expect(
      after.caretOffset,
      'first ↑ must move the caret to the start of the draft when not in history mode'
    ).toBe(0)
    expect(after.historyWalkIndex).toBe(null)
  })
})

describe('onArrowUp', () => {
  test('moves the caret to the start of the draft when still editing live text', () => {
    const out = onArrowUp(
      commandInputWith({
        lineDraft: 'ab',
        caretOffset: 2,
        committedCommands: ['x'],
      })
    )
    expect(out.lineDraft).toBe('ab')
    expect(out.caretOffset).toBe(0)
    expect(out.historyWalkIndex).toBe(null)
  })

  test('loads the newest committed line when the caret is already at the start', () => {
    const afterCaretHome = onArrowUp(
      commandInputWith({
        lineDraft: 'ab',
        caretOffset: 2,
        committedCommands: ['x'],
      })
    )
    const out = onArrowUp(afterCaretHome)
    expect(out.lineDraftBeforeHistoryWalk).toBe('ab')
    expect(out.historyWalkIndex).toBe(0)
    expect(out.lineDraft).toBe('x')
    expect(out.caretOffset).toBe(0)
  })

  test('does nothing when already showing the oldest committed line', () => {
    const state = commandInputWith({
      lineDraft: 'older',
      committedCommands: ['newest', 'older'],
      historyWalkIndex: 1,
      lineDraftBeforeHistoryWalk: 'd',
    })
    expect(onArrowUp(state)).toEqual(state)
  })
})

describe('onArrowDown', () => {
  test('shows the newer committed line with the caret at its end', () => {
    const out = onArrowDown(
      commandInputWith({
        lineDraft: 'older',
        caretOffset: 0,
        committedCommands: ['newest', 'older'],
        historyWalkIndex: 1,
        lineDraftBeforeHistoryWalk: 'draft',
      })
    )
    expect(out.historyWalkIndex).toBe(0)
    expect(out.lineDraft).toBe('newest')
    expect(out.caretOffset).toBe('newest'.length)
  })

  test('restores the draft that was suspended when walking past the newest commit', () => {
    const out = onArrowDown(
      commandInputWith({
        lineDraft: 'newest',
        caretOffset: 6,
        committedCommands: ['newest', 'older'],
        historyWalkIndex: 0,
        lineDraftBeforeHistoryWalk: 'draft',
      })
    )
    expect(out.lineDraft).toBe('draft')
    expect(out.caretOffset).toBe(5)
    expect(out.historyWalkIndex).toBe(null)
  })

  test('clears the draft when leaving history with no suspended draft', () => {
    const out = onArrowDown(
      commandInputWith({
        lineDraft: 'only',
        caretOffset: 4,
        committedCommands: ['only'],
        historyWalkIndex: 0,
        lineDraftBeforeHistoryWalk: null,
      })
    )
    expect(out.lineDraft).toBe('')
    expect(out.caretOffset).toBe(0)
  })
})

describe('insertIntoDraft', () => {
  test('ends history walk before inserting text', () => {
    const out = insertIntoDraft(
      commandInputWith({
        lineDraft: 'h',
        caretOffset: 0,
        committedCommands: ['h'],
        historyWalkIndex: 0,
        lineDraftBeforeHistoryWalk: 'x',
      }),
      'Z'
    )
    expect(out.historyWalkIndex).toBe(null)
    expect(out.lineDraftBeforeHistoryWalk).toBe(null)
    expect(out.lineDraft).toBe('Zh')
    expect(out.caretOffset).toBe(1)
  })
})

describe('deleteBeforeCaret', () => {
  test('removes the code unit before the caret', () => {
    const out = deleteBeforeCaret(
      commandInputWith({ lineDraft: 'ab', caretOffset: 1 })
    )
    expect(out.lineDraft).toBe('b')
    expect(out.caretOffset).toBe(0)
  })
})

describe('caretOneLeft', () => {
  test('moves the caret one code unit left', () => {
    const out = caretOneLeft(
      commandInputWith({ lineDraft: 'ab', caretOffset: 1 })
    )
    expect(out.caretOffset).toBe(0)
  })
})

describe('caretToDraftStart', () => {
  test('sets the caret to the start of the draft', () => {
    const out = caretToDraftStart(
      commandInputWith({ lineDraft: 'ab', caretOffset: 1 })
    )
    expect(out.caretOffset).toBe(0)
  })
})

describe('caretToDraftEnd', () => {
  test('sets the caret past the last code unit', () => {
    const out = caretToDraftEnd(
      commandInputWith({ lineDraft: 'ab', caretOffset: 0 })
    )
    expect(out.caretOffset).toBe(2)
  })
})

describe('clearLiveCommandLine', () => {
  test('empties the draft and walk state but keeps committed commands', () => {
    const out = clearLiveCommandLine(
      commandInputWith({
        lineDraft: 'x',
        caretOffset: 1,
        committedCommands: ['a'],
        historyWalkIndex: 0,
        lineDraftBeforeHistoryWalk: 'd',
      })
    )
    expect(out.lineDraft).toBe('')
    expect(out.caretOffset).toBe(0)
    expect(out.committedCommands).toEqual(['a'])
    expect(out.historyWalkIndex).toBe(null)
    expect(out.lineDraftBeforeHistoryWalk).toBe(null)
  })
})
