import { describe, expect, test } from 'vitest'
import { normalizeSpellingLineForSubmit } from '../src/commands/recall/spellingAnswerLine.js'

describe('normalizeSpellingLineForSubmit', () => {
  test('collapses newlines to spaces and trims', () => {
    expect(normalizeSpellingLineForSubmit('  foo\nbar  ')).toBe('foo bar')
    expect(normalizeSpellingLineForSubmit('a\r\nb')).toBe('a b')
  })

  test('trims unicode whitespace including NBSP', () => {
    expect(normalizeSpellingLineForSubmit('\u00A0word\u00A0')).toBe('word')
  })
})
