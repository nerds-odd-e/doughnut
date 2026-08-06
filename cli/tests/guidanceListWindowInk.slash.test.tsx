import { describe, expect, test } from 'vitest'
import {
  MORE_ABOVE,
  MORE_BELOW,
  ROW_BUDGET,
  expectBothScrollIndicatorsBracketOptions,
  expectMoreBelowLastWhenPresent,
  renderGuidancePlain,
  slashRows,
} from './guidanceListWindowInk.testHelpers.js'

describe('GuidanceListInk slash mode', () => {
  test('short list: all options, no scroll labels', () => {
    const rows = slashRows(3)
    const p = renderGuidancePlain({
      mode: 'slash',
      rowBudget: ROW_BUDGET,
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
      rowBudget: ROW_BUDGET,
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

  test('narrow terminalColumns: one row per option with ellipsis', () => {
    const rows = [
      {
        usage: '/very-long-slash-command-here',
        description: 'Description text that cannot fit on one line here',
      },
    ]
    const p = renderGuidancePlain({
      mode: 'slash',
      rowBudget: ROW_BUDGET,
      rows,
      highlightIndex: 0,
      terminalColumns: 36,
    })
    const contentLines = p
      .split('\n')
      .map((l) => l.trimEnd())
      .filter((l) => l.trim().length > 0)
    expect(contentLines).toHaveLength(1)
    expect(p).toContain('…')
    expect(p).not.toContain('/very-long-slash-command-here')
  })

  test('overflow: fixed row count; bottom indicator replaces an option row', () => {
    const rows = slashRows(11)
    const p = renderGuidancePlain({
      mode: 'slash',
      rowBudget: ROW_BUDGET,
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
    expectMoreBelowLastWhenPresent(p)
  })

  test('overflow first page: highlight before bottom row keeps only bottom indicator', () => {
    const rows = slashRows(11)
    const p = renderGuidancePlain({
      mode: 'slash',
      rowBudget: ROW_BUDGET,
      rows,
      highlightIndex: 2,
    })
    expect(p).not.toContain(MORE_ABOVE)
    expect(p).toContain(MORE_BELOW)
    expect(p).toContain('/cmd0')
    expect(p).toContain('/cmd1')
    expect(p).toContain('/cmd2')
    expect(p).toContain('/cmd3')
    expect(p).not.toContain('/cmd4')
    const listLines = p
      .split('\n')
      .map((l) => l.trimEnd())
      .filter((l) => l.includes(MORE_BELOW) || l.includes('  /cmd'))
    expect(listLines).toHaveLength(ROW_BUDGET)
  })

  test('overflow: highlight on bottom of first page shows more above without scrolling options below', () => {
    const rows = slashRows(11)
    const p = renderGuidancePlain({
      mode: 'slash',
      rowBudget: ROW_BUDGET,
      rows,
      highlightIndex: 3,
    })
    expect(p).toContain(MORE_ABOVE)
    expect(p).toContain(MORE_BELOW)
    expect(p).not.toMatch(/ {2}\/cmd0\b/)
    expect(p).toContain('/cmd1')
    expect(p).toContain('/cmd2')
    expect(p).toContain('/cmd3')
    expect(p).not.toContain('/cmd4')
    expectBothScrollIndicatorsBracketOptions(p)
  })

  test('overflow: after first-page bottom, next index scrolls (shows deeper item)', () => {
    const rows = slashRows(11)
    const p = renderGuidancePlain({
      mode: 'slash',
      rowBudget: ROW_BUDGET,
      rows,
      highlightIndex: 4,
    })
    expect(p).toContain('/cmd4')
    expectBothScrollIndicatorsBracketOptions(p)
  })

  test('mid highlight: both indicators bracket options; budget fixed; stable scroll copy', () => {
    const pMid = renderGuidancePlain({
      mode: 'slash',
      rowBudget: ROW_BUDGET,
      rows: slashRows(11),
      highlightIndex: 5,
    })
    expect(pMid).toContain(MORE_ABOVE)
    expect(pMid).toContain(MORE_BELOW)
    expect(pMid).toContain('/cmd5')
    expectBothScrollIndicatorsBracketOptions(pMid)

    const rows20 = slashRows(20)
    const pDeep = renderGuidancePlain({
      mode: 'slash',
      rowBudget: ROW_BUDGET,
      rows: rows20,
      highlightIndex: 12,
    })
    expect(pDeep).toContain('/cmd12')
    expectBothScrollIndicatorsBracketOptions(pDeep)

    const pStep = renderGuidancePlain({
      mode: 'slash',
      rowBudget: ROW_BUDGET,
      rows: rows20,
      highlightIndex: 11,
    })
    expectBothScrollIndicatorsBracketOptions(pStep)
    const minSlashCmd = (plain: string) =>
      Math.min(
        ...[...plain.matchAll(/ {2}\/cmd(\d+)\b/g)].map((m) => Number(m[1]))
      )
    expect(minSlashCmd(pStep)).toBe(10)
    expect(minSlashCmd(pDeep)).toBe(11)
  })

  test('highlight near end: top indicator, no bottom when window reaches end', () => {
    const rows = slashRows(11)
    const p = renderGuidancePlain({
      mode: 'slash',
      rowBudget: ROW_BUDGET,
      rows,
      highlightIndex: 10,
    })
    expect(p).toContain(MORE_ABOVE)
    expect(p).not.toContain(MORE_BELOW)
    expect(p).toContain('/cmd10')
  })
})
