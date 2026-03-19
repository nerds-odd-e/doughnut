import { describe, test, expect } from 'vitest'
import {
  truncateToWidth,
  isSelectionMode,
  grayBoxLinesForSelectionMode,
} from '../src/renderer.js'

describe('isSelectionMode', () => {
  test('true for tokenList only', () => {
    expect(isSelectionMode('tokenList')).toBe(true)
    expect(isSelectionMode('recallMcq')).toBe(false)
    expect(isSelectionMode('default')).toBe(false)
  })
})

describe('grayBoxLinesForSelectionMode', () => {
  test('replaces internal RESET with GREY so right border stays gray', () => {
    const line = '│ \x1b[90mtext\x1b[0m                    │'
    const result = grayBoxLinesForSelectionMode([line])
    expect(result[0]).toContain('\x1b[90m')
    // biome-ignore lint/suspicious/noControlCharactersInRegex: ANSI RESET, intentional
    expect(/\x1b\[0m\s*│/.test(result[0])).toBe(false)
  })
})

describe('truncateToWidth', () => {
  test('truncated line: ellipsis inherits style, ends with RESET to avoid state bleed', () => {
    const result = truncateToWidth('\x1b[7mhello world\x1b[0m', 8)
    // biome-ignore lint/suspicious/noControlCharactersInRegex: ANSI RESET, intentional
    expect(result).toMatch(/\.\.\.\x1b\[0m$/)
  })
})
