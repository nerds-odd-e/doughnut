import { describe, test, expect } from 'vitest'
import {
  CURRENT_GUIDANCE_MAX_VISIBLE,
  formatHighlightedList,
} from '../src/listDisplay.js'
import { stripAnsi } from '../src/renderer.js'

describe('formatHighlightedList', () => {
  test('returns maxVisible lines when "more below" appears (replaces last option)', () => {
    const lines = Array.from({ length: 9 }, (_, i) => `item${i}`)
    const result = formatHighlightedList(lines, CURRENT_GUIDANCE_MAX_VISIBLE, 0)
    expect(result).toHaveLength(8)
    expect(stripAnsi(result[7])).toContain('↓ more below')
    expect(stripAnsi(result[6])).toContain('item6')
    expect(stripAnsi(result[0])).toContain('item0')
  })

  test('returns maxVisible lines when "more above" appears (replaces first option)', () => {
    const lines = Array.from({ length: 12 }, (_, i) => `item${i}`)
    const result = formatHighlightedList(
      lines,
      CURRENT_GUIDANCE_MAX_VISIBLE,
      11
    )
    expect(result).toHaveLength(8)
    expect(stripAnsi(result[0])).toContain('↑ more above')
    expect(stripAnsi(result[1])).toContain('item5')
    expect(stripAnsi(result[7])).toContain('item11')
  })

  test('returns maxVisible lines when both "more above" and "more below" appear', () => {
    const lines = Array.from({ length: 12 }, (_, i) => `item${i}`)
    const result = formatHighlightedList(lines, CURRENT_GUIDANCE_MAX_VISIBLE, 8)
    expect(result).toHaveLength(8)
    expect(stripAnsi(result[0])).toContain('↑ more above')
    expect(stripAnsi(result[7])).toContain('↓ more below')
  })

  test('at bottom: "more above" replaces first, no "more below", last option on last line', () => {
    const lines = Array.from({ length: 12 }, (_, i) => `item${i}`)
    const result = formatHighlightedList(
      lines,
      CURRENT_GUIDANCE_MAX_VISIBLE,
      11
    )
    expect(result).toHaveLength(8)
    expect(stripAnsi(result[0])).toContain('↑ more above')
    expect(stripAnsi(result[7])).toContain('item11')
    expect(result.some((l) => stripAnsi(l).includes('↓ more below'))).toBe(
      false
    )
  })

  test('"more above" appears at index 6 without scrolling (replaces first option in place)', () => {
    const lines = Array.from({ length: 12 }, (_, i) => `item${i}`)
    const result = formatHighlightedList(lines, CURRENT_GUIDANCE_MAX_VISIBLE, 6)
    expect(result).toHaveLength(8)
    expect(stripAnsi(result[0])).toContain('↑ more above')
    expect(stripAnsi(result[1])).toContain('item1')
    expect(stripAnsi(result[7])).toContain('↓ more below')
  })

  test('at index 7 both indicators appear, list scrolls up by 1, height stays 8', () => {
    const lines = Array.from({ length: 12 }, (_, i) => `item${i}`)
    const result = formatHighlightedList(lines, CURRENT_GUIDANCE_MAX_VISIBLE, 7)
    expect(result).toHaveLength(8)
    expect(stripAnsi(result[0])).toContain('↑ more above')
    expect(stripAnsi(result[7])).toContain('↓ more below')
    expect(stripAnsi(result[6])).toContain('item7')
  })

  test('few items: no more above/below, height equals item count', () => {
    const lines = ['a', 'b', 'c']
    const result = formatHighlightedList(lines, CURRENT_GUIDANCE_MAX_VISIBLE, 0)
    expect(result).toHaveLength(3)
    expect(result.some((l) => stripAnsi(l).includes('more above'))).toBe(false)
    expect(result.some((l) => stripAnsi(l).includes('more below'))).toBe(false)
  })
})
