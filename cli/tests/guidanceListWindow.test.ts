import { describe, expect, test } from 'vitest'
import {
  GUIDANCE_LIST_ROW_BUDGET,
  GUIDANCE_MORE_ABOVE_LABEL,
  GUIDANCE_MORE_BELOW_LABEL,
  layoutNumberedListGuidanceWindow,
  layoutSlashCommandGuidanceWindow,
} from '../src/guidanceListWindow.js'

function slashRows(n: number) {
  return Array.from({ length: n }, (_, i) => ({
    usage: `/cmd${i}`,
    description: `d${i}`,
  }))
}

describe('layoutSlashCommandGuidanceWindow', () => {
  test('short list: all options, no indicators', () => {
    const rows = slashRows(3)
    const out = layoutSlashCommandGuidanceWindow(rows, 0, 5)
    expect(out).toHaveLength(3)
    expect(out.every((r) => r.kind === 'option')).toBe(true)
  })

  test('overflow: exactly budget rows; bottom indicator replaces an option row', () => {
    const rows = slashRows(11)
    const out = layoutSlashCommandGuidanceWindow(
      rows,
      0,
      GUIDANCE_LIST_ROW_BUDGET
    )
    expect(out).toHaveLength(GUIDANCE_LIST_ROW_BUDGET)
    const kinds = out.map((r) => r.kind)
    expect(kinds.filter((k) => k === 'moreBelow')).toHaveLength(1)
    expect(kinds.filter((k) => k === 'moreAbove')).toHaveLength(0)
    expect(out[out.length - 1]).toEqual({ kind: 'moreBelow' })
    const options = out.filter((r) => r.kind === 'option')
    expect(options).toHaveLength(GUIDANCE_LIST_ROW_BUDGET - 1)
  })

  test('mid highlight: both indicators and inner option count is budget - 2', () => {
    const rows = slashRows(11)
    const out = layoutSlashCommandGuidanceWindow(
      rows,
      5,
      GUIDANCE_LIST_ROW_BUDGET
    )
    expect(out).toHaveLength(GUIDANCE_LIST_ROW_BUDGET)
    expect(out[0]?.kind).toBe('moreAbove')
    expect(out[out.length - 1]?.kind).toBe('moreBelow')
    const options = out.filter((r) => r.kind === 'option')
    expect(options).toHaveLength(GUIDANCE_LIST_ROW_BUDGET - 2)
    expect(
      options.some((r) => r.kind === 'option' && r.sourceIndex === 5)
    ).toBe(true)
  })

  test('highlight near end: top indicator, no bottom when window reaches end', () => {
    const rows = slashRows(11)
    const out = layoutSlashCommandGuidanceWindow(
      rows,
      10,
      GUIDANCE_LIST_ROW_BUDGET
    )
    expect(out).toHaveLength(GUIDANCE_LIST_ROW_BUDGET)
    expect(out[0]?.kind).toBe('moreAbove')
    expect(out.some((r) => r.kind === 'moreBelow')).toBe(false)
    expect(out.some((r) => r.kind === 'option' && r.sourceIndex === 10)).toBe(
      true
    )
  })
})

describe('layoutNumberedListGuidanceWindow', () => {
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
    const out = layoutNumberedListGuidanceWindow(short, 0, 5)
    expect(out).toHaveLength(3)
    expect(out.every((r) => r.kind === 'option')).toBe(true)
  })

  test('overflow: fixed budget; highlight item with continuation lines stays visible', () => {
    const out = layoutNumberedListGuidanceWindow(lines, 1, 5)
    expect(out).toHaveLength(5)
    expect(
      out.some(
        (r) => r.kind === 'option' && r.itemIndex === 1 && r.text.includes('B')
      )
    ).toBe(true)
  })

  test('long flat list: both indicators when window is in the middle', () => {
    const many = Array.from({ length: 12 }, (_, i) => ({
      itemIndex: i,
      text: `${i + 1}. x`,
    }))
    const out = layoutNumberedListGuidanceWindow(many, 6, 5)
    expect(out).toHaveLength(5)
    expect(out[0]?.kind).toBe('moreAbove')
    expect(out[out.length - 1]?.kind).toBe('moreBelow')
    expect(out.filter((r) => r.kind === 'option')).toHaveLength(3)
  })
})

describe('guidance indicator copy', () => {
  test('stable strings', () => {
    expect(GUIDANCE_MORE_ABOVE_LABEL).toBe('↑ more above')
    expect(GUIDANCE_MORE_BELOW_LABEL).toBe('↓ more below')
    expect(GUIDANCE_LIST_ROW_BUDGET).toBe(5)
  })
})
