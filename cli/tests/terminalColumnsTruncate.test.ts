import stringWidth from 'string-width'
import { describe, expect, test } from 'vitest'
import {
  numberedTerminalListLines,
  truncateToTerminalColumns,
} from '../src/terminalColumns.js'

describe('truncateToTerminalColumns', () => {
  test('short text unchanged', () => {
    expect(truncateToTerminalColumns('hi', 10)).toBe('hi')
  })

  test('long ASCII ends with ellipsis within maxCols', () => {
    const t = truncateToTerminalColumns('hello world wide', 10)
    expect(t.endsWith('…')).toBe(true)
    expect(stringWidth(t)).toBeLessThanOrEqual(10)
  })

  test('CJK uses display width for budget', () => {
    const t = truncateToTerminalColumns('你好世界', 5)
    expect(t).toContain('…')
    expect(stringWidth(t)).toBeLessThanOrEqual(5)
  })
})

describe('numberedTerminalListLines (token picker)', () => {
  test('one line per item; ellipsis when label does not fit', () => {
    const lines = numberedTerminalListLines(
      ['alpha beta gamma delta epsilon'],
      14
    )
    expect(lines).toHaveLength(1)
    expect(lines[0]!.text).toContain('…')
    expect(stringWidth(lines[0]!.text)).toBeLessThanOrEqual(14)
  })

  test('multiple items each occupy one row', () => {
    const lines = numberedTerminalListLines(['a', 'b'], 20)
    expect(lines).toHaveLength(2)
    expect(lines[0]!.text.startsWith('1. ')).toBe(true)
    expect(lines[1]!.text.startsWith('2. ')).toBe(true)
  })
})
