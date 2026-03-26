import { describe, test, expect } from 'vitest'
import { parseRecallYesNoLine } from '../src/interactions/recallYesNo.js'

describe('parseRecallYesNoLine', () => {
  test('empty is invalid', () => {
    expect(parseRecallYesNoLine('')).toBe('invalid')
    expect(parseRecallYesNoLine('   ')).toBe('invalid')
  })

  test('y, yes, n, no', () => {
    expect(parseRecallYesNoLine('y')).toBe('yes')
    expect(parseRecallYesNoLine('YES')).toBe('yes')
    expect(parseRecallYesNoLine('n')).toBe('no')
    expect(parseRecallYesNoLine('No')).toBe('no')
  })

  test('other text is invalid', () => {
    expect(parseRecallYesNoLine('x')).toBe('invalid')
    expect(parseRecallYesNoLine('maybe')).toBe('invalid')
  })
})
