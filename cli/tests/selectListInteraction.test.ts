import { describe, expect, it } from 'vitest'
import {
  cycleListSelectionIndex,
  dispatchSelectListKey,
  selectListSubmitLineForSlashAndNumber,
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
  it('uses highlighted choice as 1-based when draft is not a listed number', () => {
    expect(selectListSubmitLineForSlashAndNumber('', 3, 0)).toBe('1')
    expect(selectListSubmitLineForSlashAndNumber('99', 3, 1)).toBe('2')
    expect(selectListSubmitLineForSlashAndNumber('abc', 3, 2)).toBe('3')
  })
  it('treats 0 as out of range, not a valid choice index', () => {
    expect(selectListSubmitLineForSlashAndNumber('0', 3, 1)).toBe('2')
  })
})

describe('dispatchSelectListKey', () => {
  const base = {
    ctrl: false,
    meta: false,
    shift: false,
    lineDraft: '',
    submitPressed: false,
  }

  it('escape: abort-list vs signal', () => {
    expect(
      dispatchSelectListKey(
        { ...base, keyName: 'escape' },
        0,
        { kind: 'highlight-only' },
        'abort-list'
      )
    ).toEqual({ result: 'abort-highlight-only-list' })
    expect(
      dispatchSelectListKey(
        { ...base, keyName: 'escape' },
        0,
        { kind: 'highlight-only' },
        'signal-escape'
      )
    ).toEqual({ result: 'escape-signaled' })
  })

  it('up/down deltas', () => {
    expect(
      dispatchSelectListKey(
        { ...base, keyName: 'up' },
        1,
        { kind: 'highlight-only' },
        'abort-list'
      )
    ).toEqual({ result: 'move-highlight', delta: -1 })
    expect(
      dispatchSelectListKey(
        { ...base, keyName: 'down' },
        1,
        { kind: 'highlight-only' },
        'abort-list'
      )
    ).toEqual({ result: 'move-highlight', delta: 1 })
  })

  it('Enter on highlight-only submits index; respects shift+Enter', () => {
    expect(
      dispatchSelectListKey(
        { ...base, submitPressed: true },
        2,
        { kind: 'highlight-only' },
        'abort-list'
      )
    ).toEqual({ result: 'submit-highlight-index', index: 2 })
    expect(
      dispatchSelectListKey(
        { ...base, submitPressed: true, shift: true },
        0,
        { kind: 'highlight-only' },
        'abort-list'
      )
    ).toEqual({ result: 'abort-highlight-only-list' })
  })

  it('Enter with slash-and-number policy maps draft via selectListSubmitLineForSlashAndNumber', () => {
    expect(
      dispatchSelectListKey(
        { ...base, submitPressed: true, lineDraft: '  2  ' },
        0,
        { kind: 'slash-and-number-or-highlight', choiceCount: 3 },
        'signal-escape'
      )
    ).toEqual({ result: 'submit-with-line', lineForProcessInput: '2' })
    expect(
      dispatchSelectListKey(
        { ...base, submitPressed: true, lineDraft: '  /stop  ' },
        0,
        { kind: 'slash-and-number-or-highlight', choiceCount: 3 },
        'signal-escape'
      )
    ).toEqual({ result: 'submit-with-line', lineForProcessInput: '/stop' })
  })

  it('backspace: edit vs abort-highlight-only', () => {
    expect(
      dispatchSelectListKey(
        { ...base, keyName: 'backspace' },
        0,
        { kind: 'slash-and-number-or-highlight', choiceCount: 2 },
        'abort-list'
      )
    ).toEqual({ result: 'edit-backspace' })
    expect(
      dispatchSelectListKey(
        { ...base, keyName: 'backspace' },
        0,
        { kind: 'highlight-only' },
        'abort-list'
      )
    ).toEqual({ result: 'abort-highlight-only-list' })
  })

  it('printable char: edit vs abort-highlight-only; ctrl suppresses edit', () => {
    expect(
      dispatchSelectListKey(
        { ...base, str: 'a' },
        0,
        { kind: 'slash-and-number-or-highlight', choiceCount: 2 },
        'abort-list'
      )
    ).toEqual({ result: 'edit-char', char: 'a' })
    expect(
      dispatchSelectListKey(
        { ...base, str: 'a' },
        0,
        { kind: 'highlight-only' },
        'abort-list'
      )
    ).toEqual({ result: 'abort-highlight-only-list' })
    expect(
      dispatchSelectListKey(
        { ...base, str: 'a', ctrl: true },
        0,
        { kind: 'slash-and-number-or-highlight', choiceCount: 2 },
        'abort-list'
      )
    ).toEqual({ result: 'redraw' })
  })

  it('unhandled keys: redraw vs abort-highlight-only', () => {
    expect(
      dispatchSelectListKey(
        { ...base, keyName: 'f1' },
        0,
        { kind: 'slash-and-number-or-highlight', choiceCount: 2 },
        'abort-list'
      )
    ).toEqual({ result: 'redraw' })
    expect(
      dispatchSelectListKey(
        { ...base, keyName: 'f1' },
        0,
        { kind: 'highlight-only' },
        'abort-list'
      )
    ).toEqual({ result: 'abort-highlight-only-list' })
  })
})
