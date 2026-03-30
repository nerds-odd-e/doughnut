import { describe, expect, test } from 'vitest'
import { numberedMcqMarkdownLinesForTerminal } from '../src/commands/recall/numberedMcqMarkdownLines.js'
import { numberedTerminalListLines } from '../src/terminalColumns.js'
import { stripAnsi } from './inkTestHelpers.js'

describe('numberedMcqMarkdownLinesForTerminal', () => {
  test('wraps long plain-text choice to multiple rows (no single-line truncate)', () => {
    const width = 32
    const longChoice =
      'one two three four five six seven eight nine ten eleven twelve'
    const lines = numberedMcqMarkdownLinesForTerminal([longChoice], width)
    const plain = lines.map((l) => ({
      itemIndex: l.itemIndex,
      text: stripAnsi(l.text),
    }))
    const forFirst = plain.filter((l) => l.itemIndex === 0)
    expect(forFirst.length).toBeGreaterThan(1)
    expect(forFirst[0]!.text).toMatch(/^\d+\.\s/)
    expect(forFirst.slice(1).every((l) => l.text.startsWith('   '))).toBe(true)
  })

  test('MCQ lines do not use ellipsis truncation from terminalColumns', () => {
    const width = 28
    const longChoice = 'word '.repeat(12).trim()
    const lines = numberedMcqMarkdownLinesForTerminal([longChoice], width)
    for (const l of lines) {
      expect(stripAnsi(l.text)).not.toContain('…')
    }
  })

  test('contrast: numberedTerminalListLines truncates same payload to one line with ellipsis', () => {
    const width = 28
    const longChoice = 'word '.repeat(12).trim()
    const tokenLines = numberedTerminalListLines([longChoice], width)
    expect(tokenLines).toHaveLength(1)
    expect(stripAnsi(tokenLines[0]!.text)).toContain('…')
  })
})
