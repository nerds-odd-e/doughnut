import { describe, expect, it } from 'vitest'
import {
  dispatchRecallSessionConfirmKey,
  parseRecallSessionYesNoSubmit,
  recallStopConfirmViewModel,
  RECALL_STOP_CONFIRM_GUIDANCE_LINE,
} from '../src/interactions/sessionYesNoInteraction.js'

describe('parseRecallSessionYesNoSubmit', () => {
  describe('empty-is-no (stop-recall confirm)', () => {
    it('treats empty as dismiss (no)', () => {
      expect(parseRecallSessionYesNoSubmit('', 'empty-is-no')).toBe('no')
    })
    it('accepts y / yes / n / no case-insensitively', () => {
      expect(parseRecallSessionYesNoSubmit('y', 'empty-is-no')).toBe('yes')
      expect(parseRecallSessionYesNoSubmit('YES', 'empty-is-no')).toBe('yes')
      expect(parseRecallSessionYesNoSubmit('n', 'empty-is-no')).toBe('no')
      expect(parseRecallSessionYesNoSubmit('No', 'empty-is-no')).toBe('no')
    })
    it('rejects other non-empty input', () => {
      expect(parseRecallSessionYesNoSubmit('x', 'empty-is-no')).toBe('invalid')
      expect(parseRecallSessionYesNoSubmit('maybe', 'empty-is-no')).toBe(
        'invalid'
      )
    })
  })

  describe('empty-is-invalid (load-more / memory y/n)', () => {
    it('treats empty as invalid', () => {
      expect(parseRecallSessionYesNoSubmit('', 'empty-is-invalid')).toBe(
        'invalid'
      )
    })
    it('still accepts y/n', () => {
      expect(parseRecallSessionYesNoSubmit('y', 'empty-is-invalid')).toBe('yes')
      expect(parseRecallSessionYesNoSubmit('n', 'empty-is-invalid')).toBe('no')
    })
  })
})

describe('dispatchRecallSessionConfirmKey', () => {
  const base = {
    lineDraft: '',
    submitPressed: false,
    ctrl: false,
    meta: false,
    shift: false,
    str: undefined as string | undefined,
    keyName: undefined as string | undefined,
  }

  const stopConfirm = 'treat-as-no' as const

  it('Escape cancels', () => {
    expect(
      dispatchRecallSessionConfirmKey(
        { ...base, keyName: 'escape' },
        stopConfirm
      )
    ).toEqual({ result: 'cancel' })
  })

  it('Enter on y submits yes', () => {
    expect(
      dispatchRecallSessionConfirmKey(
        {
          ...base,
          lineDraft: 'y',
          submitPressed: true,
        },
        stopConfirm
      )
    ).toEqual({ result: 'submit-yes' })
  })

  it('Enter on empty submits no (stop confirm)', () => {
    expect(
      dispatchRecallSessionConfirmKey(
        {
          ...base,
          lineDraft: '   ',
          submitPressed: true,
        },
        stopConfirm
      )
    ).toEqual({ result: 'submit-no' })
  })

  it('Enter on empty yields invalid (session y/n)', () => {
    expect(
      dispatchRecallSessionConfirmKey(
        {
          ...base,
          lineDraft: '   ',
          submitPressed: true,
        },
        'treat-as-invalid'
      )
    ).toEqual({
      result: 'invalid-submit',
      hint: 'Please answer y or n',
    })
  })

  it('Enter on garbage yields invalid hint', () => {
    expect(
      dispatchRecallSessionConfirmKey(
        {
          ...base,
          lineDraft: 'q',
          submitPressed: true,
        },
        stopConfirm
      )
    ).toEqual({
      result: 'invalid-submit',
      hint: 'Please answer y or n',
    })
  })

  it('Shift+Enter does not submit', () => {
    expect(
      dispatchRecallSessionConfirmKey(
        {
          ...base,
          lineDraft: 'y',
          submitPressed: true,
          shift: true,
        },
        stopConfirm
      )
    ).toEqual({ result: 'redraw' })
  })

  it('backspace edits', () => {
    expect(
      dispatchRecallSessionConfirmKey(
        { ...base, keyName: 'backspace' },
        stopConfirm
      )
    ).toEqual({ result: 'edit-backspace' })
  })

  it('prints character when not ctrl/meta', () => {
    expect(
      dispatchRecallSessionConfirmKey(
        { ...base, str: 'a', ctrl: false },
        stopConfirm
      )
    ).toEqual({ result: 'edit-char', char: 'a' })
  })

  it('ignores ctrl+char for edit', () => {
    expect(
      dispatchRecallSessionConfirmKey(
        { ...base, str: 'c', ctrl: true },
        stopConfirm
      )
    ).toEqual({ result: 'redraw' })
  })
})

describe('recallStopConfirmViewModel', () => {
  it('exposes ink-shaped fields for TTY paint', () => {
    const vm = recallStopConfirmViewModel('y or n; Esc to go back')
    expect(vm.promptLines).toEqual([])
    expect(vm.placeholder).toBe('y or n; Esc to go back')
    expect(vm.guidance).toEqual([RECALL_STOP_CONFIRM_GUIDANCE_LINE])
  })
})
