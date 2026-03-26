import { describe, test, expect } from 'vitest'
import {
  MAX_COMMITTED_COMMANDS,
  afterBareSlashEscape,
  appendCommittedCommand,
  applyLastLineEdit,
  caretOneLeft,
  clearLiveCommandLine,
  deleteBeforeCaret,
  emptyInteractiveCommandInput,
  insertIntoDraft,
  lineDraftAfterEscapingBareSlash,
  normalizeRecalledLineDraftForSlashSuggestionExit,
  onArrowDown,
  onArrowUp,
  singleLineCommandDraft,
  ttyArrowKeyUsesSlashSuggestionCycle,
  type InteractiveCommandInput,
  type SlashSuggestionPickerApplies,
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

  test('collapses newlines to spaces before trim', () => {
    expect(appendCommittedCommand([], 'a\nb')).toEqual(['a b'])
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

describe('singleLineCommandDraft', () => {
  test('replaces newlines with spaces', () => {
    expect(singleLineCommandDraft('x\ny')).toBe('x y')
    expect(singleLineCommandDraft('a\r\nb')).toBe('a b')
  })
})

describe('lineDraftAfterEscapingBareSlash', () => {
  test('returns empty string when the draft is only `/`', () => {
    expect(lineDraftAfterEscapingBareSlash('/')).toBe('')
  })

  test('normalizes then leaves non-`/` drafts unchanged aside from newlines', () => {
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
  test('replaces the draft, ends history walk, and puts the caret at end', () => {
    const out = applyLastLineEdit(
      commandInputWith({
        lineDraft: 'ab',
        historyWalkIndex: 0,
        lineDraftBeforeHistoryWalk: 'z',
      }),
      '/help '
    )
    expect(out.lineDraft).toBe('/help ')
    expect(out.caretOffset).toBe(out.lineDraft.length)
    expect(out.historyWalkIndex).toBe(null)
    expect(out.lineDraftBeforeHistoryWalk).toBe(null)
  })
})

describe('Input command history: slash suggestion picker vs ↑↓ while editing live draft', () => {
  test('first ↑ moves caret home without cycling highlights when the picker would apply', () => {
    const lineDraft = '/help_tty_arrow_up'
    const state = commandInputWith({
      lineDraft,
      caretOffset: 2,
      historyWalkIndex: null,
      committedCommands: ['/version'],
    })
    expect(
      ttyArrowKeyUsesSlashSuggestionCycle('up', state, false, true),
      'slash cycling must wait until the caret is at column 0'
    ).toBe(false)
    const after = onArrowUp(state)
    expect(after.lineDraft).toBe(lineDraft)
    expect(after.caretOffset).toBe(0)
    expect(after.historyWalkIndex).toBe(null)
  })

  test('first ↓ moves caret to end without cycling highlights when the picker would apply', () => {
    const lineDraft = '/help_tty_arrow_down'
    const state = commandInputWith({
      lineDraft,
      caretOffset: 2,
      historyWalkIndex: null,
      committedCommands: ['/version'],
    })
    expect(
      ttyArrowKeyUsesSlashSuggestionCycle('down', state, false, true),
      'slash cycling must wait until the caret is at the end of the draft'
    ).toBe(false)
    const after = onArrowDown(state)
    expect(after.lineDraft).toBe(lineDraft)
    expect(after.caretOffset).toBe(lineDraft.length)
    expect(after.historyWalkIndex).toBe(null)
  })
})

describe('Input command history: recalled drafts leave slash suggestion picker', () => {
  test('normalizeRecalledLineDraftForSlashSuggestionExit appends a trailing space when needed', () => {
    expect(
      normalizeRecalledLineDraftForSlashSuggestionExit('/help_hist', true)
    ).toBe('/help_hist ')
    expect(
      normalizeRecalledLineDraftForSlashSuggestionExit('/help_hist', false)
    ).toBe('/help_hist')
    expect(
      normalizeRecalledLineDraftForSlashSuggestionExit('a\n/help_hist', true)
    ).toBe('a /help_hist ')
  })

  test('↑↓ through history still walks entries when a recalled line would have opened the picker', () => {
    const pickerWouldApply: SlashSuggestionPickerApplies = (d) =>
      d === '/help_hist'

    const intoNewest = onArrowUp(
      commandInputWith({
        lineDraft: 'draft',
        caretOffset: 0,
        committedCommands: ['/help_hist', '/hist_beta'],
      }),
      pickerWouldApply
    )
    expect(intoNewest.historyWalkIndex).toBe(0)
    expect(intoNewest.lineDraft).toBe('/help_hist ')
    expect(intoNewest.caretOffset).toBe(0)

    const fromOlder = onArrowUp(
      commandInputWith({
        lineDraft: 'draft',
        caretOffset: 0,
        committedCommands: ['/hist_beta', '/help_hist'],
      }),
      pickerWouldApply
    )
    expect(fromOlder.historyWalkIndex).toBe(0)
    expect(fromOlder.lineDraft).toBe('/hist_beta')

    const showingHelp = onArrowUp(fromOlder, pickerWouldApply)
    expect(showingHelp.historyWalkIndex).toBe(1)
    expect(showingHelp.lineDraft).toBe('/help_hist ')

    const backToBeta = onArrowDown(showingHelp, pickerWouldApply)
    expect(backToBeta.historyWalkIndex).toBe(0)
    expect(backToBeta.lineDraft).toBe('/hist_beta')
    expect(backToBeta.caretOffset).toBe('/hist_beta'.length)
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

  test('turns pasted newlines into spaces', () => {
    const out = insertIntoDraft(
      commandInputWith({ lineDraft: 'a', caretOffset: 1 }),
      'x\ny'
    )
    expect(out.lineDraft).toBe('ax y')
    expect(out.caretOffset).toBe(4)
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
