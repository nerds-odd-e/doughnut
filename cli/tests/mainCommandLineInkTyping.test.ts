import type { Key } from 'ink'
import { describe, expect, test } from 'vitest'
import { tryApplyMainCommandLineInkTyping } from '../src/interactions/mainCommandLineInkTyping.js'
import { emptyInteractiveCommandInput } from '../src/interactiveCommandInput.js'

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

describe('tryApplyMainCommandLineInkTyping', () => {
  test('insert printable updates draft and requests slash-picker reset', () => {
    const cmd = {
      ...emptyInteractiveCommandInput(),
      lineDraft: 'ab',
      caretOffset: 1,
    }
    const r = tryApplyMainCommandLineInkTyping(cmd, 'x', nk({}))
    expect(r).toEqual({
      nextCommandInput: expect.objectContaining({
        lineDraft: 'axb',
        caretOffset: 2,
      }),
      resetSlashPicker: true,
    })
  })

  test('left arrow moves caret without slash-picker reset', () => {
    const cmd = {
      ...emptyInteractiveCommandInput(),
      lineDraft: 'a',
      caretOffset: 1,
    }
    const r = tryApplyMainCommandLineInkTyping(cmd, '', nk({ leftArrow: true }))
    expect(r).toEqual({
      nextCommandInput: expect.objectContaining({
        lineDraft: 'a',
        caretOffset: 0,
      }),
      resetSlashPicker: false,
    })
  })

  test('home and end', () => {
    const cmd = {
      ...emptyInteractiveCommandInput(),
      lineDraft: 'hey',
      caretOffset: 1,
    }
    expect(
      tryApplyMainCommandLineInkTyping(cmd, '', nk({ home: true }))
    ).toEqual({
      nextCommandInput: expect.objectContaining({ caretOffset: 0 }),
      resetSlashPicker: false,
    })
    expect(
      tryApplyMainCommandLineInkTyping(cmd, '', nk({ end: true }))
    ).toEqual({
      nextCommandInput: expect.objectContaining({ caretOffset: 3 }),
      resetSlashPicker: false,
    })
  })

  test('returns null for keys handled by command-line special handler', () => {
    const cmd = emptyInteractiveCommandInput()
    expect(tryApplyMainCommandLineInkTyping(cmd, '', nk({}))).toBe(null)
    expect(
      tryApplyMainCommandLineInkTyping(cmd, '', nk({ upArrow: true }))
    ).toBe(null)
    expect(
      tryApplyMainCommandLineInkTyping(cmd, '\r', nk({ return: true }))
    ).toBe(null)
  })

  test('ignores ctrl/meta printable', () => {
    const cmd = emptyInteractiveCommandInput()
    expect(tryApplyMainCommandLineInkTyping(cmd, 'a', nk({ ctrl: true }))).toBe(
      null
    )
  })
})
