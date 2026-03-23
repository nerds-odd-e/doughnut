import { describe, test, expect } from 'vitest'
import { INTERACTIVE_FETCH_WAIT_LINES } from '../src/interactiveFetchWait.js'
import {
  INTERACTIVE_INPUT_READY_OSC,
  interactiveInputReadyOscSuffix,
  truncateToWidth,
  isCommittedInteractiveInput,
  grayDisabledInputBoxLines,
  applyChatHistoryOutputTone,
  renderFullDisplay,
  stripAnsi,
  stripAnsiCsiAndCr,
  needsGapBeforeBox,
  buildLiveRegionLines,
  CURRENT_STAGE_BAND_BACKGROUND_SGR,
  DEFAULT_RECALL_LOADING_STAGE_INDICATOR,
  greyCurrentStageIndicatorLabel,
  INTERACTIVE_FETCH_WAIT_PROMPT_FG,
  visibleLength,
  wrapTextToLines,
  wrapTextToVisibleWidthLines,
  terminalColumnsOfPlainGrapheme,
  interactiveFetchWaitStageIndicatorLine,
  RESET,
} from '../src/renderer.js'
import type { ChatHistory } from '../src/types.js'

describe('interactiveInputReadyOscSuffix', () => {
  const readyPaint = {
    lineDraft: '',
    interactiveFetchWaitLine: null,
  } as const

  test('emits private ready OSC when draft is empty and no interactive fetch wait', () => {
    expect(interactiveInputReadyOscSuffix(readyPaint)).toBe(
      INTERACTIVE_INPUT_READY_OSC
    )
    expect(INTERACTIVE_INPUT_READY_OSC).toBe(
      '\x1b]900;doughnut-interactive-input-ready\x07'
    )
  })

  test('emits nothing when the user has typed in the input box', () => {
    expect(
      interactiveInputReadyOscSuffix({
        lineDraft: 'x',
        interactiveFetchWaitLine: null,
      })
    ).toBe('')
    expect(
      interactiveInputReadyOscSuffix({
        lineDraft: ' \n',
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
    // 'あ' through 'た' = 16 hiragana characters × 2 terminal columns = 32 cols > 30
    // Real terminals render each as 2 columns; the stem auto-wraps on screen.
    // If wide chars are counted as 1, currentPromptWrappedLines is 1 line short,
    // cursorUpStepsToLiveRegionTop is too small, and ↓ leaves the old separator
    // on screen above the repainted live region — appearing duplicated.
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
    expect(needsGapBeforeBox([], [], [])).toBe(false)
    expect(needsGapBeforeBox([], ['line'], [])).toBe(false)
    expect(needsGapBeforeBox([{ type: 'input', content: 'x' }], [], [])).toBe(
      true
    )
    expect(
      needsGapBeforeBox([{ type: 'input', content: 'x' }], ['line'], [])
    ).toBe(false)
  })

  test('false when Current Stage Indicator is present but wrapped prompt is empty', () => {
    expect(
      needsGapBeforeBox(
        [{ type: 'input', content: 'x' }],
        [],
        [`${INTERACTIVE_FETCH_WAIT_PROMPT_FG}Loading${RESET}`]
      )
    ).toBe(false)
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

  test('recall stage: Current Stage Indicator and banded separator above box, not below', () => {
    const width = 40
    const lines = buildLiveRegionLines(
      '',
      width,
      [],
      [],
      [DEFAULT_RECALL_LOADING_STAGE_INDICATOR]
    )
    const boxTopIndex = lines.findIndex((l) => stripAnsi(l).startsWith('┌'))
    expect(boxTopIndex).toBeGreaterThan(1)
    expect(lines[0]).toContain(CURRENT_STAGE_BAND_BACKGROUND_SGR)
    expect(stripAnsi(lines[0])).toMatch(/^Recalling +$/)
    expect(visibleLength(stripAnsi(lines[0]))).toBe(width)
    expect(lines[1]).toContain(CURRENT_STAGE_BAND_BACKGROUND_SGR)
    expect(stripAnsi(lines[1])).toBe('─'.repeat(width))
    expect(boxTopIndex).toBe(2)
    const recallBelowBox = lines.slice(boxTopIndex).join('\n')
    expect(recallBelowBox).not.toContain('Recalling')
  })

  test('recall stage + MCQ prompt: indicator, banded separator, stem lines, then box', () => {
    const width = 50
    const lines = buildLiveRegionLines(
      '',
      width,
      ['Notebook: X', 'Stem?'],
      [],
      [DEFAULT_RECALL_LOADING_STAGE_INDICATOR]
    )
    const boxTopIndex = lines.findIndex((l) => stripAnsi(l).startsWith('┌'))
    expect(stripAnsi(lines[0])).toMatch(/^Recalling +$/)
    expect(stripAnsi(lines[1])).toBe('─'.repeat(width))
    expect(stripAnsi(lines[2])).toBe('Notebook: X')
    expect(stripAnsi(lines[3])).toBe('Stem?')
    expect(boxTopIndex).toBe(4)
  })

  test('interactive fetch wait: banded Current Stage Indicator + separator, then box', () => {
    const width = 44
    const base = INTERACTIVE_FETCH_WAIT_LINES.recallNext
    const label = interactiveFetchWaitStageIndicatorLine(base, 0)
    const lines = buildLiveRegionLines('', width, [], [], [label], {
      placeholderContext: 'interactiveFetchWait',
    })
    const boxTopIndex = lines.findIndex((l) => stripAnsi(l).startsWith('┌'))
    expect(boxTopIndex).toBe(2)
    expect(lines[0]).toContain(CURRENT_STAGE_BAND_BACKGROUND_SGR)
    expect(lines[0]).toContain(INTERACTIVE_FETCH_WAIT_PROMPT_FG)
    expect(stripAnsi(lines[0])).toMatch(new RegExp(`^${base}\\.\\s+$`))
    expect(visibleLength(stripAnsi(lines[0]))).toBe(width)
    expect(lines[1]).toContain(CURRENT_STAGE_BAND_BACKGROUND_SGR)
    expect(stripAnsi(lines[1])).toBe('─'.repeat(width))
  })

  test('access token list: grey stage indicator, banded separator, wrapped instruction, then box', () => {
    const width = 48
    const instruction = 'Select and enter to change the default access token'
    const lines = buildLiveRegionLines(
      '',
      width,
      wrapTextToLines(instruction, width),
      [],
      [greyCurrentStageIndicatorLabel('Access tokens')],
      { placeholderContext: 'tokenList' }
    )
    const boxTopIndex = lines.findIndex((l) => stripAnsi(l).startsWith('┌'))
    expect(stripAnsi(lines[0])).toMatch(/^Access tokens +$/)
    expect(stripAnsi(lines[1])).toBe('─'.repeat(width))
    expect(boxTopIndex).toBeGreaterThan(2)
    expect(
      lines
        .slice(0, boxTopIndex)
        .map((l) => stripAnsi(l))
        .join('\n')
    ).toContain('Select and enter')
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

  test('styles error and userNotice history output lines', () => {
    const history: ChatHistory = [
      { type: 'output', lines: ['Network down'], tone: 'error' },
      { type: 'output', lines: ['Cancelled by user.'], tone: 'userNotice' },
    ]
    const lines = renderFullDisplay(history, '', 80, [], [])
    const errorLine = lines.find((l) => l.includes('Network down'))
    const noticeLine = lines.find((l) => l.includes('Cancelled by user.'))
    expect(errorLine).toContain('\x1b[31m')
    expect(noticeLine).toContain('\x1b[90m')
    expect(noticeLine).toContain('\x1b[3m')
  })

  test('omitLiveRegion skips live region (box and guidance)', () => {
    const history: ChatHistory = [{ type: 'input', content: '/help' }]
    const lines = renderFullDisplay(
      history,
      '',
      80,
      ['guidance only'],
      [],
      [],
      {
        omitLiveRegion: true,
      }
    )
    expect(lines.some((l) => stripAnsi(l).startsWith('┌'))).toBe(false)
    expect(lines.join('\n')).not.toContain('guidance only')
  })
})

describe('applyChatHistoryOutputTone', () => {
  test('plain leaves text unchanged', () => {
    expect(applyChatHistoryOutputTone('ok', 'plain')).toBe('ok')
  })
})

describe('truncateToWidth', () => {
  test('truncated line: ellipsis inherits style, ends with RESET to avoid state bleed', () => {
    const result = truncateToWidth('\x1b[7mhello world\x1b[0m', 8)
    // biome-ignore lint/suspicious/noControlCharactersInRegex: ANSI RESET, intentional
    expect(result).toMatch(/\.\.\.\x1b\[0m$/)
  })
})
