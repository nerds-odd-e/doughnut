import { Box } from 'ink'
import { render } from 'ink-testing-library'
import { describe, expect, test } from 'vitest'
import {
  GuidanceListInk,
  type GuidanceListInkProps,
} from '../src/guidanceListWindowInk.js'
import { stripAnsi } from './inkTestHelpers.js'

const MORE_ABOVE = '↑ more above'
const MORE_BELOW = '↓ more below'
const ROW_BUDGET = 5

function renderGuidancePlain(props: GuidanceListInkProps) {
  const { lastFrame } = render(
    <Box flexDirection="column">
      <GuidanceListInk {...props} />
    </Box>
  )
  return stripAnsi(lastFrame() ?? '')
}

function slashRows(n: number) {
  return Array.from({ length: n }, (_, i) => ({
    usage: `/cmd${i}`,
    description: `d${i}`,
  }))
}

describe('GuidanceListInk slash mode', () => {
  test('short list: all options, no scroll labels', () => {
    const rows = slashRows(3)
    const p = renderGuidancePlain({
      mode: 'slash',
      rows,
      highlightIndex: 0,
    })
    expect(p).not.toContain(MORE_BELOW)
    expect(p).not.toContain(MORE_ABOVE)
    const optionLines = p
      .split('\n')
      .map((l) => l.trimEnd())
      .filter((l) => l.includes('/cmd'))
    expect(optionLines).toHaveLength(3)
  })

  test('at row budget: all slash options, no scroll labels', () => {
    const rows = slashRows(ROW_BUDGET)
    const p = renderGuidancePlain({
      mode: 'slash',
      rows,
      highlightIndex: 2,
    })
    expect(p).not.toContain(MORE_ABOVE)
    expect(p).not.toContain(MORE_BELOW)
    const optionLines = p
      .split('\n')
      .map((l) => l.trimEnd())
      .filter((l) => l.includes('/cmd'))
    expect(optionLines).toHaveLength(ROW_BUDGET)
  })

  test('overflow: fixed row count; bottom indicator replaces an option row', () => {
    const rows = slashRows(11)
    const p = renderGuidancePlain({
      mode: 'slash',
      rows,
      highlightIndex: 0,
    })
    expect(p).toContain(MORE_BELOW)
    expect(p).not.toContain(MORE_ABOVE)
    const listLines = p
      .split('\n')
      .map((l) => l.trimEnd())
      .filter((l) => l.includes(MORE_BELOW) || l.includes('  /cmd'))
    expect(listLines).toHaveLength(ROW_BUDGET)
  })

  test('mid highlight: both indicators; inner option count is budget − 2', () => {
    const rows = slashRows(11)
    const p = renderGuidancePlain({
      mode: 'slash',
      rows,
      highlightIndex: 5,
    })
    expect(p).toContain(MORE_ABOVE)
    expect(p).toContain(MORE_BELOW)
    const optionLines = p
      .split('\n')
      .map((l) => l.trimEnd())
      .filter((l) => l.includes('  /cmd'))
    expect(optionLines).toHaveLength(ROW_BUDGET - 2)
    expect(p).toContain('/cmd5')
  })

  test('highlight near end: top indicator, no bottom when window reaches end', () => {
    const rows = slashRows(11)
    const p = renderGuidancePlain({
      mode: 'slash',
      rows,
      highlightIndex: 10,
    })
    expect(p).toContain(MORE_ABOVE)
    expect(p).not.toContain(MORE_BELOW)
    expect(p).toContain('/cmd10')
  })
})

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
    })
    expect(p).not.toContain(MORE_ABOVE)
    expect(p).not.toContain(MORE_BELOW)
    for (let i = 0; i < ROW_BUDGET; i++) {
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
    })
    expect(p).toContain(MORE_ABOVE)
    expect(p).toContain(MORE_BELOW)
    const betweenIndicators = p.includes(MORE_ABOVE) && p.includes(MORE_BELOW)
    expect(betweenIndicators).toBe(true)
    const numberedOrIndicator = p
      .split('\n')
      .map((l) => l.trimEnd())
      .filter(
        (l) =>
          l.includes(MORE_ABOVE) ||
          l.includes(MORE_BELOW) ||
          /^\d+\.\s+x$/.test(l.trim())
      )
    expect(numberedOrIndicator).toHaveLength(ROW_BUDGET)
  })
})

describe('GuidanceListInk scroll labels in output', () => {
  test('renders stable more-above / more-below copy when clipped', () => {
    const p = renderGuidancePlain({
      mode: 'slash',
      rows: slashRows(11),
      highlightIndex: 5,
    })
    expect(p).toContain(MORE_ABOVE)
    expect(p).toContain(MORE_BELOW)
  })
})
