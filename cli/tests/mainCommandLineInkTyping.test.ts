import type { Key } from 'ink'
import { describe, expect, test } from 'vitest'
import { applyPatchedTextInputKey } from '../src/ui/patchedTextInputKey.js'

function nk(p: Partial<Key>): Key {
  return {
    upArrow: false,
    downArrow: false,
    leftArrow: false,
    rightArrow: false,
    pageDown: false,
    pageUp: false,
    home: false,
    end: false,
    return: false,
    escape: false,
    ctrl: false,
    shift: false,
    tab: false,
    backspace: false,
    delete: false,
    meta: false,
    super: false,
    hyper: false,
    capsLock: false,
    numLock: false,
    ...p,
  }
}

describe('applyPatchedTextInputKey', () => {
  test('insert printable updates draft and caret', () => {
    const r = applyPatchedTextInputKey(
      { value: 'ab', caretOffset: 1 },
      'x',
      nk({})
    )
    expect(r).toEqual({
      kind: 'change',
      next: { value: 'axb', caretOffset: 2 },
    })
  })

  test('left arrow moves caret', () => {
    const r = applyPatchedTextInputKey(
      { value: 'a', caretOffset: 1 },
      '',
      nk({ leftArrow: true })
    )
    expect(r).toEqual({
      kind: 'change',
      next: { value: 'a', caretOffset: 0 },
    })
  })

  test('home and end', () => {
    expect(
      applyPatchedTextInputKey(
        { value: 'hey', caretOffset: 1 },
        '',
        nk({ home: true })
      )
    ).toEqual({ kind: 'change', next: { value: 'hey', caretOffset: 0 } })
    expect(
      applyPatchedTextInputKey(
        { value: 'hey', caretOffset: 1 },
        '',
        nk({ end: true })
      )
    ).toEqual({ kind: 'change', next: { value: 'hey', caretOffset: 3 } })
  })

  test('returns unhandled for command-line special keys', () => {
    expect(
      applyPatchedTextInputKey({ value: '', caretOffset: 0 }, '', nk({}))
    ).toEqual({ kind: 'unhandled' })
    expect(
      applyPatchedTextInputKey(
        { value: '', caretOffset: 0 },
        '',
        nk({ upArrow: true })
      )
    ).toEqual({ kind: 'unhandled' })
    expect(
      applyPatchedTextInputKey(
        { value: '', caretOffset: 0 },
        '\r',
        nk({ return: true })
      )
    ).toEqual({ kind: 'submit' })
  })

  test('ignores ctrl/meta printable', () => {
    expect(
      applyPatchedTextInputKey(
        { value: '', caretOffset: 0 },
        'a',
        nk({ ctrl: true })
      )
    ).toEqual({ kind: 'unhandled' })
  })
})
