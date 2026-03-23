import { describe, expect, it } from 'vitest'
import {
  dispatchRecallStopConfirmKey,
  parseRecallStopConfirmSubmit,
  recallStopConfirmViewModel,
  RECALL_STOP_CONFIRM_GUIDANCE_LINE,
} from '../src/interactions/recallStopConfirmInteraction.js'

describe('parseRecallStopConfirmSubmit', () => {
  it('treats empty as dismiss (no)', () => {
    expect(parseRecallStopConfirmSubmit('')).toBe('no')
  })
  it('accepts y / yes / n / no case-insensitively', () => {
    expect(parseRecallStopConfirmSubmit('y')).toBe('yes')
    expect(parseRecallStopConfirmSubmit('YES')).toBe('yes')
    expect(parseRecallStopConfirmSubmit('n')).toBe('no')
    expect(parseRecallStopConfirmSubmit('No')).toBe('no')
  })
  it('rejects other non-empty input', () => {
    expect(parseRecallStopConfirmSubmit('x')).toBe('invalid')
    expect(parseRecallStopConfirmSubmit('maybe')).toBe('invalid')
  })
})

describe('dispatchRecallStopConfirmKey', () => {
  const base = {
    lineDraft: '',
    submitPressed: false,
    ctrl: false,
    meta: false,
    shift: false,
    str: undefined as string | undefined,
    keyName: undefined as string | undefined,
  }

  it('Escape cancels', () => {
    expect(
      dispatchRecallStopConfirmKey({ ...base, keyName: 'escape' })
    ).toEqual({ result: 'cancel' })
  })

  it('Enter on y submits yes', () => {
    expect(
      dispatchRecallStopConfirmKey({
        ...base,
        lineDraft: 'y',
        submitPressed: true,
      })
    ).toEqual({ result: 'submit-yes' })
  })

  it('Enter on empty submits no', () => {
    expect(
      dispatchRecallStopConfirmKey({
        ...base,
        lineDraft: '   ',
        submitPressed: true,
      })
    ).toEqual({ result: 'submit-no' })
  })

  it('Enter on garbage yields invalid hint', () => {
    expect(
      dispatchRecallStopConfirmKey({
        ...base,
        lineDraft: 'q',
        submitPressed: true,
      })
    ).toEqual({
      result: 'invalid-submit',
      hint: 'Please answer y or n',
    })
  })

  it('Shift+Enter does not submit', () => {
    expect(
      dispatchRecallStopConfirmKey({
        ...base,
        lineDraft: 'y',
        submitPressed: true,
        shift: true,
      })
    ).toEqual({ result: 'redraw' })
  })

  it('backspace edits', () => {
    expect(
      dispatchRecallStopConfirmKey({ ...base, keyName: 'backspace' })
    ).toEqual({ result: 'edit-backspace' })
  })

  it('prints character when not ctrl/meta', () => {
    expect(
      dispatchRecallStopConfirmKey({ ...base, str: 'a', ctrl: false })
    ).toEqual({ result: 'edit-char', char: 'a' })
  })

  it('ignores ctrl+char for edit', () => {
    expect(
      dispatchRecallStopConfirmKey({ ...base, str: 'c', ctrl: true })
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
