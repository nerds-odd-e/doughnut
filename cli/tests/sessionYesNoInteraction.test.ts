import { describe, expect, it } from 'vitest'
import {
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

describe('recallStopConfirmViewModel', () => {
  it('exposes ink-shaped fields for TTY paint', () => {
    const vm = recallStopConfirmViewModel('y or n; Esc to go back')
    expect(vm.promptLines).toEqual([])
    expect(vm.placeholder).toBe('y or n; Esc to go back')
    expect(vm.guidance).toEqual([RECALL_STOP_CONFIRM_GUIDANCE_LINE])
  })
})
