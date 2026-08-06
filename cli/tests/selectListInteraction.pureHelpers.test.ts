import { describe, expect, it } from 'vitest'
import {
  choiceIndexFromSelectListSubmitLine,
  cycleListSelectionIndex,
  selectListSubmitLineForSlashAndNumber,
  selectListSubmitLineIsInvalidChoice,
} from '../src/interactions/selectListInteraction.js'

describe('cycleListSelectionIndex', () => {
  it('wraps forward and backward', () => {
    expect(cycleListSelectionIndex(0, 1, 3)).toBe(1)
    expect(cycleListSelectionIndex(2, 1, 3)).toBe(0)
    expect(cycleListSelectionIndex(0, -1, 3)).toBe(2)
  })
})

describe('selectListSubmitLineForSlashAndNumber', () => {
  it.each(['/stop', '/contest'])('passes through %s', (line) => {
    expect(selectListSubmitLineForSlashAndNumber(line, 2, 0)).toBe(line)
  })

  it('uses typed 1..choiceCount', () => {
    expect(selectListSubmitLineForSlashAndNumber('1', 3, 2)).toBe('1')
    expect(selectListSubmitLineForSlashAndNumber('3', 3, 0)).toBe('3')
  })

  it('empty draft confirms highlight', () => {
    expect(selectListSubmitLineForSlashAndNumber('', 3, 0)).toBe('1')
    expect(selectListSubmitLineForSlashAndNumber('', 3, 1)).toBe('2')
  })

  it('non-empty invalid passes through unchanged', () => {
    expect(selectListSubmitLineForSlashAndNumber('99', 3, 1)).toBe('99')
    expect(selectListSubmitLineForSlashAndNumber('abc', 3, 2)).toBe('abc')
    expect(selectListSubmitLineForSlashAndNumber('0', 3, 1)).toBe('0')
  })
})

describe('choiceIndexFromSelectListSubmitLine', () => {
  it('returns null for slash commands', () => {
    expect(choiceIndexFromSelectListSubmitLine('/stop', 3)).toBeNull()
  })

  it('returns 0-based index for valid 1-based numbers', () => {
    expect(choiceIndexFromSelectListSubmitLine('1', 3)).toBe(0)
    expect(choiceIndexFromSelectListSubmitLine('  3  ', 3)).toBe(2)
  })

  it.each(['0', '4', 'x'])('returns null for %j', (line) => {
    expect(choiceIndexFromSelectListSubmitLine(line, 3)).toBeNull()
  })
})

describe('selectListSubmitLineIsInvalidChoice', () => {
  it.each(['/stop', '/contest', '1', '  3  '])(
    'false for reserved or valid %j',
    (line) => {
      expect(selectListSubmitLineIsInvalidChoice(line, 3)).toBe(false)
    }
  )

  it.each(['0', '4', 'abc', ''])('true for invalid %j', (line) => {
    expect(selectListSubmitLineIsInvalidChoice(line, 3)).toBe(true)
  })
})
