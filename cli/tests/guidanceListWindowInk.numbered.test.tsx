import { describe, expect, test } from 'vitest'
import {
  MORE_ABOVE,
  MORE_BELOW,
  ROW_BUDGET,
  TALL_NUMBERED_ROW_BUDGET,
  expectBothScrollIndicatorsBracketOptions,
  expectMoreBelowLastWhenPresent,
  renderGuidancePlain,
} from './guidanceListWindowInk.testHelpers.js'

describe('GuidanceListInk numbered mode', () => {
  const lines = [
    { itemIndex: 0, text: '1. A' },
    { itemIndex: 1, text: '2. B' },
    { itemIndex: 1, text: '   b2' },
    { itemIndex: 2, text: '3. C' },
    { itemIndex: 3, text: '4. D' },
    { itemIndex: 4, text: '5. E' },
    { itemIndex: 5, text: '6. F' },
    { itemIndex: 6, text: '7. G' },
  ]

  test('fits in budget: no indicators', () => {
    const short = lines.slice(0, 3)
    const p = renderGuidancePlain({
      mode: 'numbered',
      lines: short,
      highlightItemIndex: 0,
      rowBudget: ROW_BUDGET,
    })
    expect(p).not.toContain(MORE_ABOVE)
    expect(p).not.toContain(MORE_BELOW)
    expect(p).toContain('1. A')
    expect(p).toContain('2. B')
  })

  test('at row budget: five flat lines, no indicators', () => {
    const fiveFlat = Array.from({ length: ROW_BUDGET }, (_, i) => ({
      itemIndex: i,
      text: `${i + 1}. choice`,
    }))
    const p = renderGuidancePlain({
      mode: 'numbered',
      lines: fiveFlat,
      highlightItemIndex: 2,
      rowBudget: ROW_BUDGET,
    })
    expect(p).not.toContain(MORE_ABOVE)
    expect(p).not.toContain(MORE_BELOW)
    for (let i = 0; i < ROW_BUDGET; i++) {
      expect(p).toContain(`${i + 1}. choice`)
    }
  })

  test('larger rowBudget shows more choices without scroll labels', () => {
    const tenFlat = Array.from(
      { length: TALL_NUMBERED_ROW_BUDGET },
      (_, i) => ({
        itemIndex: i,
        text: `${i + 1}. choice`,
      })
    )
    const p = renderGuidancePlain({
      mode: 'numbered',
      lines: tenFlat,
      highlightItemIndex: 2,
      rowBudget: TALL_NUMBERED_ROW_BUDGET,
    })
    expect(p).not.toContain(MORE_ABOVE)
    expect(p).not.toContain(MORE_BELOW)
    for (let i = 0; i < TALL_NUMBERED_ROW_BUDGET; i++) {
      expect(p).toContain(`${i + 1}. choice`)
    }
  })

  test('MCQ-shaped: continuation lines count toward budget; all visible when lines ≤ budget', () => {
    const wrappedFits = [
      { itemIndex: 0, text: '1. A' },
      { itemIndex: 1, text: '2. B' },
      { itemIndex: 1, text: '   b2' },
      { itemIndex: 2, text: '3. C' },
      { itemIndex: 3, text: '4. D' },
    ]
    expect(wrappedFits.length).toBe(ROW_BUDGET)
    const p = renderGuidancePlain({
      mode: 'numbered',
      lines: wrappedFits,
      highlightItemIndex: 1,
      rowBudget: ROW_BUDGET,
    })
    expect(p).not.toContain(MORE_ABOVE)
    expect(p).not.toContain(MORE_BELOW)
    expect(p).toContain('1. A')
    expect(p).toContain('2. B')
    expect(p).toContain('   b2')
    expect(p).toContain('3. C')
    expect(p).toContain('4. D')
  })

  test('overflow: fixed budget; highlighted item with continuation stays visible', () => {
    const p = renderGuidancePlain({
      mode: 'numbered',
      lines,
      highlightItemIndex: 1,
      rowBudget: ROW_BUDGET,
    })
    expect(p.split('\n').filter((l) => l.includes('B')).length).toBeGreaterThan(
      0
    )
    const listLines = p
      .split('\n')
      .map((l) => l.trimEnd())
      .filter(
        (l) =>
          l.includes(MORE_ABOVE) ||
          l.includes(MORE_BELOW) ||
          /^\s*\d+\.\s/.test(l) ||
          /^\s{3}\S/.test(l)
      )
    expect(listLines.length).toBe(ROW_BUDGET)
    expectMoreBelowLastWhenPresent(p)
  })

  test('long flat list: both indicators when window is in the middle', () => {
    const many = Array.from({ length: 12 }, (_, i) => ({
      itemIndex: i,
      text: `${i + 1}. x`,
    }))
    const p = renderGuidancePlain({
      mode: 'numbered',
      lines: many,
      highlightItemIndex: 6,
      rowBudget: ROW_BUDGET,
    })
    expectBothScrollIndicatorsBracketOptions(p)

    const manyLong = Array.from({ length: 20 }, (_, i) => ({
      itemIndex: i,
      text: `${i + 1}. x`,
    }))
    const pDeep = renderGuidancePlain({
      mode: 'numbered',
      lines: manyLong,
      highlightItemIndex: 12,
      rowBudget: ROW_BUDGET,
    })
    expect(pDeep).toContain('13. x')
    expectBothScrollIndicatorsBracketOptions(pDeep)

    const pNumStep = renderGuidancePlain({
      mode: 'numbered',
      lines: manyLong,
      highlightItemIndex: 11,
      rowBudget: ROW_BUDGET,
    })
    expectBothScrollIndicatorsBracketOptions(pNumStep)
    const minListedChoice = (plain: string) =>
      Math.min(
        ...plain
          .split('\n')
          .map((l) => l.trim())
          .filter((l) => /^\d+\.\s+x$/.test(l))
          .map((l) => Number(/^(\d+)/.exec(l)![1]))
      )
    expect(minListedChoice(pNumStep)).toBe(11)
    expect(minListedChoice(pDeep)).toBe(12)
  })
})
