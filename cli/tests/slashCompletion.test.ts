import { describe, expect, test } from 'vitest'
import {
  hasInteractiveSlashCompletions,
  normalizedInteractiveDraft,
  slashGuidanceForInk,
} from '../src/slashCompletion.js'

describe('slashCompletion (interactive TTY draft)', () => {
  test('normalizes newlines like the live editor', () => {
    expect(normalizedInteractiveDraft('a\nb')).toBe('a b')
  })

  test('hint for non-slash text or after a chosen command (trailing space)', () => {
    expect(slashGuidanceForInk('hello')).toEqual({ show: 'hint' })
    expect(slashGuidanceForInk('/help ')).toEqual({ show: 'hint' })
  })

  test('list when `/` prefix matches at least one command', () => {
    const g = slashGuidanceForInk('/help')
    expect(g.show).toBe('list')
    if (g.show === 'list') {
      expect(g.docs.some((d) => d.usage === '/help')).toBe(true)
    }
    expect(hasInteractiveSlashCompletions('/help')).toBe(true)
  })

  test('empty guidance rows when slash prefix matches nothing', () => {
    expect(slashGuidanceForInk('/zzzznotacommand')).toEqual({ show: 'empty' })
    expect(hasInteractiveSlashCompletions('/zzzznotacommand')).toBe(false)
  })
})
