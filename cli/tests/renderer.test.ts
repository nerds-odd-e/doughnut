import { describe, test, expect } from 'vitest'
import {
  CLI_READY_MARKER,
  cliReadyMarkerSuffix,
  truncateToWidth,
  isCommittedInteractiveInput,
  grayDisabledInputBoxLines,
  renderFullDisplay,
  stripAnsi,
  stripAnsiCsiAndCr,
  needsGapBeforeBox,
  buildLiveRegionLines,
} from '../src/renderer.js'
import type { ChatHistory } from '../src/types.js'

describe('cliReadyMarkerSuffix', () => {
  test('emits OSC 133;A when buffer is empty and no interactive fetch wait', () => {
    expect(cliReadyMarkerSuffix('', false)).toBe(CLI_READY_MARKER)
    expect(CLI_READY_MARKER).toBe('\x1b]133;A\x07')
  })

  test('omits marker when buffer has text', () => {
    expect(cliReadyMarkerSuffix('x', false)).toBe('')
    expect(cliReadyMarkerSuffix(' \n', false)).toBe('')
  })

  test('omits marker during interactive fetch wait', () => {
    expect(cliReadyMarkerSuffix('', true)).toBe('')
  })
})

describe('isCommittedInteractiveInput', () => {
  test('false for empty and whitespace-only', () => {
    expect(isCommittedInteractiveInput('')).toBe(false)
    expect(isCommittedInteractiveInput('   ')).toBe(false)
    expect(isCommittedInteractiveInput('\n\t ')).toBe(false)
  })

  test('true when any non-whitespace remains after trim', () => {
    expect(isCommittedInteractiveInput('a')).toBe(true)
    expect(isCommittedInteractiveInput('  x  ')).toBe(true)
    expect(isCommittedInteractiveInput('\nhi')).toBe(true)
  })
})

describe('grayDisabledInputBoxLines', () => {
  test('replaces internal RESET with GREY so right border stays gray', () => {
    const line = '│ \x1b[90mtext\x1b[0m                    │'
    const result = grayDisabledInputBoxLines([line])
    expect(result[0]).toContain('\x1b[90m')
    // biome-ignore lint/suspicious/noControlCharactersInRegex: ANSI RESET, intentional
    expect(/\x1b\[0m\s*│/.test(result[0])).toBe(false)
  })
})

describe('stripAnsiCsiAndCr', () => {
  test('removes SGR, CSI cursor codes, and carriage returns', () => {
    const raw = '\x1b[31mhi\x1b[0m\r\x1b[2A\x1b[1B'
    expect(stripAnsiCsiAndCr(raw)).toBe('hi')
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
