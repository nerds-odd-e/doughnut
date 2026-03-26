import { describe, test, expect } from 'vitest'
import {
  MAX_USER_INPUT_HISTORY_LINES,
  afterBareSlashEscape,
  appendUserInputHistoryLine,
  caretOneLeft,
  clearLiveCommandLine,
  deleteBeforeCaret,
  emptyInteractiveCommandInput,
  insertIntoDraft,
  onArrowDown,
  onArrowUp,
  replaceLiveCommandDraft,
  singleLineCommandDraft,
  ttyArrowKeyUsesSlashSuggestionCycle,
  type InteractiveCommandInput,
} from '../src/interactiveCommandInput.js'

function commandInputWith(
  partial: Partial<InteractiveCommandInput>
): InteractiveCommandInput {
  return { ...emptyInteractiveCommandInput(), ...partial }
}

describe('appendUserInputHistoryLine', () => {
  test('ignores whitespace-only lines', () => {
    expect(appendUserInputHistoryLine([], '   ')).toEqual([])
  })

  test('stores trimmed text', () => {
    expect(appendUserInputHistoryLine([], '  a  ')).toEqual(['a'])
  })

  test('collapses newlines to spaces before trim', () => {
    expect(appendUserInputHistoryLine([], 'a\nb')).toEqual(['a b'])
  })

  test('prepends so the newest commit is first', () => {
    const once = appendUserInputHistoryLine([], 'a')
    expect(appendUserInputHistoryLine(once, 'b')).toEqual(['b', 'a'])
  })

  test('does not prepend when identical to the newest entry', () => {
    const once = appendUserInputHistoryLine([], 'x')
    expect(appendUserInputHistoryLine(once, 'x')).toEqual(['x'])
    expect(appendUserInputHistoryLine(once, '  x  ')).toEqual(['x'])
  })

  test('prepends again when same text is not consecutive with newest', () => {
    const bab = appendUserInputHistoryLine(
      appendUserInputHistoryLine(appendUserInputHistoryLine([], 'a'), 'b'),
      'a'
    )
    expect(bab).toEqual(['a', 'b', 'a'])
  })

  test('drops the oldest entry when over the max length', () => {
    const full = Array.from(
      { length: MAX_USER_INPUT_HISTORY_LINES },
      (_, i) => `h${i}`
    )
    const next = appendUserInputHistoryLine(full, 'new')
    expect(next.length).toBe(MAX_USER_INPUT_HISTORY_LINES)
    expect(next[0]).toBe('new')
    expect(next[MAX_USER_INPUT_HISTORY_LINES - 1]).toBe(
      `h${MAX_USER_INPUT_HISTORY_LINES - 2}`
    )
  })

  test('duplicate of newest when full does not grow or duplicate at front', () => {
    const full = Array.from(
      { length: MAX_USER_INPUT_HISTORY_LINES },
      (_, i) => `h${i}`
    )
    const newest = full[0]!
    const next = appendUserInputHistoryLine(full, newest)
    expect(next.length).toBe(MAX_USER_INPUT_HISTORY_LINES)
    expect(next[0]).toBe(newest)
    expect(next[1]).toBe(full[1])
  })
})

describe('singleLineCommandDraft', () => {
  test('replaces newlines with spaces', () => {
    expect(singleLineCommandDraft('x\ny')).toBe('x y')
    expect(singleLineCommandDraft('a\r\nb')).toBe('a b')
  })
})

describe('afterBareSlashEscape', () => {
  test('clears a draft that is only `/`, ends user input history walk, caret at end', () => {
    const out = afterBareSlashEscape(
      commandInputWith({
        lineDraft: '/',
        userInputHistoryWalkIndex: 0,
        lineDraftBeforeUserInputHistoryWalk: 'x',
      })
    )
    expect(out.lineDraft).toBe('')
    expect(out.caretOffset).toBe(0)
    expect(out.userInputHistoryWalkIndex).toBe(null)
    expect(out.lineDraftBeforeUserInputHistoryWalk).toBe(null)
  })

  test('leaves `/foo` unchanged (only a lone `/` is dismissed)', () => {
    const out = afterBareSlashEscape(
      commandInputWith({ lineDraft: '/foo', caretOffset: 4 })
    )
    expect(out.lineDraft).toBe('/foo')
    expect(out.caretOffset).toBe(4)
  })
})

describe('replaceLiveCommandDraft', () => {
  test('replaces the draft, ends user input history walk, caret at end', () => {
    const out = replaceLiveCommandDraft(
      commandInputWith({
        lineDraft: 'ab',
        userInputHistoryWalkIndex: 0,
        lineDraftBeforeUserInputHistoryWalk: 'z',
      }),
      '/help '
    )
    expect(out.lineDraft).toBe('/help ')
    expect(out.caretOffset).toBe(out.lineDraft.length)
    expect(out.userInputHistoryWalkIndex).toBe(null)
    expect(out.lineDraftBeforeUserInputHistoryWalk).toBe(null)
  })
})

describe('User input history: slash suggestion picker vs ↑↓ while editing live draft', () => {
  test('first ↑ moves caret home without cycling highlights when the picker would apply', () => {
    const lineDraft = '/help_tty_arrow_up'
    const state = commandInputWith({
      lineDraft,
      caretOffset: 2,
      userInputHistoryWalkIndex: null,
      userInputHistoryLines: ['/version'],
    })
    expect(
      ttyArrowKeyUsesSlashSuggestionCycle('up', state, false, true),
      'slash cycling must wait until the caret is at column 0'
    ).toBe(false)
    const after = onArrowUp(state)
    expect(after.lineDraft).toBe(lineDraft)
    expect(after.caretOffset).toBe(0)
    expect(after.userInputHistoryWalkIndex).toBe(null)
  })

  test('first ↓ moves caret to end without cycling highlights when the picker would apply', () => {
    const lineDraft = '/help_tty_arrow_down'
    const state = commandInputWith({
      lineDraft,
      caretOffset: 2,
      userInputHistoryWalkIndex: null,
      userInputHistoryLines: ['/version'],
    })
    expect(
      ttyArrowKeyUsesSlashSuggestionCycle('down', state, false, true),
      'slash cycling must wait until the caret is at the end of the draft'
    ).toBe(false)
    const after = onArrowDown(state)
    expect(after.lineDraft).toBe(lineDraft)
    expect(after.caretOffset).toBe(lineDraft.length)
    expect(after.userInputHistoryWalkIndex).toBe(null)
  })
})

describe('User input history: recalled drafts leave slash suggestion picker', () => {
  test('↑↓ through user input history when a recalled line would open the picker (test predicate)', () => {
    const slashPickerWouldApplyForDraft = (d: string) => d === '/help_hist'

    const intoNewest = onArrowUp(
      commandInputWith({
        lineDraft: 'draft',
        caretOffset: 0,
        userInputHistoryLines: ['/help_hist', '/hist_beta'],
      }),
      slashPickerWouldApplyForDraft
    )
    expect(intoNewest.userInputHistoryWalkIndex).toBe(0)
    expect(intoNewest.lineDraft).toBe('/help_hist ')
    expect(intoNewest.caretOffset).toBe(0)

    const fromOlder = onArrowUp(
      commandInputWith({
        lineDraft: 'draft',
        caretOffset: 0,
        userInputHistoryLines: ['/hist_beta', '/help_hist'],
      }),
      slashPickerWouldApplyForDraft
    )
    expect(fromOlder.userInputHistoryWalkIndex).toBe(0)
    expect(fromOlder.lineDraft).toBe('/hist_beta')

    const showingHelp = onArrowUp(fromOlder, slashPickerWouldApplyForDraft)
    expect(showingHelp.userInputHistoryWalkIndex).toBe(1)
    expect(showingHelp.lineDraft).toBe('/help_hist ')

    const backToBeta = onArrowDown(showingHelp, slashPickerWouldApplyForDraft)
    expect(backToBeta.userInputHistoryWalkIndex).toBe(0)
    expect(backToBeta.lineDraft).toBe('/hist_beta')
    expect(backToBeta.caretOffset).toBe('/hist_beta'.length)
  })

  test('recalled line with embedded newline is normalized before the trailing-space rule', () => {
    const out = onArrowUp(
      commandInputWith({
        lineDraft: 'x',
        caretOffset: 0,
        userInputHistoryLines: ['a\n/help_hist'],
      }),
      (d) => d === 'a /help_hist'
    )
    expect(out.lineDraft).toBe('a /help_hist ')
  })
})

describe('onArrowUp', () => {
  test('moves the caret to the start of the draft when still editing live text', () => {
    const out = onArrowUp(
      commandInputWith({
        lineDraft: 'ab',
        caretOffset: 2,
        userInputHistoryLines: ['x'],
      })
    )
    expect(out.lineDraft).toBe('ab')
    expect(out.caretOffset).toBe(0)
    expect(out.userInputHistoryWalkIndex).toBe(null)
  })

  test('loads the newest committed line when the caret is already at the start', () => {
    const afterCaretHome = onArrowUp(
      commandInputWith({
        lineDraft: 'ab',
        caretOffset: 2,
        userInputHistoryLines: ['x'],
      })
    )
    const out = onArrowUp(afterCaretHome)
    expect(out.lineDraftBeforeUserInputHistoryWalk).toBe('ab')
    expect(out.userInputHistoryWalkIndex).toBe(0)
    expect(out.lineDraft).toBe('x')
    expect(out.caretOffset).toBe(0)
  })

  test('does nothing when already showing the oldest committed line', () => {
    const state = commandInputWith({
      lineDraft: 'older',
      userInputHistoryLines: ['newest', 'older'],
      userInputHistoryWalkIndex: 1,
      lineDraftBeforeUserInputHistoryWalk: 'd',
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
        userInputHistoryLines: ['newest', 'older'],
        userInputHistoryWalkIndex: 1,
        lineDraftBeforeUserInputHistoryWalk: 'draft',
      })
    )
    expect(out.userInputHistoryWalkIndex).toBe(0)
    expect(out.lineDraft).toBe('newest')
    expect(out.caretOffset).toBe('newest'.length)
  })

  test('restores the draft that was suspended when walking past the newest commit', () => {
    const out = onArrowDown(
      commandInputWith({
        lineDraft: 'newest',
        caretOffset: 6,
        userInputHistoryLines: ['newest', 'older'],
        userInputHistoryWalkIndex: 0,
        lineDraftBeforeUserInputHistoryWalk: 'draft',
      })
    )
    expect(out.lineDraft).toBe('draft')
    expect(out.caretOffset).toBe(5)
    expect(out.userInputHistoryWalkIndex).toBe(null)
  })

  test('clears the draft when leaving user input history walk with no suspended draft', () => {
    const out = onArrowDown(
      commandInputWith({
        lineDraft: 'only',
        caretOffset: 4,
        userInputHistoryLines: ['only'],
        userInputHistoryWalkIndex: 0,
        lineDraftBeforeUserInputHistoryWalk: null,
      })
    )
    expect(out.lineDraft).toBe('')
    expect(out.caretOffset).toBe(0)
  })
})

describe('insertIntoDraft', () => {
  test('ends user input history walk before inserting text', () => {
    const out = insertIntoDraft(
      commandInputWith({
        lineDraft: 'h',
        caretOffset: 0,
        userInputHistoryLines: ['h'],
        userInputHistoryWalkIndex: 0,
        lineDraftBeforeUserInputHistoryWalk: 'x',
      }),
      'Z'
    )
    expect(out.userInputHistoryWalkIndex).toBe(null)
    expect(out.lineDraftBeforeUserInputHistoryWalk).toBe(null)
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
  test('empties the draft and walk state but keeps user input history lines', () => {
    const out = clearLiveCommandLine(
      commandInputWith({
        lineDraft: 'x',
        caretOffset: 1,
        userInputHistoryLines: ['a'],
        userInputHistoryWalkIndex: 0,
        lineDraftBeforeUserInputHistoryWalk: 'd',
      })
    )
    expect(out.lineDraft).toBe('')
    expect(out.caretOffset).toBe(0)
    expect(out.userInputHistoryLines).toEqual(['a'])
    expect(out.userInputHistoryWalkIndex).toBe(null)
    expect(out.lineDraftBeforeUserInputHistoryWalk).toBe(null)
  })
})
