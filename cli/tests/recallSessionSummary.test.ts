import { describe, expect, test } from 'vitest'
import { recallSessionSummaryLine } from '../src/commands/recall/recallSessionSummary.js'

describe('recallSessionSummaryLine', () => {
  test('singular for one', () => {
    expect(recallSessionSummaryLine(1)).toBe('Recalled 1 note')
  })

  test.each([0, 2, 10])('plural for %i', (n) => {
    expect(recallSessionSummaryLine(n)).toBe(`Recalled ${n} notes`)
  })
})
