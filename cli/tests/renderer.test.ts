import { describe, test, expect } from 'vitest'
import { INTERACTIVE_FETCH_WAIT_LINES } from '../src/interactiveFetchWait.js'
import {
  INTERACTIVE_INPUT_READY_OSC,
  interactiveInputReadyOscSuffix,
  truncateToWidth,
  isCommittedInteractiveInput,
  applyCliAssistantMessageTone,
  stripAnsi,
  stripAnsiCsiAndCr,
  needsGapBeforeLiveRegion,
  buildSuggestionLinesForInk,
  formatInteractiveCommandLineInkRows,
  formatCurrentStageIndicatorLine,
  CURRENT_STAGE_BAND_BACKGROUND_SGR,
  DEFAULT_RECALL_LOADING_STAGE_INDICATOR,
  greyCurrentStageIndicatorLabel,
  INTERACTIVE_FETCH_WAIT_PROMPT_FG,
  GREY,
  visibleLength,
  wrapTextToVisibleWidthLines,
  terminalColumnsOfPlainGrapheme,
  interactiveFetchWaitStageIndicatorLine,
  RESET,
} from '../src/renderer.js'

describe('interactiveInputReadyOscSuffix', () => {
  const readyPaint = {
    lineDraft: '',
    interactiveFetchWaitLine: null,
  } as const

  test('emits private ready OSC when draft is empty and no interactive fetch wait', () => {
    expect(interactiveInputReadyOscSuffix(readyPaint)).toBe(
      INTERACTIVE_INPUT_READY_OSC
    )
    expect(INTERACTIVE_INPUT_READY_OSC.startsWith('\x1b]900;')).toBe(true)
    expect(INTERACTIVE_INPUT_READY_OSC.endsWith('\x07')).toBe(true)
  })

  test('emits nothing when the user has typed in the command line', () => {
    expect(
      interactiveInputReadyOscSuffix({
        lineDraft: 'x',
        interactiveFetchWaitLine: null,
      })
    ).toBe('')
    expect(
      interactiveInputReadyOscSuffix({
        lineDraft: '  ',
        interactiveFetchWaitLine: null,
      })
    ).toBe('')
  })

  test('emits nothing while interactive fetch wait is shown', () => {
    expect(
      interactiveInputReadyOscSuffix({
        lineDraft: '',
        interactiveFetchWaitLine: INTERACTIVE_FETCH_WAIT_LINES.recallNext,
      })
    ).toBe('')
  })
})

describe('wrapTextToVisibleWidthLines (recall MCQ stem / ANSI terminal strings)', () => {
  test('wraps plain text at visible width with word break when possible', () => {
    expect(wrapTextToVisibleWidthLines('hello world', 5)).toEqual([
      'hello',
      'world',
    ])
  })

  test('preserves ANSI sequences while wrapping at visible column count', () => {
    const boldHi = '\x1b[1mhi\x1b[0m there'
    const lines = wrapTextToVisibleWidthLines(boldHi, 3)
    expect(lines[0]).toContain('\x1b[1m')
    const joined = stripAnsi(lines.join(''))
    expect(joined).toContain('hi')
    expect(joined).toContain('there')
    expect(lines.every((l) => visibleLength(l) <= 3)).toBe(true)
  })

  test('CJK wide characters count as 2 terminal columns each', () => {
    const japanese16 = 'あいうえおかきくけこさしすせそた'
    expect(
      visibleLength(japanese16),
      'Each CJK/hiragana character occupies 2 terminal columns, so 16 chars = 32 columns.'
    ).toBe(32)
    const lines = wrapTextToVisibleWidthLines(japanese16, 30)
    expect(
      lines.length,
      '32 terminal-column text must wrap to ≥2 lines at width 30.'
    ).toBeGreaterThanOrEqual(2)
    expect(
      lines.every((l) => visibleLength(l) <= 30),
      'Every wrapped line must fit within the terminal width.'
    ).toBe(true)
  })

  test('emoji and emoji presentation use 2 terminal columns', () => {
    expect(terminalColumnsOfPlainGrapheme('😀')).toBe(2)
    expect(visibleLength('a😀b')).toBe(4)
    expect(visibleLength('❤️')).toBe(2)
  })

  test('flag regional-indicator pair is 2 columns', () => {
    expect(terminalColumnsOfPlainGrapheme('🇯🇵')).toBe(2)
  })

  test('ZWJ emoji sequence counts as 2 columns', () => {
    expect(terminalColumnsOfPlainGrapheme('👨‍👩‍👧')).toBe(2)
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

describe('stripAnsiCsiAndCr', () => {
  test('removes SGR, CSI cursor codes, and carriage returns', () => {
    const raw = '\x1b[31mhi\x1b[0m\r\x1b[2A\x1b[1B'
    expect(stripAnsiCsiAndCr(raw)).toBe('hi')
  })
})

describe('needsGapBeforeLiveRegion', () => {
  test('returns true only when past messages are non-empty and no current prompt', () => {
    expect(needsGapBeforeLiveRegion([], [], [])).toBe(false)
    expect(needsGapBeforeLiveRegion([], ['line'], [])).toBe(false)
    expect(
      needsGapBeforeLiveRegion([{ role: 'user', content: 'x' }], [], [])
    ).toBe(true)
    expect(
      needsGapBeforeLiveRegion([{ role: 'user', content: 'x' }], ['line'], [])
    ).toBe(false)
  })

  test('false when Current Stage Indicator is present but wrapped prompt is empty', () => {
    expect(
      needsGapBeforeLiveRegion(
        [{ role: 'user', content: 'x' }],
        [],
        [
          interactiveFetchWaitStageIndicatorLine(
            INTERACTIVE_FETCH_WAIT_LINES.recallNext
          ),
        ]
      )
    ).toBe(false)
  })
})

describe('buildSuggestionLinesForInk', () => {
  test('slash completion returns rows without per-line ellipsis (Ink wraps)', () => {
    const ink = buildSuggestionLinesForInk('/h', 0)
    expect(ink.length).toBeGreaterThan(0)
    expect(ink.some((l) => stripAnsi(l).includes('...'))).toBe(false)
  })

  test('many completions: full rows, no truncation marker in strings', () => {
    const ink = buildSuggestionLinesForInk('/', 0)
    expect(ink.length).toBeGreaterThan(1)
    expect(ink.some((l) => stripAnsi(l).includes('...'))).toBe(false)
  })

  test('non-slash buffer yields single / commands hint line', () => {
    const ink = buildSuggestionLinesForInk('hello', 0)
    expect(ink).toHaveLength(1)
    expect(stripAnsi(ink[0]!)).toContain('/ commands')
  })

  test('unknown slash prefix returns empty', () => {
    expect(buildSuggestionLinesForInk('/unknown', 0)).toEqual([])
  })
})

describe('formatInteractiveCommandLineInkRows', () => {
  test('default: one row with prompt, caret, and placeholder', () => {
    const rows = formatInteractiveCommandLineInkRows('', 80, 0, {
      placeholderContext: 'default',
    })
    expect(rows).toHaveLength(1)
    expect(rows[0]).toContain('\x1b[7m')
    expect(rows[0]).toMatch(/^→ /)
    expect(stripAnsi(rows[0])).toContain('`exit` to quit.')
  })

  test('fetch-wait: grey row with loading placeholder', () => {
    const rows = formatInteractiveCommandLineInkRows('', 44, 0, {
      placeholderContext: 'interactiveFetchWait',
    })
    expect(rows[0]).toContain(GREY)
    expect(stripAnsi(rows[0])).toContain('loading')
  })

  test('caret at end of line: trailing inverse space (ink-ui TextInput style)', () => {
    const rows = formatInteractiveCommandLineInkRows('ab', 80, 2)
    expect(rows[0]).toContain('→ ab')
    expect(rows[0]).toContain('\x1b[7m \x1b[27m')
  })

  test('CJK: caret inverts full grapheme', () => {
    const rows = formatInteractiveCommandLineInkRows('aあb', 80, 1)
    expect(rows[0]).toContain('\x1b[7mあ\x1b[27m')
  })
})

describe('Current Stage Indicator (band lines for Ink)', () => {
  test('recall loading indicator is full-width band', () => {
    const width = 40
    const line = formatCurrentStageIndicatorLine(
      DEFAULT_RECALL_LOADING_STAGE_INDICATOR,
      width
    )
    expect(line).toContain(CURRENT_STAGE_BAND_BACKGROUND_SGR)
    expect(stripAnsi(line)).toMatch(/^Recalling +$/)
    expect(visibleLength(stripAnsi(line))).toBe(width)
  })

  test('fetch-wait indicator carries blue label and band padding', () => {
    const width = 44
    const base = INTERACTIVE_FETCH_WAIT_LINES.recallNext
    const label = interactiveFetchWaitStageIndicatorLine(base)
    const line = formatCurrentStageIndicatorLine(label, width)
    expect(line).toContain(CURRENT_STAGE_BAND_BACKGROUND_SGR)
    expect(line).toContain(INTERACTIVE_FETCH_WAIT_PROMPT_FG)
    expect(stripAnsi(line)).toMatch(new RegExp(`^${base}\\s+$`))
    expect(visibleLength(stripAnsi(line))).toBe(width)
  })

  test('access-token stage label + wrapped instruction (grey prompt block)', () => {
    const width = 48
    const instruction = 'Select and enter to change the default access token'
    const stage = formatCurrentStageIndicatorLine(
      greyCurrentStageIndicatorLabel('Access tokens'),
      width
    )
    const wrapped = wrapTextToVisibleWidthLines(instruction, width).map(
      (l) => `${GREY}${l}${RESET}`
    )
    expect(stripAnsi(stage)).toMatch(/^Access tokens +$/)
    expect(wrapped.map(stripAnsi).join('\n')).toContain('Select and enter')
  })
})

describe('applyCliAssistantMessageTone', () => {
  test('plain leaves text unchanged', () => {
    expect(applyCliAssistantMessageTone('ok', 'plain')).toBe('ok')
  })

  test('error uses red', () => {
    expect(applyCliAssistantMessageTone('Network down', 'error')).toContain(
      'Network down'
    )
    expect(applyCliAssistantMessageTone('Network down', 'error')).toContain(
      '\x1b[31m'
    )
  })

  test('userNotice uses grey italic', () => {
    const line = applyCliAssistantMessageTone('Cancelled.', 'userNotice')
    expect(line).toContain('Cancelled.')
    expect(line).toContain('\x1b[90m')
    expect(line).toContain('\x1b[3m')
  })
})

describe('truncateToWidth', () => {
  test('truncated line: ellipsis inherits style, ends with RESET to avoid state bleed', () => {
    const result = truncateToWidth('\x1b[7mhello world\x1b[0m', 8)
    // biome-ignore lint/suspicious/noControlCharactersInRegex: ANSI RESET, intentional
    expect(result).toMatch(/\.\.\.\x1b\[0m$/)
  })
})
