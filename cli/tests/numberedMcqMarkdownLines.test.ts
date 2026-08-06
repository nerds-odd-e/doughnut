import { describe, expect, test } from 'vitest'
import { numberedMcqMarkdownLinesForTerminal } from '../src/commands/recall/numberedMcqMarkdownLines.js'
import { numberedTerminalListLines } from '../src/terminalColumns.js'
import { stripAnsi } from './inkTestHelpers.js'

describe('numberedMcqMarkdownLinesForTerminal', () => {
  test('wraps long plain-text choice to multiple rows without ellipsis', () => {
    const width = 32
    const longChoice =
      'one two three four five six seven eight nine ten eleven twelve'
    const lines = numberedMcqMarkdownLinesForTerminal([longChoice], width)
    const forFirst = lines
      .filter((l) => l.itemIndex === 0)
      .map((l) => stripAnsi(l.text))
    expect(forFirst.length).toBeGreaterThan(1)
    expect(forFirst[0]).toMatch(/^\d+\.\s/)
    expect(forFirst.slice(1).every((l) => l.startsWith('   '))).toBe(true)
    expect(forFirst.every((l) => !l.includes('…'))).toBe(true)
  })

  test('contrast: numberedTerminalListLines truncates same payload to one ellipsis line', () => {
    const width = 28
    const longChoice = 'word '.repeat(12).trim()
    const tokenLines = numberedTerminalListLines([longChoice], width)
    expect(tokenLines).toHaveLength(1)
    expect(stripAnsi(tokenLines[0]!.text)).toContain('…')
  })
})
