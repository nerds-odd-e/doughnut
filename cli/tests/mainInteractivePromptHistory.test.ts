import { describe, expect, test } from 'vitest'
import {
  MAX_USER_INPUT_HISTORY_LINES,
  appendUserInputHistoryLine,
  emptyMainInteractivePromptHistoryState,
  exitHistoryWalkOnDraftEdit,
  maskInteractiveInputLineForStorage,
  onArrowDown,
  onArrowUp,
  singleLineCommandDraft,
} from '../src/mainInteractivePrompt/mainInteractivePromptHistory.js'

describe('maskInteractiveInputLineForStorage', () => {
  test('redacts trailing secret after /add-access-token', () => {
    expect(
      maskInteractiveInputLineForStorage('/add-access-token my-secret-token')
    ).toBe('/add-access-token <redacted>')
  })

  test('command match is case-insensitive', () => {
    expect(maskInteractiveInputLineForStorage('/Add-Access-Token  abc  ')).toBe(
      '/add-access-token <redacted>'
    )
  })

  test('leaves line unchanged when argument is only whitespace', () => {
    expect(maskInteractiveInputLineForStorage('/add-access-token   ')).toBe(
      '/add-access-token   '
    )
  })

  test('leaves other slash commands unchanged', () => {
    expect(maskInteractiveInputLineForStorage('/exit')).toBe('/exit')
  })
})

describe('history append uses mask: recalled line is redacted', () => {
  test('append masked storage then ↑ shows redacted form, not raw secret', () => {
    const raw = '/add-access-token super-secret-value'
    const lines = appendUserInputHistoryLine(
      [],
      maskInteractiveInputLineForStorage(raw)
    )
    expect(lines[0]).toBe('/add-access-token <redacted>')
    const s = {
      ...emptyMainInteractivePromptHistoryState(),
      lineDraft: '',
      caretOffset: 0,
      userInputHistoryLines: lines,
    }
    expect(onArrowUp(s)).toMatchObject({
      lineDraft: '/add-access-token <redacted>',
      userInputHistoryWalkIndex: 0,
    })
  })
})

describe('singleLineCommandDraft', () => {
  test('collapses CR, LF, and CRLF to spaces', () => {
    expect(singleLineCommandDraft('a\rb')).toBe('a b')
    expect(singleLineCommandDraft('a\nb')).toBe('a b')
    expect(singleLineCommandDraft('a\r\nb')).toBe('a b')
  })
})

describe('appendUserInputHistoryLine', () => {
  test('returns same array for empty or whitespace-only after trim', () => {
    const lines = ['x']
    expect(appendUserInputHistoryLine(lines, '')).toBe(lines)
    expect(appendUserInputHistoryLine(lines, '  \n  ')).toBe(lines)
  })

  test('does not duplicate when new line equals newest entry', () => {
    expect(appendUserInputHistoryLine(['a', 'b'], 'a')).toEqual(['a', 'b'])
  })

  test('prepends trimmed single-line draft and caps length', () => {
    expect(appendUserInputHistoryLine(['old'], '  hi\nthere  ')).toEqual([
      'hi there',
      'old',
    ])
    const many = Array.from(
      { length: MAX_USER_INPUT_HISTORY_LINES },
      (_, i) => `L${i}`
    )
    const next = appendUserInputHistoryLine(many, 'new')
    expect(next).toHaveLength(MAX_USER_INPUT_HISTORY_LINES)
    expect(next[0]).toBe('new')
  })
})

describe('onArrowUp / onArrowDown (history + caret, no slash list)', () => {
  test('with empty history: up moves caret to 0 when caret > 0', () => {
    const s = {
      ...emptyMainInteractivePromptHistoryState(),
      lineDraft: 'abc',
      caretOffset: 2,
    }
    expect(onArrowUp(s)).toEqual({ ...s, caretOffset: 0 })
  })

  test('with empty history: up is no-op at caret 0', () => {
    const s = {
      ...emptyMainInteractivePromptHistoryState(),
      lineDraft: 'x',
      caretOffset: 0,
    }
    expect(onArrowUp(s)).toEqual(s)
  })

  test('with empty history: down moves caret to EOL when before end', () => {
    const s = {
      ...emptyMainInteractivePromptHistoryState(),
      lineDraft: 'abc',
      caretOffset: 1,
    }
    expect(onArrowDown(s)).toEqual({ ...s, caretOffset: 3 })
  })

  test('with empty history: down is no-op at EOL', () => {
    const s = {
      ...emptyMainInteractivePromptHistoryState(),
      lineDraft: 'ab',
      caretOffset: 2,
    }
    expect(onArrowDown(s)).toEqual(s)
  })

  test('starts walk at newest line when caret at 0 and history non-empty', () => {
    const s: ReturnType<typeof emptyMainInteractivePromptHistoryState> = {
      ...emptyMainInteractivePromptHistoryState(),
      lineDraft: 'typing',
      caretOffset: 0,
      userInputHistoryLines: ['newest', 'older'],
    }
    expect(onArrowUp(s)).toEqual({
      ...s,
      lineDraftBeforeUserInputHistoryWalk: 'typing',
      userInputHistoryWalkIndex: 0,
      lineDraft: 'newest',
      caretOffset: 0,
    })
  })

  test('walks to older entries on repeated up', () => {
    let s: ReturnType<typeof emptyMainInteractivePromptHistoryState> = {
      ...emptyMainInteractivePromptHistoryState(),
      lineDraft: '',
      caretOffset: 0,
      userInputHistoryLines: ['a', 'b', 'c'],
      userInputHistoryWalkIndex: 0,
      lineDraftBeforeUserInputHistoryWalk: 'draft',
    }
    s = onArrowUp(s)
    expect(s.userInputHistoryWalkIndex).toBe(1)
    expect(s.lineDraft).toBe('b')
    expect(s.caretOffset).toBe(0)
    s = onArrowUp(s)
    expect(s.userInputHistoryWalkIndex).toBe(2)
    expect(s.lineDraft).toBe('c')
    s = onArrowUp(s)
    expect(s).toEqual(s)
  })

  test('down from oldest walks forward; down from newest restores pre-walk draft', () => {
    let s: ReturnType<typeof emptyMainInteractivePromptHistoryState> = {
      ...emptyMainInteractivePromptHistoryState(),
      lineDraft: 'c',
      caretOffset: 0,
      userInputHistoryLines: ['a', 'b', 'c'],
      userInputHistoryWalkIndex: 2,
      lineDraftBeforeUserInputHistoryWalk: 'my draft',
    }
    s = onArrowDown(s)
    expect(s.userInputHistoryWalkIndex).toBe(1)
    expect(s.lineDraft).toBe('b')
    expect(s.caretOffset).toBe(1)
    s = onArrowDown(s)
    expect(s.userInputHistoryWalkIndex).toBe(0)
    expect(s.lineDraft).toBe('a')
    expect(s.caretOffset).toBe(1)
    s = onArrowDown(s)
    expect(s.userInputHistoryWalkIndex).toBeNull()
    expect(s.lineDraftBeforeUserInputHistoryWalk).toBeNull()
    expect(s.lineDraft).toBe('my draft')
    expect(s.caretOffset).toBe('my draft'.length)
  })

  test('down from newest with null saved draft restores empty string', () => {
    const s: ReturnType<typeof emptyMainInteractivePromptHistoryState> = {
      ...emptyMainInteractivePromptHistoryState(),
      lineDraft: 'only',
      caretOffset: 0,
      userInputHistoryLines: ['only'],
      userInputHistoryWalkIndex: 0,
      lineDraftBeforeUserInputHistoryWalk: null,
    }
    const next = onArrowDown(s)
    expect(next.lineDraft).toBe('')
    expect(next.caretOffset).toBe(0)
    expect(next.userInputHistoryWalkIndex).toBeNull()
  })
})

describe('exitHistoryWalkOnDraftEdit', () => {
  test('clears walk fields when walking, keeps draft and caret', () => {
    const s: ReturnType<typeof emptyMainInteractivePromptHistoryState> = {
      ...emptyMainInteractivePromptHistoryState(),
      lineDraft: 'from history',
      caretOffset: 3,
      userInputHistoryLines: ['from history'],
      userInputHistoryWalkIndex: 0,
      lineDraftBeforeUserInputHistoryWalk: 'saved',
    }
    expect(exitHistoryWalkOnDraftEdit(s)).toEqual({
      ...s,
      userInputHistoryWalkIndex: null,
      lineDraftBeforeUserInputHistoryWalk: null,
    })
  })

  test('is identity when not walking', () => {
    const s = {
      ...emptyMainInteractivePromptHistoryState(),
      lineDraft: 'x',
      caretOffset: 1,
    }
    expect(exitHistoryWalkOnDraftEdit(s)).toEqual(s)
  })
})
