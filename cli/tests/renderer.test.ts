import { describe, test, expect } from 'vitest'
import {
  truncateToWidth,
  isSelectionMode,
  grayBoxLinesForSelectionMode,
  renderFullDisplay,
  stripAnsi,
  needsGapBeforeBox,
  buildLiveRegionLines,
} from '../src/renderer.js'
import type { ChatHistory } from '../src/types.js'

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

describe('needsGapBeforeBox', () => {
  test('returns true only when history is non-empty and no current prompt', () => {
    expect(needsGapBeforeBox([], [])).toBe(false)
    expect(needsGapBeforeBox([], ['line'])).toBe(false)
    expect(needsGapBeforeBox([{ type: 'input', content: 'x' }], [])).toBe(true)
    expect(needsGapBeforeBox([{ type: 'input', content: 'x' }], ['line'])).toBe(
      false
    )
  })
})

describe('buildLiveRegionLines', () => {
  test('prompt-present: includes separator and grey prompt lines before box', () => {
    const lines = buildLiveRegionLines(
      '',
      80,
      ['Select token', 'wrapped'],
      [],
      []
    )
    const boxTopIndex = lines.findIndex((l) => stripAnsi(l).startsWith('┌'))
    expect(boxTopIndex).toBeGreaterThan(0)
    expect(stripAnsi(lines[0])).toMatch(/^─+$/)
    expect(stripAnsi(lines[1])).toBe('Select token')
    expect(stripAnsi(lines[2])).toBe('wrapped')
    expect(boxTopIndex).toBe(3)
  })

  test('prompt-absent: box first, no separator', () => {
    const lines = buildLiveRegionLines('', 80, [], [], [])
    expect(stripAnsi(lines[0])).toMatch(/^┌.*┐$/)
  })
})

describe('renderFullDisplay', () => {
  test('has one empty line between history output and input box', () => {
    const history: ChatHistory = [
      { type: 'input', content: '/help' },
      { type: 'output', lines: ['Available commands...'] },
    ]
    const lines = renderFullDisplay(history, '', 80, [], [])
    const boxTopIndex = lines.findIndex((l) => stripAnsi(l).startsWith('┌'))
    expect(boxTopIndex).toBeGreaterThan(0)
    expect(stripAnsi(lines[boxTopIndex - 1])).toBe('')
  })
})

describe('truncateToWidth', () => {
  test('truncated line: ellipsis inherits style, ends with RESET to avoid state bleed', () => {
    const result = truncateToWidth('\x1b[7mhello world\x1b[0m', 8)
    // biome-ignore lint/suspicious/noControlCharactersInRegex: ANSI RESET, intentional
    expect(result).toMatch(/\.\.\.\x1b\[0m$/)
  })
})
