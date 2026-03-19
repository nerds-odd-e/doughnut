import { describe, test, expect } from 'vitest'
import { truncateToWidth } from '../src/renderer.js'

describe('truncateToWidth', () => {
  test('when truncating ANSI-decorated string, result ends with RESET before "..." to avoid state bleed', () => {
    const result = truncateToWidth('\x1b[7mhello world\x1b[0m', 8)
    // biome-ignore lint/suspicious/noControlCharactersInRegex: ANSI RESET escape, intentional
    expect(result).toMatch(/\.\.\.\x1b\[0m$/)
  })
})
