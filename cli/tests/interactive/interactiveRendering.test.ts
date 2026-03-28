/**
 * `renderer.ts`: visible width, command-line draft rows, past user message paint, slash highlight.
 * TTY behavior: `interactiveTty*.test.ts`.
 */
import './interactiveTestMocks.js'
import { describe, test, expect } from 'vitest'
import {
  buildCommandInputDraftLines,
  formatInteractiveCommandLineInkRows,
  highlightRecognizedCommand,
  renderPastUserMessage,
  stripAnsi,
  visibleLength,
  buildSuggestionLinesForInk,
} from '../../src/renderer.js'
import { boldCyan, GREY_BG_PAST_INPUT } from './interactiveTestHelpers.js'

describe('visibleLength', () => {
  test('returns length without ANSI codes', () => {
    expect(visibleLength('hello')).toBe(5)
    expect(visibleLength('\x1b[90mhello\x1b[0m')).toBe(5)
    expect(visibleLength('→ \x1b[90m`exit` to quit.\x1b[0m')).toBe(17)
  })
})

describe('highlightRecognizedCommand', () => {
  test('returns plain text when line does not start with /', () => {
    expect(highlightRecognizedCommand('hello')).toBe('hello')
  })

  test('does not highlight incomplete prefix', () => {
    expect(highlightRecognizedCommand('/he')).toBe('/he')
  })

  test('highlights exact command match', () => {
    const result = highlightRecognizedCommand('/help')
    expect(result).toStrictEqual(boldCyan('/help'))
  })

  test('highlights only command part when param follows', () => {
    const result = highlightRecognizedCommand('/add-access-token x')
    expect(result).toStrictEqual(`${boldCyan('/add-access-token')} x`)
  })

  test('returns plain when no match', () => {
    expect(highlightRecognizedCommand('/unknown')).toBe('/unknown')
  })

  test('does not highlight lone slash', () => {
    expect(highlightRecognizedCommand('/')).toBe('/')
  })
})

describe('buildCommandInputDraftLines', () => {
  test('empty buffer shows placeholder with prompt', () => {
    const lines = buildCommandInputDraftLines('', 40)
    expect(lines).toHaveLength(1)
    expect(lines[0]).toContain('→')
    expect(lines[0]).toContain('`exit` to quit.')
  })

  test('single-line buffer shows prompt + text', () => {
    const lines = buildCommandInputDraftLines('hello', 40)
    expect(lines).toHaveLength(1)
    expect(lines[0]).toBe('→ hello')
  })

  test('command-line draft is a single Ink row (no embedded newlines in buffer)', () => {
    const lines = buildCommandInputDraftLines('line1 line2', 40)
    expect(lines).toHaveLength(1)
    expect(lines[0]).toBe('→ line1 line2')
  })

  test('recognized command gets bold+colored in first line', () => {
    const lines = buildCommandInputDraftLines('/help', 40)
    expect(lines[0]).toContain('→')
    expect(lines[0]).toContain(boldCyan('/help'))
  })

  test('command with param highlights only command part', () => {
    const lines = buildCommandInputDraftLines('/add-access-token mylabel', 40)
    expect(lines[0]).toContain(`${boldCyan('/add-access-token')} mylabel`)
  })

  test('empty buffer in selection mode shows token-list placeholder', () => {
    const lines = buildCommandInputDraftLines('', 80, {
      placeholderContext: 'tokenList',
    })
    expect(lines).toHaveLength(1)
    expect(lines[0]).toContain('↑↓ Enter to select; other keys cancel')
  })

  test('empty buffer with default context shows default placeholder', () => {
    const lines = buildCommandInputDraftLines('', 80, {
      placeholderContext: 'default',
    })
    expect(lines[0]).toContain('`exit` to quit.')
  })

  test.each([
    ['recallMcq', '↑↓ Enter or number to select; Esc to cancel'],
    ['recallStopConfirmation', 'y or n; Esc to go back'],
    ['recallYesNo', 'y or n; /stop to exit recall'],
  ] as const)('placeholderContext %s shows correct placeholder', (ctx, phrase) => {
    const lines = buildCommandInputDraftLines('', 80, {
      placeholderContext: ctx,
    })
    expect(lines[0]).toContain(phrase)
  })

  test('long token-list placeholder truncates to terminal width with ellipsis', () => {
    const width = 25
    const row = formatInteractiveCommandLineInkRows('', width, 0, {
      placeholderContext: 'tokenList',
    })[0]!
    expect(visibleLength(row)).toBeLessThanOrEqual(width)
    expect(stripAnsi(row)).toContain('...')
  })
})

describe('buildSuggestionLinesForInk', () => {
  test('with /rec, returns non-empty completion rows (Ink wraps to terminal width)', () => {
    const lines = buildSuggestionLinesForInk('/rec', 0)
    expect(lines.length).toBeGreaterThan(0)
    expect(lines.some((l) => l.endsWith('...'))).toBe(false)
  })

  test('with /remove-access-token-completely prefix, returns rows without string-level ellipsis', () => {
    const lines = buildSuggestionLinesForInk(
      '/remove-access-token-completely',
      0
    )
    expect(lines.length).toBeGreaterThan(0)
    expect(lines.some((l) => l.includes('...'))).toBe(false)
  })

  test('without slash prefix returns / commands hint (one line)', () => {
    const lines = buildSuggestionLinesForInk('hello', 0)
    expect(lines).toHaveLength(1)
    expect(stripAnsi(lines[0]!)).toContain('/ commands')
  })

  test('with slash prefix but no match returns empty', () => {
    expect(buildSuggestionLinesForInk('/unknown', 0)).toHaveLength(0)
  })

  const sgrCloserEnd = new RegExp(
    `${String.fromCharCode(27)}\\[(?:0|39|49|23|22|24|25|26|27|55|59|53|65)m$`
  )
  test.each([
    ['/list'],
    ['/'],
  ] as const)('Current guidance Ink rows with ANSI end with an SGR closer for buffer %s', (buffer) => {
    for (const line of buildSuggestionLinesForInk(buffer, 0)) {
      if (line.includes('\x1b')) {
        expect(line).toMatch(sgrCloserEnd)
      }
    }
  })
})

describe('renderPastUserMessage', () => {
  test('single-line hello: grey block, padding, trailing blank, no arrow', () => {
    const result = renderPastUserMessage('hello', 30)
    expect(result).toContain('hello')
    expect(result).toContain(GREY_BG_PAST_INPUT)
    const lines = result.split('\n')
    expect(visibleLength(lines[0]!)).toBe(28)
    // biome-ignore lint/suspicious/noControlCharactersInRegex: stripping ANSI escapes
    expect(lines[0]!.replace(/\x1b\[[0-9;]*m/g, '').trim()).toBe('')
    const lastBgLine = lines[lines.length - 2]!
    // biome-ignore lint/suspicious/noControlCharactersInRegex: stripping ANSI escapes
    expect(lastBgLine.replace(/\x1b\[[0-9;]*m/g, '').trim()).toBe('')
    expect(lines[lines.length - 1]).toBe('')
  })

  test('handles multi-line input', () => {
    const result = renderPastUserMessage('line1\nline2', 30)
    expect(result).toContain('line1')
    expect(result).toContain('line2')
    const lines = result.split('\n')
    const bgLines = lines.filter((l) => l.includes(GREY_BG_PAST_INPUT))
    expect(bgLines).toHaveLength(4)
  })
})
