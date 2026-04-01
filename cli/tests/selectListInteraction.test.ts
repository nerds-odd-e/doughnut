import { describe, expect, it, vi } from 'vitest'
import {
  choiceIndexFromSelectListSubmitLine,
  cycleListSelectionIndex,
  handleSelectListInkKey,
  selectListKeyEventFromInk,
  selectListSubmitLineForSlashAndNumber,
  selectListSubmitLineIsInvalidChoice,
} from '../src/interactions/selectListInteraction.js'

describe('cycleListSelectionIndex', () => {
  it('wraps forward and backward', () => {
    expect(cycleListSelectionIndex(0, 1, 3)).toBe(1)
    expect(cycleListSelectionIndex(2, 1, 3)).toBe(0)
    expect(cycleListSelectionIndex(0, -1, 3)).toBe(2)
  })
})

describe('selectListSubmitLineForSlashAndNumber', () => {
  it('passes through /stop and /contest', () => {
    expect(selectListSubmitLineForSlashAndNumber('/stop', 2, 0)).toBe('/stop')
    expect(selectListSubmitLineForSlashAndNumber('/contest', 2, 0)).toBe(
      '/contest'
    )
  })
  it('uses typed 1..choiceCount', () => {
    expect(selectListSubmitLineForSlashAndNumber('1', 3, 2)).toBe('1')
    expect(selectListSubmitLineForSlashAndNumber('3', 3, 0)).toBe('3')
  })
  it('empty draft confirms highlight; non-empty invalid passes through', () => {
    expect(selectListSubmitLineForSlashAndNumber('', 3, 0)).toBe('1')
    expect(selectListSubmitLineForSlashAndNumber('', 3, 1)).toBe('2')
    expect(selectListSubmitLineForSlashAndNumber('99', 3, 1)).toBe('99')
    expect(selectListSubmitLineForSlashAndNumber('abc', 3, 2)).toBe('abc')
    expect(
      choiceIndexFromSelectListSubmitLine(
        selectListSubmitLineForSlashAndNumber('99', 3, 1),
        3
      )
    ).toBeNull()
  })
  it('treats 0 as out of range, passes draft through', () => {
    expect(selectListSubmitLineForSlashAndNumber('0', 3, 1)).toBe('0')
  })
})

describe('choiceIndexFromSelectListSubmitLine', () => {
  it('returns null for slash commands', () => {
    expect(choiceIndexFromSelectListSubmitLine('/stop', 3)).toBeNull()
    expect(choiceIndexFromSelectListSubmitLine('/contest', 3)).toBeNull()
  })
  it('returns 0-based index for valid 1-based numbers', () => {
    expect(choiceIndexFromSelectListSubmitLine('1', 3)).toBe(0)
    expect(choiceIndexFromSelectListSubmitLine('  3  ', 3)).toBe(2)
  })
  it('returns null for out-of-range or non-numeric', () => {
    expect(choiceIndexFromSelectListSubmitLine('0', 3)).toBeNull()
    expect(choiceIndexFromSelectListSubmitLine('4', 3)).toBeNull()
    expect(choiceIndexFromSelectListSubmitLine('x', 3)).toBeNull()
  })
})

describe('selectListSubmitLineIsInvalidChoice', () => {
  it('false for reserved slashes and valid indices', () => {
    expect(selectListSubmitLineIsInvalidChoice('/stop', 3)).toBe(false)
    expect(selectListSubmitLineIsInvalidChoice('/contest', 3)).toBe(false)
    expect(selectListSubmitLineIsInvalidChoice('1', 3)).toBe(false)
    expect(selectListSubmitLineIsInvalidChoice('  3  ', 3)).toBe(false)
  })
  it('true for out-of-range, non-numeric, empty', () => {
    expect(selectListSubmitLineIsInvalidChoice('0', 3)).toBe(true)
    expect(selectListSubmitLineIsInvalidChoice('4', 3)).toBe(true)
    expect(selectListSubmitLineIsInvalidChoice('abc', 3)).toBe(true)
    expect(selectListSubmitLineIsInvalidChoice('', 3)).toBe(true)
  })
})

describe('handleSelectListInkKey', () => {
  const emptyKey = {}

  it('highlight-only: moves highlight and submits index', () => {
    const onSetHighlightIndex = vi.fn()
    const onSubmitHighlightIndex = vi.fn()
    handleSelectListInkKey(
      '',
      { upArrow: true },
      '',
      1,
      3,
      { kind: 'highlight-only' },
      'abort-list',
      { onSetHighlightIndex, onSubmitHighlightIndex }
    )
    expect(onSetHighlightIndex).toHaveBeenCalledWith(0)

    handleSelectListInkKey(
      '\r',
      { return: true },
      '',
      2,
      3,
      { kind: 'highlight-only' },
      'abort-list',
      { onSetHighlightIndex, onSubmitHighlightIndex }
    )
    expect(onSubmitHighlightIndex).toHaveBeenCalledWith(2)
  })

  it('highlight-only: abort calls onAbortHighlightOnlyList', () => {
    const onAbortHighlightOnlyList = vi.fn()
    handleSelectListInkKey(
      '\u001b',
      {},
      '',
      0,
      2,
      { kind: 'highlight-only' },
      'abort-list',
      {
        onSetHighlightIndex: vi.fn(),
        onSubmitHighlightIndex: vi.fn(),
        onAbortHighlightOnlyList,
      }
    )
    expect(onAbortHighlightOnlyList).toHaveBeenCalledOnce()
  })

  it('slash-and-number: edits draft and submits parsed line', () => {
    const onSetHighlightIndex = vi.fn()
    const onSubmitHighlightIndex = vi.fn()
    const onSubmitWithLine = vi.fn()
    handleSelectListInkKey(
      '2',
      emptyKey,
      '',
      0,
      3,
      {
        kind: 'slash-and-number-or-highlight',
        choiceCount: 3,
      },
      'signal-escape',
      {
        onSetHighlightIndex,
        onSubmitHighlightIndex,
        onSubmitWithLine,
        onEditChar: vi.fn(),
      }
    )
    expect(onSubmitWithLine).not.toHaveBeenCalled()

    handleSelectListInkKey(
      '\r',
      { return: true },
      '2',
      0,
      3,
      {
        kind: 'slash-and-number-or-highlight',
        choiceCount: 3,
      },
      'signal-escape',
      {
        onSetHighlightIndex,
        onSubmitHighlightIndex,
        onSubmitWithLine,
      }
    )
    expect(onSubmitWithLine).toHaveBeenCalledWith('2')
  })

  it('slash-and-number: escape signals only', () => {
    const onEscapeSignaled = vi.fn()
    handleSelectListInkKey(
      '\u001b',
      {},
      '',
      0,
      2,
      {
        kind: 'slash-and-number-or-highlight',
        choiceCount: 2,
      },
      'signal-escape',
      {
        onSetHighlightIndex: vi.fn(),
        onSubmitHighlightIndex: vi.fn(),
        onEscapeSignaled,
      }
    )
    expect(onEscapeSignaled).toHaveBeenCalledOnce()
  })

  it('highlight-only: escape with signal-escape calls onEscapeSignaled', () => {
    const onEscapeSignaled = vi.fn()
    handleSelectListInkKey(
      '\u001b',
      {},
      '',
      0,
      2,
      { kind: 'highlight-only' },
      'signal-escape',
      {
        onSetHighlightIndex: vi.fn(),
        onSubmitHighlightIndex: vi.fn(),
        onEscapeSignaled,
      }
    )
    expect(onEscapeSignaled).toHaveBeenCalledOnce()
  })

  it('highlight-only: down arrow moves highlight', () => {
    const onSetHighlightIndex = vi.fn()
    handleSelectListInkKey(
      '',
      { downArrow: true },
      '',
      1,
      3,
      { kind: 'highlight-only' },
      'abort-list',
      { onSetHighlightIndex, onSubmitHighlightIndex: vi.fn() }
    )
    expect(onSetHighlightIndex).toHaveBeenCalledWith(2)
  })

  it('highlight-only: shift+Enter aborts list', () => {
    const onAbortHighlightOnlyList = vi.fn()
    handleSelectListInkKey(
      '\r',
      { return: true, shift: true },
      '',
      0,
      2,
      { kind: 'highlight-only' },
      'abort-list',
      {
        onSetHighlightIndex: vi.fn(),
        onSubmitHighlightIndex: vi.fn(),
        onAbortHighlightOnlyList,
      }
    )
    expect(onAbortHighlightOnlyList).toHaveBeenCalledOnce()
  })

  it('slash-and-number: Enter maps trimmed draft via selectListSubmitLineForSlashAndNumber', () => {
    const onSubmitWithLine = vi.fn()
    const handlers = {
      onSetHighlightIndex: vi.fn(),
      onSubmitHighlightIndex: vi.fn(),
      onSubmitWithLine,
    }
    const slash = {
      kind: 'slash-and-number-or-highlight' as const,
      choiceCount: 3,
    }
    handleSelectListInkKey(
      '\r',
      { return: true },
      '  2  ',
      0,
      3,
      slash,
      'signal-escape',
      handlers
    )
    expect(onSubmitWithLine).toHaveBeenCalledWith('2')
    handleSelectListInkKey(
      '\r',
      { return: true },
      '  /stop  ',
      0,
      3,
      slash,
      'signal-escape',
      handlers
    )
    expect(onSubmitWithLine).toHaveBeenCalledWith('/stop')
  })

  it('slash-and-number: invalid draft is passed through, not coerced to highlight', () => {
    const onSubmitWithLine = vi.fn()
    const handlers = {
      onSetHighlightIndex: vi.fn(),
      onSubmitHighlightIndex: vi.fn(),
      onSubmitWithLine,
    }
    const policy = {
      kind: 'slash-and-number-or-highlight' as const,
      choiceCount: 3,
    }
    handleSelectListInkKey(
      '\r',
      { return: true },
      '99',
      1,
      3,
      policy,
      'signal-escape',
      handlers
    )
    expect(onSubmitWithLine).toHaveBeenCalledWith('99')
  })

  it('slash-and-number: invalid draft invokes onInvalidSelectListSubmitLine when provided', () => {
    const onSubmitWithLine = vi.fn()
    const onInvalidSelectListSubmitLine = vi.fn()
    const policy = {
      kind: 'slash-and-number-or-highlight' as const,
      choiceCount: 3,
    }
    handleSelectListInkKey(
      '\r',
      { return: true },
      '99',
      1,
      3,
      policy,
      'signal-escape',
      {
        onSetHighlightIndex: vi.fn(),
        onSubmitHighlightIndex: vi.fn(),
        onSubmitWithLine,
        onInvalidSelectListSubmitLine,
      }
    )
    expect(onInvalidSelectListSubmitLine).toHaveBeenCalledOnce()
    expect(onSubmitWithLine).not.toHaveBeenCalled()
  })

  it('slash-and-number: /stop still reaches onSubmitWithLine when onInvalid provided', () => {
    const onSubmitWithLine = vi.fn()
    const onInvalidSelectListSubmitLine = vi.fn()
    const policy = {
      kind: 'slash-and-number-or-highlight' as const,
      choiceCount: 3,
    }
    handleSelectListInkKey(
      '\r',
      { return: true },
      '  /stop  ',
      0,
      3,
      policy,
      'signal-escape',
      {
        onSetHighlightIndex: vi.fn(),
        onSubmitHighlightIndex: vi.fn(),
        onSubmitWithLine,
        onInvalidSelectListSubmitLine,
      }
    )
    expect(onInvalidSelectListSubmitLine).not.toHaveBeenCalled()
    expect(onSubmitWithLine).toHaveBeenCalledWith('/stop')
  })

  it('slash-and-number: backspace edits; highlight-only backspace aborts', () => {
    const onEditBackspace = vi.fn()
    handleSelectListInkKey(
      '',
      { backspace: true },
      '',
      0,
      2,
      { kind: 'slash-and-number-or-highlight', choiceCount: 2 },
      'abort-list',
      {
        onSetHighlightIndex: vi.fn(),
        onSubmitHighlightIndex: vi.fn(),
        onEditBackspace,
      }
    )
    expect(onEditBackspace).toHaveBeenCalledOnce()

    const onAbortHighlightOnlyList = vi.fn()
    handleSelectListInkKey(
      '',
      { backspace: true },
      '',
      0,
      2,
      { kind: 'highlight-only' },
      'abort-list',
      {
        onSetHighlightIndex: vi.fn(),
        onSubmitHighlightIndex: vi.fn(),
        onAbortHighlightOnlyList,
      }
    )
    expect(onAbortHighlightOnlyList).toHaveBeenCalledOnce()
  })

  it('slash-and-number: typed char edits; highlight-only typed char aborts; ctrl suppresses char', () => {
    const onEditChar = vi.fn()
    handleSelectListInkKey(
      'a',
      {},
      '',
      0,
      2,
      { kind: 'slash-and-number-or-highlight', choiceCount: 2 },
      'signal-escape',
      {
        onSetHighlightIndex: vi.fn(),
        onSubmitHighlightIndex: vi.fn(),
        onEditChar,
      }
    )
    expect(onEditChar).toHaveBeenCalledWith('a')

    const onAbortHighlightOnlyList = vi.fn()
    handleSelectListInkKey(
      'a',
      {},
      '',
      0,
      2,
      { kind: 'highlight-only' },
      'abort-list',
      {
        onSetHighlightIndex: vi.fn(),
        onSubmitHighlightIndex: vi.fn(),
        onAbortHighlightOnlyList,
      }
    )
    expect(onAbortHighlightOnlyList).toHaveBeenCalledOnce()

    const onRedraw = vi.fn()
    handleSelectListInkKey(
      'a',
      { ctrl: true },
      '',
      0,
      2,
      { kind: 'slash-and-number-or-highlight', choiceCount: 2 },
      'abort-list',
      {
        onSetHighlightIndex: vi.fn(),
        onSubmitHighlightIndex: vi.fn(),
        onRedraw,
      }
    )
    expect(onRedraw).toHaveBeenCalledOnce()
  })

  it('unhandled key: slash-and-number redraws; highlight-only aborts', () => {
    const onRedraw = vi.fn()
    handleSelectListInkKey(
      '',
      { name: 'f1' },
      '',
      0,
      2,
      { kind: 'slash-and-number-or-highlight', choiceCount: 2 },
      'abort-list',
      {
        onSetHighlightIndex: vi.fn(),
        onSubmitHighlightIndex: vi.fn(),
        onRedraw,
      }
    )
    expect(onRedraw).toHaveBeenCalledOnce()

    const onAbortHighlightOnlyList = vi.fn()
    handleSelectListInkKey(
      '',
      { name: 'f1' },
      '',
      0,
      2,
      { kind: 'highlight-only' },
      'abort-list',
      {
        onSetHighlightIndex: vi.fn(),
        onSubmitHighlightIndex: vi.fn(),
        onAbortHighlightOnlyList,
      }
    )
    expect(onAbortHighlightOnlyList).toHaveBeenCalledOnce()
  })
})

describe('selectListKeyEventFromInk', () => {
  const emptyKey = {}

  it('treats bare CR/LF as submit, not typed text', () => {
    expect(selectListKeyEventFromInk('\n', emptyKey, 'draft')).toMatchObject({
      submitPressed: true,
      str: undefined,
      lineDraft: 'draft',
    })
    expect(selectListKeyEventFromInk('\r', emptyKey, '')).toMatchObject({
      submitPressed: true,
      str: undefined,
    })
  })

  it('maps a plain character with empty key flags', () => {
    expect(selectListKeyEventFromInk('a', emptyKey, '')).toMatchObject({
      submitPressed: false,
      str: 'a',
    })
  })
})
