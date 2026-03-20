import './interactiveTestMocks.js'
import { describe, test, expect } from 'vitest'
import {
  buildBoxLines,
  highlightRecognizedCommand,
  renderBox,
  renderPastInput,
  visibleLength,
} from '../../src/interactive.js'
import { buildSuggestionLines } from '../../src/renderer.js'
import {
  ANSI_RESET,
  BOLD_CYAN,
  GREY_BG_PAST_INPUT,
} from './interactiveTestHelpers.js'

describe('visibleLength', () => {
  test('returns length without ANSI codes', () => {
    expect(visibleLength('hello')).toBe(5)
    expect(visibleLength('\x1b[90mhello\x1b[0m')).toBe(5)
    expect(visibleLength('→ \x1b[90m`exit` to quit.\x1b[0m')).toBe(17)
  })
})

describe('renderBox', () => {
  test.each([
    { width: 100, check: 'top' as const },
    { width: 120, check: 'content' as const },
  ])('box respects width $width ($check row)', ({ width, check }) => {
    const result = renderBox(['hi'], width)
    const line = result.split('\n')[check === 'top' ? 0 : 1]
    if (check === 'top') expect(line.length).toBe(width)
    else expect(visibleLength(line)).toBe(width)
  })

  test('renders a single-line box', () => {
    const result = renderBox(['hello'], 20)
    const lines = result.split('\n')
    expect(lines).toHaveLength(3)
    expect(lines[0]).toBe('┌──────────────────┐')
    expect(lines[1]).toContain('hello')
    expect(lines[1]).toMatch(/^│.*│$/)
    expect(lines[2]).toBe('└──────────────────┘')
  })

  test('renders a multi-line box (box expands with newlines)', () => {
    const result = renderBox(['line 1', 'line 2', 'line 3'], 20)
    const lines = result.split('\n')
    expect(lines).toHaveLength(5)
    expect(lines[0]).toBe('┌──────────────────┐')
    expect(lines[1]).toContain('line 1')
    expect(lines[2]).toContain('line 2')
    expect(lines[3]).toContain('line 3')
    expect(lines[4]).toBe('└──────────────────┘')
  })

  test('pads short lines to fill the box width', () => {
    const result = renderBox(['hi'], 20)
    const lines = result.split('\n')
    expect(lines[1]).toBe('│ hi               │')
    expect(lines[1].length).toBe(20)
  })

  test('pads correctly when line contains ANSI codes', () => {
    const grey = '\x1b[90m'
    const reset = '\x1b[0m'
    const result = renderBox([`${grey}hi${reset}`], 20)
    const lines = result.split('\n')
    expect(visibleLength(lines[1])).toBe(20)
    expect(lines[1]).toContain('hi')
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
    expect(result).toStrictEqual(`${BOLD_CYAN}/help${ANSI_RESET}`)
  })

  test('highlights only command part when param follows', () => {
    const result = highlightRecognizedCommand('/add-access-token x')
    expect(result).toStrictEqual(`${BOLD_CYAN}/add-access-token${ANSI_RESET} x`)
  })

  test('returns plain when no match', () => {
    expect(highlightRecognizedCommand('/unknown')).toBe('/unknown')
  })

  test('does not highlight lone slash', () => {
    expect(highlightRecognizedCommand('/')).toBe('/')
  })
})

describe('buildBoxLines', () => {
  test('empty buffer shows placeholder with prompt', () => {
    const lines = buildBoxLines('', 40)
    expect(lines).toHaveLength(1)
    expect(lines[0]).toContain('→')
    expect(lines[0]).toContain('`exit` to quit.')
  })

  test('single-line buffer shows prompt + text', () => {
    const lines = buildBoxLines('hello', 40)
    expect(lines).toHaveLength(1)
    expect(lines[0]).toBe('→ hello')
  })

  test('multi-line buffer produces one line per newline', () => {
    const lines = buildBoxLines('line1\nline2\nline3', 40)
    expect(lines).toHaveLength(3)
    expect(lines[0]).toBe('→ line1')
    expect(lines[1]).toBe('  line2')
    expect(lines[2]).toBe('  line3')
  })

  test('recognized command gets bold+colored in first line', () => {
    const lines = buildBoxLines('/help', 40)
    expect(lines[0]).toContain('→')
    expect(lines[0]).toContain(`${BOLD_CYAN}/help${ANSI_RESET}`)
  })

  test('non-command line has no ANSI highlight', () => {
    const lines = buildBoxLines('hello', 40)
    // biome-ignore lint/suspicious/noControlCharactersInRegex: checking ANSI codes
    expect(lines[0]).not.toMatch(/\x1b\[1;36m/)
  })

  test('command with param highlights only command part', () => {
    const lines = buildBoxLines('/add-access-token mylabel', 40)
    expect(lines[0]).toContain(
      `${BOLD_CYAN}/add-access-token${ANSI_RESET} mylabel`
    )
  })

  test('empty buffer in selection mode shows placeholder without arrow', () => {
    const lines = buildBoxLines('', 80, { placeholderContext: 'tokenList' })
    expect(lines).toHaveLength(1)
    expect(lines[0]).not.toContain('→')
    expect(lines[0]).toContain('↑↓ Enter to select; other keys cancel')
  })

  test('empty buffer with default context shows default placeholder', () => {
    const lines = buildBoxLines('', 80, { placeholderContext: 'default' })
    expect(lines[0]).toContain('`exit` to quit.')
  })

  test.each([
    ['recallMcq', '↑↓ Enter or number to select; Esc to cancel'],
    ['recallStopConfirmation', 'y or n; Esc to go back'],
    ['recallYesNo', 'y or n; /stop to exit recall'],
    ['recallSpelling', 'type your answer; /stop to exit recall'],
  ] as const)('placeholderContext %s shows correct placeholder', (ctx, phrase) => {
    const lines = buildBoxLines('', 80, { placeholderContext: ctx })
    expect(lines[0]).toContain(phrase)
  })

  test('placeholder truncates in narrow window for any long context', () => {
    const width = 25
    const lines = buildBoxLines('', width, { placeholderContext: 'tokenList' })
    const box = renderBox(lines, width)
    const boxLines = box.split('\n')
    for (let i = 1; i < boxLines.length - 1; i++) {
      expect(visibleLength(boxLines[i])).toBeLessThanOrEqual(width)
    }
    expect(lines[0]).toContain('↑↓')
    expect(boxLines[1]).toContain('...')
  })
})

describe('buildSuggestionLines', () => {
  test('with /rec and narrow width (40), each line has visible length ≤ width or ends with "..."', () => {
    const lines = buildSuggestionLines('/rec', 0, 40)
    expect(lines.length).toBeGreaterThan(0)
    for (const line of lines) {
      expect(visibleLength(line)).toBeLessThanOrEqual(40)
      if (visibleLength(line) === 40) {
        expect(line).toContain('...')
      }
    }
  })

  test('with /rec and wide width (120), no truncation', () => {
    const lines = buildSuggestionLines('/rec', 0, 120)
    expect(lines.length).toBeGreaterThan(0)
    expect(lines.some((l) => l.endsWith('...'))).toBe(false)
  })

  test('with /recall-status prefix, narrow width truncates long descriptions', () => {
    const lines = buildSuggestionLines('/recall-status', 0, 30)
    expect(lines.length).toBeGreaterThan(0)
    for (const line of lines) {
      expect(visibleLength(line)).toBeLessThanOrEqual(30)
    }
  })

  test('without slash prefix returns / commands hint, truncated when narrow', () => {
    const lines = buildSuggestionLines('hello', 0, 5)
    expect(lines).toHaveLength(1)
    expect(visibleLength(lines[0])).toBeLessThanOrEqual(5)
    expect(lines[0]).toContain('...')
  })

  test('with slash prefix but no match returns empty', () => {
    const lines = buildSuggestionLines('/unknown', 0, 80)
    expect(lines).toHaveLength(0)
  })

  test.each([
    ['/list', 25],
    ['/list', 30],
    ['/', 25],
    ['/', 30],
  ] as const)('Current guidance lines with ANSI end with RESET for buffer %s width %s', (buffer, width) => {
    for (const line of buildSuggestionLines(buffer, 0, width)) {
      if (line.includes('\x1b')) {
        // biome-ignore lint/suspicious/noControlCharactersInRegex: ANSI RESET escape, intentional
        expect(line).toMatch(/\x1b\[0m$/)
      }
    }
  })
})

describe('renderPastInput', () => {
  test('single-line hello: grey block, no border, padding, trailing blank, no arrow', () => {
    const result = renderPastInput('hello', 30)
    expect(result).not.toContain('┌')
    expect(result).not.toContain('│')
    expect(result).toContain('hello')
    expect(result).toContain(GREY_BG_PAST_INPUT)
    expect(result).not.toContain('→')
    const lines = result.split('\n')
    expect(visibleLength(lines[0])).toBe(28)
    // biome-ignore lint/suspicious/noControlCharactersInRegex: stripping ANSI escapes
    expect(lines[0].replace(/\x1b\[[0-9;]*m/g, '').trim()).toBe('')
    const lastBgLine = lines[lines.length - 2]
    // biome-ignore lint/suspicious/noControlCharactersInRegex: stripping ANSI escapes
    expect(lastBgLine.replace(/\x1b\[[0-9;]*m/g, '').trim()).toBe('')
    expect(lines[lines.length - 1]).toBe('')
  })

  test('handles multi-line input', () => {
    const result = renderPastInput('line1\nline2', 30)
    expect(result).toContain('line1')
    expect(result).toContain('line2')
    const lines = result.split('\n')
    const bgLines = lines.filter((l) => l.includes(GREY_BG_PAST_INPUT))
    expect(bgLines).toHaveLength(4)
  })
})
