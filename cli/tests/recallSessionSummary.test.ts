import { describe, expect, test } from 'vitest'
import { recallSessionSummaryLine } from '../src/commands/recall/recallSessionSummary.js'

describe('recallSessionSummaryLine', () => {
  test('singular for one', () => {
    expect(recallSessionSummaryLine(1)).toBe('Recalled 1 note')
  })

  test('plural for zero, two, and large counts', () => {
    expect(recallSessionSummaryLine(0)).toBe('Recalled 0 notes')
    expect(recallSessionSummaryLine(2)).toBe('Recalled 2 notes')
    expect(recallSessionSummaryLine(10)).toBe('Recalled 10 notes')
  })
})
